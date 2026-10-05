package com.qianyunyi.rider.entity.kaijin.shocker.data;

/**
 * 修卡组织关系枚举
 * HOSTILE: 敌对关系 (value = -1)
 * NEUTRAL: 中立关系 (value = 0)
 * FRIENDLY: 友好关系 (value = 1)
 *
 * 原为 ShockerOrg 的内部枚举，提取为顶层类以解决 Forge ModuleClassLoader 的内部类加载问题。
 */
public enum ShockerRelationship {
    HOSTILE(-1),
    NEUTRAL(0),
    FRIENDLY(1);

    private final int value;

    ShockerRelationship(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }
}