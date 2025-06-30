
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
            /** 订单表，包含订单的基本信息
       * order_id: 订单ID
       * customer_id: 顾客ID
       * merchant_id: 商家ID
       * rider_id: 骑手ID（可为空）
       * product_list: 商品信息列表
       * destination_address: 送达地址
       * order_status: 当前订单状态 (WaitingForDish/WaitingForDelivery/Delivering/Completed)
       * order_time: 订单创建时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."order_table" (
            order_id VARCHAR NOT NULL PRIMARY KEY,
            customer_id TEXT NOT NULL,
            merchant_id TEXT NOT NULL,
            rider_id TEXT,
            product_list TEXT NOT NULL,
            destination_address TEXT NOT NULL,
            order_status TEXT NOT NULL,
            order_time TIMESTAMP NOT NULL
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
    