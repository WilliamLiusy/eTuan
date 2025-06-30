package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.OrderService.{OrderInfo, OrderStatus}
import Objects.ProductService.ProductInfo
import cats.effect.IO
import io.circe.Json
import org.slf4j.LoggerFactory
import io.circe.parser.decode
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Objects.OrderService.OrderStatus
import Objects.OrderService.OrderInfo
import Objects.ProductService.ProductInfo
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class GetUnassignedOrdersPlanner(
    override val planContext: PlanContext
) extends Planner[List[OrderInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[OrderInfo]] = {
    for {
      // Step 1: Query all orders with status 'WaitingForAssign'
      _ <- IO(logger.info("[Step 1] 开始查询所有状态为WaitingForAssign的订单"))
      ordersJson <- getOrdersWithStatus(OrderStatus.WaitingForAssign.toString) // 数据库查询
      _ <- IO(logger.info(s"[Step 1] 查询到符合条件的订单数为${ordersJson.size}"))

      // Step 2: Map JSON results to List[OrderInfo]
      _ <- IO(logger.info("[Step 2] 开始将查询结果转换为List[OrderInfo]"))
      orderList <- mapToOrderInfoList(ordersJson) // 数据映射
      _ <- IO(logger.info(s"[Step 2] 转换完成，生成的OrderInfo对象列表个数为${orderList.size}"))
    } yield orderList
  }

  // 查询状态为指定值的订单信息
  private def getOrdersWithStatus(orderStatus: String)(using PlanContext): IO[List[Json]] = {
    IO(logger.info(s"[getOrdersWithStatus] 开始创建查询状态为${orderStatus}订单的数据库指令")) >>
      IO {
        val sql =
          s"""
             SELECT * 
             FROM ${schemaName}.order_table
             WHERE order_status = ?;
          """
        logger.info(s"[getOrdersWithStatus] 指令为：${sql}")
        sql
      }.flatMap { sql =>
        logger.info("[getOrdersWithStatus] 开始执行查询状态为指定状态订单的数据库指令")
        readDBRows(
          sql,
          parameters = List(SqlParameter("String", orderStatus))
        )
      }
  }

  // 将查询结果JSON映射为 List[OrderInfo]
  private def mapToOrderInfoList(jsonList: List[Json])(using PlanContext): IO[List[OrderInfo]] = {
    IO {
      jsonList.map { json =>
        // 使用 decodeField 提取 JSON 中字段并转换
        val orderID = decodeField[String](json, "order_id")
        val customerID = decodeField[String](json, "customer_id")
        val merchantID = decodeField[String](json, "merchant_id")
        val riderID = decodeField[Option[String]](json, "rider_id")
        val productList = decodeField[List[ProductInfo]](json, "product_list")
        val destinationAddress = decodeField[String](json, "destination_address")
        val orderStatusStr = decodeField[String](json, "order_status")
        val orderStatus = OrderStatus.fromString(orderStatusStr) // 转换为枚举类型
        val orderTime = decodeField[DateTime](json, "order_time")

        // 构造 OrderInfo 对象
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
  }
}