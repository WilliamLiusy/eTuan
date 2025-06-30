export enum RiderStatus {
    Idle = '空闲',
    Delivering = '配送中',
    OffDuty = '下班'
}

export const riderStatusList = Object.values(RiderStatus)

export function getRiderStatus(newType: string): RiderStatus {
    return riderStatusList.filter(t => t === newType)[0]
}
