package com.qianyunyi.rider.client.autoaim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientAutoAim {
    private static final Minecraft mc = Minecraft.getInstance();

    // 状态变量
    private static boolean shouldAim = false;
    private static Entity targetEntity = null;
    private static int aimDuration = 0;
    private static int targetRefreshCooldown = 0;

    // 平滑控制变量
    private static float smoothYawVelocity = 0;
    private static float smoothPitchVelocity = 0;
    private static Vec3 lastTargetPosition = null;
    private static float lastYaw = 0;
    private static float lastPitch = 0;

    // 参数配置
    private static final float SMOOTH_TIME = 0.15f;          // 平滑时间（秒）
    private static final float MAX_ROTATION_SPEED = 10.0f;  // 每tick最大旋转度数
    private static final float MIN_ROTATION_THRESHOLD = 0.05f; // 最小旋转阈值
    private static final float TARGET_SWITCH_THRESHOLD = 0.3f; // 目标切换阈值
    private static final float MAX_AIM_DISTANCE = 50.0f;    // 最大瞄准距离
    private static final float FIELD_OF_VIEW = 45.0f;       // 搜索视场角

    // 预测参数
    private static final boolean USE_PREDICTION = false;     // 是否使用预测
    private static final float PROJECTILE_SPEED = 30.0f;     // 弹道速度（方块/秒）

    // 弹簧插值参数（可选）
    private static final float STIFFNESS = 0.15f;           // 刚度
    private static final float DAMPING = 0.8f;              // 阻尼
    private static float springYawVelocity = 0;
    private static float springPitchVelocity = 0;

    // 外部取消标志
    private static boolean externalCancelRequested = false;

    // ==================== 外部API方法 ====================

    /**
     * 请求客户端自动瞄准
     * @param duration 瞄准持续时间（tick）
     */
    public static void requestAutoAim(int duration) {
        shouldAim = true;
        aimDuration = duration;
        targetRefreshCooldown = 0;
        externalCancelRequested = false;
        // 重置平滑变量
        smoothYawVelocity = 0;
        smoothPitchVelocity = 0;
        springYawVelocity = 0;
        springPitchVelocity = 0;
        lastTargetPosition = null;
        findTarget();
    }

    /**
     * 外部调用取消自瞄
     */
    public static void cancelAutoAim() {
        externalCancelRequested = true;
        shouldAim = false;
        targetEntity = null;
        aimDuration = 0;
        // 重置平滑变量
        smoothYawVelocity = 0;
        smoothPitchVelocity = 0;
        springYawVelocity = 0;
        springPitchVelocity = 0;
        lastTargetPosition = null;
    }

    /**
     * 检查自瞄是否正在运行
     */
    public static boolean isAutoAimActive() {
        return shouldAim && aimDuration > 0;
    }

    /**
     * 获取剩余自瞄时间
     */
    public static int getRemainingAimDuration() {
        return aimDuration;
    }

    /**
     * 获取当前目标实体
     */
    public static Entity getCurrentTarget() {
        return targetEntity;
    }

    // ==================== 内部实现 ====================

    /**
     * 内部取消自瞄
     */
    private static void internalCancelAutoAim() {
        shouldAim = false;
        targetEntity = null;
        aimDuration = 0;
        externalCancelRequested = false;
        // 重置平滑变量
        smoothYawVelocity = 0;
        smoothPitchVelocity = 0;
        springYawVelocity = 0;
        springPitchVelocity = 0;
        lastTargetPosition = null;
    }

    /**
     * 快速查找最近目标
     */
    private static void findTarget() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        Entity bestTarget = null;
        float bestAngle = Float.MAX_VALUE;
        float bestDistance = Float.MAX_VALUE;

        AABB searchBox = mc.player.getBoundingBox().inflate(MAX_AIM_DISTANCE);
        List<Entity> entities = mc.level.getEntities(mc.player, searchBox,
                entity -> entity instanceof LivingEntity &&
                        entity.isAlive() &&
                        entity != mc.player &&
                        !mc.player.isAlliedTo(entity));

        Vec3 playerLook = mc.player.getLookAngle();
        Vec3 playerPos = mc.player.getEyePosition();

        for (Entity entity : entities) {
            // 检查是否可见
            if (!isShootable(entity)) {
                continue;
            }

            Vec3 targetPos = entity.getBoundingBox().getCenter();
            Vec3 toTarget = targetPos.subtract(playerPos).normalize();

            float angle = (float) Math.acos(Math.min(1.0, Math.max(-1.0, playerLook.dot(toTarget))));
            float angleDegrees = (float) Math.toDegrees(angle);

            // 限制在视野范围内
            if (angleDegrees > FIELD_OF_VIEW) continue;

            float distance = (float) playerPos.distanceTo(targetPos);

            // 综合评分：角度权重 + 距离权重
            float angleScore = angleDegrees;
            float distanceScore = distance / MAX_AIM_DISTANCE * 60.0f; // 距离转换为角度评分
            float totalScore = angleScore + distanceScore;

            if (totalScore < bestAngle + bestDistance * 0.001f) {
                bestTarget = entity;
                bestAngle = totalScore;
                bestDistance = distance;
            }
        }

        // 平滑切换目标
        if (bestTarget != null && targetEntity != null && targetEntity != bestTarget) {
            Vec3 currentTargetPos = targetEntity.getBoundingBox().getCenter();
            Vec3 newTargetPos = bestTarget.getBoundingBox().getCenter();
            double distanceToCurrent = mc.player.distanceTo(targetEntity);
            double distanceToNew = mc.player.distanceTo(bestTarget);

            // 只有当新目标明显更好时才切换
            if (distanceToNew < distanceToCurrent * 0.7f ||
                    !isShootable(targetEntity)) {
                targetEntity = bestTarget;
            }
        } else {
            targetEntity = bestTarget;
        }
    }

    /**
     * 检查实体是否可以被射击（考虑视线遮挡）
     */
    private static boolean isShootable(Entity target) {
        if (mc.player == null || mc.level == null || target == null) {
            return false;
        }

        Vec3 playerEyePos = mc.player.getEyePosition();
        Vec3 targetPos = target.getBoundingBox().getCenter();

        // 方法1：检查玩家眼睛到目标中心的直线
        if (hasLineOfSight(playerEyePos, targetPos)) {
            return true;
        }

        // 方法2：如果方法1失败，检查玩家眼睛到目标碰撞箱的多个点
        AABB targetBoundingBox = target.getBoundingBox();

        // 检查碰撞箱的8个角
        Vec3[] testPoints = {
                new Vec3(targetBoundingBox.minX, targetBoundingBox.minY, targetBoundingBox.minZ),
                new Vec3(targetBoundingBox.maxX, targetBoundingBox.minY, targetBoundingBox.minZ),
                new Vec3(targetBoundingBox.minX, targetBoundingBox.maxY, targetBoundingBox.minZ),
                new Vec3(targetBoundingBox.maxX, targetBoundingBox.maxY, targetBoundingBox.minZ),
                new Vec3(targetBoundingBox.minX, targetBoundingBox.minY, targetBoundingBox.maxZ),
                new Vec3(targetBoundingBox.maxX, targetBoundingBox.minY, targetBoundingBox.maxZ),
                new Vec3(targetBoundingBox.minX, targetBoundingBox.maxY, targetBoundingBox.maxZ),
                new Vec3(targetBoundingBox.maxX, targetBoundingBox.maxY, targetBoundingBox.maxZ),

                // 中心点
                targetPos
        };

        // 检查是否有至少一条视线是畅通的
        for (Vec3 testPoint : testPoints) {
            if (hasLineOfSight(playerEyePos, testPoint)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查两点之间是否有直线视线
     */
    private static boolean hasLineOfSight(Vec3 start, Vec3 end) {
        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        );

        HitResult hitResult = mc.level.clip(context);

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            return true; // 命中实体
        }

        return hitResult.getType() == HitResult.Type.MISS;
    }

    /**
     * 平滑阻尼角度算法（类似Unity的SmoothDampAngle）
     */
    private static float smoothDampAngle(float current, float target, float smoothTime, float deltaTime) {
        // 计算角度差（处理-180到180的循环）
        float diff = normalizeAngleDiff(target - current);

        // 如果角度差非常小，直接到达目标
        if (Math.abs(diff) < MIN_ROTATION_THRESHOLD) {
            return target;
        }

        // 使用指数衰减的平滑
        float exponentialDecay = 1.0f - (float)Math.exp(-deltaTime / smoothTime);
        return current + diff * exponentialDecay;
    }

    /**
     * 计算目标位置（包含预测）
     */
    private static Vec3 calculateTargetPosition(Entity target) {
        if (!USE_PREDICTION || target == null) {
            return target.getBoundingBox().getCenter();
        }

        Vec3 currentPos = target.getBoundingBox().getCenter();
        Vec3 velocity = target.getDeltaMovement();

        // 如果速度很小，不需要预测
        if (velocity.length() < 0.1) {
            return currentPos;
        }

        // 计算到目标的距离
        double distance = mc.player.getEyePosition().distanceTo(currentPos);

        // 计算弹道飞行时间
        double timeToTarget = distance / PROJECTILE_SPEED;

        // 预测位置（考虑重力）
        Vec3 predictedPos = currentPos.add(
                velocity.x * timeToTarget,
                velocity.y * timeToTarget + 0.5 * 0.08 * timeToTarget * timeToTarget, // 重力补偿
                velocity.z * timeToTarget
        );

        return predictedPos;
    }

    /**
     * 主瞄准方法（平滑版本）
     */
    private static void aimAtTargetSmooth(Entity target) {
        if (mc.player == null || target == null) {
            return;
        }

        // 检查目标是否仍然可见
        if (!isShootable(target)) {
            targetEntity = null;
            lastTargetPosition = null;
            return;
        }

        // 计算目标位置（可能包含预测）
        Vec3 targetPos = calculateTargetPosition(target);

        // 记录上一次的目标位置用于平滑
        if (lastTargetPosition == null) {
            lastTargetPosition = targetPos;
        } else {
            // 对目标位置进行平滑，避免目标移动导致的抖动
            targetPos = lastTargetPosition.add(targetPos.subtract(lastTargetPosition).scale(0.7));
            lastTargetPosition = targetPos;
        }

        // 获取玩家当前角度
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        // 计算目标角度
        Vec3 playerPos = mc.player.getEyePosition();

        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;

        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(deltaY, distanceXZ) * (180.0 / Math.PI)));

        // 规范化角度
        targetYaw = normalizeAngle(targetYaw);
        targetPitch = Math.max(-89.9F, Math.min(89.9F, targetPitch));

        // 计算角度差
        float yawDiff = normalizeAngleDiff(targetYaw - currentYaw);
        float pitchDiff = normalizeAngleDiff(targetPitch - currentPitch);

        // 动态平滑系数：角度差越大旋转越快，实现快速启动+平滑收尾
        float angleDiffMagnitude = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        float smoothFactor = Math.min(0.35f, 0.10f + angleDiffMagnitude * 0.005f);
        float newYaw = currentYaw + yawDiff * smoothFactor;
        float newPitch = currentPitch + pitchDiff * smoothFactor;

        // 限制最大旋转速度
        float yawChange = normalizeAngleDiff(newYaw - currentYaw);
        float pitchChange = newPitch - currentPitch;

        if (Math.abs(yawChange) > MAX_ROTATION_SPEED) {
            yawChange = Math.signum(yawChange) * MAX_ROTATION_SPEED;
            newYaw = currentYaw + yawChange;
        }

        if (Math.abs(pitchChange) > MAX_ROTATION_SPEED) {
            pitchChange = Math.signum(pitchChange) * MAX_ROTATION_SPEED;
            newPitch = currentPitch + pitchChange;
        }

        // 再次规范化角度
        newYaw = normalizeAngle(newYaw);
        newPitch = Math.max(-89.9F, Math.min(89.9F, newPitch));

        // 如果角度变化非常小，直接设置为目标值避免抖动
        if (Math.abs(yawDiff) < MIN_ROTATION_THRESHOLD && Math.abs(pitchDiff) < MIN_ROTATION_THRESHOLD) {
            newYaw = targetYaw;
            newPitch = targetPitch;
        }

        // 记录上一次的角度用于下一次平滑
        lastYaw = newYaw;
        lastPitch = newPitch;

        // 设置角度
        mc.player.setYRot(newYaw);
        mc.player.setXRot(newPitch);
        mc.player.setYHeadRot(newYaw);
    }

    /**
     * 辅助方法：规范化角度到 [-180, 180]
     */
    private static float normalizeAngle(float angle) {
        angle = angle % 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        if (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    /**
     * 辅助方法：规范化角度差到 [-180, 180]
     */
    private static float normalizeAngleDiff(float diff) {
        diff = diff % 360.0F;
        if (diff > 180.0F) diff -= 360.0F;
        if (diff < -180.0F) diff += 360.0F;
        return diff;
    }

    /**
     * 客户端tick事件
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.player != null) {
            // 检查外部取消请求
            if (externalCancelRequested && shouldAim) {
                internalCancelAutoAim();
                return;
            }

            if (shouldAim && aimDuration > 0) {
                // 定期刷新目标（但不要过于频繁）
                if (targetRefreshCooldown <= 0) {
                    findTarget();
                    targetRefreshCooldown = 2;
                } else {
                    targetRefreshCooldown--;
                }

                // 检查目标有效性
                if (targetEntity != null &&
                        targetEntity.isAlive() &&
                        targetEntity.distanceTo(mc.player) < MAX_AIM_DISTANCE &&
                        isShootable(targetEntity)) {

                    // 使用平滑的瞄准方法
                    aimAtTargetSmooth(targetEntity);
                    aimDuration--;

                    // 检查是否对准目标（使用更严格的阈值）
                    Vec3 targetPos = targetEntity.getBoundingBox().getCenter();
                    Vec3 lookVec = mc.player.getLookAngle();
                    Vec3 toTargetVec = targetPos.subtract(mc.player.getEyePosition()).normalize();
                    float dotProduct = (float) lookVec.dot(toTargetVec);

                    // 当对准时，可以适当加快时间消耗
                    if (dotProduct > 0.995f) {
                        aimDuration = Math.max(0, aimDuration - 2);
                    }

                } else {
                    // 目标无效，尝试重新寻找
                    findTarget();
                    aimDuration--;

                    // 如果连续找不到目标，清空平滑变量
                    if (targetEntity == null) {
                        smoothYawVelocity = 0;
                        smoothPitchVelocity = 0;
                        springYawVelocity = 0;
                        springPitchVelocity = 0;
                        lastTargetPosition = null;
                    }

                    // 如果连续3次找不到目标，提前结束
                    if (targetEntity == null) {
                        aimDuration = Math.max(0, aimDuration - 5);
                    }
                }

                // 自瞄时间结束
                if (aimDuration <= 0) {
                    internalCancelAutoAim();
                }
            }
        }
    }

    /**
     * 在渲染阶段绘制调试信息（可选）
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        // 可以添加调试渲染，例如绘制目标框或视线
        // 这里留空，如有需要可以添加
    }
}