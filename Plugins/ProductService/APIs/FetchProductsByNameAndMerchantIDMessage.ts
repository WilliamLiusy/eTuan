/**
 * FetchProductsByNameAndMerchantIDMessage
 * desc: 根据商家ID和商品名称筛选商品，返回匹配的商品信息。
 * @param merchantID: String (商家ID，用于找到属于该商家的商品。)
 * @param name: String (商品名称，用于匹配特定商品。)
 * @return product: ProductInfo:1065 (匹配的商品信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class FetchProductsByNameAndMerchantIDMessage extends TongWenMessage {
    constructor(
        public  merchantID: string,
        public  name: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

