/**
 * GetUnassignedOrders
 * desc: 查询所有状态为WaitingForAssign的订单并返回
 * @return orderList: OrderInfo:1067 (订单列表，包含所有状态为WaitingForAssign的订单)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetUnassignedOrders extends TongWenMessage {
    constructor(
        
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

