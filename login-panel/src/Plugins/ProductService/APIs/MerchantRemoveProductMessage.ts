/**
 * MerchantRemoveProductMessage
 * desc: 商家验证身份后，在ProductTable中匹配名字为name的商品将其删除，返回操作成功与否
 * @param merchantToken: String (商家身份令牌，用于验证商家身份)
 * @param name: String (商品名称，用于匹配待删除商品)
 * @return successOrNot: String (操作是否成功的结果)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class MerchantRemoveProductMessage extends TongWenMessage {
    constructor(
        public  merchantToken: string,
        public  name: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

