package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SupernaturalWater implements IRiderSkill {

    private static final double WATER_RANGE = 10.0;
    private static final int SLOW_DURATION_TICKS = 40;
    private static final int SLOW_AMPLIFIER = 7;
    private static final int WATER_DURATION_TICKS = 200;
    private static final int TICK_INTERVAL = 10;
    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST_PER_SECOND = 5.0f;
    private static final float ENERGY_COST_PER_TICK = ENERGY_COST_PER_SECOND / 20.0f;

    private static final Map<UUID, Long> PLAYER_COOLDOWN = new HashMap<>();

    @Override
    public String getDisplayName() {
        return "超自然之水";
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_water.png");
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        UUID playerUuid = serverPlayer.getUUID();

        if (!isAvailable(serverPlayer)) {
            return;
        }

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        if (kuugaData.isSupernaturalWaterActive()) {
            stopWater(serverPlayer);
        } else {
            startWater(serverPlayer);
        }

        PLAYER_COOLDOWN.put(playerUuid, serverPlayer.level().getGameTime());
    }

    private void startWater(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        kuugaData.setSupernaturalWaterActive(true);

        MusicHandler.playSound(serverPlayer, SoundEvents.PLAYER_SPLASH_HIGH_SPEED);

        UUID taskId = DelayedTask.repeat(serverPlayer.level(), TICK_INTERVAL, level -> {
            if (level.getPlayerByUUID(serverPlayer.getUUID()) instanceof ServerPlayer player) {
                RiderPlayerData playerData = RiderPlayerVariablesProvider.get(player);
                if (playerData == null) return;

                KuugaDataModule playerKuugaData = playerData.getKuugaData();
                if (playerKuugaData == null) return;

                if (!playerKuugaData.isSupernaturalWaterActive()) {
                    return;
                }

                float energyNeeded = ENERGY_COST_PER_TICK * TICK_INTERVAL;
                if (playerKuugaData.getAmadamEnergy() < energyNeeded) {
                    stopWater(player);
                    return;
                }

                playerKuugaData.removeAmadamEnergy(energyNeeded);
                applyWaterEffect(player);
            }
        });

        if (taskId != null) {
            kuugaData.setSupernaturalWaterTaskId(taskId);
        }

        DelayedTask.run(serverPlayer.level(), WATER_DURATION_TICKS, () -> {
            RiderPlayerData lateData = RiderPlayerVariablesProvider.get(serverPlayer);
            if (lateData != null && lateData.getKuugaData() != null) {
                if (lateData.getKuugaData().isSupernaturalWaterActive()) {
                    stopWater(serverPlayer);
                }
            }
        });
    }

    private void applyWaterEffect(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        AABB area = new AABB(player.blockPosition()).inflate(WATER_RANGE);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !RiderCompanionUtil.isPlayerCompanion(e, player.getUUID()));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(
                    KamenRiderInterwovenWorldsModMobEffects.SUPERNATURAL_WATER.get(),
                    SLOW_DURATION_TICKS + TICK_INTERVAL, 0, false, false
            ));
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, false
            ));

            double x = target.getX();
            double y = target.getY();
            double z = target.getZ();
            for (int i = 0; i < 5; i++) {
                double ox = (serverLevel.random.nextDouble() - 0.5) * 0.8;
                double oz = (serverLevel.random.nextDouble() - 0.5) * 0.8;
                serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                        x + ox, y + 0.05, z + oz,
                        1, 0, 0, 0, 0.01);
                serverLevel.sendParticles(ParticleTypes.RAIN,
                        x + ox, y + 1.0, z + oz,
                        1, 0.1, 0.3, 0.1, 0.01);
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                        x + ox, y + 0.1, z + oz,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void stopWater(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        kuugaData.setSupernaturalWaterActive(false);

        UUID taskId = kuugaData.getSupernaturalWaterTaskId();
        if (taskId != null) {
            DelayedTask.cancel(taskId);
            kuugaData.setSupernaturalWaterTaskId(null);
        }
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        if (isAutoRelease) {
            stopWater(serverPlayer);
        }
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        UUID playerUuid = serverPlayer.getUUID();
        Long lastUseTime = PLAYER_COOLDOWN.get(playerUuid);

        if (lastUseTime == null) {
            return true;
        }

        long currentTick = serverPlayer.level().getGameTime();
        long elapsedTicks = currentTick - lastUseTime;

        if (elapsedTicks < COOLDOWN_TICKS) {
            long remaining = (COOLDOWN_TICKS - elapsedTicks) / 20;
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.BLUE);
            return false;
        }
        return true;
    }
}