package com.qianyunyi.rider.internal.henshin.behavior;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.api.rider.behavior.IHenshinBehavior;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;



public abstract class BaseHenshinBehavior implements IHenshinBehavior {

    /**
     * 处理快速变身流程
     *
     * <p>在玩家下蹲状态下触发</p>
     *
     * @param serverPlayer 执行变身的玩家
     * @param config 骑士配置对象
     * @param formConfig 形态配置
     */
    @Override
    public void handleQuickTransformation(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        // 保存玩家当前装备（全部）
        CompoundTag standbyGear = HenshinUtil.getStandbyGear(serverPlayer);
        CompoundTag activeGear = HenshinUtil.getActiveGear(serverPlayer);

        //standbyGear.put("legs", serverPlayer.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        //standbyGear.put("mainhand", serverPlayer.getItemBySlot(EquipmentSlot.MAINHAND).save(new CompoundTag()));

        activeGear.put("head", serverPlayer.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
        activeGear.put("chest", serverPlayer.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
        standbyGear.put("legs", serverPlayer.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        activeGear.put("feet", serverPlayer.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));

        // 装备完整装甲套装
        formConfig.equipArmor(serverPlayer);

        // 移除主手道具
        HenshinUtil.removeMainhandItem(serverPlayer, formConfig.getMainHenshinItem());

        // 播放变身音效
        SoundEvent transformSound = formConfig.getSounds().get(RiderSoundType.HENSHIN);
        if (transformSound != null) {
            MusicHandler.playSound(serverPlayer, transformSound);
        }

        // 设置状态和消息
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);

        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(formConfig.getFormName());
        // 应用属性修饰符
        HenshinUtil.applyAttributeModifiers(serverPlayer, formConfig);
        // 保存数据
        data.setStandbyGear(standbyGear);
        data.setActiveGear(activeGear);
        data.setCurrentState(HenshinState.ACTIVE);
    }

    /**
     * 处理变身取消流程
     *
     * <p>在玩家站起状态下触发</p>
     *
     * @param serverPlayer 执行取消的玩家
     * @param riderConfig 骑士配置对象
     * @param formConfig 形态配置
     */
    @Override
    public void handleDeactivation(ServerPlayer serverPlayer, RiderConfig riderConfig, FormConfig formConfig) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        HenshinState currentState = data.getCurrentState();

        // 清除装备
        formConfig.removeArmor(serverPlayer);
        HenshinUtil.restorePlayerGear(serverPlayer);
        data.clearGear();

        // 停止音效
        MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.DRIVER));
        MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
        MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.HENSHIN));
        //播放解除变身音效
        MusicHandler.playSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.UNFORM));

        // 立即清除玩家的形态buff
        HenshinUtil.removeBuffLists(serverPlayer, formConfig.getEffects());

        // 移除属性修饰符
        HenshinUtil.removeAttributeModifiers(serverPlayer, riderConfig.getForm("base"));
        HenshinUtil.removeAttributeModifiers(serverPlayer, formConfig);
        serverPlayer.setHealth((serverPlayer.getMaxHealth()));

        data.setCurrentState(HenshinState.INACTIVE);
        data.setActiveRiderName("");
        data.setActiveFormName("");
    }

    /**
     * 取消正在进行中的变身流程
     *
     * <p>在变身过程中取消时调用</p>
     *
     * @param serverPlayer 执行取消的玩家
     */
    @Override
    public void cancelHenshin(ServerPlayer serverPlayer) {
        // 默认实现：恢复玩家状态
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        serverPlayer.setInvulnerable(false);
        data.setHenshining(false);
    }
}