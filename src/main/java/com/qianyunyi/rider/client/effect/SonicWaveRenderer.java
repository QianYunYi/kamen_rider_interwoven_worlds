package com.qianyunyi.rider.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 音波效果渲染器
 * 使用渲染器直接在世界中绘制音波扩散环效果
 * 音波环平面始终面向目标方向
 */
public class SonicWaveRenderer {

    private static final List<WaveState> activeWaves = new ArrayList<>();

    private static final float RING_SEGMENTS = 48;
    private static final float MAX_RADIUS = 6.0f;
    private static final int DURATION_TICKS = 30; // 1.5秒

    private static class WaveState {
        final String dimensionId;
        final double x, y, z;
        final float dirX, dirY, dirZ;
        final long startGameTime;
        final int duration;

        WaveState(String dimensionId, double x, double y, double z,
                  float dirX, float dirY, float dirZ,
                  long startGameTime, int duration) {
            this.dimensionId = dimensionId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
            this.startGameTime = startGameTime;
            this.duration = duration;
        }

        boolean isExpired(long currentGameTime) {
            return currentGameTime - startGameTime > duration;
        }

        float getProgress(long currentGameTime, float partialTick) {
            return Math.min((currentGameTime - startGameTime + partialTick) / duration, 1.0f);
        }
    }

    /**
     * 在指定位置触发音波效果，指定传播方向
     */
    public static void triggerWave(Level level, double x, double y, double z,
                                   double dirX, double dirY, double dirZ) {
        if (level == null) return;
        // 归一化方向向量
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 0.0001) {
            // 无方向时默认朝上
            activeWaves.add(new WaveState(
                    level.dimension().location().toString(),
                    x, y, z, 0, 1, 0,
                    level.getGameTime(), DURATION_TICKS));
            return;
        }
        activeWaves.add(new WaveState(
                level.dimension().location().toString(),
                x, y, z,
                (float) (dirX / len), (float) (dirY / len), (float) (dirZ / len),
                level.getGameTime(), DURATION_TICKS
        ));
    }

    /**
     * 在指定实体眼部位置触发音波效果，朝向实体目标方向
     */
    public static void triggerWaveAtEntity(Entity entity) {
        if (entity == null || entity.level() == null) return;
        double x = entity.getX();
        double y = entity.getY() + entity.getEyeHeight();
        double z = entity.getZ();

        // 计算朝向目标的方向
        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            if (target != null) {
                Vec3 dir = new Vec3(
                        target.getX() - entity.getX(),
                        (target.getY() + target.getEyeHeight()) - (entity.getY() + entity.getEyeHeight()),
                        target.getZ() - entity.getZ()
                );
                triggerWave(entity.level(), x, y, z, dir.x, dir.y, dir.z);
                return;
            }
        }
        // 无目标时朝实体面朝方向
        Vec3 look = entity.getLookAngle();
        triggerWave(entity.level(), x, y, z, look.x, look.y, look.z);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;
        if (activeWaves.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();
        long gameTime = mc.level.getGameTime();

        // 清理过期音波
        activeWaves.removeIf(w -> w.isExpired(gameTime));

        if (activeWaves.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        Iterator<WaveState> iter = activeWaves.iterator();
        while (iter.hasNext()) {
            WaveState wave = iter.next();

            // 只渲染当前世界的音波
            if (!wave.dimensionId.equals(mc.level.dimension().location().toString())) {
                iter.remove();
                continue;
            }

            float progress = wave.getProgress(gameTime, partialTick);
            float radius = progress * MAX_RADIUS;
            float baseAlpha = (1.0f - progress) * 0.85f;
            if (baseAlpha <= 0.01f) {
                iter.remove();
                continue;
            }

            // 计算相对于相机的位置
            float dx = (float) (wave.x - camPos.x);
            float dy = (float) (wave.y - camPos.y);
            float dz = (float) (wave.z - camPos.z);

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            // 旋转音波环使其朝向目标方向（默认环在 XZ 平面，法线向上(0,1,0)）
            float ndx = wave.dirX, ndy = wave.dirY, ndz = wave.dirZ;
            // 计算旋转：从默认法线 (0,1,0) 到方向向量 (ndx,ndy,ndz)
            float dot = ndy; // dot((0,1,0), (ndx,ndy,ndz))
            // 叉积 = (ndz, 0, -ndx)
            float crossLen = (float) Math.sqrt(ndz * ndz + ndx * ndx);
            if (crossLen > 0.0001f) {
                float angle = (float) Math.acos(Math.min(1.0f, Math.max(-1.0f, dot)));
                // 归一化旋转轴
                float ax = ndz / crossLen;
                float az = -ndx / crossLen;
                poseStack.mulPose(new Quaternionf().rotationAxis(angle, ax, 0, az));
            } else if (dot < -0.9999f) {
                // 方向完全向下 (0,-1,0)，绕 X 轴旋转 180度
                poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.PI, 1, 0, 0));
            }

            Matrix4f mat = poseStack.last().pose();

            // 绘制3层同心扩散环
            for (int ring = 0; ring < 3; ring++) {
                float ringOffset = ring * 0.6f; // 环间距
                float ringRadius = radius - ringOffset;
                if (ringRadius <= 0.1f) continue;

                float ringAlpha = baseAlpha * (1.0f - ring * 0.25f);
                if (ringAlpha <= 0.01f) continue;

                drawRing(buf, mat, ringRadius, 0.25f, ringAlpha, progress);
            }

            // 中心辉光
            float glowAlpha = baseAlpha * 0.6f * (1.0f - progress * 0.5f);
            if (glowAlpha > 0.01f) {
                drawCenterGlow(buf, mat, 0.8f * (1.0f - progress * 0.3f), glowAlpha);
            }

            poseStack.popPose();
        }

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * 绘制单个圆环
     */
    private static void drawRing(BufferBuilder buf, Matrix4f mat,
                                 float radius, float thickness, float alpha, float progress) {
        float innerR = radius - thickness / 2;
        float outerR = radius + thickness / 2;

        // 颜色渐变：从蓝白到青蓝，随进度变化
        float r = 0.3f + 0.5f * (1.0f - progress);
        float g = 0.6f + 0.3f * (1.0f - progress);
        float b = 1.0f;

        for (int i = 0; i < RING_SEGMENTS; i++) {
            double angle1 = 2.0 * Math.PI * i / RING_SEGMENTS;
            double angle2 = 2.0 * Math.PI * (i + 1) / RING_SEGMENTS;

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            // 内圈三角形
            buf.vertex(mat, innerR * cos1, 0, innerR * sin1).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, outerR * cos1, 0, outerR * sin1).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, innerR * cos2, 0, innerR * sin2).color(r, g, b, alpha).endVertex();

            // 外圈三角形
            buf.vertex(mat, innerR * cos2, 0, innerR * sin2).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, outerR * cos1, 0, outerR * sin1).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, outerR * cos2, 0, outerR * sin2).color(r, g, b, alpha).endVertex();
        }
    }

    /**
     * 绘制中心辉光
     */
    private static void drawCenterGlow(BufferBuilder buf, Matrix4f mat,
                                       float glowRadius, float alpha) {
        int segments = 24;
        float r = 0.8f;
        float g = 0.9f;
        float b = 1.0f;

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            // 中心到边缘的渐变三角形
            buf.vertex(mat, 0, 0, 0).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, glowRadius * cos1, 0, glowRadius * sin1).color(r, g, b, alpha * 0.3f).endVertex();
            buf.vertex(mat, glowRadius * cos2, 0, glowRadius * sin2).color(r, g, b, alpha * 0.3f).endVertex();
        }
    }
}