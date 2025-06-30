package Impl


/**
 * Planner for UpdateOrderStatus: 根据订单ID更新订单状态
 *
 * 输入参数：
 *   - orderID : String
 *   - newStatus : OrderStatus
 *
 * 输出参数：
 *   - successOrNot : String ("true" 或 "false")
 */
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import Common.Object.SqlParameter
import Objects.OrderService.OrderStatus
import Utils.OrderManagementProcess.updateOrderStatus
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
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
import Objects.ProductService.ProductInfo
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.ProductService.ProductInfo

case class UpdateOrderStatusPlanner(
    orderID: String,
    newStatus: OrderStatus,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate orderID existence
      _ <- IO(logger.info(s"Step 1: Checking if orderID=${orderID} exists in OrderTable"))
      orderExists <- checkOrderExists(orderID)
      successOrNot <- if (!orderExists) {
        // If orderID doesn't exist, log and return failure message
        IO(logger.error(s"Order with orderID=${orderID} does not exist")).as("false")
      } else {
        for {
          // Step 2: Update order status
          _ <- IO(logger.info(s"Step 2: Updating order status to newStatus=${newStatus} for orderID=${orderID}"))
          updateResult <- updateOrderStatusImpl(orderID, newStatus)
          result <- if (updateResult != "Success") {
            // Log and return failure message if update failed
            IO(logger.error(s"Failed to update order status for orderID=${orderID}. Reason: ${updateResult}")).as("false")
          } else {
            // Step 3: Return success result
            IO(logger.info(s"Step 3: Successfully updated order status for orderID=${orderID}")).as("true")
          }
        } yield result
      }
    } yield successOrNot
  }

  /**
   * 检查订单是否存在
   */
  private def checkOrderExists(orderID: String)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
SELECT order_id
FROM ${schemaName}.order_table
WHERE order_id = ?
       """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("String", orderID))).map {
      case Some(_) => true
      case None    => false
    }
  }

  /**
   * 更新订单状态
   */
  private def updateOrderStatusImpl(orderID: String, newStatus: OrderStatus)(using PlanContext): IO[String] = {
    updateOrderStatus(orderID, newStatus)
  }
}