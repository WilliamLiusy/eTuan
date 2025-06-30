package Objects.UserCenter

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[UserTypeSerializer])
@JsonDeserialize(`using` = classOf[UserTypeDeserializer])
enum UserType(val desc: String):

  override def toString: String = this.desc

  case Customer extends UserType("顾客") // 顾客
  case Merchant extends UserType("商家") // 商家
  case Rider extends UserType("骑手") // 骑手


object UserType:
  given encode: Encoder[UserType] = Encoder.encodeString.contramap[UserType](toString)

  given decode: Decoder[UserType] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):UserType  = s match
    case "顾客" => Customer
    case "商家" => Merchant
    case "骑手" => Rider
    case _ => throw Exception(s"Unknown UserType: $s")

  def fromStringEither(s: String):Either[String, UserType]  = s match
    case "顾客" => Right(Customer)
    case "商家" => Right(Merchant)
    case "骑手" => Right(Rider)
    case _ => Left(s"Unknown UserType: $s")

  def toString(t: UserType): String = t match
    case Customer => "顾客"
    case Merchant => "商家"
    case Rider => "骑手"


// Jackson 序列化器
class UserTypeSerializer extends JsonSerializer[UserType] {
  override def serialize(value: UserType, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(UserType.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class UserTypeDeserializer extends JsonDeserializer[UserType] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): UserType = {
    UserType.fromString(p.getText)
  }
}

