export enum OrderStatus {
    waitingForDish = '等待出餐',
    waitingForAssign = '等待分配骑手',
    delivering = '正在配送',
    completed = '已完成'
}

export const orderStatusList = Object.values(OrderStatus)

export function getOrderStatus(newType: string): OrderStatus {
    return orderStatusList.filter(t => t === newType)[0]
}
