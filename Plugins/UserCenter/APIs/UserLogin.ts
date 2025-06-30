/**
 * UserLogin
 * desc: 用户登录接口，用于验证用户名和密码，如果匹配则生成新的用户令牌
 * @param name: String (用户名，用于识别用户的唯一标识之一)
 * @param password: String (用户密码，加密存储，用于身份验证)
 * @return userToken: String (登录成功后生成的用户令牌，代表用户会话)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class UserLogin extends TongWenMessage {
    constructor(
        public  name: string,
        public  password: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

