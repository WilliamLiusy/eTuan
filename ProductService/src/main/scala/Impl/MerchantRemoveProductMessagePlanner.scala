package Impl


import APIs.UserCenter.GetUserInfoByToken
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.ProductService.ProductInfo
import APIs.ProductService.FetchProductsByNameAndMerchantIDMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import Common.Serialize.CustomColumnTypes._
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
import Objects.UserCenter.RiderStatus
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.UserCenter.RiderStatus



case class MerchantRemoveProductMessagePlanner(
    merchantToken: String,
    name: String,
    override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: 验证商家身份令牌 merchantToken 的合法性
      _ <- IO(logger.info(s"[Step 1] 验证商家令牌 merchantToken=$merchantToken 的合法性"))
      userInfo <- validateMerchant()

      // Step 2: 根据提供的商品名称匹配商家的商品
      _ <- IO(logger.info(s"[Step 2] 查找商家ID=${userInfo.userID}与商品名称=$name 匹配的商品"))
      productOption <- fetchProductByNameAndMerchantID(userInfo.userID)
      result <- productOption match {
        case Some(_) =>
            // Step 3: 删除匹配的商品记录
//            _ <- IO(logger.info(s"[Step 3] 匹配到商品，准备删除"))
            IO(logger.info(s"[Step 3] 匹配到商品，准备删除")) >>
            deleteProduct(userInfo.userID).flatMap {deleteSuccess =>
            if (deleteSuccess) {
            IO (logger.info (s"[Step 3.1] 商品成功删除，返回 Success"))
            IO.pure ("Success")
          } else {
            IO (logger.error (s"[Step 3.2] 商品删除失败，返回 Failure"))
            IO.pure ("Failure")
            }
            }
        case None =>
//            _ <- IO(logger.info(s"[Step 3] 未找到匹配商品，返回 ProductNotFound"))
            IO(logger.info(s"[Step 3] 未找到匹配商品，返回 ProductNotFound")) >>
            IO.pure("ProductNotFound")
      }
    } yield result
  }

  // 验证商家身份
  private def validateMerchant()(using PlanContext): IO[UserInfo] = {
    GetUserInfoByToken(merchantToken).send.handleErrorWith { error =>
      val errorMessage = s"[Step 1.1] 无法通过令牌 merchantToken=$merchantToken 获取用户信息: ${error.getMessage}"
      IO(logger.error(errorMessage)) >>
        IO.raiseError(new IllegalStateException("Unauthorized"))
    }.flatMap { userInfo =>
      if (userInfo.userType == UserType.Merchant) {
        IO(logger.info(s"[Step 1.2] 令牌验证通过，用户类型为商家，商家ID=${userInfo.userID}"))
        IO.pure(userInfo)
      } else {
        val errorMessage = s"[Step 1.3] 用户信息验证失败，用户类型非商家: userType=${userInfo.userType}"
        IO(logger.error(errorMessage)) >>
          IO.raiseError(new IllegalStateException("Unauthorized"))
      }
    }
  }

  // 根据商家ID和商品名称获取商品信息
  private def fetchProductByNameAndMerchantID(merchantID: String)(using PlanContext): IO[Option[ProductInfo]] = {
    FetchProductsByNameAndMerchantIDMessage(merchantID, name).send.map { result =>
      logger.info(s"Response from .send: $result")
      result
    }
      .handleErrorWith { error =>
      IO(logger.error(s"[Step 2.1] 根据商家ID=$merchantID 和商品名称=$name 获取商品失败: ${error.getMessage}")) >>
        IO.pure(None)
    }
  }

  // 删除商品
  private def deleteProduct(merchantID: String)(using PlanContext): IO[Boolean] = {
    val deleteSQL =
      s"""
DELETE FROM ${schemaName}.product_table
WHERE merchant_id = ? AND name = ?;
      """.stripMargin

    writeDB(
      deleteSQL,
      List(
        SqlParameter("String", merchantID),
        SqlParameter("String", name)
      )
    ).attempt.flatMap {
      case Right(response) =>
        IO(logger.info(s"[Step 3.1.1] 商品删除成功，返回数据库响应: $response")) >> IO.pure(true)
      case Left(error) =>
        IO(logger.error(s"[Step 3.1.2] 商品删除失败: ${error.getMessage}")) >> IO.pure(false)
    }
  }
}