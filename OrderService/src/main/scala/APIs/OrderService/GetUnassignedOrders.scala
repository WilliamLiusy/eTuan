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
import Objects.OrderService.OrderInfo

/**
 * GetUnassignedOrders
 * desc: 查询所有状态为WaitingForAssign的订单并返回
 * @return orderList: OrderInfo:1067 (订单列表，包含所有状态为WaitingForAssign的订单)
 */

case class GetUnassignedOrders(
  
) extends API[List[OrderInfo]](OrderServiceCode)



case object GetUnassignedOrders{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GetUnassignedOrders] = deriveEncoder
  private val circeDecoder: Decoder[GetUnassignedOrders] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GetUnassignedOrders] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GetUnassignedOrders] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GetUnassignedOrders]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given getUnassignedOrdersEncoder: Encoder[GetUnassignedOrders] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given getUnassignedOrdersDecoder: Decoder[GetUnassignedOrders] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

