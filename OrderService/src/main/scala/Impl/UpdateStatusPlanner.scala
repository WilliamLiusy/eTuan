package Impl


import Utils.OrderManagementProcess.updateOrderStatus
import Objects.UserCenter.{UserType, UserInfo}
import Objects.OrderService.OrderStatus
import APIs.UserCenter.GetUserInfoByToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import io.circe._
import cats.effect.IO
import cats.implicits._
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
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.ProductService.ProductInfo
import Objects.UserCenter.RiderStatus
import Objects.OrderService.OrderStatus
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class UpdateStatusPlanner(
  userToken: String,
  orderID: String,
  newStatus: OrderStatus,
  override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate user token and get user information
      _ <- IO(logger.info(s"[Step 1] Validating user token: ${userToken}"))
      userInfo <- validateUserToken(userToken)

      // Step 2: Validate user role
      _ <- IO(logger.info(s"[Step 2] Validating user role for userID=${userInfo.userID}, userType=${userInfo.userType}"))
      _ <- validateUserRole(userInfo)

      // Step 3: Check if order exists
      _ <- IO(logger.info(s"[Step 3] Checking order existence: orderID=${orderID}"))
      _ <- checkOrderExists(orderID)

      // Step 4: Update order status
      _ <- IO(logger.info(s"[Step 4] Updating order status for orderID=${orderID} to newStatus=${newStatus}"))
      updateResult <- updateOrderStatus(orderID, newStatus)

      // Step 5: Log and return result
      _ <- IO(logger.info(s"[Step 5] Order status updated successfully for orderID=${orderID}, result=${updateResult}"))
    } yield "Success"
  }

  private def validateUserToken(token: String)(using PlanContext): IO[UserInfo] = {
    GetUserInfoByToken(token).send.map { userInfo =>
      logger.info(s"User token is valid. Fetched userInfo: ${userInfo}")
      userInfo
    }
  }

  private def validateUserRole(userInfo: UserInfo)(using PlanContext): IO[Unit] = {
    val validRoles = Set(UserType.Merchant, UserType.Rider)
    if (validRoles.contains(userInfo.userType)) {
      IO(logger.info(s"User has a valid role: ${userInfo.userType}"))
    } else {
      val errorMessage = s"Invalid user role: ${userInfo.userType}. Only Merchant or Rider roles are allowed."
      IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalArgumentException(errorMessage))
    }
  }

  private def checkOrderExists(orderID: String)(using PlanContext): IO[Unit] = {
    val querySql = 
      s"""
         SELECT order_id FROM ${schemaName}.order_table WHERE order_id = ?
       """.stripMargin

    readDBJsonOptional(querySql, List(SqlParameter("String", orderID))).flatMap {
      case Some(_) =>
        IO(logger.info(s"Order with ID=${orderID} exists in the database"))
      case None =>
        val errorMessage = s"Order with ID=${orderID} does not exist in the database"
        IO(logger.error(errorMessage)) >> IO.raiseError(new NoSuchElementException(errorMessage))
    }
  }
}