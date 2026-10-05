package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SupernaturalThunder implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "超自然雷击";
    }

    private static final String SUPERNATURAL_THUNDER_ANIM = "supernatural_fire";

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_thunder.png");
    }

        // 技能配置参数
        // 闪电伤害
        private static final float LIGHTNING_DAMAGE = 100.0f;
        // 扇形角度
        private static final float FAN_ANGLE = 60.0f;
        // 最大距离
        private static final double MAX_DISTANCE = 32.0;
        // 扇形半径
        private static final double FAN_RADIUS = 8.0;
        // 冷却时间（秒）
        private static final int COOLDOWN_SECONDS = 10;
        // 升华能量消耗
        private static final float SUPER_ENERGY_COST = 100.0f;
    private long cooldownStartTick = 0;

    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;
        data.setSkillActive(true);

        // 检查并消耗升华能量
        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) {
            data.setSkillActive(false);
            return;
        }
        if (kuugaData.getSuperEnergy() < SUPER_ENERGY_COST) {
            HenshinUtil.sendMessage(serverPlayer, "升华能量不足！需要 " + (int) SUPER_ENERGY_COST + " 点升华能量", ChatFormatting.BLUE);
            data.setSkillActive(false);
            return;
        }
        kuugaData.removeSuperEnergy(SUPER_ENERGY_COST);

        cooldownStartTick = serverPlayer.level().getGameTime();

        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(SUPERNATURAL_THUNDER_ANIM, serverPlayer.getUUID(), true), serverPlayer);

        serverPlayer.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 0.8F);

        DelayedTask.run(serverPlayer.level(), 20, () -> {
            Vec3 lookVec = serverPlayer.getLookAngle().normalize();
            Vec3 playerPos = serverPlayer.position().add(0, serverPlayer.getEyeHeight(), 0);

            AABB fanArea = new AABB(
                    playerPos.x - MAX_DISTANCE, playerPos.y - MAX_DISTANCE, playerPos.z - MAX_DISTANCE,
                    playerPos.x + MAX_DISTANCE, playerPos.y + MAX_DISTANCE, playerPos.z + MAX_DISTANCE
            );

            List<LivingEntity> targets = serverPlayer.level().getEntitiesOfClass(
                    LivingEntity.class,
                    fanArea,
                    entity -> isInFanArea(entity, playerPos, lookVec)
                            && !RiderCompanionUtil.isPlayerCompanion(entity, serverPlayer.getUUID())
            );
            // 对目标应用闪电伤害和压制能量效果
            for (LivingEntity target : targets) {
                DamageSource lightningDamage = new PlayerLightningDamageSource(serverPlayer);
                target.hurt(lightningDamage, LIGHTNING_DAMAGE);
                // 添加压制能量效果 5 秒
                target.addEffect(new MobEffectInstance(
                        KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                        20, 8, false, false
                ));

                if (target.level() instanceof ServerLevel serverLevel) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                    if (bolt != null) {
                        bolt.moveTo(target.getX(), target.getY(), target.getZ());
                        serverLevel.addFreshEntity(bolt);
                    }
                }

                addThunderParticles(target, (ServerLevel) serverPlayer.level());
            }
            addFanParticles(serverPlayer, playerPos, lookVec, (ServerLevel) serverPlayer.level());
            data.setSkillActive(false);
        });
    }

    private boolean isInFanArea(LivingEntity entity, Vec3 playerPos, Vec3 lookVec) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 toEntity = entityPos.subtract(playerPos).normalize();
        double distance = entityPos.subtract(playerPos).length();
        if (distance > MAX_DISTANCE) return false;
        double angle = Math.acos(lookVec.dot(toEntity)) * (180.0 / Math.PI);
        return angle <= FAN_ANGLE / 2;
    }

    private void addThunderParticles(LivingEntity target, ServerLevel level) {
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.1);
    }

    // 的扇形粒子方法
    private void addFanParticles(ServerPlayer player, Vec3 playerPos, Vec3 lookVec, ServerLevel level) {
        double distanceStep = 2.0;
        for (double distance = 1.0; distance <= MAX_DISTANCE; distance += distanceStep) {
            for (double angleDeg = -FAN_ANGLE / 2; angleDeg <= FAN_ANGLE / 2; angleDeg += 5.0) {
                double angleRad = Math.toRadians(angleDeg);

                Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 dir = lookVec.scale(Math.cos(angleRad))
                        .add(right.scale(Math.sin(angleRad)));
                dir = dir.normalize();

                Vec3 pos = playerPos.add(dir.scale(distance));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.1);
            }
        }
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {}

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;
        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.BLUE);
            return false;
        }
        return true;
    }
}