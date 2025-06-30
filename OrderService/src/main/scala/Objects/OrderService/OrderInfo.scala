package Objects.OrderService


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
import Objects.OrderService.OrderStatus

/**
 * OrderInfo
 * desc: 订单信息，包含订单的基础数据和状态
 * @param orderID: String (订单ID)
 * @param customerID: String (顾客ID)
 * @param merchantID: String (商家ID)
 * @param riderID: String (骑手ID（空表示未分配）)
 * @param productList: ProductInfo (商品信息列表)
 * @param destinationAddress: String (送达地址)
 * @param orderStatus: OrderStatus:1070 (当前订单状态)
 * @param orderTime: DateTime (订单创建时间)
 */

case class OrderInfo(
  orderID: String,
  customerID: String,
  merchantID: String,
  riderID: Option[String] = None,
  productList: List[ProductInfo],
  destinationAddress: String,
  orderStatus: OrderStatus,
  orderTime: DateTime
){

  //process class code 预留标志位，不要删除


}


case object OrderInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[OrderInfo] = deriveEncoder
  private val circeDecoder: Decoder[OrderInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[OrderInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[OrderInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[OrderInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given orderInfoEncoder: Encoder[OrderInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given orderInfoDecoder: Decoder[OrderInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

