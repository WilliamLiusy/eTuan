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
import Objects.ProductService.ProductInfo

/**
 * CreateOrder
 * desc: 创建订单接口
 * @param customerToken: String (顾客令牌，用于验证顾客身份)
 * @param merchantID: String (商家ID，表示订单的商家来源)
 * @param productList: ProductInfo:1065 (商品信息列表，包含订单中的所有商品)
 * @param destinationAddress: String (送达地址，顾客订单的目标地址)
 * @return orderID: String (生成的订单ID，是订单的唯一标识符)
 */

case class CreateOrder(
  customerToken: String,
  merchantID: String,
  productList: List[ProductInfo],
  destinationAddress: String
) extends API[String](OrderServiceCode)



case object CreateOrder{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CreateOrder] = deriveEncoder
  private val circeDecoder: Decoder[CreateOrder] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CreateOrder] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CreateOrder] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CreateOrder]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given createOrderEncoder: Encoder[CreateOrder] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given createOrderDecoder: Decoder[CreateOrder] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

