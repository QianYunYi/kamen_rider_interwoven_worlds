package com.qianyunyi.rider.potion.action;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModGameRules;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class SuppressEnergyExplodeAction {

    // 最大能量传递距离
    private static final double MAX_TRANSFER_DISTANCE = 8.0;
    // 最大传递目标数量
    private static final int MAX_TRANSFER_TARGETS = 3;
    // 能量传递比例
    private static final float ENERGY_TRANSFER_RATIO = 0.4f;
    // 传递延迟 (刻)
    private static final int TRANSFER_DELAY = 10; // 0.5秒延迟

    // 形态常量
    private static final String FORM_TAG_KEY = "form";
    private static final String BASE_FORM = "base";
    private static final String RISING_MIGHTY_FORM = "rising_mighty_form";

    // 触发爆炸
    public static void triggerExplosion(Level level, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return;

        // 检查是否应该触发爆炸
        if (!shouldTriggerExplosion(livingEntity)) return;

        // 防止重复触发（is_exploding 标志覆盖整个爆炸期）
        if (entity.getPersistentData().getBoolean("is_exploding")) return;

        // 移除爆炸标记，设置爆炸中标志（覆盖整个爆炸期，拦截中间死亡）
        entity.getPersistentData().remove("is_explosive");
        entity.getPersistentData().putBoolean("is_exploding", true);

        //给予不可移动效果
        livingEntity.addEffect(new MobEffectInstance(KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(), 200, 0, false, false));
        // 爆炸期间无敌并停止AI，防止提前死亡或乱动
        livingEntity.setInvulnerable(true);
        if (livingEntity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        // 计算爆炸位置
        Vec3 centerPos = new Vec3(
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2.0,
                entity.getZ()
        );

        // 获取镇压能量值（0-2000）
        float suppressEnergy = (float) entity.getPersistentData().getDouble("suppress_energy");
        // 清除镇压能量
        entity.getPersistentData().remove("suppress_energy");
        // 获取造成爆炸的玩家
        ServerPlayer causingPlayer = getCausingPlayer(level, entity);

        // 计算视觉效果强度
        float visualIntensity = calculateVisualIntensity(livingEntity, suppressEnergy);

        // 延迟3秒创建粒子爆炸效果
        DelayedTask.run(level, 60, () -> {
            createParticleEffects(level, centerPos, visualIntensity, entity);
            if (level.getGameRules().getBoolean(KamenRiderInterwovenWorldsModGameRules.SKILL_EXPLOSION)) {
                createLevelExplosion((ServerLevel) level, centerPos, suppressEnergy, livingEntity, causingPlayer);
            }
        });

        // 延迟65刻(3.25秒)后执行伤害
        if (level instanceof ServerLevel serverLevel) {
            DelayedTask.run(serverLevel, 65, () -> {
                if (!livingEntity.isAlive()) return;

                // 清除爆炸中标志，允许实体正常死亡
                entity.getPersistentData().remove("is_exploding");
                // 再次清除爆炸标记（防止能量传递在之前重新设置了该标记导致循环爆炸）
                entity.getPersistentData().remove("is_explosive");

                // 恢复AI和可受伤状态
                livingEntity.setInvulnerable(false);
                if (livingEntity instanceof Mob mob) {
                    mob.setNoAi(false);
                }

                // 应用击退效果
                applyKnockback(serverLevel, entity, centerPos, visualIntensity);

                // 玩家伤害确保爆炸时死亡（归属为玩家击杀）
                livingEntity.hurt(causingPlayer != null
                        ? serverLevel.damageSources().playerAttack(causingPlayer)
                        : serverLevel.damageSources().generic(), 99999f);

                // 触发白屏效果
                if (causingPlayer != null) {
                    triggerWhiteScreenEffect(causingPlayer, suppressEnergy);
                }
            });

            // 延迟传递能量 (在爆炸发生后10刻/0.5秒)
            Entity finalEntity = entity;
            DelayedTask.run(serverLevel, TRANSFER_DELAY, () -> {
                // 检测实体5格内是否有实体
                List<Entity> nearbyEntities = findNearbyEntities(serverLevel, centerPos, MAX_TRANSFER_DISTANCE);

                // 如果有附近实体，将镇压能量衰减一半到目标
                if (!nearbyEntities.isEmpty()) {
                    transferEnergyToNearbyEntities(serverLevel, centerPos, suppressEnergy, causingPlayer, nearbyEntities, finalEntity);
                }
            });
        }
    }

    /**
     * 查找附近的实体
     */
    private static List<Entity> findNearbyEntities(ServerLevel level, Vec3 centerPos, double radius) {
        AABB area = new AABB(
                centerPos.x - radius, centerPos.y - radius, centerPos.z - radius,
                centerPos.x + radius, centerPos.y + radius, centerPos.z + radius
        );
        return level.getEntities(null, area);
    }

    /**
     * 将能量传递给附近的实体
     */
    private static void transferEnergyToNearbyEntities(ServerLevel level, Vec3 centerPos, float suppressEnergy,
                                                       ServerPlayer causingPlayer, List<Entity> nearbyEntities,
                                                       Entity sourceEntity) {
        // 计算传递的能量值
        float transferEnergy = suppressEnergy * ENERGY_TRANSFER_RATIO;

        // 限制传递目标数量
        int transferCount = Math.min(nearbyEntities.size(), MAX_TRANSFER_TARGETS);

        for (int i = 0; i < transferCount; i++) {
            Entity target = nearbyEntities.get(i);

            // 跳过源实体自身（防止能量传递回正在爆炸的实体导致循环爆炸）
            if (target == sourceEntity) continue;
            // 跳过玩家和非生物实体
            if (target instanceof ServerPlayer || !(target instanceof LivingEntity livingTarget)) continue;

            // 检查目标是否已有镇压效果
            if (hasSuppressEffect(livingTarget)) {
                continue;
            }

            // 设置目标实体的镇压能量
            target.getPersistentData().putDouble("suppress_energy", transferEnergy);
            target.getPersistentData().putBoolean("is_explosive", true);

            if (causingPlayer != null) {
                target.getPersistentData().putString("explosion_player", causingPlayer.getUUID().toString());
            }

            // 创建能量传递视觉效果
            createEnergyTransferEffect(level, centerPos, target.position());
        }
    }

    /**
     * 检查目标是否已有镇压效果
     */
    private static boolean hasSuppressEffect(LivingEntity entity) {
        // 检查是否有镇压效果状态
        if (entity.hasEffect(KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get())) {
            return true;
        }

        // 检查是否已有镇压能量
        return entity.getPersistentData().contains("suppress_energy");
    }

    /**
     * 创建能量传递视觉效果
     */
    private static void createEnergyTransferEffect(ServerLevel level, Vec3 startPos, Vec3 endPos) {
        // 计算方向向量
        Vec3 direction = endPos.subtract(startPos).normalize();

        // 计算粒子数量（基于距离）
        double distance = startPos.distanceTo(endPos);
        int particleCount = (int) (distance * 2);

        // 创建能量束粒子效果
        for (int i = 0; i < particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 particlePos = startPos.add(direction.scale(distance * progress));

            // 添加随机偏移
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            // 发送粒子
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    particlePos.x + offsetX,
                    particlePos.y + offsetY,
                    particlePos.z + offsetZ,
                    1, // 粒子数量
                    0, 0, 0, // 速度
                    0.1 // 速度范围
            );
        }
    }

    /**
     * 计算视觉效果强度
     * @param entity 目标实体
     * @param suppressEnergy 镇压能量
     * @return 视觉效果强度 (0-10)
     */
    private static float calculateVisualIntensity(LivingEntity entity, float suppressEnergy) {
        // 基础强度 = 镇压能量 / 200 (范围0-10)
        float baseIntensity = suppressEnergy / 200.0f;

        // 效果等级加成 (每级+15%)
        float effectMultiplier = 1.0f;
        MobEffectInstance effect = entity.getEffect(KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get());
        if (effect != null) {
            effectMultiplier += effect.getAmplifier() * 0.15f;
        }

        return Math.min(baseIntensity * effectMultiplier, 15.0f);
    }

    /**
     * 根据镇压能量生成真实爆炸
     * @param energy 0-2000
     */
    private static void createLevelExplosion(ServerLevel level, Vec3 pos, float energy,
                                             LivingEntity livingEntity, ServerPlayer causingPlayer) {
        CompoundTag persistentData = livingEntity.getPersistentData();
        String form = persistentData.getString(FORM_TAG_KEY);

        float radius = 5.0F;
        float maxRadius = 30.0F;

        if (RISING_MIGHTY_FORM.equals(form)) {
            radius = maxRadius;
        } else {
            radius = Math.min(energy / 100F, maxRadius);
        }

        float strength = radius / 1.125F;

        boolean wasInvulnerable = false;
        if (causingPlayer != null) {
            wasInvulnerable = causingPlayer.isInvulnerable();
            causingPlayer.setInvulnerable(true);
        }

        try {
            Explosion explosion = new Explosion(
                    level,
                    causingPlayer,
                    pos.x, pos.y, pos.z,
                    strength,
                    false,
                    Explosion.BlockInteraction.DESTROY
            );
            explosion.explode();
            explosion.finalizeExplosion(true);
        } finally {
            if (causingPlayer != null) {
                causingPlayer.setInvulnerable(wasInvulnerable);
            }
        }

        createShockwaveEffect(level, pos, radius);
    }


    /**
     * 创建冲击波效果
     */
    private static void createShockwaveEffect(ServerLevel level, Vec3 center, float radius) {
        // 冲击波粒子数量根据半径计算
        int particleCount = (int) (radius * 10);

        // 在爆炸中心向外扩散的圆形上生成粒子
        for (int i = 0; i < particleCount; i++) {
            double angle = Math.PI * 2 * i / particleCount;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            // 在圆形上均匀分布粒子
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    center.x + dx,
                    center.y,
                    center.z + dz,
                    5, // 每个点5个粒子
                    0, 0.5, 0, // 随机偏移
                    0.2 // 速度
            );
        }

        // 添加垂直冲击波效果
        for (int i = 0; i < particleCount / 2; i++) {
            double height = (level.random.nextDouble() - 0.5) * radius;
            level.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x,
                    center.y + height,
                    center.z,
                    3,
                    radius * 0.3, 0, radius * 0.3,
                    0.1
            );
        }
    }

    private static void createParticleEffects(Level level, Vec3 centerPos, float intensity, Entity entity) {
        if (level instanceof ServerLevel serverLevel) {
            // 获取实体碰撞箱宽度作为基础大小
            float entityWidth = entity.getBbWidth();
            // 计算粒子大小比例 (1.0为基础大小，根据实体大小调整)
            float sizeScale = Math.max(5.0f, entityWidth * 1.5f);

            // 获取自定义粒子类型
            SimpleParticleType particle = KamenRiderInterwovenWorldsModParticleTypes.SPECIAL_EFFECTS_EXPLOSION.get();

            // 自定义爆炸粒子 - 根据实体大小调整数量和范围
            serverLevel.sendParticles(particle,
                    centerPos.x, centerPos.y, centerPos.z,
                    (int)(intensity * 15 * sizeScale),  // 粒子数量随大小增加
                    intensity * 0.3 * sizeScale,       // X轴扩散范围
                    intensity * 0.3 * sizeScale,       // Y轴扩散范围
                    intensity * 0.3 * sizeScale,       // Z轴扩散范围
                    0.5 * sizeScale);                  // 粒子速度

            // 火花粒子 - 也根据实体大小调整
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    centerPos.x, centerPos.y, centerPos.z,
                    (int)(intensity * 20 * sizeScale),
                    intensity * 0.4 * sizeScale,
                    intensity * 0.4 * sizeScale,
                    intensity * 0.4 * sizeScale,
                    0.2 * sizeScale);

            // 添加边缘粒子效果 - 模拟爆炸冲击波
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    centerPos.x, centerPos.y, centerPos.z,
                    (int)(intensity * 10 * sizeScale),
                    intensity * 0.5 * sizeScale,
                    intensity * 0.5 * sizeScale,
                    intensity * 0.5 * sizeScale,
                    0.3 * sizeScale);
        }
    }

    private static void applyKnockback(Level level, Entity sourceEntity, Vec3 centerPos, float intensity) {
        if (level instanceof ServerLevel serverLevel) {
            // 击退半径 = 强度 * 2.5 (范围0-25)
            double radius = intensity * 2.5;
            // 击退强度 = 强度 * 0.4 (范围0-4)
            double strength = intensity * 0.4;

            SpecialEffectsUtil.createSphericalPushEffect(
                    serverLevel,
                    centerPos,
                    radius,
                    strength,
                    sourceEntity
            );
        }
    }

    private static void applySealingDamage(LivingEntity target, float sealingPower, ServerPlayer causingPlayer) {
        if (target instanceof Monster) {
            // 造成等值镇压能量伤害，伤害来源为玩家
            DamageSources damageSources = causingPlayer.level().damageSources();
            DamageSource damageSource = damageSources.playerAttack(causingPlayer);
            target.hurt(damageSource, sealingPower);
        }
    }

    private static void triggerWhiteScreenEffect(ServerPlayer player, float sealingPower) {
        // 白屏持续时间 = 1.0 + 封印能量/1000 (范围1.0-3.0秒)
        float duration = 1.0f + sealingPower / 1000.0f;
        SpecialEffectsUtil.triggerComicBlackWhiteEffectInRadius(player, 3.0f, 1.0f, 10.0f, true);
    }

    private static boolean shouldTriggerExplosion(LivingEntity entity) {
        return entity.getPersistentData().getBoolean("is_explosive");
    }

    private static ServerPlayer getCausingPlayer(Level level, Entity entity) {
        String explosionPlayerUUID = entity.getPersistentData().getString("explosion_player");
        if (!explosionPlayerUUID.isEmpty() && level instanceof ServerLevel serverLevel) {
            try {
                return serverLevel.getServer().getPlayerList().getPlayer(UUID.fromString(explosionPlayerUUID));
            } catch (IllegalArgumentException e) {
                KamenRiderInterwovenWorldsMod.LOGGER.error("无效的UUID格式: {}", explosionPlayerUUID, e);
            }
        }
        return null;
    }
}