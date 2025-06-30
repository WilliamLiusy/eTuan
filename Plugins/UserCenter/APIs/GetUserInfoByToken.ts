/**
 * GetUserInfoByToken
 * desc: 根据用户令牌获取用户信息，支持顾客、商家、骑手账号的统一处理
 * @param userToken: String (用户令牌，用于标识和验证用户身份的信息)
 * @return userInfo: UserInfo:1069 (用户信息，包括用户ID、用户名、联系方式、用户角色、地址和状态等)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class GetUserInfoByToken extends TongWenMessage {
    constructor(
        public  userToken: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

