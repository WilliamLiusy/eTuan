package Impl


import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Utils.UserInfoProcess.validateUserToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import io.circe.Json
import io.circe.parser.decode
import io.circe.generic.auto._
import cats.effect.IO
import org.slf4j.LoggerFactory
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
import Utils.UserInfoProcess.validateUserToken
import io.circe._
import io.circe.syntax._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class UpdateStatusPlanner(
    userToken: String,
    newStatus: RiderStatus,
    override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 校验用户令牌合法性
      _ <- IO(logger.info(s"开始校验用户令牌：userToken=${userToken}"))
      userID <- validateUserToken(userToken)

      // Step 2: 获取并校验用户类型是否为 Rider
      _ <- IO(logger.info(s"获取用户信息：userID=${userID}"))
      userInfo <- getUserInfo(userID)
      _ <- IO(logger.info(s"校验用户类型是否为骑手：userType=${userInfo.userType}"))
      _ <- checkUserTypeIsRider(userInfo)

      // Step 3: 更新用户状态
      _ <- IO(logger.info(s"用户验证通过，更新用户状态为：newStatus=${newStatus.toString}"))
      updateResult <- updateUserStatus(userID, newStatus)
    } yield {
      // Step 4: 返回更新结果
      logger.info(s"更新状态结果：${updateResult}")
      updateResult
    }
  }

  private def getUserInfo(userID: String)(using PlanContext): IO[UserInfo] = {
    val query = s"SELECT * FROM ${schemaName}.user_info_table WHERE user_id = ?"
    val params = List(SqlParameter("String", userID))

    readDBJson(query, params).map { json =>
      decodeType[UserInfo](json)
    }
  }

  private def checkUserTypeIsRider(userInfo: UserInfo)(using PlanContext): IO[Unit] = {
    if (userInfo.userType != UserType.Rider) {
      val errorMessage = s"用户类型校验失败：userID=${userInfo.userID}, userType=${userInfo.userType}"
      logger.error(errorMessage)
      IO.raiseError(new Exception(errorMessage))
    } else {
      IO(logger.info("用户类型校验通过")).void
    }
  }

  private def updateUserStatus(userID: String, newStatus: RiderStatus)(using PlanContext): IO[String] = {
    val query = s"UPDATE ${schemaName}.user_info_table SET status = ? WHERE user_id = ?"
    val params = List(
      SqlParameter("String", newStatus.toString), // RiderStatus 转化为 String
      SqlParameter("String", userID)
    )

    writeDB(query, params)
  }
}