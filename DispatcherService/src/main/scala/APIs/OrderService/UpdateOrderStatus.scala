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
 * UpdateOrderStatus
 * desc: 根据订单ID更新订单状态的接口
 * @param orderID: String (订单ID，用于标识具体订单)
 * @param newStatus: OrderStatus:1070 (订单新的状态，例如等待配送或已完成)
 * @return successOrNot: String (描述更新操作是否成功)
 */

case class UpdateOrderStatus(
  orderID: String,
  newStatus: OrderStatus
) extends API[String](OrderServiceCode)



case object UpdateOrderStatus{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateOrderStatus] = deriveEncoder
  private val circeDecoder: Decoder[UpdateOrderStatus] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateOrderStatus] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateOrderStatus] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateOrderStatus]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateOrderStatusEncoder: Encoder[UpdateOrderStatus] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateOrderStatusDecoder: Decoder[UpdateOrderStatus] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

