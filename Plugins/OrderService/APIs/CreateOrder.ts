/**
 * CreateOrder
 * desc: 创建订单接口
 * @param customerToken: String (顾客令牌，用于验证顾客身份)
 * @param merchantID: String (商家ID，表示订单的商家来源)
 * @param productList: ProductInfo:1065 (商品信息列表，包含订单中的所有商品)
 * @param destinationAddress: String (送达地址，顾客订单的目标地址)
 * @return orderID: String (生成的订单ID，是订单的唯一标识符)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ProductInfo } from 'Plugins/ProductService/Objects/ProductInfo';


export class CreateOrder extends TongWenMessage {
    constructor(
        public  customerToken: string,
        public  merchantID: string,
        public  productList: ProductInfo[],
        public  destinationAddress: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

