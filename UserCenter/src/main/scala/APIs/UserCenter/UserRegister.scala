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
import Objects.UserCenter.UserType

/**
 * UserRegister
 * desc: 用户注册接口，用于创建新的用户账号并返回用户令牌
 * @param name: String (用户名)
 * @param contactNumber: String (联系方式)
 * @param password: String (密码)
 * @param userType: UserType (用户类型 (Customer/Merchant/Rider))
 * @param address: String (地址)
 * @return userToken: String (用户令牌)
 */

case class UserRegister(
  name: String,
  contactNumber: String,
  password: String,
  userType: UserType,
  address: String
) extends API[String](UserCenterCode)



case object UserRegister{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UserRegister] = deriveEncoder
  private val circeDecoder: Decoder[UserRegister] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UserRegister] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UserRegister] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UserRegister]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given userRegisterEncoder: Encoder[UserRegister] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given userRegisterDecoder: Decoder[UserRegister] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

