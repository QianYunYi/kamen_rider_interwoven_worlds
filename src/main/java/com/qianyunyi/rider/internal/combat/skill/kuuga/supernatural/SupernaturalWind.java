package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.entity.effect.SkillEffectEntity;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 超自然之风技能 - 持续向前移动，吸附周围怪物并造成伤害
 */
public class SupernaturalWind implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "超自然之风";
    }

    private static final String SUPERNATURAL_WIND_ANIM = "supernatural_wind";

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_wind.png");
    }

    // 技能配置参数 - 大幅增强聚怪效果
    private static final float WIND_DAMAGE = 20.0f;          // 每次伤害值
    private static final int DAMAGE_INTERVAL = 5;            // 伤害间隔（tick）
    private static final double WIND_SPEED = 1.0;            // 风的移动速度（降低速度，让聚怪更明显）
    private static final double WIND_RANGE = 15.0;           // 风的吸附范围（扩大到15格）
    private static final double WIND_PULL_STRENGTH = 0.8;    // 吸附强度（大幅增强）
    private static final int WIND_LIFETIME = 200;            // 风的持续时间（延长到10秒）
    private static final int COOLDOWN_SECONDS = 10;          // 冷却时间（秒）
    // 亚玛达姆能量消耗
    private static final float ENERGY_COST = 35.0f;
    private long cooldownStartTick = 0;

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

        // 播放动画和音效
        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(SUPERNATURAL_WIND_ANIM, serverPlayer.getUUID(), true), serverPlayer);
        // serverPlayer.playSound(SoundEvents.WIND_CHARGE, 1.0F, 0.8F);

        // 延迟创建风实体（等待动画播放）
        DelayedTask.run(serverPlayer.level(), 5, (level) -> {
            if (!serverPlayer.isAlive()) return;

            // 创建风实体
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            Vec3 footPos = serverPlayer.position();
            Vec3 lookVec = serverPlayer.getLookAngle().normalize();

            // 创建技能效果实体
            SkillEffectEntity windEntity = new SkillEffectEntity(
                    EntityRegistrationHandler.SKILL_EFFECT,
                    serverPlayer,
                    serverLevel,
                    "supernatural_wind",
                    "supernatural_wind",
                    "idle"
            );

            DelayedTask.run(serverPlayer.level(), 5, () ->
                    windEntity.setAnimationName("idle")
            );
            DelayedTask.run(serverPlayer.level(), 195, () ->
                    windEntity.setAnimationName("idle")
            );
            DelayedTask.run(serverPlayer.level(), 200, windEntity::clearEffect
            );

            // 设置风实体属性
            windEntity.setDeadTime(WIND_LIFETIME);
            windEntity.setDying(true);
            windEntity.setPos(footPos.x, footPos.y, footPos.z);
            windEntity.setDeltaMovement(lookVec.x * WIND_SPEED, lookVec.y * WIND_SPEED, lookVec.z * WIND_SPEED);
            windEntity.setFollowOwner(false);
            windEntity.setEnableCollision(true);
            windEntity.setEnableCollisionDamage(false);
            windEntity.setCollisionBoxSize(3.0f, 3.0f);
            windEntity.setDamageOwner(serverPlayer);

            // 加入世界
            serverLevel.addFreshEntity(windEntity);

            // 启动风的吸附和伤害逻辑
            startWindLogic(windEntity, serverPlayer);

            // 播放风的持续音效
            // serverPlayer.playSound(SoundEvents.WIND_AMBIENT, 1.0F, 0.5F);

            data.setSkillActive(false);
        });
    }

    /**
     * 启动风的吸附和伤害逻辑
     */
    private void startWindLogic(SkillEffectEntity windEntity, ServerPlayer player) {
        ServerLevel level = (ServerLevel) windEntity.level();

        // 每tick执行一次吸附和伤害
        DelayedTask.repeat(level, 1, WIND_LIFETIME, (l) -> {
            if (!windEntity.isAlive()) return;

            Vec3 windPos = windEntity.position();

            // 1. 吸附周围怪物
            AABB pullArea = new AABB(
                    windPos.x - WIND_RANGE, windPos.y - WIND_RANGE, windPos.z - WIND_RANGE,
                    windPos.x + WIND_RANGE, windPos.y + WIND_RANGE, windPos.z + WIND_RANGE
            );

            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    pullArea,
                    entity -> entity.isAlive() && entity.isPushable()
                            && !RiderCompanionUtil.isPlayerCompanion(entity, player.getUUID())
            );

            for (LivingEntity target : targets) {
                Vec3 targetPos = target.position();
                Vec3 toWind = windPos.subtract(targetPos).normalize();
                double distance = targetPos.distanceTo(windPos);

                // 根据距离调整吸附强度（越近越强）
                double pullStrength = WIND_PULL_STRENGTH * (1.0 - (distance / WIND_RANGE));

                // 修复：只应用水平方向的吸附，避免怪物被卷到空中
                target.push(toWind.x * pullStrength, 0.05, toWind.z * pullStrength); // 轻微向上的力防止卡住

                // 修复：确保怪物被吸附到风实体位置，而不是被击退
                if (distance > 1.0) { // 只有当距离大于1格时才吸附
                    // 缓慢调整怪物的朝向和速度，确保向风实体移动
                    target.setYRot((float) Math.toDegrees(Math.atan2(toWind.x, toWind.z)));
                    target.setXRot(0);
                    target.setDeltaMovement(
                            target.getDeltaMovement().x + toWind.x * pullStrength * 0.1,
                            target.getDeltaMovement().y + 0.01,
                            target.getDeltaMovement().z + toWind.z * pullStrength * 0.1
                    );
                }

                // 播放风粒子效果
                if (level.getGameTime() % 2 == 0) {
                    level.sendParticles(
                            ParticleTypes.CLOUD,
                            target.getX() + level.random.nextGaussian() * 0.5,
                            target.getY() + target.getBbHeight() * 0.5 + level.random.nextGaussian() * 0.5,
                            target.getZ() + level.random.nextGaussian() * 0.5,
                            2, 0.1, 0.1, 0.1, 0.05
                    );
                }
            }
            // 2. 定期造成伤害
            if (level.getGameTime() % DAMAGE_INTERVAL == 0) {
                AABB damageArea = new AABB(
                        windPos.x - 3.0, windPos.y - 3.0, windPos.z - 3.0,
                        windPos.x + 3.0, windPos.y + 3.0, windPos.z + 3.0
                );

                List<LivingEntity> damageTargets = level.getEntitiesOfClass(
                        LivingEntity.class,
                        damageArea,
                        entity -> entity.isAlive()
                                && !RiderCompanionUtil.isPlayerCompanion(entity, player.getUUID())
                );

                DamageSource windDamage = player.damageSources().mobProjectile(windEntity, player);
                for (LivingEntity target : damageTargets) {
                    if (target.hurt(windDamage, WIND_DAMAGE)) {
                        // 伤害成功后播放粒子效果
                        level.sendParticles(
                                ParticleTypes.EXPLOSION,
                                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                8, 0.8, 0.8, 0.8, 0.2
                        );
                    }
                }
            }

            // 3. 播放强烈的风粒子效果
            if (level.getGameTime() % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double offsetX = level.random.nextGaussian() * 2.0;
                    double offsetY = level.random.nextGaussian() * 2.0;
                    double offsetZ = level.random.nextGaussian() * 2.0;

                    level.sendParticles(
                            ParticleTypes.CLOUD,
                            windPos.x + offsetX,
                            windPos.y + offsetY,
                            windPos.z + offsetZ,
                            1, 0.0, 0.0, 0.0, 0.3
                    );

                    // 偶尔生成更大的云团
                    if (level.random.nextFloat() < 0.3) {
                        level.sendParticles(
                                ParticleTypes.CLOUD,
                                windPos.x + offsetX * 0.5,
                                windPos.y + offsetY * 0.5,
                                windPos.z + offsetZ * 0.5,
                                5, 0.5, 0.5, 0.5, 0.1
                        );
                    }
                }
            }
        });
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {}

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