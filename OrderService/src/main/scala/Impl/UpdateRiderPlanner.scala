package Impl


import Objects.OrderService.OrderStatus
import Objects.OrderService.OrderInfo
import Utils.OrderManagementProcess.updateOrderStatus
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.ProductService.ProductInfo
import Objects.UserCenter.RiderStatus
import Common.API.{PlanContext, Planner, API}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import Objects.UserCenter.RiderStatus
import Objects.OrderService.{OrderInfo, OrderStatus}
import Objects.UserCenter.{UserType, RiderStatus}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class UpdateRiderMessage(orderID: String, newRider: String) extends API[String]("UpdateRider")

case class UpdateRiderPlanner(orderID: String, newRider: String, override val planContext: PlanContext) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate OrderID and RiderID
      _ <- IO(logger.info(s"Starting validation for orderID=${orderID} and newRider=${newRider}"))
      _ <- validateOrderID(orderID)
      _ <- validateRiderID(newRider)

      // Step 2: Update Order Status and RiderID
      _ <- IO(logger.info(s"Updating order status to Delivering and riderID for orderID=${orderID}"))
      _ <- updateOrderStatus(orderID, OrderStatus.Delivering)
      _ <- updateRiderInfo(orderID, newRider)

      // Step 3: Return success result
      _ <- IO(logger.info(s"Successfully updated rider for orderID=${orderID} to newRider=${newRider}"))
    } yield "Success"
  }

  private def validateOrderID(orderID: String)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating existence of orderID=${orderID}"))
      sql <- IO {
        s"""
           SELECT * 
           FROM ${schemaName}.order_table
           WHERE order_id = ?
          """
      }
      orderOpt <- readDBJsonOptional(sql, List(SqlParameter("String", orderID)))
      _ <- orderOpt match {
        case None =>
          IO(logger.error(s"Order with orderID=${orderID} does not exist")) >>
          IO.raiseError(new IllegalArgumentException(s"Order with orderID=${orderID} does not exist"))
        case Some(_) =>
          IO(logger.info(s"Order with orderID=${orderID} exists"))
      }
    } yield ()
  }

  private def validateRiderID(riderID: String)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating riderID=${riderID}"))
      sql <- IO {
        s"""
           SELECT * 
           FROM ${schemaName}.user_info
           WHERE user_id = ?
          """
      }
      userOpt <- readDBJsonOptional(sql, List(SqlParameter("String", riderID)))
      _ <- userOpt match {
        case None =>
          IO(logger.error(s"User with riderID=${riderID} does not exist")) >>
          IO.raiseError(new IllegalArgumentException(s"User with riderID=${riderID} does not exist"))
        case Some(json) =>
          val userType: UserType = decodeField[UserType](json, "user_type")
          if (userType != UserType.Rider) {
            IO(logger.error(s"User with riderID=${riderID} is not of type Rider")) >>
            IO.raiseError(new IllegalArgumentException(s"User with riderID=${riderID} is not of type Rider"))
          } else {
            IO(logger.info(s"User with riderID=${riderID} is valid and of type Rider"))
          }
      }
    } yield ()
  }

  private def updateRiderInfo(orderID: String, newRider: String)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Updating riderID to ${newRider} for orderID=${orderID}"))
      sql <- IO {
        s"""
           UPDATE ${schemaName}.order_table
           SET rider_id = ?
           WHERE order_id = ?
          """
      }
      result <- writeDB(
        sql,
        List(
          SqlParameter("String", newRider),
          SqlParameter("String", orderID)
        )
      )
      _ <- IO(logger.info(s"Successfully updated riderID for orderID=${orderID}. Result: ${result}"))
    } yield ()
  }
}