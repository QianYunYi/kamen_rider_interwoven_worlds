package com.qianyunyi.rider.data.enums;

public enum HenshinState {
    INACTIVE,           // 未变身
    STANDBY,            // 一段待机
    ACTIVE,             // 一段变身
    STANDBY_AWAKENING,  // 二段待机（强化待机）
    ACTIVE_AWAKENING,   // 二段变身（强化形态）
    DEACTIVATING;        // 解除变身中

    /** 把名字写进 NBT */
    public String getName() {
        return this.name();
    }

    /** 从 NBT 读名字 → 枚举，容错返回 INACTIVE */
    public static HenshinState fromName(String name) {
        if (name == null || name.isEmpty()) return INACTIVE;
        try {
            return HenshinState.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INACTIVE;
        }
    }
}