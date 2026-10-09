package com.qianyunyi.rider.internal.henshin.behavior;


import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;



public abstract class AwakeningHenshinBehavior extends BaseHenshinBehavior {

    /**
     * 解除变身流程
     *
     * <p>在玩家下蹲状态下触发</p>
     *
     * @param serverPlayer 执行变身的玩家
     * @param riderConfig 骑士配置对象
     * @param formConfig 形态配置
     */
    // 在 AwakeningHenshinBehavior 中
    @Override
    public void handleUnForm(ServerPlayer serverPlayer, RiderConfig riderConfig, FormConfig formConfig) {
        handleCommonDeactivation(serverPlayer, riderConfig, formConfig);

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        data.setCurrentState(HenshinState.INACTIVE);
        data.setActiveRiderName("");
        data.setActiveFormName("");

        GlobalListenerManager.updateListenerState(serverPlayer);
    }


    /**
     * 处理通用的变身解除流程
     *
     * @param serverPlayer 执行变身的玩家
     * @param riderConfig 骑士配置对象
     * @param formConfig 形态配置
     */
    protected void handleCommonDeactivation(ServerPlayer serverPlayer, RiderConfig riderConfig, FormConfig formConfig) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        HenshinState currentState = data.getCurrentState();
        FormConfig baseForm = riderConfig.getForm("base");

        // 装备处理
        if (currentState == HenshinState.STANDBY_AWAKENING) {
            if (baseForm != null && HenshinUtil.isEquippedArmor(serverPlayer, baseForm.getArmorSet())) {
                formConfig.removeArmor(serverPlayer);
                baseForm.removeArmor(serverPlayer);
            } else {
                formConfig.removeArmor(serverPlayer);
            }
        } else if (currentState == HenshinState.ACTIVE_AWAKENING) {
            formConfig.removeArmor(serverPlayer);
        }

        HenshinUtil.restorePlayerGear(serverPlayer);
        data.clearGear();

        // 音效处理
        stopAllSounds(serverPlayer, baseForm, formConfig);

        // 清除效果
        serverPlayer.removeEffect(MobEffects.INVISIBILITY);
        HenshinUtil.removeBuffLists(serverPlayer, formConfig.getEffects());

        // 属性处理
        HenshinUtil.removeAttributeModifiers(serverPlayer, riderConfig.getForm("base"));
        HenshinUtil.removeAttributeModifiers(serverPlayer, formConfig);
    }

        /**
         * 停止所有相关音效
         *
         * @param serverPlayer 执行变身的玩家
         * @param baseForm 基础形态配置
         * @param formConfig 目标形态配置
         */
        private void stopAllSounds(ServerPlayer serverPlayer, FormConfig baseForm, FormConfig formConfig) {
        if (baseForm != null) {
            MusicHandler.stopSound(serverPlayer, baseForm.getSounds().get(RiderSoundType.DRIVER));
            MusicHandler.stopSound(serverPlayer, baseForm.getSounds().get(RiderSoundType.HENSHIN));
            MusicHandler.stopSound(serverPlayer, baseForm.getSounds().get(RiderSoundType.HENSHIN));
        }
            MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
            MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.HENSHIN));
    }
}