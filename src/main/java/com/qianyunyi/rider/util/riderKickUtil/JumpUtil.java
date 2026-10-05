package com.qianyunyi.rider.util.riderKickUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 骑士踢起跳：垂直/斜向起跳 + 重力落回
 */
public final class JumpUtil {

    /**
     * 让目标玩家起跳
     * @param player   目标玩家（仅服务端）
     * @param hSpeed   水平速度倍数（1.0 = 沿视线水平，0 = 纯垂直）
     * @param vSpeed   垂直初始速度（方块/秒）
     * @param duration Tick 时长，用于 Tick 任务结束检测（一般留 40-60 Tick）
     * @param onJump   起跳完成回调（立即执行）
     */
    public static void jump(ServerPlayer player,
                            double hSpeed,
                            double vSpeed,
                            int duration,
                            Runnable onJump) {
        jump((Entity) player, hSpeed, vSpeed, duration, onJump);
    }


    /**
     * 生物起跳
     * @param entity  目标生物（仅服务端）
     * @param hSpeed  水平速度倍数（1.0 = 沿视线水平，0 = 纯垂直）
     * @param vSpeed  垂直初始速度（方块/秒）
     * @param duration Tick 时长，用于 Tick 任务结束检测（一般留 40-60 Tick）
     * @param onJump  起跳完成回调（立即执行）
     */
    public static void jump(Entity entity,
                            double hSpeed,
                            double vSpeed,
                            int duration,
                            Runnable onJump) {
        // 添加缓降效果（防止落地受伤）
        if (entity instanceof LivingEntity livingEntity) {
            // 缓降效果持续时间：duration + 20 tick（额外20tick确保安全落地）
            int slowFallingDuration = duration + 20;

            // 添加缓降效果，等级1，不显示粒子效果，不显示图标
            MobEffectInstance slowFalling = new MobEffectInstance(
                    MobEffects.SLOW_FALLING,  // 缓降效果
                    slowFallingDuration,      // 持续时间
                    0,                       // 等级（0为基础等级）
                    false,                   // 环境粒子效果
                    false,                   // 图标显示
                    true                    // 可被信标等效果影响
            );

            // 添加效果
            livingEntity.addEffect(slowFalling);

            // 可选：添加抗性提升效果，减少落地伤害
            MobEffectInstance resistance = new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    slowFallingDuration,
                    1,                       // 等级1（减少20%伤害）
                    false,
                    false,
                    true
            );

            livingEntity.addEffect(resistance);
        }

        // 计算速度向量 - 使用玩家旋转角度
        float yRot = entity.getYRot(); // 获取玩家的水平旋转角度（偏航角）
        double radianYRot = Math.toRadians(yRot);
        double x = -Math.sin(radianYRot); // 水平方向X分量
        double z = Math.cos(radianYRot);  // 水平方向Z分量

        Vec3 vel = new Vec3(
                x * hSpeed,
                vSpeed / 20.0, // 转换为每tick速度
                z * hSpeed
        );

        // 设置速度并强制同步
        entity.setDeltaMovement(vel);
        entity.hurtMarked = true; // 关键：强制同步运动状态

        // 添加粒子效果和音效
        if (entity.level() instanceof ServerLevel serverLevel) {
            // 添加云粒子效果
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.CLOUD,
                    entity.getX(), entity.getY(), entity.getZ(),
                    20, 0.5, 0.5, 0.5, 0.2
            );

            // 添加缓降粒子效果
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.POOF,
                    entity.getX(), entity.getY(), entity.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1
            );
        }

        // 播放起跳音效
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0F, 0.8F);

        // 播放缓降音效
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                net.minecraft.sounds.SoundEvents.ELYTRA_FLYING,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5F, 1.2F);

        // 立即执行起跳完成回调
        onJump.run();
    }

    /**
     * 为骑士踢添加缓降效果（在骑士踢技能中使用）
     * @param entity 目标实体
     * @param duration 缓降持续时间（tick）
     */
    public static void addSlowFallingForRiderKick(Entity entity, int duration) {
        if (entity instanceof LivingEntity livingEntity) {
            // 添加缓降效果
            MobEffectInstance slowFalling = new MobEffectInstance(
                    MobEffects.SLOW_FALLING,
                    duration,
                    0,
                    false,
                    false,
                    false
            );

            livingEntity.addEffect(slowFalling);

            // 添加抗性提升效果
            MobEffectInstance resistance = new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    duration,
                    2,  // 等级2（减少40%伤害），骑士踢需要更强的保护
                    false,
                    false,
                    false
            );

            livingEntity.addEffect(resistance);

            // 添加粒子效果
            if (entity.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = entity.position();
                for (int i = 0; i < 20; i++) {
                    double offsetX = (serverLevel.getRandom().nextDouble() - 0.5) * 2;
                    double offsetY = serverLevel.getRandom().nextDouble() * 2;
                    double offsetZ = (serverLevel.getRandom().nextDouble() - 0.5) * 2;
                    serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.CLOUD,
                            pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                            3, 0.1, 0.1, 0.1, 0.05
                    );
                }
            }

            // 播放音效
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    1.0F, 0.5F
            );
        }
    }

    /**
     * 移除缓降效果
     * @param entity 目标实体
     */
    public static void removeSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            // 移除缓降效果
            livingEntity.removeEffect(MobEffects.SLOW_FALLING);

            // 移除抗性提升效果
            livingEntity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
    }

    /**
     * 检查实体是否有缓降效果
     * @param entity 目标实体
     * @return 是否有缓降效果
     */
    public static boolean hasSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.hasEffect(MobEffects.SLOW_FALLING);
        }
        return false;
    }
}