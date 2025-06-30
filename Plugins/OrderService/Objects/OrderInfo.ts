/**
 * OrderInfo
 * desc: 订单信息，包含订单的基础数据和状态
 * @param orderID: String (订单ID)
 * @param customerID: String (顾客ID)
 * @param merchantID: String (商家ID)
 * @param riderID: String (骑手ID（空表示未分配）)
 * @param productList: ProductInfo (商品信息列表)
 * @param destinationAddress: String (送达地址)
 * @param orderStatus: OrderStatus:1070 (当前订单状态)
 * @param orderTime: DateTime (订单创建时间)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ProductInfo } from 'Plugins/ProductService/Objects/ProductInfo';
import { OrderStatus } from 'Plugins/OrderService/Objects/OrderStatus';


export class OrderInfo extends Serializable {
    constructor(
        public  orderID: string,
        public  customerID: string,
        public  merchantID: string,
        public  riderID: string | null,
        public  productList: ProductInfo[],
        public  destinationAddress: string,
        public  orderStatus: OrderStatus,
        public  orderTime: number
    ) {
        super()
    }
}


