package Objects.UserCenter


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
import Objects.UserCenter.UserType
import Objects.UserCenter.RiderStatus

/**
 * UserInfo
 * desc: 用户信息，包括基础信息和角色属性
 * @param userID: String (用户ID)
 * @param name: String (用户名)
 * @param contactNumber: String (联系方式)
 * @param userType: UserType (用户角色类型)
 * @param address: String (地址，商家必填，其他选填（None）)
 * @param status: RiderStatus:1074 (状态，骑手默认设置为Idle，其他角色为None)
 * @param createTime: DateTime (用户创建时间)
 */

case class UserInfo(
  userID: String,
  name: String,
  contactNumber: String,
  userType: UserType,
  address: Option[String] = None,
  status: Option[RiderStatus] = None,
  createTime: DateTime
){

  //process class code 预留标志位，不要删除


}


case object UserInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UserInfo] = deriveEncoder
  private val circeDecoder: Decoder[UserInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UserInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UserInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given userInfoEncoder: Encoder[UserInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given userInfoDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

