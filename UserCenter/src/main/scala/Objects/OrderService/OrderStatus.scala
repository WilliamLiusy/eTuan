package Objects.OrderService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[OrderStatusSerializer])
@JsonDeserialize(`using` = classOf[OrderStatusDeserializer])
enum OrderStatus(val desc: String):

  override def toString: String = this.desc

  case WaitingForDish extends OrderStatus("等待出餐") // 等待出餐
  case WaitingForAssign extends OrderStatus("等待分配骑手") // 等待分配骑手
  case Delivering extends OrderStatus("正在配送") // 正在配送
  case Completed extends OrderStatus("已完成") // 已完成


object OrderStatus:
  given encode: Encoder[OrderStatus] = Encoder.encodeString.contramap[OrderStatus](toString)

  given decode: Decoder[OrderStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):OrderStatus  = s match
    case "等待出餐" => WaitingForDish
    case "等待分配骑手" => WaitingForAssign
    case "正在配送" => Delivering
    case "已完成" => Completed
    case _ => throw Exception(s"Unknown OrderStatus: $s")

  def fromStringEither(s: String):Either[String, OrderStatus]  = s match
    case "等待出餐" => Right(WaitingForDish)
    case "等待分配骑手" => Right(WaitingForAssign)
    case "正在配送" => Right(Delivering)
    case "已完成" => Right(Completed)
    case _ => Left(s"Unknown OrderStatus: $s")

  def toString(t: OrderStatus): String = t match
    case WaitingForDish => "等待出餐"
    case WaitingForAssign => "等待分配骑手"
    case Delivering => "正在配送"
    case Completed => "已完成"


// Jackson 序列化器
class OrderStatusSerializer extends JsonSerializer[OrderStatus] {
  override def serialize(value: OrderStatus, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(OrderStatus.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class OrderStatusDeserializer extends JsonDeserializer[OrderStatus] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): OrderStatus = {
    OrderStatus.fromString(p.getText)
  }
}

