package Impl


import Utils.UserInfoProcess.generateUserToken
import Objects.UserCenter.UserType
import Objects.UserCenter.UserInfo
import Objects.UserCenter.RiderStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI.*
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserCenter.RiderStatus
import cats.implicits.*

import java.security.MessageDigest

case class UserRegisterPlanner(
  name: String,  // 用户名
  contactNumber: String,  // 联系电话
  password: String,  // 密码
  userType: UserType,  // 用户角色类型
  address: String,  // 地址
  override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 检查用户名是否重复
      _ <- IO(logger.info("Step 1: 检查用户名是否重复"))
      _ <- checkUsernameExists()

      // Step 2: 检查商家地址是否提供
      _ <- IO(logger.info("Step 2: 检查商家是否提供了地址"))
      _ <- checkMerchantAddress()

      // Step 3: 生成新用户ID
      _ <- IO(logger.info("Step 3: 生成新的用户ID"))
      userID <- generateUserID()

      // Step 4: 保存用户加密后的密码信息到 UserPasswordTable
      _ <- IO(logger.info("Step 4: 保存密码信息到 UserPasswordTable"))
      _ <- saveUserPassword(userID)

      // Step 5: 保存用户信息到 UserInfoTable
      _ <- IO(logger.info("Step 5: 保存用户信息到 UserInfoTable"))
      _ <- saveUserInfo(userID)

      // Step 6: 生成用户令牌并保存到 UserSessionTable
      _ <- IO(logger.info("Step 6: 生成用户令牌并更新至 UserSessionTable"))
      userToken <- generateUserToken(userID)

      // Step 7: 返回用户令牌
      _ <- IO(logger.info(s"Step 7: 返回用户令牌: ${userToken}"))
    } yield userToken
  }

  // 检查用户名是否已存在
  private def checkUsernameExists()(using PlanContext): IO[Unit] = {
    val sql =
      s"""
SELECT 1
FROM ${schemaName}.user_info_table
WHERE name = ?
         """.stripMargin
    readDBJsonOptional(sql, List(SqlParameter("String", name))).flatMap {
      case Some(_) =>
        IO.raiseError(new IllegalArgumentException("用户名重复"))
      case None => IO(logger.info("用户名未重复"))
    }
  }

  // 如果用户是商家，检查地址是否为空
  private def checkMerchantAddress()(using PlanContext): IO[Unit] = {
    if (userType == UserType.Merchant && address.trim.isEmpty)
      IO.raiseError(new IllegalArgumentException("商家必须填写地址"))
    else IO(logger.info("商家地址校验通过"))
  }

  // 生成新用户的唯一ID
  private def generateUserID()(using PlanContext): IO[String] = {
    IO(java.util.UUID.randomUUID().toString).flatTap(userID => IO(logger.info(s"生成用户ID: ${userID}")))
  }

  // 保存用户的加密密码到 UserPasswordTable
  private def saveUserPassword(userID: String)(using PlanContext): IO[Unit] = {
    val hashedPassword = MessageDigest.getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
    val sql =
      s"""
INSERT INTO ${schemaName}.user_password_table (user_id, password)
VALUES (?, ?)
         """.stripMargin
    writeDB(sql, List(SqlParameter("String", userID), SqlParameter("String", hashedPassword))).void.flatMap { _ =>
      IO(logger.info("用户密码保存成功"))
    }
  }

  // 保存用户的基础信息到 UserInfoTable
  private def saveUserInfo(userID: String)(using PlanContext): IO[Unit] = {
    val createTime = DateTime.now
    val status = Some(RiderStatus.OffDuty)
    val userInfo = UserInfo(
      userID = userID,
      name = name,
      contactNumber = contactNumber,
      userType = userType,
      address = if (address.trim.isEmpty) None else Some(address),
      status = status,
      createTime = createTime
    )
    val sql =
      s"""
INSERT INTO ${schemaName}.user_info_table (user_id, name, contact_number, address, user_type, status, create_time)
VALUES (?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?))
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("String", userInfo.userID),
      SqlParameter("String", userInfo.name),
      SqlParameter("String", userInfo.contactNumber),
      SqlParameter("String", userInfo.address.getOrElse("None")),
      SqlParameter("String", userInfo.userType.toString),
      SqlParameter("String", userInfo.status.map(_.toString).getOrElse("None")),
      SqlParameter("Double", (createTime.getMillis.toDouble / 1000).toString)
    )).void.flatMap { _ =>
      IO(logger.info("用户基本信息保存成功"))
    }
  }
}