package com.qianyunyi.rider.internal.henshin.behavior.impl;

import com.qianyunyi.rider.api.rider.IFormSwitchBehavior;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 默认的形态切换行为
 */
public class DefaultFormSwitchBehavior implements IFormSwitchBehavior {

    @Override
    public void handleFormSwitch(Player player, RiderConfig riderConfig,
                                 FormConfig currentForm, FormConfig targetForm) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 解除当前形态
        currentForm.getBehavior().handleUnForm(serverPlayer, riderConfig, currentForm);

        // 给予自身隐身效果
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));

        // 给予1秒无敌
        serverPlayer.invulnerableTime = 20;

        // 快速变身为目标形态
        targetForm.getBehavior().handleQuickHenshin(serverPlayer, riderConfig, targetForm);
    }
}