package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import APIs.UserCenter.{GetAllIdleRiders, UpdateStatus}
import APIs.OrderService.{GetUnassignedOrders, UpdateRider}
import Objects.UserCenter.{RiderStatus, UserInfo}
import Objects.OrderService.{OrderInfo, OrderStatus}
import Common.API.PlanContext
import cats.effect.IO
import cats.implicits._
import Common.Object.SqlParameter
import cats.implicits.*
import Common.API.{PlanContext, Planner}
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.OrderService.OrderStatus
import APIs.UserCenter.GetAllIdleRiders
import APIs.OrderService.GetUnassignedOrders
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.ProductService.ProductInfo
import APIs.UserCenter.UpdateStatus
import Objects.UserCenter.RiderStatus
import APIs.OrderService.UpdateRider
import Objects.OrderService.OrderInfo
import cats.effect.unsafe.implicits.global

case object OrderAssignProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def OrderAssignPlanner()(using PlanContext): IO[Option[Any]] = {
    for {
      // 开始流程，记录日志
      _ <- IO(logger.info(s"[OrderAssignPlanner] 开始执行订单分配流程"))
      
      // 调用 API 获取未分配的订单
      unassignedOrders <- GetUnassignedOrders().send
      _ <- IO(logger.info(s"[OrderAssignPlanner] 获取未分配订单，共计 ${unassignedOrders.size} 个"))
      
      // 调用 API 获取空闲的骑手
      idleRiders <- GetAllIdleRiders().send
      _ <- IO(logger.info(s"[OrderAssignPlanner] 获取空闲骑手，共计 ${idleRiders.size} 名"))
  
      // 如果有未分配订单和空闲骑手，开始分配逻辑
      _ <- if (unassignedOrders.nonEmpty && idleRiders.nonEmpty) {
        IO(logger.info("[OrderAssignPlanner] 开始分配订单")) >> 
        IO {
          unassignedOrders.zipWithIndex.foreach { case (order, index) =>
            // 选择一名骑手（按索引轮换方式）
            val riderIndex = index % idleRiders.size
            val selectedRider = idleRiders(riderIndex)
  
            logger.info(s"[OrderAssignPlanner] 分配订单：${order.orderID} 给骑手：${selectedRider.userID}")
            
            // 更新骑手和订单信息
            // 每次更新分别调用 UpdateRider 和 UpdateStatus API
            val updateFlow = for {
              _ <- UpdateRider(order.orderID, selectedRider.userID).send
              _ <- UpdateStatus(selectedRider.userID, RiderStatus.Delivering).send
              _ <- IO(logger.info(s"[OrderAssignPlanner] 更新成功：骑手 ${selectedRider.userID} 状态为 Delivering"))
            } yield ()
            updateFlow.unsafeRunSync() // 同步执行更新逻辑，因为仅在循环体内
          }
        }.void
      } else {
        // 没有需要分配的订单或没有空闲骑手的情况
        IO {
          if (unassignedOrders.isEmpty) logger.info("[OrderAssignPlanner] 无未分配的订单，流程终止")
          if (idleRiders.isEmpty) logger.info("[OrderAssignPlanner] 无空闲骑手，流程终止")
        }.void
      }
  
      // 记录流程结束
      _ <- IO(logger.info("[OrderAssignPlanner] 订单分配流程结束"))
    } yield None
  }
}
