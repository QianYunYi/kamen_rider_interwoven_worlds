package com.qianyunyi.rider.api.rider.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 技能接口
 */
public interface IRiderSkill {
    /**
     * 获取技能名称
     */
    String getDisplayName();

    /**
     * 获取技能图标纹理路径（用于轮盘显示）
     */
    ResourceLocation getIcon();

    /**
     * 释放技能
     */
    void skill(ServerPlayer serverPlayer);

    /**
     * 释放技能（松手触发）
     */
    void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease);

    /**
     * 检查技能是否可用
     */
    boolean isAvailable(ServerPlayer serverPlayer);
}