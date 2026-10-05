package com.qianyunyi.rider.entity.vehicle.gouram;

/**
 * 哥莱姆实体状态机
 * 优先级从低到高排列，数值越大优先级越高
 * 高优先级状态不可被低优先级状态打断
 */
public enum GouramState {

    IDLE(0, true, "空闲绕圈巡逻"),
    FOLLOW_OWNER(1, true, "普通召唤飞向玩家"),
    DIVE_ATTACK(2, true, "自主俯冲攻击"),
    MERGE_VEHICLE(3, true, "与陆地载具合体"),
    RIDE_BACK(4, false, "玩家骑背空战"),
    GRAB_HAND(5, false, "玩家抓脚吊挂"),
    EMERGENCY_RESCUE(6, false, "半血救援，霸体状态");

    private final int priority;
    private final boolean interruptible;
    private final String description;

    GouramState(int priority, boolean interruptible, String description) {
        this.priority = priority;
        this.interruptible = interruptible;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isInterruptible() {
        return interruptible;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 判断当前状态能否被目标状态打断
     * @param target 目标状态
     * @return true 如果可以打断
     */
    public boolean canBeInterruptedBy(GouramState target) {
        if (this.interruptible) {
            return true;
        }
        return target.priority > this.priority;
    }

    /**
     * 根据优先级数值获取状态
     */
    public static GouramState fromPriority(int priority) {
        for (GouramState state : values()) {
            if (state.priority == priority) {
                return state;
            }
        }
        return IDLE;
    }
}