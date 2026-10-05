package com.qianyunyi.rider.api.rider.behavior;

import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.server.level.ServerPlayer;


/**
 * 骑士变身行为接口
 *
 * <p>定义骑士变身系统的核心行为方法</p>
 * @author QianYunYi
 */
public interface IHenshinBehavior {

    /**
     * 进入待机状态
     *
     * @param serverPlayer 执行变身的玩家
     * @param config 骑士配置对象
     */
    void enterStandbyState(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig);

    /**
     * 处理标准变身流程
     *
     * <p>在玩家非下蹲状态下触发</p>
     *
     * @param serverPlayer 执行变身的玩家
     * @param config 骑士配置对象
     */
    void handleHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig);

    /**
     * 处理快速变身流程
     *
     * <p>在玩家下蹲状态下触发</p>
     *
     * @param serverPlayer 执行变身的玩家
     * @param config 骑士配置对象
     */
    void handleQuickTransformation(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig);

    /**
     * 处理解除变身流程
     *
     * <p>在玩家下蹲状态下触发</p>
     *
     * @param serverPlayer 执行解除变身的玩家
     * @param config 骑士配置对象
     */
    void handleDeactivation(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig);

    /**
     * 取消变身
     *
     * @param serverPlayer 执行取消变身的玩家
     */
    void cancelHenshin(ServerPlayer serverPlayer);

    /**
     * 检查玩家是否可以变身
     *
     * @param serverPlayer 检查的玩家
     * @return 是否可以变身
     */
    boolean canTransform(ServerPlayer serverPlayer);
}