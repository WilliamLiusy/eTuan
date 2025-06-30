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
 * MerchantAddProductMessage
 * desc: 商家验证身份后，在ProductTable数据库中插入商品信息，并返回操作成功与否的状态
 * @param merchantToken: String (商家的身份令牌，用于验证身份)
 * @param name: String (商品名称)
 * @param price: Double (商品价格)
 * @param description: String (商品描述)
 * @return successOrNot: String (操作成功与否的状态)
 */

case class MerchantAddProductMessage(
  merchantToken: String,
  name: String,
  price: Double,
  description: String
) extends API[String](ProductServiceCode)



case object MerchantAddProductMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[MerchantAddProductMessage] = deriveEncoder
  private val circeDecoder: Decoder[MerchantAddProductMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[MerchantAddProductMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[MerchantAddProductMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[MerchantAddProductMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given merchantAddProductMessageEncoder: Encoder[MerchantAddProductMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given merchantAddProductMessageDecoder: Decoder[MerchantAddProductMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

