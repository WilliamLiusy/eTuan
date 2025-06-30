package Impl


import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.Json
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
import Objects.UserCenter.{UserInfo, UserType, RiderStatus}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class GetAllMerchantsPlanner(override val planContext: PlanContext) extends Planner[List[UserInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[UserInfo]] = {
    for {
      _ <- IO(logger.info("开始从UserInfoTable中查询所有用户类型为Merchant的记录"))

      // Step 1: 从UserInfoTable表中查询所有用户类型为Merchant的记录。
      merchantsJson <- getMerchantRecords()
      _ <- IO(logger.info(s"成功查询到${merchantsJson.length}条商家记录"))

      // Step 2 & 3: 将查询结果逐条转换为UserInfo对象，并返回组装完成的List[UserInfo]。
      merchantList <- convertToUserInfoList(merchantsJson)
      _ <- IO(logger.info(s"完成转换，共${merchantList.length}条商家记录"))
    } yield merchantList
  }

  private def getMerchantRecords()(using PlanContext): IO[List[Json]] = {
    val query = s"""
         SELECT user_id, name, contact_number, address, user_type, status, create_time
         FROM ${schemaName}.user_info_table
         WHERE user_type = ?;
       """
    val parameters = List(SqlParameter("String", UserType.Merchant.toString))

    IO(logger.info("开始执行商家查询的数据库指令")) >>
      IO(logger.info(s"SQL: ${query}")) >>
      readDBRows(query, parameters)
  }

  private def convertToUserInfoList(jsonList: List[Json])(using PlanContext): IO[List[UserInfo]] = {
    IO {
      logger.info("开始将查询结果转换为UserInfo对象列表")
      jsonList.map { json =>
        val userID = decodeField[String](json, "user_id")
        val name = decodeField[String](json, "name")
        val contactNumber = decodeField[String](json, "contact_number")
        val address = decodeField[Option[String]](json, "address")
        val userType = UserType.fromString(decodeField[String](json, "user_type"))
        val status = decodeField[Option[String]](json, "status") match {
          case Some(value) if value.nonEmpty => Some(RiderStatus.fromString(value))
          case _ => None
        }
        val createTime = decodeField[DateTime](json, "create_time")
        UserInfo(userID, name, contactNumber, userType, address, status, createTime)
      }
    }
  }
}