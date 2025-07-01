/**
 * UserInfo
 * desc: 用户信息，包括基础信息和角色属性
 * @param userID: String (用户ID)
 * @param name: String (用户名)
 * @param contactNumber: String (联系方式)
 * @param userType: UserType (用户角色类型)
 * @param address: String (地址，商家必填，其他选填（None）)
 * @param status: RiderStatus:1074 (状态，骑手默认设置为Idle，其他角色为None)
 * @param createTime: DateTime (用户创建时间)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { UserType } from 'Plugins/UserCenter/Objects/UserType';
import { RiderStatus } from 'Plugins/UserCenter/Objects/RiderStatus';


export class UserInfo extends Serializable {
    constructor(
        public  userID: string,
        public  name: string,
        public  contactNumber: string,
        public  userType: UserType,
        public  address: string | null,
        public  status: RiderStatus | null,
        public  createTime: number
    ) {
        super()
    }
}


