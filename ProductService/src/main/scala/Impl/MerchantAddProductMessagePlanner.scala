package Impl


import APIs.UserCenter.GetUserInfoByToken
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
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
import java.util.UUID
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class MerchantAddProductMessagePlanner(
                                             merchantToken: String,
                                             name: String,
                                             price: Double,
                                             description: String,
                                             override val planContext: PlanContext
                                           ) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Verify merchant identity
      _ <- IO(logger.info(s"[Step 1] 验证商家身份中，merchantToken=${merchantToken}"))
      merchantInfo <- validateMerchantIdentity(merchantToken)

      // Step 2: Generate unique product ID
      _ <- IO(logger.info(s"[Step 2] 开始生成商品唯一编号"))
      productID <- IO(generateProductID())
      _ <- IO(logger.info(s"[Step 2] 成功生成商品唯一编号：productID=${productID}"))

      // Step 3: Insert product into ProductTable
      _ <- IO(logger.info(s"[Step 3] 准备插入商品信息到 ProductTable：productID=${productID}, merchantID=${merchantInfo.userID}, name=${name}, price=${price}, description=${description}"))
      _ <- insertProduct(productID, merchantInfo.userID, name, price, description)
      _ <- IO(logger.info("[Step 3] 商品信息插入成功"))

    } yield {
      logger.info("[All Steps Done] 商品添加操作完成，状态为：Success")
      "Success"
    }
  }.handleErrorWith { error =>
    IO(logger.error("[Operation Failed] 商品添加操作失败！", error)) *> IO.pure("Failure")
  }

  private def validateMerchantIdentity(merchantToken: String)(using PlanContext): IO[UserInfo] = {
    GetUserInfoByToken(merchantToken).send.flatMap { userInfo =>
      if (userInfo.userType == UserType.Merchant) {
        IO(logger.info(s"[Step 1.1] 商家身份验证通过，merchantID=${userInfo.userID}, name=${userInfo.name}")) *> IO.pure(userInfo)
      } else {
        val errorMessage = s"用户身份验证失败，用户类型不是商家。userType=${userInfo.userType}"
        IO(logger.error(errorMessage)) *> IO.raiseError(new IllegalArgumentException(errorMessage))
      }
    }.handleErrorWith { error =>
      val errorMessage = s"[Step 1.2] 商家身份验证失败，原因：${error.getMessage}"
      IO(logger.error(errorMessage, error)) *> IO.raiseError(new IllegalArgumentException(errorMessage))
    }
  }

  private def generateProductID(): String = {
    UUID.randomUUID().toString
  }

  private def insertProduct(productID: String, merchantID: String, name: String, price: Double, description: String)(using PlanContext): IO[Unit] = {
    val insertSQL =
      s"""
         |INSERT INTO ${schemaName}.product_table
         |(product_id, merchant_id, name, price, description)
         |VALUES (?, ?, ?, ?, ?);
       """.stripMargin

    val parameters = List(
      SqlParameter("String", productID),
      SqlParameter("String", merchantID),
      SqlParameter("String", name),
      SqlParameter("Double", price.toString),
      SqlParameter("String", description)
    )

    writeDB(insertSQL, parameters).map { _ => 
      logger.info(s"[Step 3] 成功将商品信息写入数据库，productID=${productID}")
    }.void
  }
}