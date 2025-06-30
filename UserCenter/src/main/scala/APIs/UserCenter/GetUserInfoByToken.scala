package APIs.UserCenter

import Common.API.API
import Global.ServiceCenter.UserCenterCode

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
import Objects.UserCenter.UserInfo

/**
 * GetUserInfoByToken
 * desc: 根据用户令牌获取用户信息，支持顾客、商家、骑手账号的统一处理
 * @param userToken: String (用户令牌，用于标识和验证用户身份的信息)
 * @return userInfo: UserInfo:1069 (用户信息，包括用户ID、用户名、联系方式、用户角色、地址和状态等)
 */

case class GetUserInfoByToken(
  userToken: String
) extends API[UserInfo](UserCenterCode)



case object GetUserInfoByToken{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GetUserInfoByToken] = deriveEncoder
  private val circeDecoder: Decoder[GetUserInfoByToken] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GetUserInfoByToken] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GetUserInfoByToken] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GetUserInfoByToken]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given getUserInfoByTokenEncoder: Encoder[GetUserInfoByToken] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given getUserInfoByTokenDecoder: Decoder[GetUserInfoByToken] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

