package com.qianyunyi.rider.api.data;

import com.qianyunyi.rider.data.module.IRiderDataModule;

/**
 * 攻击监听器数据模块接口
 * 用于管理玩家的攻击监听器状态
 */
public interface IAttackListenerModule extends IRiderDataModule {
    /**
     * 检查监听器是否已注册
     * @return 是否已注册
     */
    boolean isListenerRegistered();

    /**
     * 设置监听器注册状态
     * @param registered 是否已注册
     */
    void setListenerRegistered(boolean registered);

    /**
     * 激活监听器
     */
    void activateListener();

    /**
     * 停用监听器
     */
    void deactivateListener();

    /**
     * 检查监听器是否应该激活
     * @return 是否应该激活
     */
    boolean shouldBeActive();
}