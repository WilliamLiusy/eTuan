package APIs.ProductService

import Common.API.API
import Global.ServiceCenter.ProductServiceCode

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
 * FetchProductsByMerchantIDMessage
 * desc: 根据商家ID筛选商品列表
 * @param merchantID: String (商家ID，用于筛选对应商家的商品)
 * @return products: ProductInfo:1065 (筛选出的商品列表，包含所有匹配的商品信息)
 */

case class FetchProductsByMerchantIDMessage(
  merchantID: String
) extends API[List[ProductInfo]](ProductServiceCode)



case object FetchProductsByMerchantIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[FetchProductsByMerchantIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[FetchProductsByMerchantIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FetchProductsByMerchantIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FetchProductsByMerchantIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FetchProductsByMerchantIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given fetchProductsByMerchantIDMessageEncoder: Encoder[FetchProductsByMerchantIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given fetchProductsByMerchantIDMessageDecoder: Decoder[FetchProductsByMerchantIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

