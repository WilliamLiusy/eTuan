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
import Objects.UserCenter.RiderStatus

/**
 * UpdateStatus
 * desc: 更新骑手状态接口
 * @param userToken: String (用户令牌，用于验证用户身份)
 * @param newStatus: RiderStatus:1074 (需要更新的新骑手状态)
 * @return successOrNot: String (操作是否成功，成功返回对应标志)
 */

case class UpdateStatus(
  userToken: String,
  newStatus: RiderStatus
) extends API[String](UserCenterCode)



case object UpdateStatus{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateStatus] = deriveEncoder
  private val circeDecoder: Decoder[UpdateStatus] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateStatus] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateStatus] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateStatus]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateStatusEncoder: Encoder[UpdateStatus] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateStatusDecoder: Decoder[UpdateStatus] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

