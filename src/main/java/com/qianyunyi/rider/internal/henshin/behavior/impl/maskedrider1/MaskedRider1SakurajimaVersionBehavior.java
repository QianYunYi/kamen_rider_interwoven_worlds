package com.qianyunyi.rider.internal.henshin.behavior.impl.maskedrider1;


import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.henshin.behavior.AwakeningHenshinBehavior;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaskedRider1SakurajimaVersionBehavior extends AwakeningHenshinBehavior {

    // ========== 动画常量 ==========
    private final String GROUND_HENSHIN_ANIMATION = "masked_rider_1_henshin";
    private final String AERIAL_HENSHIN_ANIMATION = "aerial_masked_rider_1_henshin";
    private final String GENERATE_ANIMATION = "generate";

    private final List<UUID> activeTasks = new ArrayList<>();

    @Override
    public void enterStandbyState(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        handleHenshin(serverPlayer, config, formConfig);
    }

    @Override
    public void handleHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        // ====================== 环境判定：地面 / 空中 / 骑乘 ======================
        boolean isRiding = serverPlayer.isPassenger();
        boolean isInAir = !serverPlayer.onGround() && !isRiding;

        // 标记变身中
        dataManager.setHenshining(true);

        DelayedTask.ChainBuilder chainBuilder;

        if (isRiding) {
            // ================ 骑乘变身（摩托变身） ================
            // 无飞行动作，直接装备
            chainBuilder = DelayedTask.chain(serverPlayer.level())
                    .then(1, () -> {
                        savePlayerGear(serverPlayer, dataManager);
                        stopStandbyAndPlayTransformSound(serverPlayer, formConfig);
                        formConfig.equipBelt(serverPlayer);
                        HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer, formConfig, GENERATE_ANIMATION);
                    })
                    .then(15, () -> finishHenshin(serverPlayer, config, formConfig));

        } else if (isInAir) {
            // ================ 空中变身 ================
            HenshinUtil.sendAnimationMessage(serverPlayer, AERIAL_HENSHIN_ANIMATION);
            chainBuilder = DelayedTask.chain(serverPlayer.level())
                    .then(1, () -> {
                        // 强力向上弹射
                        Vec3 motion = serverPlayer.getDeltaMovement();
                        serverPlayer.setDeltaMovement(motion.x, 1.8, motion.z);
                        serverPlayer.hurtMarked = true;
                    })
                    .then(20, () -> {
                        savePlayerGear(serverPlayer, dataManager);
                        stopStandbyAndPlayTransformSound(serverPlayer, formConfig);
                        formConfig.equipBelt(serverPlayer);
                        HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer, formConfig, GENERATE_ANIMATION);
                    })
                    .then(5, () -> {
                        Vec3 motion = serverPlayer.getDeltaMovement();
                        serverPlayer.setDeltaMovement(motion.x, -0.25, motion.z);
                        serverPlayer.hurtMarked = true;
                    })
                    .then(10, () -> {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));
                    })
                    .then(1, () -> finishHenshin(serverPlayer, config, formConfig));

        } else {
            // ================ 地面变身（樱岛一号原版） ================
            HenshinUtil.sendAnimationMessage(serverPlayer, GROUND_HENSHIN_ANIMATION);
            chainBuilder = DelayedTask.chain(serverPlayer.level())
                    // 2.38秒（48刻）→ 向上飞行
                    .then(48, () -> {
                        Vec3 motion = serverPlayer.getDeltaMovement();
                        serverPlayer.setDeltaMovement(motion.x, 0.65, motion.z);
                        serverPlayer.hurtMarked = true;
                    })
                    .then(26, () -> {
                        savePlayerGear(serverPlayer, dataManager);
                        stopStandbyAndPlayTransformSound(serverPlayer, formConfig);
                        formConfig.equipBelt(serverPlayer);
                        HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer, formConfig, GENERATE_ANIMATION);
                    })
                    .then(5, () -> {
                        // 缓慢下降
                        Vec3 motion = serverPlayer.getDeltaMovement();
                        serverPlayer.setDeltaMovement(motion.x, -0.15, motion.z);
                        serverPlayer.hurtMarked = true;
                    })
                    .then(5, () -> {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));
                    })
                    .then(1, () -> finishHenshin(serverPlayer, config, formConfig));
        }

        // 任务链启动与收尾
        final UUID chainId = chainBuilder.start();
        chainBuilder.onComplete(() -> activeTasks.remove(chainId))
                .onCancel(() -> {
                    serverPlayer.setInvulnerable(false);
                    dataManager.setHenshining(false);
                    activeTasks.remove(chainId);
                });

        if (chainId != null) {
            activeTasks.add(chainId);
        }
    }

    @Override
    public boolean canTransform(ServerPlayer serverPlayer) {
        return false;
    }

    // ========== 辅助方法：保存玩家当前装备 ==========
    private void savePlayerGear(ServerPlayer serverPlayer, RiderPlayerData dataManager) {
        CompoundTag activeGear = dataManager.getActiveGear();
        activeGear.put("head", serverPlayer.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
        activeGear.put("chest", serverPlayer.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
        activeGear.put("legs", serverPlayer.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        activeGear.put("feet", serverPlayer.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));
        dataManager.setActiveGear(activeGear);
    }

    // ========== 辅助方法：停止待机音效并播放变身音效 ==========
    private void stopStandbyAndPlayTransformSound(ServerPlayer serverPlayer, FormConfig formConfig) {
        MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
        SoundEvent sound = formConfig.getSounds().get(RiderSoundType.HENSHIN);
        if (sound != null) {
            MusicHandler.playSound(serverPlayer, sound);
        }
    }

    // ========== 统一变身完成逻辑 ==========
    private void finishHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        double healthRatio = serverPlayer.getHealth() / serverPlayer.getMaxHealth();
        HenshinUtil.applyAttributeModifiers(serverPlayer, formConfig);
        serverPlayer.setHealth((float) (serverPlayer.getMaxHealth() * healthRatio));

        dataManager.setActiveRiderName(config.getRiderName());
        dataManager.setActiveFormName(formConfig.getFormName());

        dataManager.setHenshining(false);
        dataManager.setCurrentState(HenshinState.ACTIVE);

        serverPlayer.setInvulnerable(false);
        GlobalListenerManager.updateListenerState(serverPlayer);
    }
}