package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.PlanContext
import Common.DBAPI.{decodeField, readDBJsonOptional}
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.API.{PlanContext}

case object UserInfoProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def validateUserToken(userToken: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
    logger.info(s"[validateUserToken] 开始验证用户令牌的有效性：userToken=${userToken}")
  
    val query =
      s"""
         SELECT user_id, expire_time
         FROM ${schemaName}.user_session_table
         WHERE user_token = ?
       """
    val params = List(SqlParameter("String", userToken))
  
    for {
      // 查询数据库是否存在满足条件的数据
      userSessionOpt <- readDBJsonOptional(query, params)
      userID <- userSessionOpt match {
        case Some(json) =>
          val userID = decodeField[String](json, "user_id")
          val expireTime = decodeField[DateTime](json, "expire_time")
  
          logger.info(s"[validateUserToken] 找到记录：userID=${userID}, expireTime=${expireTime}")
  
          if (expireTime.isBeforeNow) {
            val errorMessage = s"[validateUserToken] 令牌已过期：userToken=${userToken}"
            logger.error(errorMessage)
            IO.raiseError(new Exception(errorMessage))
          } else {
            logger.info(s"[validateUserToken] 令牌未过期：expireTime=${expireTime}")
            IO.pure(userID)
          }
  
        case None =>
          val errorMessage = s"[validateUserToken] 未找到有效的令牌记录：userToken=${userToken}"
          logger.error(errorMessage)
          IO.raiseError(new Exception(errorMessage))
      }
    } yield {
      logger.info(s"[validateUserToken] 验证成功，返回userID=${userID}")
      userID
    }
  }
  
  
  def generateUserToken(userID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("generateUserToken")  // 同文后端处理: logger 统一
  
    logger.info(s"开始为用户 ${userID} 生成新的 userToken")
    
    val newUserToken = java.util.UUID.randomUUID().toString
    val generateTime = DateTime.now
    val expireTime = generateTime.plusHours(1) // Token 过期时间为1小时
  
    val checkSql = s"""
      SELECT user_token
      FROM ${schemaName}.user_session_table
      WHERE user_id = ?
    """.stripMargin
    val checkParams = List(SqlParameter("String", userID))
  
    val insertSql = s"""
      INSERT INTO ${schemaName}.user_session_table (user_id, user_token, generate_time, expire_time)
      VALUES (?, ?, ?, ?)
    """.stripMargin
    val insertParams = List(
      SqlParameter("String", userID),
      SqlParameter("String", newUserToken),
      SqlParameter("Long", generateTime.getMillis.toString),
      SqlParameter("Long", expireTime.getMillis.toString)
    )
  
    val updateSql = s"""
      UPDATE ${schemaName}.user_session_table
      SET user_token = ?, generate_time = ?, expire_time = ?
      WHERE user_id = ?
    """.stripMargin
    val updateParams = List(
      SqlParameter("String", newUserToken),
      SqlParameter("Long", generateTime.getMillis.toString),
      SqlParameter("Long", expireTime.getMillis.toString),
      SqlParameter("String", userID)
    )
  
    for {
      _ <- IO(logger.info(s"检查数据库中是否存在 userID=${userID} 的记录"))
      userExistsOpt <- readDBJsonOptional(checkSql, checkParams)
  
      _ <- userExistsOpt match {
        case Some(_) =>
          IO(logger.info(s"找到已存在 userID=${userID} 的记录，更新 userToken")) >>
          writeDB(updateSql, updateParams) >> 
          IO(logger.info(s"更新成功: userToken=${newUserToken}"))
        case None =>
          IO(logger.info(s"未找到 userID=${userID} 的记录，插入新 userToken")) >>
          writeDB(insertSql, insertParams) >>
          IO(logger.info(s"插入成功: userToken=${newUserToken}"))
      }
  
      _ <- IO(logger.info(s"成功为用户 ${userID} 生成的 userToken: ${newUserToken}"))
    } yield newUserToken
  }
}
