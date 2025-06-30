package Objects.UserCenter

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[RiderStatusSerializer])
@JsonDeserialize(`using` = classOf[RiderStatusDeserializer])
enum RiderStatus(val desc: String):

  override def toString: String = this.desc

  case Idle extends RiderStatus("空闲") // 空闲
  case Delivering extends RiderStatus("配送中") // 配送中
  case OffDuty extends RiderStatus("下班") // 下班


object RiderStatus:
  given encode: Encoder[RiderStatus] = Encoder.encodeString.contramap[RiderStatus](toString)

  given decode: Decoder[RiderStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):RiderStatus  = s match
    case "空闲" => Idle
    case "配送中" => Delivering
    case "下班" => OffDuty
    case _ => throw Exception(s"Unknown RiderStatus: $s")

  def fromStringEither(s: String):Either[String, RiderStatus]  = s match
    case "空闲" => Right(Idle)
    case "配送中" => Right(Delivering)
    case "下班" => Right(OffDuty)
    case _ => Left(s"Unknown RiderStatus: $s")

  def toString(t: RiderStatus): String = t match
    case Idle => "空闲"
    case Delivering => "配送中"
    case OffDuty => "下班"


// Jackson 序列化器
class RiderStatusSerializer extends JsonSerializer[RiderStatus] {
  override def serialize(value: RiderStatus, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(RiderStatus.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class RiderStatusDeserializer extends JsonDeserializer[RiderStatus] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): RiderStatus = {
    RiderStatus.fromString(p.getText)
  }
}

