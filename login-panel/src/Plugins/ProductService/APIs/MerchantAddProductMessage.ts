/**
 * MerchantAddProductMessage
 * desc: 商家验证身份后，在ProductTable数据库中插入商品信息，并返回操作成功与否的状态
 * @param merchantToken: String (商家的身份令牌，用于验证身份)
 * @param name: String (商品名称)
 * @param price: Double (商品价格)
 * @param description: String (商品描述)
 * @return successOrNot: String (操作成功与否的状态)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class MerchantAddProductMessage extends TongWenMessage {
    constructor(
        public  merchantToken: string,
        public  name: string,
        public  price: number,
        public  description: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

