package com.qianyunyi.rider.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 屏幕震动控制器 — 可复用的震动效果
 *
 * 震动在相机局部坐标系（右/上方向）中施加随机偏移，
 * 强度随时间线性衰减，支持多个震动叠加。
 *
 * 使用方式：
 *   ScreenShakeController.getInstance().addShake(0.3f, 10);
 *   // 强度 0.3 方块，持续 10 tick
 */
@OnlyIn(Dist.CLIENT)
public class ScreenShakeController {
    private static final ScreenShakeController INSTANCE = new ScreenShakeController();
    private final List<ShakeInstance> activeShakes = new ArrayList<>();
    private final Random random = new Random();

    private ScreenShakeController() {}

    public static ScreenShakeController getInstance() {
        return INSTANCE;
    }

    /** 添加一次震动 */
    public void addShake(float intensity, int durationTicks) {
        activeShakes.add(new ShakeInstance(intensity, durationTicks));
    }

    /** 每 tick 更新震动计时 */
    public void tick() {
        for (ShakeInstance shake : activeShakes) {
            shake.tick();
        }
    }

    /** 每帧在 ViewportEvent.ComputeCameraAngles 中调用，返回累计震动偏移 */
    public Vec3 getShakeOffset(Camera camera, float partialTick) {
        // 清理过期震动并累加强度
        float totalIntensity = 0f;
        Iterator<ShakeInstance> it = activeShakes.iterator();
        while (it.hasNext()) {
            ShakeInstance shake = it.next();
            if (shake.isExpired(partialTick)) {
                it.remove();
            } else {
                totalIntensity += shake.getCurrentIntensity(partialTick);
            }
        }

        if (totalIntensity <= 0.001f) return Vec3.ZERO;

        // 相机局部方向
        float yaw = (float) Math.toRadians(camera.getYRot());
        float pitch = (float) Math.toRadians(camera.getXRot());

        // 右向量
        Vec3 right = new Vec3(
                Math.cos(yaw),
                0,
                -Math.sin(yaw)
        ).normalize();

        // 上向量
        Vec3 up = new Vec3(
                -Math.sin(pitch) * Math.sin(yaw),
                Math.cos(pitch),
                -Math.sin(pitch) * Math.cos(yaw)
        ).normalize();

        // 在相机局部XY平面内随机偏移
        double rx = (random.nextDouble() - 0.5) * 2.0 * totalIntensity;
        double ry = (random.nextDouble() - 0.5) * 2.0 * totalIntensity;

        return right.scale(rx).add(up.scale(ry));
    }

    /** 单次震动实例 */
    private static class ShakeInstance {
        final float intensity;
        final int totalTicks;
        int elapsedTicks;

        ShakeInstance(float intensity, int totalTicks) {
            this.intensity = intensity;
            this.totalTicks = Math.max(1, totalTicks);
            this.elapsedTicks = 0;
        }

        void tick() {
            elapsedTicks++;
        }

        boolean isExpired(float partialTick) {
            return elapsedTicks >= totalTicks;
        }

        /** 当前强度（线性衰减） */
        float getCurrentIntensity(float partialTick) {
            float progress = Mth.clamp(
                    (elapsedTicks + partialTick) / totalTicks, 0f, 1f);
            return intensity * (1f - progress);
        }
    }
}