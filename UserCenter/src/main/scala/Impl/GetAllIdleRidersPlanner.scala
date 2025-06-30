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
import io.circe.Json
import org.joda.time.DateTime
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
import Objects.UserCenter.RiderStatus
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class GetAllIdleRidersPlanner(
                                    override val planContext: PlanContext
                                  ) extends Planner[List[UserInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[UserInfo]] = {
    for {
      // Step 1: 查询所有处于空闲状态的骑手
      _ <- IO(logger.info(s"开始查询处于空闲状态的骑手信息"))
      idleRidersJson <- queryIdleRiders()

      // Step 2: 将查询结果由Json转换为 List[UserInfo]
      _ <- IO(logger.info(s"将查询结果转换为UserInfo对象列表"))
      idleRiders <- convertToUserInfoList(idleRidersJson)

      _ <- IO(logger.info(s"成功转换为UserInfo列表，空闲骑手总数为：${idleRiders.size}"))
    } yield idleRiders
  }

  /**
   * 查询UserInfoTable中的所有身份为Rider且状态为Idle的记录
   */
  private def queryIdleRiders()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         SELECT *
         FROM ${schemaName}.user_info_table
         WHERE user_type = ? AND status = ?;
       """
    val params = List(
      SqlParameter("String", UserType.Rider.toString), // 枚举值用 .toString 写入SQL参数
      SqlParameter("String", RiderStatus.Idle.toString)
    )
    logger.info(s"SQL语句为: $sql, 参数为: ${params.map(_.value).mkString(", ")}")
    readDBRows(sql, params)
  }

  /**
   * 将查询结果 List[Json] 转换为 List[UserInfo]
   */
  private def convertToUserInfoList(jsonList: List[Json]): IO[List[UserInfo]] = {
    IO {
      jsonList.map { json =>
        try {
          decodeType[UserInfo](json)
        } catch {
          case ex: Exception =>
            logger.error(s"解析UserInfo失败, Json: ${json.noSpaces}, 错误信息: ${ex.getMessage}")
            throw ex
        }
      }
    }
  }
}