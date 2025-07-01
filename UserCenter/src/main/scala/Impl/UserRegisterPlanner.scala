package Impl


import Utils.UserInfoProcess.generateUserToken
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import Objects.UserCenter.RiderStatus
import Objects.UserCenter.{UserType, UserInfo, RiderStatus}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
    import java.security.MessageDigest
//import io.circe.generic.auto._ // 确保这行在作用域内
//import io.circe.parser.decode

case class UserRegisterPlanner(
                                name: String,
                                contactNumber: String,
                                password: String,
                                userType: UserType,
                              override val planContext: PlanContext) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Generate a new userID
      _ <- IO(logger.info("生成新的用户ID"))
      userID = java.util.UUID.randomUUID().toString

      // Step 2: Save encrypted password to UserPasswordTable
      _ <- IO(logger.info("保存加密后的密码至UserPasswordTable"))
      _ <- saveEncryptedPassword(userID, password)

      // Step 3: Save user basic information to UserInfoTable
      _ <- IO(logger.info("保存用户基本信息至UserInfoTable"))
      _ <- saveUserInfo(userID, name, contactNumber, userType)

      // Step 4: Generate userToken
      _ <- IO(logger.info("生成用户令牌"))
      userToken <- generateUserToken(userID)

      // Step 5: Return userToken
      _ <- IO(logger.info(s"用户注册成功，返回用户令牌: ${userToken}"))
    } yield userToken
  }

  private def saveEncryptedPassword(userID: String, password: String)(using PlanContext): IO[Unit] = {
    import java.security.MessageDigest
    val encryptedPassword = MessageDigest.getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString

    val sql =
      s"""
        INSERT INTO ${schemaName}.user_password_table (user_id, password)
        VALUES (?, ?)
      """.stripMargin

    val params = List(
      SqlParameter("String", userID),
      SqlParameter("String", encryptedPassword)
    )

    writeDB(sql, params).void
  }

  private def saveUserInfo(userID: String, name: String, contactNumber: String, userType: UserType)(using PlanContext): IO[Unit] = {
    val currentTime = DateTime.now

    // Determine `address` and `status` based on userType
    val address: Option[String] = if (userType == UserType.Merchant) Some("") else None
    val status: Option[RiderStatus] = Some(RiderStatus.Idle)

    val userInfo = UserInfo(
      userID = userID,
      name = name,
      contactNumber = contactNumber,
      userType = userType,
      address = address,
      status = status,
      createTime = currentTime
    )

    val sql =
      s"""
        INSERT INTO ${schemaName}.user_info_table (user_id, name, contact_number, address, user_type, status, create_time)
        VALUES (?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?))
      """.stripMargin

    val params = List(
      SqlParameter("String", userInfo.userID),
      SqlParameter("String", userInfo.name),
      SqlParameter("String", userInfo.contactNumber),
      SqlParameter("String", userInfo.address.getOrElse("")),
      SqlParameter("String", userInfo.userType.toString),
      SqlParameter("String", userInfo.status.map(_.toString).getOrElse("None")),
      SqlParameter("Double", (userInfo.createTime.getMillis.toDouble/1000).toString)
    )

    writeDB(sql, params).void
  }
}