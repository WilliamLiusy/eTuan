
package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils.schemaName
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID
import Global.DBConfig
import Process.ProcessUtils.server2DB
import Global.GlobalVariables

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 存储用户的加密密码信息
       * user_id: 用户的唯一ID
       * password: 加密后的密码
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."user_password_table" (
            user_id VARCHAR NOT NULL PRIMARY KEY,
            password TEXT NOT NULL
        );
         
        """,
        List()
      )
      /** 用户信息表，包含用户的基本属性和状态
       * user_id: 用户ID
       * name: 用户名
       * contact_number: 联系方式
       * address: 地址，商家必填，其他选填（None）
       * user_type: 用户角色类型（Customer/Merchant/Rider）
       * status: 状态，骑手默认设置为Idle，其他角色为None
       * create_time: 用户注册时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."user_info_table" (
            user_id VARCHAR NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            contact_number TEXT NOT NULL,
            address TEXT,
            user_type TEXT NOT NULL,
            status TEXT DEFAULT 'None',
            create_time TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 用户会话表，记录用户令牌及其生成和过期时间，支持用户登录验证。
       * user_id: 用户的唯一ID，作为外键关联其他表
       * user_token: 用户令牌，用于用户身份验证
       * generate_time: 令牌生成时间
       * expire_time: 令牌过期时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."user_session_table" (
            user_id VARCHAR NOT NULL PRIMARY KEY,
            user_token TEXT NOT NULL,
            generate_time TIMESTAMP NOT NULL,
            expire_time TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    