/**
 * UpdateRider
 * desc: 根据订单ID更新订单的配送骑手为新的骑手ID
 * @param orderID: String (需要更新骑手ID的订单ID)
 * @param newRider: String (新的骑手ID)
 * @return successOrNot: String (状态更新成功与否)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class UpdateRider extends TongWenMessage {
    constructor(
        public  orderID: string,
        public  newRider: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

