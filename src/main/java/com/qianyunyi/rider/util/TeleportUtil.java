package com.qianyunyi.rider.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TeleportUtil {

    public static void teleportTo(Entity entity, double x, double y, double z) {
        if (entity.isAlive()) {
            Level world = entity.level();
            BlockPos targetPos = BlockPos.containing(x, y, z);

            // 确保位置安全
            if (isPositionSafe(world, targetPos)) {
                entity.setPos(x, y, z);
                playTeleportSound(entity);
                return;
            }

            // 尝试在附近寻找安全位置
            for (int i = 0; i < 10; i++) {
                double offsetX = x + (world.random.nextDouble() - 0.5) * 4.0;
                double offsetY = y + (world.random.nextDouble() - 0.5) * 4.0;
                double offsetZ = z + (world.random.nextDouble() - 0.5) * 4.0;
                BlockPos newPos = BlockPos.containing(offsetX, offsetY, offsetZ);

                if (isPositionSafe(world, newPos)) {
                    entity.setPos(offsetX, offsetY, offsetZ);
                    playTeleportSound(entity);
                    return;
                }
            }

            // 尝试向上寻找安全位置
            for (int i = 1; i <= 10; i++) {
                BlockPos upPos = targetPos.above(i);
                if (isPositionSafe(world, upPos)) {
                    entity.setPos(upPos.getX() + 0.5, upPos.getY(), upPos.getZ() + 0.5);
                    playTeleportSound(entity);
                    return;
                }
            }

            // 如果找不到安全位置，使用原始位置
            entity.setPos(x, y, z);
            playTeleportSound(entity);
        }
    }

    /**
     * 传送到目标后面
     * @param target 目标实体
     */
    public static void teleportBehindTarget(Entity entity, LivingEntity target) {
        if (target == null) return;
        Vec3 lookVec = target.getLookAngle().normalize();
        Vec3 behindPos = target.position().subtract(lookVec.scale(2));
        behindPos = new Vec3(behindPos.x, target.getY(), behindPos.z);
        TeleportUtil.teleportTo(entity, behindPos.x, behindPos.y, behindPos.z);
    }

    private static boolean isPositionSafe(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(pos.below());

        return belowState.isSolid()
                && !state.blocksMotion()
                && !world.getBlockState(pos.above()).blocksMotion();
    }

    private static void playTeleportSound(Entity entity) {
        entity.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    }

    public static void teleportRandomly(Entity entity) {
        double d0 = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 16.0;
        double d1 = entity.getY() + (entity.level().random.nextInt(16) - 8);
        double d2 = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 16.0;
        teleportTo(entity, d0, d1, d2);
    }

    /**
     * 传送到实体附近
     * @param entity 实体
     */
    public static void teleportShortDistance(Entity entity) {
        double d0 = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 3.0;
        double d1 = entity.getY() + (entity.level().random.nextDouble() - 0.5) * 3.0;
        double d2 = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 3.0;
        teleportTo(entity, d0, d1, d2);
    }

    /**
     * 传送到目标前面
     * @param target 目标实体
     */
    public static void teleportTowardsTarget(Entity entity, Entity target) {
        if (entity.level() == null) return;

        double tx = target.getX();
        double ty = target.getY();
        double tz = target.getZ();

        double angle = entity.level().random.nextDouble() * Math.PI * 2;
        double radius = 2 + entity.level().random.nextDouble() * 2;

        double dx = tx + Math.cos(angle) * radius;
        double dz = tz + Math.sin(angle) * radius;
        double dy = findValidY(entity.level(), dx, ty, dz);

        teleportTo(entity, dx, dy, dz);
    }

    private static double findValidY(Level world, double x, double baseY, double z) {
        for (int i = 0; i < 10; i++) {
            double testY = baseY + i - 5;
            BlockPos pos = BlockPos.containing(x, testY, z);
            if (isPositionSafe(world, pos)) {
                return testY;
            }
        }
        return baseY;
    }
}