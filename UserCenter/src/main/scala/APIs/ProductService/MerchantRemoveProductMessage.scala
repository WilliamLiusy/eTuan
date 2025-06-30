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


/**
 * MerchantRemoveProductMessage
 * desc: 商家验证身份后，在ProductTable中匹配名字为name的商品将其删除，返回操作成功与否
 * @param merchantToken: String (商家身份令牌，用于验证商家身份)
 * @param name: String (商品名称，用于匹配待删除商品)
 * @return successOrNot: String (操作是否成功的结果)
 */

case class MerchantRemoveProductMessage(
  merchantToken: String,
  name: String
) extends API[String](ProductServiceCode)



case object MerchantRemoveProductMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[MerchantRemoveProductMessage] = deriveEncoder
  private val circeDecoder: Decoder[MerchantRemoveProductMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[MerchantRemoveProductMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[MerchantRemoveProductMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[MerchantRemoveProductMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given merchantRemoveProductMessageEncoder: Encoder[MerchantRemoveProductMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given merchantRemoveProductMessageDecoder: Decoder[MerchantRemoveProductMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

