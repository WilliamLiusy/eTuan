package Impl


import Objects.OrderService.{OrderInfo, OrderStatus}
import Objects.UserCenter.{UserInfo, UserType, RiderStatus}
import Objects.ProductService.ProductInfo
import APIs.UserCenter.GetUserInfoByToken
import Utils.OrderManagementProcess.createOrderRecord
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
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
import Objects.OrderService.OrderInfo
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.OrderService.OrderStatus
import Objects.UserCenter.RiderStatus
import Utils.OrderManagementProcess.createOrderRecord
import Objects.UserCenter.{UserInfo, UserType}
import io.circe._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class CreateOrderPlanner(
                               customerToken: String,
                               merchantID: String,
                               productList: List[ProductInfo],
                               destinationAddress: String,
                               override val planContext: PlanContext
                             ) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate customerToken and retrieve customerID
      _ <- IO(logger.info(s"Validating customerToken: $customerToken"))
      userInfo <- validateCustomerToken()

      // Step 2: Build the OrderInfo object and save to database
      _ <- IO(logger.info(s"Building order info for customerID: ${userInfo.userID}"))
      orderInfo <- buildOrderInfo(userInfo.userID)
      _ <- IO(logger.info(s"Saving order to database: $orderInfo"))
      orderID <- createOrderRecord(orderInfo)

      // Step 3: Return the generated orderID
      _ <- IO(logger.info(s"Order created successfully, orderID: $orderID"))
    } yield orderID
  }

  // Validates the customer token and retrieves the UserInfo object if valid.
  private def validateCustomerToken()(using PlanContext): IO[UserInfo] = {
    GetUserInfoByToken(customerToken).send.flatMap { userInfo =>
      if (userInfo.userType != UserType.Customer) {
        val error = s"Invalid token: The provided token does not belong to a customer."
        IO(logger.error(error)) *> IO.raiseError(new IllegalArgumentException(error))
      } else {
        IO.pure(userInfo)
      }
    }
  }

  // Builds the OrderInfo object for the provided customer ID.
  private def buildOrderInfo(customerID: String)(using PlanContext): IO[OrderInfo] = {
    IO {
      val currentTime = DateTime.now
      OrderInfo(
        orderID = "", // Order ID will be generated during database insertion
        customerID = customerID,
        merchantID = merchantID,
        riderID = None, // Rider not assigned at creation
        productList = productList,
        destinationAddress = destinationAddress,
        orderStatus = OrderStatus.WaitingForDish, // Initial order status
        orderTime = currentTime // Current time of order creation
      )
    }
  }
}