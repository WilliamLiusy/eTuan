package Impl


import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Utils.UserInfoProcess.validateUserToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import cats.implicits.*
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
import Utils.UserInfoProcess.validateUserToken
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class GetUserInfoByTokenPlanner(
                                      userToken: String,
                                      override val planContext: PlanContext
                                    ) extends Planner[UserInfo] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[UserInfo] = {
    for {
      // Step 1: Validate the user token
      _ <- IO(logger.info(s"[Step 1] 开始校验用户令牌格式: userToken=${userToken}"))
      userID <- validateUserToken(userToken)

      // Step 2: Fetch user info from UserInfoTable
      _ <- IO(logger.info(s"[Step 2] 验证成功，开始从数据库获取用户信息: userID=${userID}"))
      userInfo <- fetchUserInfo(userID)
    } yield {
      logger.info(s"[Result] 成功获取用户信息: ${userInfo}")
      userInfo
    }
  }

  private def fetchUserInfo(userID: String)(using PlanContext): IO[UserInfo] = {
    val sql =
      s"""
         |SELECT user_id, name, contact_number, user_type, address, status, create_time
         |FROM ${schemaName}.user_info_table
         |WHERE user_id = ?;
       """.stripMargin

    logger.info(s"[fetchUserInfo] 执行SQL查询用户信息: SQL=${sql}, userID=${userID}")

    readDBJson(sql, List(SqlParameter("String", userID))).map { json =>
      val id = decodeField[String](json, "user_id")
      val name = decodeField[String](json, "name")
      val contactNumber = decodeField[String](json, "contact_number")
      val userType = UserType.fromString(decodeField[String](json, "user_type"))
      val address = decodeField[Option[String]](json, "address")
      val status = decodeField[Option[String]](json, "status")
      val createTime = new DateTime(decodeField[Long](json, "create_time"))

      UserInfo(
        userID = id,
        name = name,
        contactNumber = contactNumber,
        userType = userType,
        address = address,
        status = status.map(RiderStatus.fromString),
        createTime = createTime
      )
    }
  }
}