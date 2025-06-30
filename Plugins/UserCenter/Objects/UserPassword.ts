/**
 * UserPassword
 * desc: 用户密码信息
 * @param userID: String (用户ID)
 * @param password: String (加密后的密码)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UserPassword extends Serializable {
    constructor(
        public  userID: string,
        public  password: string
    ) {
        super()
    }
}


