package com.qianyunyi.rider.util;

import com.qianyunyi.rider.entity.effect.FallingBlockEffectEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GroundShatterEffect {

    private static final int DEFAULT_HOVER_TIME = 0;

    public static void createFlyUpEffect(Level level, Vec3 center, double radius,
                                         int duration, float density, int maxDelay) {
        createEffect(level, center, radius, FallingBlockEffectEntity.EffectType.FLY_UP,
                duration, DEFAULT_HOVER_TIME, density, maxDelay, null);
    }

    public static void createFlyUpEffect(Level level, Vec3 center, double radius,
                                         int duration, int hoverTime, float density, int maxDelay) {
        createEffect(level, center, radius, FallingBlockEffectEntity.EffectType.FLY_UP,
                duration, hoverTime, density, maxDelay, null);
    }

    public static void createFlyUpEffect(Level level, Vec3 center, double radius,
                                         int duration, float density, int maxDelay,
                                         ServerPlayer owner) {
        createEffect(level, center, radius, FallingBlockEffectEntity.EffectType.FLY_UP,
                duration, DEFAULT_HOVER_TIME, density, maxDelay, owner);
    }

    public static void createFlyUpEffect(Level level, Vec3 center, double radius,
                                         int duration, int hoverTime, float density, int maxDelay,
                                         ServerPlayer owner) {
        createEffect(level, center, radius, FallingBlockEffectEntity.EffectType.FLY_UP,
                duration, hoverTime, density, maxDelay, owner);
    }

    public static void createCrackGroundEffect(Level level, Vec3 center, double radius,
                                               int duration, float density, int maxDelay) {
        createEffect(level, center, radius, FallingBlockEffectEntity.EffectType.CRACK_GROUND,
                duration, 0, density, maxDelay, null);
    }

    private static void createEffect(Level level, Vec3 center, double radius,
                                     FallingBlockEffectEntity.EffectType effectType,
                                     int duration, int hoverTime, float density, int maxDelay,
                                     ServerPlayer owner) {
        if (level.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level;
        Random random = new Random();

        List<BlockPos> positions = calculateCirclePositions(level, center, radius);

        List<BlockPos> selectedPositions = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (random.nextFloat() <= density) {
                selectedPositions.add(pos);
            }
        }

        for (BlockPos pos : selectedPositions) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            int delay = maxDelay > 0 ? random.nextInt(maxDelay) : 0;

            DelayedTask.run(level, delay, () -> {
                FallingBlockEffectEntity fallingBlock = new FallingBlockEffectEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY(),
                        pos.getZ() + 0.5,
                        state,
                        effectType,
                        center,
                        duration,
                        hoverTime,
                        owner
                );

                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.1
                );

                level.addFreshEntity(fallingBlock);
            });
        }
    }

    private static List<BlockPos> calculateCirclePositions(Level level, Vec3 center, double radius) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = (int) center.x;
        int centerZ = (int) center.z;
        int centerY = (int) center.y;

        int radiusInt = (int) Math.ceil(radius);
        int radiusSq = (int) (radius * radius);

        for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
            for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                double dx = x - center.x;
                double dz = z - center.z;
                double distanceSq = dx * dx + dz * dz;

                if (distanceSq <= radiusSq) {
                    BlockPos groundPos = findTopSolidBlock(level, x, z, centerY);
                    if (groundPos != null) {
                        positions.add(groundPos);
                    }
                }
            }
        }

        return positions;
    }

    private static BlockPos findTopSolidBlock(Level level, int x, int z, int centerY) {
        for (int y = centerY + 10; y > centerY - 10; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    public static void playImpactEffect(ServerLevel level, Vec3 pos, AABB boundingBox,
                                        ServerPlayer owner, DamageSources damageSources) {
        double x = pos.x, y = pos.y, z = pos.z;
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 15, 1.5, 1.5, 1.5, 0.3);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 30, 2.5, 1.0, 2.5, 0.2);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 10, 0.8, 0.8, 0.8, 0.05);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.5F, 0.7F);
        level.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);

        AABB impactArea = boundingBox.inflate(4.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, impactArea,
                e -> e.isAlive()
                        && (owner == null || !RiderCompanionUtil.isPlayerCompanion(e, owner.getUUID())));
        for (LivingEntity target : targets) {
            DamageSource ds = owner != null
                    ? damageSources.playerAttack(owner)
                    : damageSources.generic();
            target.hurt(ds, 60.0f);
        }
    }

    public static boolean tryDealFallingDamage(Entity self, ServerPlayer owner, float damage) {
        AABB hitBox = self.getBoundingBox().inflate(0.5);
        List<LivingEntity> targets = self.level().getEntitiesOfClass(LivingEntity.class, hitBox);
        for (LivingEntity target : targets) {
            if (target != null && target.isAlive()
                    && target != owner
                    && owner != null
                    && !RiderCompanionUtil.isPlayerCompanion(target, owner.getUUID())) {
                DamageSource ds = owner != null
                        ? self.damageSources().playerAttack(owner)
                        : self.damageSources().generic();
                target.hurt(ds, damage);
                self.discard();
                return true;
            }
        }
        return false;
    }

    /**
     * 冲击波效果：环形逐圈方块飞起 + AOE伤害（猛击专用）
     * 直径6格，半径3格，分为3圈（每圈1格宽度）
     * 无随机旋转，直上直下，如波浪一般
     */
    public static void createShockWaveEffect(Level level, Vec3 center, double radius,
                                              int flyDuration, int hoverTime, LivingEntity source) {
        if (level.isClientSide) return;
        ServerLevel serverLevel = (ServerLevel) level;

        // ===== AOE伤害 + 缓慢III + 击退 =====
        double dmgRadius = radius;
        AABB damageArea = new AABB(
                center.x - dmgRadius, center.y - 1, center.z - dmgRadius,
                center.x + dmgRadius, center.y + 2, center.z + dmgRadius
        );
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, damageArea,
                e -> e != source && e.isAlive());

        for (LivingEntity entity : targets) {
            double distSq = entity.distanceToSqr(center);
            if (distSq <= dmgRadius * dmgRadius) {
                float dist = (float) Math.sqrt(distSq);
                float damageMultiplier = 1.0f - (dist / (float) dmgRadius) * 0.5f;
                float baseDamage = (float) source.getAttributeValue(Attributes.ATTACK_DAMAGE);
                entity.hurt(source.damageSources().mobAttack(source), baseDamage * damageMultiplier * 0.8f);

                // 缓慢III（3秒）
                entity.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        60, 2, false, false, true
                ));

                // 击退
                Vec3 knockback = new Vec3(
                        entity.getX() - center.x, 0, entity.getZ() - center.z
                ).normalize().scale(0.8);
                entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
            }
        }

        // ===== 方块波浪效果：只有外圈（去掉中心距离<1），每格微差起伏 =====
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        int centerY = (int) center.y;
        int radiusInt = (int) Math.ceil(radius);

        // 外圈按距离分组：ringIndex 0 = 距离1~2, ringIndex 1 = 距离2~3
        List<List<BlockPos>> rings = new ArrayList<>();
        for (int i = 0; i < 2; i++) rings.add(new ArrayList<>());

        for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
            for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                double dx = x + 0.5 - center.x;
                double dz = z + 0.5 - center.z;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist <= radius + 0.01 && dist >= 1.0) { // 只取外圈，去掉中心
                    BlockPos groundPos = findTopSolidBlock(level, x, z, centerY);
                    if (groundPos != null) {
                        BlockState state = level.getBlockState(groundPos);
                        if (state.isAir() || !state.isSolidRender(level, groundPos)) continue;

                        int ringIndex = Math.min(1, (int) ((dist - 1.0) / 1.0));
                        rings.get(ringIndex).add(groundPos.immutable());
                    }
                }
            }
        }

        // 逐环 + 格内微差延迟，形成涟漪般的起伏
        Random random = new Random();
        int[] ringDelays = {0, 5}; // 外圈先起（距离1~2），远圈稍后（距离2~3）
        for (int ringIdx = 0; ringIdx < 2; ringIdx++) {
            int ringBaseDelay = ringDelays[ringIdx];
            for (BlockPos pos : rings.get(ringIdx)) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;

                // 每格在环内增加0~2 tick随机微差，形成圈内起伏
                int perBlockDelay = random.nextInt(3);
                int totalDelay = ringBaseDelay + perBlockDelay;

                DelayedTask.run(level, totalDelay, () -> {
                    FallingBlockEffectEntity fallingBlock = new FallingBlockEffectEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            state,
                            FallingBlockEffectEntity.EffectType.FLY_UP,
                            center,
                            flyDuration,
                            hoverTime,
                            null
                    );

                    // 无随机旋转，直上直下
                    fallingBlock.setRotation(0, 0, 0);
                    fallingBlock.setDeltaMovement(new Vec3(0, 0.6, 0));

                    // 方块破碎粒子
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            3, 0.1, 0.1, 0.1, 0.05
                    );

                    level.addFreshEntity(fallingBlock);
                });
            }
        }

        // 冲击波声音 + 爆炸粒子
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 0.8F);
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y, center.z, 10, 1.5, 0.5, 1.5, 0.2);
    }
}