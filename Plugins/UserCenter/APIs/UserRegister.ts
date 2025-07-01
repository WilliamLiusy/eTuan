/**
 * UserRegister
 * desc: 用户注册接口，用于创建新的用户账号并返回用户令牌
 * @param name: String (用户名)
 * @param contactNumber: String (联系方式)
 * @param password: String (密码)
 * @param userType: UserType (用户类型 (Customer/Merchant/Rider))
 * @param address: String (地址)
 * @return userToken: String (用户令牌)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { UserType } from 'Plugins/UserCenter/Objects/UserType';


export class UserRegister extends TongWenMessage {
    constructor(
        public  name: string,
        public  contactNumber: string,
        public  password: string,
        public  userType: UserType,
        public  address: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

