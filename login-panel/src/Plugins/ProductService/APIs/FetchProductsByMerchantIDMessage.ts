/**
 * FetchProductsByMerchantIDMessage
 * desc: 根据商家ID筛选商品列表
 * @param merchantID: String (商家ID，用于筛选对应商家的商品)
 * @return products: ProductInfo:1065 (筛选出的商品列表，包含所有匹配的商品信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class FetchProductsByMerchantIDMessage extends TongWenMessage {
    constructor(
        public  merchantID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

