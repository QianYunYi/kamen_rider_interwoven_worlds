package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerFireDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SupernaturalFire implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "超自然起火";
    }

    private static final String SUPERNATURAL_FIRE_ANIM = "supernatural_fire";


    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_fire.png");
    }

    // 技能配置参数
    // 火焰伤害
    private static final float FIRE_DAMAGE = 100.0f;
    // 扇形角度
    private static final float FAN_ANGLE = 60.0f;
    // 最大距离
    private static final double MAX_DISTANCE = 32.0;
    // 扇形半径
    private static final double FAN_RADIUS = 8.0;
    // 冷却时间（秒）
    private static final int COOLDOWN_SECONDS = 10;
    // 亚玛达姆能量消耗
    private static final float ENERGY_COST = 50.0f;
    private long cooldownStartTick = 0;

    /**
     * 释放技能
     * @param serverPlayer 玩家
     */
    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;
        data.setSkillActive(true);

        // 检查并消耗亚玛达姆能量
        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) {
            data.setSkillActive(false);
            return;
        }
        if (kuugaData.getAmadamEnergy() < ENERGY_COST) {
            HenshinUtil.sendMessage(serverPlayer, "亚玛达姆能量不足！需要 " + (int) ENERGY_COST + " 点能量", ChatFormatting.RED);
            data.setSkillActive(false);
            return;
        }
        kuugaData.removeAmadamEnergy(ENERGY_COST);

        cooldownStartTick = serverPlayer.level().getGameTime();

        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(SUPERNATURAL_FIRE_ANIM, serverPlayer.getUUID(), true), serverPlayer);

        serverPlayer.playSound(SoundEvents.FIRE_AMBIENT, 1.0F, 0.5F);

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

            // 创建自定义火焰伤害源
            PlayerFireDamageSource fireDamageSource = new PlayerFireDamageSource(serverPlayer);

            for (LivingEntity target : targets) {
                // 使用自定义火焰伤害源
                target.hurt(fireDamageSource, FIRE_DAMAGE);
                target.setSecondsOnFire(10);
                // 添加超自然起火效果 5 秒
                target.addEffect(new MobEffectInstance(
                        KamenRiderInterwovenWorldsModMobEffects.SUPERNATURAL_FIRE.get(),
                        100, 8, false, false
                ));
                addFireParticles(target, (ServerLevel) serverPlayer.level());
            }
            // 添加扇形火焰粒子
            addFanParticles(serverPlayer, playerPos, lookVec, (ServerLevel) serverPlayer.level());
            data.setSkillActive(false);
        });
    }
    /**
     * 检查实体是否在扇形区域内
     * @param entity 实体
     * @param playerPos 玩家位置
     * @param lookVec 玩家朝向向量
     * @return 是否在扇形区域内
     */
    private boolean isInFanArea(LivingEntity entity, Vec3 playerPos, Vec3 lookVec) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 toEntity = entityPos.subtract(playerPos).normalize();
        double distance = entityPos.subtract(playerPos).length();
        if (distance > MAX_DISTANCE) return false;
        double angle = Math.acos(lookVec.dot(toEntity)) * (180.0 / Math.PI);
        return angle <= FAN_ANGLE / 2;
    }

    /**
     * 添加火焰粒子
     * @param target 目标实体
     * @param level 服务器等级
     */
    private void addFireParticles(LivingEntity target, ServerLevel level) {
        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * 添加扇形火焰粒子
     * @param player 玩家
     * @param playerPos 玩家位置
     * @param lookVec 玩家朝向向量
     * @param level 服务器等级
     */
    private void addFanParticles(ServerPlayer player, Vec3 playerPos, Vec3 lookVec, ServerLevel level) {
        double distanceStep = 2.0;
        for (double distance = 1.0; distance <= MAX_DISTANCE; distance += distanceStep) {
            for (double angleDeg = -FAN_ANGLE / 2; angleDeg <= FAN_ANGLE / 2; angleDeg += 5.0) {
                double angleRad = Math.toRadians(angleDeg);

                Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向
                Vec3 dir = lookVec.scale(Math.cos(angleRad))  // 前方分量
                        .add(right.scale(Math.sin(angleRad))); // 右方分量
                dir = dir.normalize();

                Vec3 pos = playerPos.add(dir.scale(distance));
                level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.1);
            }
        }
    }

    /**
     * 释放技能
     * @param serverPlayer 玩家
     * @param isAutoRelease 是否自动释放
     */
    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {}
    /**
     * 检查技能是否可用
     * @param serverPlayer 玩家
     * @return 是否可用
     */
    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;
        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.RED);
            return false;
        }
        return true;
    }
}