package Impl


/**
 * Planner for FetchProductsByMerchantIDMessage.
 * 根据商家ID筛选商品信息，返回一个ProductInfo的列表。
 */
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

case class FetchProductsByMerchantIDMessagePlanner(
                                                    merchantID: String,
                                                    override val planContext: PlanContext
                                                  ) extends Planner[List[ProductInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  /**
   * 核心方法：执行 FetchProductsByMerchantIDMessage 请求流程。
   */
  override def plan(using planContext: PlanContext): IO[List[ProductInfo]] = {
    for {
      // Step 1: Validate merchantID
      _ <- IO(logger.info(s"[Step 1] Validating merchantID: ${merchantID}"))
      _ <- validateMerchantID()

      // Step 2: Query products from ProductTable by merchantID
      _ <- IO(logger.info(s"[Step 2] Fetching products for merchantID: ${merchantID} from database"))
      products <- fetchProductsByMerchantID()

      // Step 3: Logging the result
      _ <- IO(logger.info(s"[Step 3] Fetched ${products.length} products for merchantID: ${merchantID}"))
    } yield products
  }

  /**
   * 验证商家ID是否合法：非空字符串。
   */
  private def validateMerchantID()(using PlanContext): IO[Unit] = {
    if (merchantID.trim.isEmpty) {
      val errorMessage = s"[Error] Validation failed: merchantID cannot be empty."
      IO(logger.error(errorMessage)) >>
        IO.raiseError(new IllegalArgumentException(errorMessage))
    } else {
      IO(logger.info(s"[Validation] MerchantID: ${merchantID} is valid."))
    }
  }

  /**
   * 根据商家ID从数据库中查询商品信息。
   */
  private def fetchProductsByMerchantID()(using PlanContext): IO[List[ProductInfo]] = {
    val sql =
      s"""
         |SELECT product_id, merchant_id, name, price, description
         |FROM ${schemaName}.product_table
         |WHERE merchant_id = ?;
         |""".stripMargin
    val parameters = List(SqlParameter("String", merchantID))

    // 打印 SQL 和参数用于调试
    IO(logger.info(s"Executing SQL Query: ${sql} with parameters: ${parameters.map(_.value).mkString(", ")}")) >>
      readDBRows(sql, parameters).map { rows => 
        // 对数据库返回的每一行结果进行解码，并转为 ProductInfo 对象
        rows.map { row =>
          try {
            decodeType[ProductInfo](row)
          } catch {
            case ex: Exception =>
              val decodeError = s"[Error] Failed to decode row: ${row.noSpaces}, Exception: ${ex.getMessage}"
              logger.error(decodeError)
              throw new RuntimeException(decodeError, ex)
          }
        }
      }
  }
}