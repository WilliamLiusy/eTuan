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
 * GetOrderDetails
 * desc: 根据订单ID获取订单详情
 * @param orderID: String (订单ID，用于唯一标识一个订单。)
 * @return orderInfo: OrderInfo:1067 (订单详情，包括ID、顾客、商家、骑手信息、商品列表等。)
 */

case class GetOrderDetails(
  orderID: String
) extends API[OrderInfo](OrderServiceCode)



case object GetOrderDetails{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GetOrderDetails] = deriveEncoder
  private val circeDecoder: Decoder[GetOrderDetails] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GetOrderDetails] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GetOrderDetails] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GetOrderDetails]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given getOrderDetailsEncoder: Encoder[GetOrderDetails] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given getOrderDetailsDecoder: Decoder[GetOrderDetails] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

