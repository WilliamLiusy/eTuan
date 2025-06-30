/**
 * UpdateOrderStatus
 * desc: 根据订单ID更新订单状态的接口
 * @param orderID: String (订单ID，用于标识具体订单)
 * @param newStatus: OrderStatus:1070 (订单新的状态，例如等待配送或已完成)
 * @return successOrNot: String (描述更新操作是否成功)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { OrderStatus } from 'Plugins/OrderService/Objects/OrderStatus';


export class UpdateOrderStatus extends TongWenMessage {
    constructor(
        public  orderID: string,
        public  newStatus: OrderStatus
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

