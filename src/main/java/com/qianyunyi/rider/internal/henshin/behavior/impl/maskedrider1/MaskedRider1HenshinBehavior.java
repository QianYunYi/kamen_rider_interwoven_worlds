package com.qianyunyi.rider.internal.henshin.behavior.impl.maskedrider1;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.henshin.behavior.BaseHenshinBehavior;
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

/**
 * 假面骑士1号变身行为
 */
public class MaskedRider1HenshinBehavior extends BaseHenshinBehavior {

    private final String HENSHIN_ANIMATION = "masked_rider_1_henshin";
    private final String AERIAL_HENSHIN_ANIMATION = "aerial_masked_rider_1_henshin";
    private final String GENERATE_ANIMATION = "generate";

    private final List<UUID> activeTasks = new ArrayList<>();

    @Override
    public boolean canTransform(ServerPlayer serverPlayer) {
        return false;
    }


    @Override
    public void enterStandbyState(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        handleHenshin(serverPlayer, config, formConfig);
    }

    @Override
    public void handleHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        // ====================== 核心判定：只有 空中 / 骑乘 才能变身 ======================
        boolean isRiding = serverPlayer.isPassenger();
        boolean isInAir = !serverPlayer.onGround() && !isRiding;

        // 既不在空中，也不在骑乘 → 直接取消变身，不执行任何动作
        if (!isRiding && !isInAir) {
            dataManager.setHenshining(false);
            serverPlayer.setInvulnerable(false);
            return;
        }

        // 标记变身中
        dataManager.setHenshining(true);
        serverPlayer.setInvulnerable(true);

        DelayedTask.ChainBuilder chainBuilder;

        if (isRiding) {
            // ================ 骑乘变身：无动作，直接装备 ================
            chainBuilder = DelayedTask.chain(serverPlayer.level())
                    .then(1, () -> {
                        saveCurrentArmor(serverPlayer, dataManager);

                        // 播放变身音效
                        stopStandbySound(serverPlayer, formConfig);
                        playTransformSound(serverPlayer, formConfig);

                        formConfig.equipBelt(serverPlayer);
                        HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer, formConfig, GENERATE_ANIMATION);
                    })
                    .then(15, () -> finishHenshin(serverPlayer, config, formConfig));

        } else {
            // ================ 空中变身：仅限空中触发 ================
            HenshinUtil.sendAnimationMessage(serverPlayer, AERIAL_HENSHIN_ANIMATION);
            chainBuilder = DelayedTask.chain(serverPlayer.level())
                    .then(1, () -> {
                        Vec3 motion = serverPlayer.getDeltaMovement();
                        serverPlayer.setDeltaMovement(motion.x, 1.8, motion.z);
                        serverPlayer.hurtMarked = true;
                    })
                    .then(20, () -> {
                        saveCurrentArmor(serverPlayer, dataManager);

                        // 播放变身音效
                        stopStandbySound(serverPlayer, formConfig);
                        playTransformSound(serverPlayer, formConfig);

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


    /**
     * 保存当前装备
     */
    private void saveCurrentArmor(ServerPlayer serverPlayer, RiderPlayerData dataManager) {
        CompoundTag activeGear = dataManager.getActiveGear();
        activeGear.put("head", serverPlayer.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
        activeGear.put("chest", serverPlayer.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
        activeGear.put("legs", serverPlayer.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        activeGear.put("feet", serverPlayer.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));
        dataManager.setActiveGear(activeGear);
    }

    /**
     * 停止待机音效
     */
    private void stopStandbySound(ServerPlayer serverPlayer, FormConfig formConfig) {
        if (formConfig.getSounds() != null) {
            SoundEvent standbySound = formConfig.getSounds().get(RiderSoundType.STANDBY);
            if (standbySound != null) {
                MusicHandler.stopSound(serverPlayer, standbySound);
            }
        }
    }

    /**
     * 播放变身音效
     */
    private void playTransformSound(ServerPlayer serverPlayer, FormConfig formConfig) {
        if (formConfig.getSounds() != null) {
            SoundEvent transformSound = formConfig.getSounds().get(RiderSoundType.HENSHIN);
            if (transformSound != null) {
                MusicHandler.playSound(serverPlayer, transformSound);
            }
        }
    }

    // 统一变身完成逻辑
    private void finishHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        // 保存血量比例并应用属性修饰符
        double healthRatio = serverPlayer.getHealth() / serverPlayer.getMaxHealth();
        HenshinUtil.applyAttributeModifiers(serverPlayer, formConfig);
        serverPlayer.setHealth((float) (serverPlayer.getMaxHealth() * healthRatio));

        // 更新骑士和形态信息
        dataManager.setActiveRiderName(config.getRiderName());
        dataManager.setActiveFormName(formConfig.getFormName());

        // 更新变身状态
        dataManager.setHenshining(false);
        dataManager.setCurrentState(HenshinState.ACTIVE);

        // 取消无敌状态
        serverPlayer.setInvulnerable(false);
    }


}