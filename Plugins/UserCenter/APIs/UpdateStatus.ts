/**
 * UpdateStatus
 * desc: 更新骑手状态接口
 * @param userToken: String (用户令牌，用于验证用户身份)
 * @param newStatus: RiderStatus:1074 (需要更新的新骑手状态)
 * @return successOrNot: String (操作是否成功，成功返回对应标志)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { RiderStatus } from 'Plugins/UserCenter/Objects/RiderStatus';


export class UpdateStatus extends TongWenMessage {
    constructor(
        public  userToken: string,
        public  newStatus: RiderStatus
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

