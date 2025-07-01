/**
 * UpdateStatus
 * desc: 更新订单状态的接口，验证用户身份后更新订单状态。
 * @param userToken: String (用户令牌，用于验证用户身份。)
 * @param orderID: String (订单ID，用于标识当前需要更新状态的订单。)
 * @param newStatus: OrderStatus:1070 (订单的新状态，如WaitingForDish、Delivering等。)
 * @return successOrNot: String (操作成功与否的信息，返回给调用方。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { OrderStatus } from 'Plugins/OrderService/Objects/OrderStatus';


export class UpdateStatus extends TongWenMessage {
    constructor(
        public  userToken: string,
        public  orderID: string,
        public  newStatus: OrderStatus
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

