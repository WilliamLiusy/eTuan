/**
 * GetAllIdleRiders
 * desc: 获取所有处于空闲状态的骑手信息接口
 * @return allIdleRiders: UserInfo:1069 (包含所有空闲状态骑手的用户信息列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetAllIdleRiders extends TongWenMessage {
    constructor(
        
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

