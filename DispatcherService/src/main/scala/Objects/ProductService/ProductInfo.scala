package Objects.ProductService


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
 * ProductInfo
 * desc: 商品信息
 * @param productID: String (商品的唯一ID)
 * @param merchantID: String (商家的唯一ID)
 * @param name: String (商品名称)
 * @param price: Double (商品价格)
 * @param description: String (商品描述)
 */

case class ProductInfo(
  productID: String,
  merchantID: String,
  name: String,
  price: Double,
  description: String
){

  //process class code 预留标志位，不要删除


}


case object ProductInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ProductInfo] = deriveEncoder
  private val circeDecoder: Decoder[ProductInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ProductInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ProductInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ProductInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given productInfoEncoder: Encoder[ProductInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given productInfoDecoder: Decoder[ProductInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


  implicit val optionProductInfoDecoder: Decoder[Option[ProductInfo]] =
    Decoder[List[ProductInfo]].map(_.headOption)

  //process object code 预留标志位，不要删除


}

