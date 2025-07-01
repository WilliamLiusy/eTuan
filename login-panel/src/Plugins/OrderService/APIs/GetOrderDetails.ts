/**
 * GetOrderDetails
 * desc: 根据订单ID获取订单详情
 * @param orderID: String (订单ID，用于唯一标识一个订单。)
 * @return orderInfo: OrderInfo:1067 (订单详情，包括ID、顾客、商家、骑手信息、商品列表等。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetOrderDetails extends TongWenMessage {
    constructor(
        public  orderID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

