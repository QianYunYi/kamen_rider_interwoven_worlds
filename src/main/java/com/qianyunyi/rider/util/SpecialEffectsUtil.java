package com.qianyunyi.rider.util;

import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.MessageComicBlackWhiteEffect;
import com.qianyunyi.rider.network.packet.WhiteScreenEffectMessage;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import java.util.List;

public class SpecialEffectsUtil {


    /**
     * 使用原版击退机制创建球形推力效果
     */
    public static void createSphericalPushEffect(ServerLevel level, Vec3 center, double radius, double strength, Entity centerEntity) {

        // 计算球形区域的边界框
        AABB area = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        // 获取区域内的所有实体
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != centerEntity && entity.isPushable()
        );


        for (LivingEntity entity : entities) {
            // 计算从中心到实体的向量
            Vec3 toEntity = entity.position().subtract(center);
            double distance = toEntity.length();
            // 如果实体在球体内
            if (distance < radius) {
                // 计算击退方向 (从中心向外推)
                Vec3 direction = toEntity.normalize().reverse();

                // 根据距离调整击退强度
                double distanceFactor = 1.0 - (distance / radius);
                double knockbackStrength = strength * distanceFactor;

                // 创建击退效果
                applyKnockback(entity, centerEntity, direction, knockbackStrength);
            }
        }

        // 处理抛射物实体
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                entity -> entity != centerEntity
        );

        for (Projectile projectile : projectiles) {
            // 计算从中心到抛射物的向量
            Vec3 toProjectile = projectile.position().subtract(center);
            double distance = toProjectile.length();

            // 如果抛射物在球体内
            if (distance < radius) {
                // 计算方向 (从中心向外推)
                Vec3 direction = toProjectile.normalize().reverse();

                // 根据距离调整速度
                double distanceFactor = 1.0 - (distance / radius);
                Vec3 newVelocity = direction.scale(strength * 0.5 * distanceFactor);

                // 应用新速度
                projectile.setDeltaMovement(newVelocity);
                projectile.hurtMarked = true;
            }
        }
    }

    /**
     * 应用原版击退效果
     */
    private static void applyKnockback(LivingEntity target, Entity source, Vec3 direction, double strength) {

        // 为击退创建源实体（如果源是玩家则更真实）
        Entity sourceEntity = source instanceof Player ? source : null;

        // 对生物应用击退
        target.knockback(strength, direction.x, direction.z);
        target.hurtMarked = true;

        // 如果是怪物，设置最后一个伤害源
        if (target instanceof Mob mob && sourceEntity != null) {
            mob.setLastHurtByMob(sourceEntity instanceof LivingEntity ? (LivingEntity) sourceEntity : null);
        }

        // 可选：应用伤害
        // float damageAmount = 0.5f; // 轻微伤害不会杀死但会产生效果
        // target.hurt(DamageSource.mobAttack((LivingEntity) source), damageAmount);
    }

    /**
     * 在玩家位置创建球形推力效果
     */
    public static void createPlayerSphericalPush(Player player, double radius, double strength) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 使用玩家位置作为中心点
            Vec3 center = player.position();
            createSphericalPushEffect(serverLevel, center, radius, strength, player);
        }
    }


        /**
         * 触发默认白屏眩晕效果（1秒，中等强度）
         *
         * @param player 目标玩家
         */
        public static void triggerDefaultWhiteScreenEffect(ServerPlayer player) {
            triggerWhiteScreenEffect(player, 1.0f, 0.7f);
        }

    /**
     * 触发白屏眩晕效果
     *
     * @param player 目标玩家
     * @param duration 持续时间（秒）
     * @param intensity 强度（0-1）
     */
    public static void triggerWhiteScreenEffect(ServerPlayer player, float duration, float intensity) {
        NetworkHandler.sendToPlayer(new WhiteScreenEffectMessage(duration, intensity), player);
    }

    /**触发黑白闪效果
     *
     *
     */
    public static void triggerComicBlackWhiteEffectInRadius(ServerPlayer center, float radius, float duration, float intensity, boolean excludeSelf) {
        ServerLevel level = center.serverLevel().getLevel();

        for (ServerPlayer player : level.players()) {
            // 排除自身检查
            if (excludeSelf && player == center) {
                continue;
            }

            double distance = player.distanceTo(center);
            if (distance <= radius) {
                float distanceFactor = 1.0f - (float)(distance / radius);
                float adjustedIntensity = intensity * distanceFactor;
                DelayedTask.chain(center.level())
                        .then(10, () -> NetworkHandler.sendToTracking(center, new MessageComicBlackWhiteEffect(true, 40, 0.5f, center.getId(), "comic_black_white", 1.0f)))
                        .then(10, () -> NetworkHandler.sendToTracking(center, new MessageComicBlackWhiteEffect(true, 20, 0.5f, center.getId(), "comic_black_white", 0.0f)))
                        .start();
            }
        }
    }

    /**
     * 对范围内的所有玩家触发白屏效果（可排除自身）
     *
     * @param center 中心玩家
     * @param radius 半径
     * @param duration 持续时间（秒）
     * @param intensity 强度（0-1）
     * @param excludeSelf 是否排除自身
     */
    public static void triggerWhiteScreenEffectInRadius(ServerPlayer center, float radius, float duration, float intensity, boolean excludeSelf) {
        ServerLevel level = center.serverLevel().getLevel();

        for (ServerPlayer player : level.players()) {
            // 排除自身检查
            if (excludeSelf && player == center) {
                continue;
            }

            double distance = player.distanceTo(center);
            if (distance <= radius) {
                float distanceFactor = 1.0f - (float)(distance / radius);
                float adjustedIntensity = intensity * distanceFactor;
                triggerWhiteScreenEffect(player, duration, adjustedIntensity);
            }
        }
    }


    /**
     * 创建闪电圆形冲击波效果（逐圈扩展版）
     * @param level 世界
     * @param centerEntity 中心实体
     * @param maxRadius 最大半径
     * @param strength 击退强度
     * @param particleCount 粒子数量
     * @param durationTicks 持续时间(刻)
     */
    public static void createCircularShockwave(ServerLevel level, Entity centerEntity,
                                               double maxRadius, double strength,
                                               int particleCount, int durationTicks) {
        // 初始爆发效果
        level.sendParticles(ParticleTypes.FLASH,
                centerEntity.getX(), centerEntity.getY() + 0.5, centerEntity.getZ(),
                5, 0.3, 0.3, 0.3, 0);

        // 计算每圈扩展的间隔（刻）
        int intervalTicks = 5;
        // 计算总圈数（从1到maxRadius，每圈半径增加1）
        int totalCircles = (int) maxRadius;

        // 使用DelayedTask逐圈扩展
        for (int circle = 1; circle <= totalCircles; circle++) {
            final int currentCircle = circle;
            // 使用DelayedTask安排任务
            DelayedTask.run(level, circle * intervalTicks, (l) -> {
                createShockwaveCircle(level, centerEntity, maxRadius, strength,
                        particleCount, currentCircle, totalCircles);
            });
        }
    }

    private static void createShockwaveCircle(ServerLevel level, Entity entity,
                                              double maxRadius, double strength,
                                              int particleCount, int currentCircle,
                                              int totalCircles) {
        // 当前圈半径
        double currentRadius = currentCircle;

        // 获取自定义粒子
        //SimpleParticleType lightningParticle = (SimpleParticleType) RiderAdventureAffiliatedModParticleTypes.LIGHTNING.get();

        // 生成当前圈的粒子环
        for (int i = 0; i < particleCount; i++) {
            double angle = 2 * Math.PI * i / particleCount;
            double x = entity.getX() + currentRadius * Mth.cos((float)angle);
            double z = entity.getZ() + currentRadius * Mth.sin((float)angle);
            double y = entity.getY() + 0.5;

            // 使用粒子组合
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.1, 0.05, 0.1, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.02, 0.05, 0.05);
            //level.sendParticles(lightningParticle, x, y, z, 1, 0.05, 0.02, 0.05, 0.05);
        }

        // 执行击退效果（范围跟随当前半径）
        createSphericalPushEffect(
                level,
                entity.position(),
                currentRadius,
                strength * (1 - (currentCircle / (double)totalCircles)),
                entity
        );
    }

    /**
     * 高亮周围scope格内的敌人
     * @param scope 范围
     * @param player 玩家
     * @param duration 持续时间（tick）
     */
    public static void highlightNearbyEnemies(float scope, Player player, int duration) {
        // 查找周围scope格内的怪物
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(scope),
                entity ->
                        entity != player && // 排除自己
                                entity.isAlive() && // 只选择活着的实体
                                !entity.isSpectator() // 排除旁观者
        );

        for (LivingEntity entity : nearbyEntities) {
            // 给予发光效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, duration, 0, false, false
            ));
        }
    }

    /**
     * 清除周围scope格内敌人的发光效果
     * @param scope 范围
     * @param player 玩家
     */
    public static void clearHighlightNearbyEnemies(float scope, Player player) {
        // 查找周围scope格内的怪物
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(scope),
                entity ->
                        entity != player && // 排除自己
                                entity.isAlive() && // 只选择活着的实体
                                !entity.isSpectator() // 排除旁观者
        );

        for (LivingEntity entity : nearbyEntities) {
            // 移除发光效果
            entity.removeEffect(MobEffects.GLOWING);
        }
    }

    /**
     * 在原地生成黑雾效果（用于瞬移离场时的视觉特效）
     * 使用纯黑色 Dust 粒子，聚成人形轮廓（约 0.6宽 × 1.8高 × 0.3厚）
     * @param level 服务端世界
     * @param position 生成位置（脚底）
     */
    public static void spawnBlackMist(ServerLevel level, Vec3 position) {
        // 纯黑色 Dust 粒子（RGB 极低 ≈ 深黑）
        DustParticleOptions blackDust = new DustParticleOptions(new Vector3f(0.05f, 0.05f, 0.05f), 1.8f);
        // 稍亮一点的暗灰色，增加层次
        DustParticleOptions darkGrayDust = new DustParticleOptions(new Vector3f(0.12f, 0.12f, 0.12f), 1.5f);

        // 人形参数（以脚底 position 为原点）
        double bodyWidth = 0.6;    // 身体宽度（肩宽）
        double bodyDepth = 0.3;    // 身体厚度
        double bodyHeight = 1.8;   // 总身高
        double headRadius = 0.25;  // 头部半径

        // === 躯干（主体：椭圆柱形，从脚0.1到头顶1.6）===
        for (int i = 0; i < 60; i++) {
            // Y 从 0.1 到 1.6，躯干范围
            double y = 0.1 + level.random.nextDouble() * 1.5;
            // 椭圆截面，越往上肩越宽（上宽下窄）
            double widthScale = 0.7 + (y / 1.8) * 0.3;
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(level.random.nextDouble()) * 0.9; // 0~0.9 半径随机
            double dx = Math.cos(angle) * r * bodyWidth * widthScale;
            double dz = Math.sin(angle) * r * bodyDepth * widthScale;

            // 中心区域用纯黑，边缘用暗灰
            DustParticleOptions particle = r < 0.5 ? blackDust : darkGrayDust;
            level.sendParticles(particle,
                    position.x + dx,
                    position.y + y,
                    position.z + dz,
                    1, 0.02, 0.02, 0.02, 0.001);
        }

        // === 头部（球形，在躯干顶部，Y 1.5~1.9）===
        for (int i = 0; i < 20; i++) {
            double theta = level.random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double r = headRadius * Math.cbrt(level.random.nextDouble());
            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.cos(phi) + 1.7;
            double dz = r * Math.sin(phi) * Math.sin(theta);

            level.sendParticles(blackDust,
                    position.x + dx,
                    position.y + dy,
                    position.z + dz,
                    1, 0.02, 0.02, 0.02, 0.001);
        }

        // === 肩部加宽（强调肩宽，Y 1.4~1.6）===
        for (int i = 0; i < 15; i++) {
            double y = 1.4 + level.random.nextDouble() * 0.2;
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(level.random.nextDouble()) * 0.7;
            double dx = Math.cos(angle) * r * bodyWidth * 1.3;
            double dz = Math.sin(angle) * r * bodyDepth * 0.6;

            level.sendParticles(darkGrayDust,
                    position.x + dx,
                    position.y + y,
                    position.z + dz,
                    1, 0.02, 0.02, 0.02, 0.001);
        }

        // === 双腿（从脚0.0到0.4，两条腿分叉）===
        for (int i = 0; i < 20; i++) {
            double y = level.random.nextDouble() * 0.4;
            // 左腿或右腿偏移
            double legOffset = (level.random.nextBoolean() ? -0.2 : 0.2);
            double dx = legOffset + (level.random.nextDouble() - 0.5) * 0.2;
            double dz = (level.random.nextDouble() - 0.5) * 0.2;

            level.sendParticles(blackDust,
                    position.x + dx,
                    position.y + y,
                    position.z + dz,
                    1, 0.02, 0.02, 0.02, 0.001);
        }
    }
}