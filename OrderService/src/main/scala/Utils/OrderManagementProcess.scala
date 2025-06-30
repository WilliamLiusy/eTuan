package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Objects.OrderService.OrderStatus
import Objects.OrderService.OrderInfo
import Objects.ProductService.ProductInfo
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import io.circe.Json
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.PlanContext
import Common.Object.{ParameterList, SqlParameter}

case object OrderManagementProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def queryOrdersByUserID(userID: String)(using PlanContext): IO[List[OrderInfo]] = {
    // Step 1: Validate input parameter
    if (userID.isEmpty) {
      IO.raiseError(new IllegalArgumentException("userID cannot be empty"))
    } else {
      val querySql = 
        s"""
  SELECT order_id, customer_id, merchant_id, rider_id, product_list, destination_address, order_status, order_time
  FROM ${schemaName}.order_table
  WHERE customer_id = ? OR merchant_id = ? OR rider_id = ?;
  """.stripMargin
  
      val queryParams = List(
        SqlParameter("String", userID),
        SqlParameter("String", userID),
        SqlParameter("String", userID)
      )
  
      for {
        // Log the start of the query operation
        _ <- IO(logger.info(s"Querying orders for userID: ${userID}"))
  
        // Fetch rows from the database
        rows <- readDBRows(querySql, queryParams)
  
        // Log number of rows fetched
        _ <- IO(logger.info(s"Fetched ${rows.size} rows from the database"))
  
        // Map the results to List[OrderInfo]
        orders <- IO {
          rows.map { row =>
            val orderID = decodeField[String](row, "order_id")
            val customerID = decodeField[String](row, "customer_id")
            val merchantID = decodeField[String](row, "merchant_id")
            val riderID = decodeField[Option[String]](row, "rider_id")
            val productList = decodeField[List[ProductInfo]](row, "product_list")
            val destinationAddress = decodeField[String](row, "destination_address")
            val orderStatusStr = decodeField[String](row, "order_status")
            val orderStatus = OrderStatus.fromString(orderStatusStr)
            val orderTimeMillis = decodeField[Long](row, "order_time")
            val orderTime = new DateTime(orderTimeMillis)
  
            // Construct and return the OrderInfo object
            OrderInfo(
              orderID = orderID,
              customerID = customerID,
              merchantID = merchantID,
              riderID = riderID,
              productList = productList,
              destinationAddress = destinationAddress,
              orderStatus = orderStatus,
              orderTime = orderTime
            )
          }
        }
  
        // Log the number of orders mapped
        _ <- IO(logger.info(s"Successfully mapped ${orders.size} orders for userID: ${userID}"))
      } yield orders
    }
  }
  
  def updateOrderStatus(orderID: String, newStatus: OrderStatus)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    for {
      // Step 1: Validate input parameters
      _ <- IO {
        logger.info(s"Validating input parameters: orderID=${orderID}, newStatus=${newStatus}")
      }
      _ <- if (orderID.isEmpty) {
        IO.raiseError(new IllegalArgumentException("orderID cannot be empty"))
      } else {
        IO.unit
      }
      _ <- if (newStatus == null) {
        IO.raiseError(new IllegalArgumentException("newStatus cannot be null"))
      } else {
        IO.unit
      }
  
      // Step 2: Query the OrderTable to verify if the order exists
      _ <- IO(logger.info(s"Checking if order with orderID=${orderID} exists in the database"))
      querySql <- IO {
        s"""
  SELECT order_status
  FROM ${schemaName}.order_table
  WHERE order_id = ?
           """.stripMargin
      }
      orderOpt <- readDBJsonOptional(querySql, List(SqlParameter("String", orderID)))
      _ <- IO(orderOpt match {
        case None =>
          logger.error(s"Order with orderID=${orderID} not found in the database")
          throw new NoSuchElementException(s"Order with orderID=${orderID} not found")
        case Some(_) =>
          logger.info(s"Order with orderID=${orderID} found in the database")
      })
  
      // Step 3: Update the order status in the database
      _ <- IO(logger.info(s"Updating order status for orderID=${orderID} to newStatus=${newStatus}"))
      updateSql <- IO {
        s"""
  UPDATE ${schemaName}.order_table
  SET order_status = ?
  WHERE order_id = ?
           """.stripMargin
      }
      updateParams <- IO {
        List(
          SqlParameter("String", newStatus.toString),
          SqlParameter("String", orderID)
        )
      }
      updateResult <- writeDB(updateSql, updateParams)
  
      // Step 4: Log the update result and return success
      _ <- IO(logger.info(s"Order status update result: ${updateResult}"))
    } yield "Success"
  }
  
  def createOrderRecord(orderInfo: OrderInfo)(using PlanContext): IO[String] = {
    // Logger initialization
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    // Step 1: Validate input parameters
    IO(logger.info(s"Validating the input parameter: orderInfo = ${orderInfo}")) >>
      IO {
        if (orderInfo.customerID.isEmpty || 
          orderInfo.merchantID.isEmpty || 
          orderInfo.destinationAddress.isEmpty || 
          orderInfo.productList.isEmpty
        ) {
          throw new IllegalArgumentException("Invalid orderInfo input: missing necessary fields.")
        }
        if (orderInfo.productList.exists(p => p.productID.isEmpty || 
                                              p.merchantID.isEmpty || 
                                              p.name.isEmpty || 
                                              p.price <= 0
        )) {
          throw new IllegalArgumentException("Invalid product in productList: missing fields or invalid price.")
        }
      } >>
      // Step 2: Generate a unique order ID
      IO {
        val orderID = java.util.UUID.randomUUID().toString
        logger.info(s"Generated unique order ID: ${orderID}")
        orderID
      }.flatMap { orderID =>
        // Step 3: Construct and write order information to OrderTable
        val sql =
          s"""
  INSERT INTO ${schemaName}.order_table 
  (order_id, customer_id, merchant_id, rider_id, product_list, destination_address, order_status, order_time) 
  VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  """.stripMargin
  
        val parameters = List(
          SqlParameter("String", orderID),
          SqlParameter("String", orderInfo.customerID),
          SqlParameter("String", orderInfo.merchantID),
          SqlParameter("String", orderInfo.riderID.getOrElse("")), // Option field
          SqlParameter("String", orderInfo.productList.asJson.noSpaces), // Convert List[ProductInfo] to JSON
          SqlParameter("String", orderInfo.destinationAddress),
          SqlParameter("String", orderInfo.orderStatus.toString), // Enum to string
          SqlParameter("DateTime", orderInfo.orderTime.getMillis.toString)
        )
  
        IO(logger.info(s"Executing SQL to insert order record: SQL=${sql}, parameters=${parameters.map(_.value).mkString(", ")}")) >>
          writeDB(sql, parameters).map { result =>
            logger.info(s"Insert operation result: ${result}")
            // Step 4: Return the generated order ID
            orderID
          }
      }
  }
}
