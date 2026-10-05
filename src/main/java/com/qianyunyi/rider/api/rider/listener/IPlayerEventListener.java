package com.qianyunyi.rider.api.rider.listener;

import net.minecraft.server.level.ServerPlayer;

/**
 * 监听器接口
 */
public interface IPlayerEventListener {
    /**
     * 初始化监听器
     * @param serverPlayer 玩家
     */
    void initialize(ServerPlayer serverPlayer);

    /**
     * 检查是否已注册
     * @return 是否已注册
     */
    boolean isRegistered();

    /**
     * 注册监听器
     */
    void register();

    /**
     * 注销监听器
     */
    void unregister();

    /**
     * 检查是否需要激活监听
     * @param serverPlayer 玩家
     * @return 是否需要激活监听
     */
    boolean shouldBeActive(ServerPlayer serverPlayer);

    /**
     * 获取监听器类型
     * @return 监听器类型
     */
    String getType();

    /**
     * 检查是否永久禁用
     * @param serverPlayer 玩家
     * @return 是否永久禁用
     */
    default boolean isPermanentlyDisabled(ServerPlayer serverPlayer) {
        return false;
    }

}