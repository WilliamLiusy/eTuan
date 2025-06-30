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
 * FetchProductsByNameAndMerchantIDMessage
 * desc: 根据商家ID和商品名称筛选商品，返回匹配的商品信息。
 * @param merchantID: String (商家ID，用于找到属于该商家的商品。)
 * @param name: String (商品名称，用于匹配特定商品。)
 * @return product: ProductInfo:1065 (匹配的商品信息。)
 */

case class FetchProductsByNameAndMerchantIDMessage(
  merchantID: String,
  name: String
) extends API[Option[ProductInfo]](ProductServiceCode)



case object FetchProductsByNameAndMerchantIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[FetchProductsByNameAndMerchantIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[FetchProductsByNameAndMerchantIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FetchProductsByNameAndMerchantIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FetchProductsByNameAndMerchantIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FetchProductsByNameAndMerchantIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given fetchProductsByNameAndMerchantIDMessageEncoder: Encoder[FetchProductsByNameAndMerchantIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given fetchProductsByNameAndMerchantIDMessageDecoder: Decoder[FetchProductsByNameAndMerchantIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

