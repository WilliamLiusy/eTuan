package Impl


import APIs.UserCenter.GetUserInfoByToken
import Objects.OrderService.{OrderInfo, OrderStatus}
import Objects.UserCenter.{UserInfo, UserType}
import Utils.OrderManagementProcess.queryOrdersByUserID
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.ProductService.ProductInfo
import Objects.UserCenter.RiderStatus
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
import Objects.OrderService.OrderInfo
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.OrderService.OrderStatus
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.OrderService.OrderStatus

case class QueryOrdersByUserPlanner(
    userToken: String,
    override val planContext: PlanContext
) extends Planner[List[OrderInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  /** Plan method to implement the logic for QueryOrdersByUser */
  override def plan(using planContext: PlanContext): IO[List[OrderInfo]] = {
    for {
      // Step 1: Validate the user token and fetch user info
      _ <- IO(logger.info("[Step 1] 开始校验用户令牌"))
      userInfo <- validateUserTokenAndFetchUserInfo()

      // Step 2: Query orders based on userID and userType
      _ <- IO(logger.info("[Step 2] 根据用户ID和类型查询订单信息"))
      orderList <- fetchOrdersByUserIDAndType(userInfo)

      // Step 3: Log and return the final result
      _ <- IO(logger.info(s"[Step 3] 查询到的用户订单数量: ${orderList.size}"))
    } yield orderList
  }

  /** Step 1: Validate user token and fetch user info */
  private def validateUserTokenAndFetchUserInfo()(using PlanContext): IO[UserInfo] = {
    for {
      _ <- IO(logger.info(s"验证令牌，调用GetUserInfoByToken接口: userToken=${userToken}"))
      userInfo <- GetUserInfoByToken(userToken).send
      _ <- IO(logger.info(s"用户信息验证成功，解析用户信息: userID=${userInfo.userID}, userType=${userInfo.userType}"))
    } yield userInfo
  }

  /** Step 2: Fetch orders based on userID and userType */
  private def fetchOrdersByUserIDAndType(userInfo: UserInfo)(using PlanContext): IO[List[OrderInfo]] = {
    val userID = userInfo.userID
    val userType = userInfo.userType

    for {
      _ <- IO(logger.info(s"解析用户类型: userType=${userType} 转换为查询字段"))
      queryField <- IO {
        userType match {
          case UserType.Customer => "customer_id"
          case UserType.Merchant => "merchant_id"
          case UserType.Rider    => "rider_id"
        }
      }
      _ <- IO(logger.info(s"根据userID=${userID} 和 查询字段=${queryField} 查询订单表"))
      orders <- queryOrdersFromDatabase(userID, queryField)
      _ <- IO(logger.info(s"成功查询到订单数量: ${orders.size}"))
    } yield orders
  }

  /** Step 2.1: Query orders from the database using userID and query field */
  private def queryOrdersFromDatabase(userID: String, queryField: String)(using PlanContext): IO[List[OrderInfo]] = {
    val querySql =
      s"""
         |SELECT order_id, customer_id, merchant_id, rider_id, product_list, destination_address, order_status, order_time
         |FROM ${schemaName}.order_table
         |WHERE ${queryField} = ?;
       """.stripMargin

    val queryParams = List(SqlParameter("String", userID))

    for {
      rows <- readDBRows(querySql, queryParams)
      _ <- IO(logger.info(s"[数据库查询] 查询到 ${rows.size} 行订单数据"))
      orders <- IO {
        rows.map { row =>
          val orderID = decodeField[String](row, "order_id")
          val customerID = decodeField[String](row, "customer_id")
          val merchantID = decodeField[String](row, "merchant_id")
          val riderID = decodeField[Option[String]](row, "rider_id")
          val rawProductList = decodeField[String](row, "product_list")
          val productList = decodeType[List[ProductInfo]](rawProductList)
          val destinationAddress = decodeField[String](row, "destination_address")
          val orderStatusStr = decodeField[String](row, "order_status")
          val orderStatus = OrderStatus.fromString(orderStatusStr)
          val orderTime = decodeField[DateTime](row, "order_time")

          // Construct OrderInfo object
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
    } yield orders
  }
}