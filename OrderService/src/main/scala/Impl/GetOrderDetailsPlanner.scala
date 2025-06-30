package Impl


import Objects.OrderService.OrderStatus
import Objects.OrderService.OrderInfo
import Objects.ProductService.ProductInfo
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.ProductService.ProductInfo
import Objects.OrderService.{OrderInfo, OrderStatus}
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class GetOrderDetailsPlanner(orderID: String, override val planContext: PlanContext) extends Planner[OrderInfo] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan function
  override def plan(using PlanContext): IO[OrderInfo] = for {
    // Step 1: Validate the order ID
    _ <- IO(logger.info(s"Validating orderID: ${orderID}"))
    _ <- validateOrderID(orderID)

    // Step 2: Query OrderTable to get the order data
    _ <- IO(logger.info(s"Fetching order details from the database for orderID: ${orderID}"))
    orderData <- fetchOrderData(orderID)

    // Step 3: Map the order data to OrderInfo
    _ <- IO(logger.info(s"Mapping database order data to OrderInfo for orderID: ${orderID}"))
    orderInfo <- mapOrderDataToOrderInfo(orderData)
  } yield orderInfo

  // Step 1.1: Validate the input orderID
  private def validateOrderID(orderID: String)(using PlanContext): IO[Unit] = {
    if (orderID.trim.isEmpty)
      IO.raiseError(new IllegalArgumentException("Order ID must be a non-empty string."))
    else
      IO(logger.info(s"OrderID: ${orderID} is valid."))
  }

  // Step 2.1: Fetch order details from the OrderTable
  private def fetchOrderData(orderID: String)(using PlanContext): IO[Json] = {
    val sqlQuery =
      s"""
        SELECT order_id, customer_id, merchant_id, rider_id, product_list, destination_address, order_status, order_time
        FROM ${schemaName}.order_table
        WHERE order_id = ?;
      """.stripMargin
    logger.info(s"SQL Query: ${sqlQuery}")

    readDBJsonOptional(sqlQuery, List(SqlParameter("String", orderID))).flatMap {
      case Some(json) => IO.pure(json)
      case None => IO.raiseError(new NoSuchElementException(s"Order with ID '${orderID}' does not exist."))
    }
  }

  // Step 3.1: Map database fields to an OrderInfo object
  private def mapOrderDataToOrderInfo(orderData: Json)(using PlanContext): IO[OrderInfo] = IO {
    logger.info(s"Mapping order data to OrderInfo object: ${orderData.noSpaces}")
    OrderInfo(
      orderID = decodeField[String](orderData, "order_id"),
      customerID = decodeField[String](orderData, "customer_id"),
      merchantID = decodeField[String](orderData, "merchant_id"),
      riderID = decodeField[Option[String]](orderData, "rider_id"),
      productList = decodeType[List[ProductInfo]](decodeField[String](orderData, "product_list")),
      destinationAddress = decodeField[String](orderData, "destination_address"),
      orderStatus = OrderStatus.fromString(decodeField[String](orderData, "order_status")),
      orderTime = new DateTime(decodeField[String](orderData, "order_time").toLong)
    )
  }
}