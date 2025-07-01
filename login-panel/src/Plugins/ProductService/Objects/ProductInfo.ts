/**
 * ProductInfo
 * desc: 商品信息
 * @param productID: String (商品的唯一ID)
 * @param merchantID: String (商家的唯一ID)
 * @param name: String (商品名称)
 * @param price: Double (商品价格)
 * @param description: String (商品描述)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class ProductInfo extends Serializable {
    constructor(
        public  productID: string,
        public  merchantID: string,
        public  name: string,
        public  price: number,
        public  description: string
    ) {
        super()
    }
}


