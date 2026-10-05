package com.qianyunyi.rider.internal.combat.skill.kuuga;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class DarkSurgePalm implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "暗黑波动掌";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    private static final String DARK_SURGE_PALM_ANIM = "dark_surge_palm";
    private static final Random random = new Random();

    // 光柱参数
    private static final int CYLINDER_LENGTH_SEGMENTS = 20; // 光柱长度分段
    private static final int RADIAL_SEGMENTS = 12; // 径向分段
    private static final float MIN_RADIUS = 0.4f; // 核心最小半径
    private static final float MAX_RADIUS = 2.0f; // 外部最大半径
    private static final float TRAVEL_SPEED = 2.5f; // 光柱飞行速度
    private static final float MAX_DISTANCE = 25.0f; // 最大距离

    // 黑紫色渐变粒子颜色定义
    private final DustParticleOptions CORE_PARTICLE = new DustParticleOptions(
            new Vector3f(0.7f, 0.1f, 1.0f), 2.0f  // 核心亮紫色
    );

    private final DustParticleOptions MIDDLE_PARTICLE = new DustParticleOptions(
            new Vector3f(0.5f, 0.0f, 0.7f), 1.8f  // 中间紫色
    );

    private final DustParticleOptions OUTER_PARTICLE = new DustParticleOptions(
            new Vector3f(0.3f, 0.0f, 0.5f), 1.5f  // 外层深紫色
    );

    private final DustParticleOptions EDGE_PARTICLE = new DustParticleOptions(
            new Vector3f(0.1f, 0.0f, 0.3f), 1.2f  // 边缘暗紫色
    );

    // 技能配置参数
    // 爆炸参数
    private static final float EXPLOSION_RADIUS = 3.5f;
    // 击退参数
    private static final float KNOCKBACK_STRENGTH = 1.8f;
    // 冷却时间参数
    private static final int COOLDOWN_SECONDS = 25;
    // 黑暗效果持续时间参数
    private static final int DARKNESS_DURATION = 5;
    // 虚弱效果持续时间参数
    private static final int WEAKNESS_DURATION = 3;

    // 伤害分次施加参数
    private static final int DAMAGE_HITS = 10;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;

    private long cooldownStartTick = 0;

    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;
        data.setSkillActive(true);
        cooldownStartTick = serverPlayer.level().getGameTime();

        // 播放动画
        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(DARK_SURGE_PALM_ANIM, serverPlayer.getUUID(), true), serverPlayer);

        //给予自身1s不可移动
        serverPlayer.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(),
                20,
                0,
                false,
                false
        ));

        // 播放蓄力音效
        serverPlayer.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f);

        // 在玩家手部生成初始效果
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                DelayedTask.run(serverLevel, i, () -> {
                    Vec3 handPos = getPlayerHandPosition(serverPlayer);
                    Vec3 offset = new Vec3(
                            random.nextDouble() - 0.5,
                            random.nextDouble() - 0.5,
                            random.nextDouble()
                    ).normalize().scale(0.4);

                    serverLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            handPos.x + offset.x,
                            handPos.y + offset.y,
                            handPos.z + offset.z,
                            2, 0.08, 0.08, 0.08, 0.015
                    );
                    serverLevel.sendParticles(
                            (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.DARK_LIGHTNING.get(),
                            handPos.x, handPos.y, handPos.z,
                            5, 0.05, 0.05, 0.05, 0.01
                    );
                });
            }
        }

        // 延迟20tick后发射光柱
        DelayedTask.run(serverPlayer.level(), 20, () -> {
            if (serverPlayer.isAlive()) {
                createDarkWavePalmBeam(serverPlayer);
            } else {
                data.setSkillActive(false);
            }
        });
    }

    /**
     * 创建暗黑波动掌光柱
     */
    private void createDarkWavePalmBeam(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            RiderPlayerVariablesProvider.get(player).setSkillActive(false);
            return;
        }

        // 计算起点和终点
        Vec3 startPos = getPlayerHandPosition(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = startPos.add(lookVec.scale(MAX_DISTANCE));

        // 进行射线检测，寻找碰撞点
        Vec3 hitPos = performRaycast(player, startPos, endPos);
        double distance = startPos.distanceTo(hitPos);

        // 计算方向向量
        Vec3 direction = hitPos.subtract(startPos).normalize();

        // 播放发射音效
        player.playSound(SoundEvents.WITHER_SHOOT, 1.3f, 0.8f);

        // 计算单位长度
        double segmentLength = distance / CYLINDER_LENGTH_SEGMENTS;

        // 创建光柱粒子
        for (int segment = 0; segment <= CYLINDER_LENGTH_SEGMENTS; segment++) {
            int delay = (int) (segment * segmentLength / TRAVEL_SPEED);
            int currentSegment = segment;

            DelayedTask.run(serverLevel, delay, () -> {
                // 计算当前分段位置
                double progress = (double) currentSegment / CYLINDER_LENGTH_SEGMENTS;
                Vec3 currentPos = startPos.add(direction.scale(distance * progress));

                // 根据光柱位置计算半径（中间粗两端细）
                float radiusFactor = 1.0f;
                if (progress < 0.3f) {
                    radiusFactor = (float) (progress / 0.3f);
                } else if (progress > 0.7f) {
                    radiusFactor = (float) (1.0f - (progress - 0.7f) / 0.3f);
                }

                float currentRadius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * radiusFactor;

                // 生成径向粒子
                for (int radial = 0; radial < RADIAL_SEGMENTS; radial++) {
                    double angle = 2 * Math.PI * radial / RADIAL_SEGMENTS;

                    // 计算径向偏移
                    Vec3 right = getPerpendicularVector(direction).normalize();
                    Vec3 up = direction.cross(right).normalize();

                    float radiusRatio = (float) radial / RADIAL_SEGMENTS;
                    float particleRadius = currentRadius * radiusRatio;

                    Vec3 radialOffset = right.scale(Math.cos(angle) * particleRadius)
                            .add(up.scale(Math.sin(angle) * particleRadius));

                    Vec3 particlePos = currentPos.add(radialOffset);

                    // 根据半径比例选择粒子颜色（从中心向外渐变）
                    DustParticleOptions particleType;
                    if (radiusRatio < 0.25f) {
                        particleType = CORE_PARTICLE;  // 核心亮紫色
                    } else if (radiusRatio < 0.5f) {
                        particleType = MIDDLE_PARTICLE;  // 中间紫色
                    } else if (radiusRatio < 0.75f) {
                        particleType = OUTER_PARTICLE;  // 外层深紫色
                    } else {
                        particleType = EDGE_PARTICLE;  // 边缘暗紫色
                    }

                    // 添加一些随机偏移使渐变更自然
                    float randomOffset = 0.08f;
                    Vec3 finalPos = particlePos.add(
                            (random.nextFloat() - 0.5f) * randomOffset,
                            (random.nextFloat() - 0.5f) * randomOffset,
                            (random.nextFloat() - 0.5f) * randomOffset
                    );

                    // 生成粒子
                    serverLevel.sendParticles(
                            particleType,
                            finalPos.x, finalPos.y, finalPos.z,
                            2, 0, 0, 0, 0
                    );

                    // 在边缘添加暗黑闪电粒子增强效果
                    if (radiusRatio > 0.8f && random.nextFloat() < 0.3f) {
                        if (random.nextFloat() < 0.2f) {
                            serverLevel.sendParticles(
                                    (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.DARK_LIGHTNING.get(),
                                    finalPos.x, finalPos.y, finalPos.z,
                                    2, 0.05, 0.05, 0.05, 0.01
                            );
                        }
                    }
                }

                // 在光柱中心添加高亮核心
                if (currentSegment % 3 == 0) {
                    for (int i = 0; i < 2; i++) {
                        Vec3 coreOffset = new Vec3(
                                (random.nextFloat() - 0.5f) * 0.15,
                                (random.nextFloat() - 0.5f) * 0.15,
                                (random.nextFloat() - 0.5f) * 0.15
                        );

                        serverLevel.sendParticles(
                                ParticleTypes.DRAGON_BREATH,
                                currentPos.x + coreOffset.x,
                                currentPos.y + coreOffset.y,
                                currentPos.z + coreOffset.z,
                                2, 0, 0, 0, 0.04
                        );
                    }
                }

                // 如果是最后一节，触发爆炸
                if (currentSegment == CYLINDER_LENGTH_SEGMENTS) {
                    createBeamImpactEffect(serverLevel, player, hitPos);

                    // 结束技能
                    RiderPlayerVariablesProvider.get(player).setSkillActive(false);
                }
            });
        }
    }

    /**
     * 执行射线检测
     */
    private Vec3 performRaycast(ServerPlayer player, Vec3 startPos, Vec3 endPos) {
        ServerLevel level = (ServerLevel) player.level();
        double maxDistance = MAX_DISTANCE;
        Vec3 lookVec = player.getLookAngle();

        // 进行方块碰撞检测
        var blockHitResult = level.clip(new ClipContext(
                startPos,
                startPos.add(lookVec.scale(maxDistance)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult.getLocation();
        }

        // 进行实体碰撞检测
        LivingEntity targetEntity = null;
        double closestDistance = maxDistance;

        AABB aabb = new AABB(
                startPos.x - 1.0, startPos.y - 1.0, startPos.z - 1.0,
                startPos.x + 1.0, startPos.y + 1.0, startPos.z + 1.0
        ).expandTowards(lookVec.scale(maxDistance)).inflate(2.0);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb,
                entity -> entity.isAlive()
                        && !RiderCompanionUtil.isPlayerCompanion(entity, player.getUUID()));

        for (LivingEntity entity : entities) {
            var entityHitResult = entity.getBoundingBox().clip(startPos, endPos);
            if (entityHitResult.isPresent()) {
                double distance = startPos.distanceTo(entityHitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetEntity = entity;
                }
            }
        }

        if (targetEntity != null) {
            return targetEntity.getBoundingBox().getCenter();
        }

        return endPos;
    }

    /**
     * 获取玩家手部位置
     */
    private Vec3 getPlayerHandPosition(ServerPlayer player) {
        // 根据玩家朝向计算手部位置
        Vec3 lookVec = player.getLookAngle();
        Vec3 right = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 up = lookVec.cross(right).normalize();

        // 假设是右手发射技能
        Vec3 handOffset = right.scale(-0.6).add(up.scale(-0.5));return player.getEyePosition().add(handOffset);
    }

    /**
     * 创建光柱命中效果
     */
    private void createBeamImpactEffect(ServerLevel serverLevel, ServerPlayer player, Vec3 impactPos) {
        // 播放命中音效
        serverLevel.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.5f, 0.8f);

        // 核心爆炸
        for (int i = 0; i < 25; i++) {
            Vec3 offset = new Vec3(
                    (random.nextDouble() - 0.5) * 1.8,
                    (random.nextDouble() - 0.5) * 1.8,
                    (random.nextDouble() - 0.5) * 1.8
            );

            serverLevel.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    impactPos.x + offset.x,
                    impactPos.y + offset.y,
                    impactPos.z + offset.z,
                    1, 0, 0, 0, 0.15
            );
        }

        // 暗紫色冲击波
        for (int ring = 0; ring < 3; ring++) {
            int currentRing = ring;
            DelayedTask.run(serverLevel, currentRing * 2, () -> {
                float radius = 1.0f + currentRing * 1.0f;
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    DustParticleOptions ringParticle = currentRing == 0 ? CORE_PARTICLE :
                            currentRing == 1 ? OUTER_PARTICLE : EDGE_PARTICLE;

                    serverLevel.sendParticles(
                            ringParticle,
                            impactPos.x + x,
                            impactPos.y + 0.5,
                            impactPos.z + z,
                            2, 0, 0.08, 0, 0.08
                    );
                }
            });
        }

        // 对范围内的所有实体造成伤害
        applyAreaDamage(player, impactPos);

        // 播放额外的打击音效
        player.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 0.6f);
    }

    /**
     * 应用范围伤害
     */
    private void applyAreaDamage(ServerPlayer player, Vec3 center) {
        // 获取范围内的所有实体
        AABB aabb = new AABB(
                center.x - EXPLOSION_RADIUS, center.y - EXPLOSION_RADIUS, center.z - EXPLOSION_RADIUS,
                center.x + EXPLOSION_RADIUS, center.y + EXPLOSION_RADIUS, center.z + EXPLOSION_RADIUS
        );

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, aabb,
                entity -> entity.isAlive()
                        && !RiderCompanionUtil.isPlayerCompanion(entity, player.getUUID()));

        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);

        for (LivingEntity targetEntity : entities) {
            // 计算距离衰减
            double distance = targetEntity.distanceToSqr(center.x, center.y, center.z);
            if (distance > EXPLOSION_RADIUS * EXPLOSION_RADIUS) continue;

            // 获取玩家当前属性攻击力
            float distanceFactor = 1.0f - (float)Math.sqrt(distance) / EXPLOSION_RADIUS;
            float damage = calculateTotalDamage(player,distance);

            // 应用伤害
            applyMultiHitDamage(player, targetEntity, damage);
            markAsExplosive(targetEntity,player,dataManager.getActiveFormName());
            applySuppressEffect(targetEntity);

            // 击退效果
            Vec3 knockbackDir = targetEntity.position().subtract(center).normalize();
            float knockbackStrength = 0.8f + distanceFactor * KNOCKBACK_STRENGTH;
            targetEntity.push(
                    knockbackDir.x * knockbackStrength,
                    0.2 + distanceFactor * 0.4,
                    knockbackDir.z * knockbackStrength
            );

            // 添加虚弱效果
            targetEntity.addEffect(new MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS,
                    WEAKNESS_DURATION * 20,
                    0,
                    false,
                    false
                    ));

            // 添加命中粒子效果
            Vec3 hitPos = targetEntity.getBoundingBox().getCenter();
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        hitPos.x,
                        hitPos.y,
                        hitPos.z,
                        8, 0.2, 0.2, 0.2, 0.1
                );

                // 添加黑紫色命中粒子
                for (int i = 0; i < 6; i++) {
                    Vec3 offset = new Vec3(
                            (random.nextDouble() - 0.5) * 0.4,
                            (random.nextDouble() - 0.5) * 0.4,
                            (random.nextDouble() - 0.5) * 0.4
                    );
                    serverLevel.sendParticles(
                            OUTER_PARTICLE,
                            hitPos.x + offset.x,
                            hitPos.y + offset.y,
                            hitPos.z + offset.z,
                            1, 0, 0, 0, 0
                    );
                }
            }
        }
    }

    /**
     * 获取垂直向量
     */
    private Vec3 getPerpendicularVector(Vec3 direction) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 perpendicular = direction.cross(up);

        if (perpendicular.lengthSqr() < 0.0001) {
            perpendicular = direction.cross(new Vec3(1, 0, 0));
        }

        return perpendicular.normalize();
    }


    /** 计算总伤害 */
    protected float calculateTotalDamage(ServerPlayer serverPlayer,double distance) {
        // 获取玩家当前属性攻击力
        float baseAttack = (float) Objects.requireNonNull(serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
        float distanceFactor = 1.0f - (float)Math.sqrt(distance) / EXPLOSION_RADIUS;
        return baseAttack * 10  * distanceFactor;
    }


    /** 应用多段伤害 */
    protected void applyMultiHitDamage(ServerPlayer serverPlayer, LivingEntity target,
                                       float totalDamage) {
        float baseDamagePerHit = totalDamage * 0.1f ;

        for (int i = 0; i < DAMAGE_HITS; i++) {
            if (!target.isAlive()) break;

            int finalI = i;
            DelayedTask.run(serverPlayer.level(), HIT_DELAY_TICKS * i, () -> {
                if (!target.isAlive()) return;

                float damageVariation = 1.0f + (random.nextFloat() - 0.5f) * (2 * DAMAGE_VARIATION);
                float finalDamage = baseDamagePerHit * damageVariation;

                target.invulnerableTime = 0;
                target.hurt(new PlayerLightningDamageSource(serverPlayer), finalDamage);

                // 每3次伤害播放爆炸音效
                if (finalI % 3 == 0) {
                    serverPlayer.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                            0.5f, 0.9f + random.nextFloat() * 0.2f);
                }
            });
        }
    }


    /** 应用镇压效果 */
    protected void applySuppressEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                20, 8, false, false));
    }

    /** 标记为爆炸性实体 */
    protected void markAsExplosive(LivingEntity target, ServerPlayer serverPlayer,String form) {
        target.getPersistentData().putBoolean("is_explosive", true);
        target.getPersistentData().putString("explosion_player", serverPlayer.getUUID().toString());
        target.getPersistentData().putString("form", form);
    }

    /**
     * 释放技能（可选实现）
     */
    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        // 可选的技能释放逻辑
    }

    /**
     * 检查技能是否可用
     */
    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;

        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "暗黑波动掌冷却中，剩余时间：" + remaining + "秒", ChatFormatting.DARK_PURPLE);
            return false;
        }

        return true;
    }
}