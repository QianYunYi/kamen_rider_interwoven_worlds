package com.qianyunyi.rider.api.rider;

import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.world.entity.player.Player;


/**
 * 形态切换行为接口
 */
public interface IFormSwitchBehavior {
    /**
     * 处理形态切换
     * @param player 玩家实例
     * @param riderConfig 骑士配置
     * @param currentForm 当前形态配置
     * @param targetForm 目标形态配置
     */
    void handleFormSwitch(Player player, RiderConfig riderConfig,
                          FormConfig currentForm, FormConfig targetForm);
}