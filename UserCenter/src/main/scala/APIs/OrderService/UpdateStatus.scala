package APIs.OrderService

import Common.API.API
import Global.ServiceCenter.OrderServiceCode

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID
import Objects.OrderService.OrderStatus

/**
 * UpdateStatus
 * desc: 更新订单状态的接口，验证用户身份后更新订单状态。
 * @param userToken: String (用户令牌，用于验证用户身份。)
 * @param orderID: String (订单ID，用于标识当前需要更新状态的订单。)
 * @param newStatus: OrderStatus:1070 (订单的新状态，如WaitingForDish、Delivering等。)
 * @return successOrNot: String (操作成功与否的信息，返回给调用方。)
 */

case class UpdateStatus(
  userToken: String,
  orderID: String,
  newStatus: OrderStatus
) extends API[String](OrderServiceCode)



case object UpdateStatus{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateStatus] = deriveEncoder
  private val circeDecoder: Decoder[UpdateStatus] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateStatus] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateStatus] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateStatus]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateStatusEncoder: Encoder[UpdateStatus] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateStatusDecoder: Decoder[UpdateStatus] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

