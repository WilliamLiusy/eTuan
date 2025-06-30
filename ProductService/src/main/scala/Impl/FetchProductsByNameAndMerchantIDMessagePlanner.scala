package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.ProductService.ProductInfo
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
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
import io.circe._
import io.circe.syntax._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class FetchProductsByNameAndMerchantIDMessagePlanner(
                                                           merchantID: String,
                                                           name: String,
                                                           override val planContext: PlanContext
                                                         ) extends Planner[Option[List[ProductInfo]]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[List[ProductInfo]]] = {
    for {
      // Step 1: Validate input parameters
      _ <- validateParameters()

      // Step 2: Query products by merchant ID and name
      _ <- IO(logger.info(s"查询商家ID为${merchantID}且名称包含${name}的商品"))
      productsJson <- queryProductsByMerchantIDAndName()

      // Step 3: Convert results to ProductInfo objects
      _ <- IO(logger.info("将查询结果转换为ProductInfo对象"))
      products <- IO { productsJson.map(decodeType[ProductInfo]) }

      // Step 4: Return results as Option
      result = if (products.isEmpty) None else Some(products)
      _ <- IO(logger.info(s"查询到的商品数量为${products.size}, 返回结果为${result.isDefined}"))
    } yield result
  }

  private def validateParameters()(using PlanContext): IO[Unit] = {
    if (merchantID.trim.isEmpty || name.trim.isEmpty) {
      IO.raiseError(
        new IllegalArgumentException(
          s"商家ID和商品名称不能为空，输入值为 merchantID=${merchantID}, name=${name}"
        )
      )
    } else {
      IO(logger.info(s"输入参数验证通过，merchantID=${merchantID}, name=${name}"))
    }
  }

  private def queryProductsByMerchantIDAndName()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
        SELECT product_id, merchant_id, name, price, description
        FROM ${schemaName}.product_table
        WHERE merchant_id = ? AND name LIKE ?;
       """
    val parameters = List(
      SqlParameter("String", merchantID),
      SqlParameter("String", s"%${name}%") // Partial match for name
    )
    IO(logger.info(s"执行SQL查询: ${sql}")) >>
      IO(logger.info(s"SQL参数为: merchantID=${merchantID}, name包含=${name}")) >>
      readDBRows(sql, parameters)
  }
}