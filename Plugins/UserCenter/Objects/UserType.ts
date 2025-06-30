export enum UserType {
    customer = '顾客',
    merchant = '商家',
    rider = '骑手'
}

export const userTypeList = Object.values(UserType)

export function getUserType(newType: string): UserType {
    return userTypeList.filter(t => t === newType)[0]
}
