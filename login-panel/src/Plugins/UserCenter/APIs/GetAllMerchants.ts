/**
 * GetAllMerchants
 * desc: 获取所有商家信息的接口
 * @return allMerchants: UserInfo:1069 (所有身份为商家的用户信息列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetAllMerchants extends TongWenMessage {
    constructor(
        
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

