/**
 * QueryOrdersByUser
 * desc: 通过用户令牌，查询用户所有的订单列表
 * @param userToken: String (用户令牌，用于标识并验证当前访问的用户身份)
 * @return orderList: OrderInfo:1067 (用户订单列表，包含订单的详细信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryOrdersByUser extends TongWenMessage {
    constructor(
        public  userToken: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

