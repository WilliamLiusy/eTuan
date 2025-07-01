package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.UserInfoProcess.generateUserToken
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
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
import Utils.UserInfoProcess.generateUserToken
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

import java.security.MessageDigest

//case class UserLoginMessage(name: String, password: String)

case class UserLoginPlanner(name: String, password: String, override val planContext: PlanContext)
  extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"收到用户登录请求，用户名: ${name}"))

      // Step 1: Validate input parameters
      _ <- validateInput()

      // Step 2: Retrieve userID from UserInfoTable
      userID <- getUserIDByName(name)

      // Step 3: Validate password from UserPasswordTable
      _ <- validatePassword(userID, password)

      // Step 4.1: Generate user token
      userToken <- generateUserToken(userID)

    } yield userToken
  }

  private def validateInput()(using PlanContext): IO[Unit] = {
    if (name.trim.isEmpty)
      IO.raiseError(new IllegalArgumentException("用户名不能为空"))
    else if (password.trim.isEmpty)
      IO.raiseError(new IllegalArgumentException("密码不能为空"))
    else IO.unit
  }

  private def getUserIDByName(name: String)(using PlanContext): IO[String] = {
    val sql = s"SELECT user_id FROM ${schemaName}.user_info_table WHERE name = ?"
    val params = List(SqlParameter("String", name))

    for {
      resultOpt <- readDBJsonOptional(sql, params)
      userID <- resultOpt match {
        case Some(json) => IO(decodeField[String](json, "user_id"))
        case None =>
          IO.raiseError(new IllegalArgumentException("身份验证失败：用户名或密码不正确"))
      }
    } yield userID
  }

  private def validatePassword(userID: String, inputPassword: String)(using PlanContext): IO[Unit] = {
    // Fetch encrypted password from DB
    val sql = s"SELECT password FROM ${schemaName}.user_password_table WHERE user_id = ?"
    val params = List(SqlParameter("String", userID))
    val encryptedInput = MessageDigest.getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
    for {
      encryptedPassword <- readDBString(sql, params)
      // Verify with encrypted password (assuming bcrypt for hashing)
      isPasswordMatch <- IO(java.util.Objects.equals(encryptedPassword, encryptedInput)) // Replace this with a secure comparison logic
//      _ <- IO(logger.info(s"encrypted: ${encryptedPassword}, input: ${inputPassword}"))
      _ <- if (!isPasswordMatch)
        IO.raiseError(new IllegalArgumentException("身份验证失败：用户名或密码不正确"))
      else IO.unit
    } yield ()
  }
}