package com.qianyunyi.rider.client.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qianyunyi.rider.client.camera.CameraController;
import com.qianyunyi.rider.network.packet.SupernaturalSensesTrackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 超自然感官——潜行追踪模式客户端处理器
 *
 * 职责：
 * 1. 接收服务端发来的 {@link SupernaturalSensesTrackPacket} 并维护追踪状态
 * 2. 切换摄像机为上帝视角（CameraController SUPERNATURAL_SENSES_TRACKING 模式）
 * 3. 每帧渲染暗色遮罩（暗角效果），模拟"画面变暗"
 */
public class SupernaturalSensesTrackHandler {

    private static final Map<String, BiConsumer<SupernaturalSensesTrackPacket, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    // 当前状态
    private static boolean trackingActive = false;
    private static int targetEntityId = -1;
    private static Entity targetEntity = null;

    // 遮罩参数
    private static final float OVERLAY_ALPHA = 0.65f;
    private static final int VIGNETTE_SEGMENTS = 64;

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    trackingActive = msg.isActive();
                    targetEntityId = msg.getTargetEntityId();

                    Minecraft mc = Minecraft.getInstance();
                    CameraController camController = CameraController.getInstance();

                    if (trackingActive) {
                        if (targetEntityId != -1 && mc.level != null) {
                            // 追踪模式：锁定目标实体
                            targetEntity = mc.level.getEntity(targetEntityId);
                            camController.setTrackingTarget(targetEntity);
                        } else {
                            // 自视模式(targeEntityId == -1)：无目标，摄像机俯视玩家自身
                            targetEntity = null;
                            camController.setTrackingTarget(null);
                        }
                        // 激活上帝视角追踪模式（无论自视还是追踪都使用同一相机模式）
                        camController.requestMode(CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING);
                    } else {
                        // 完全退出追踪
                        targetEntity = null;
                        if (camController.getCurrentMode() == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING
                                || camController.getTargetMode() == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING) {
                            camController.setTrackingTarget(null);
                            camController.requestMode(CameraController.CameraMode.NORMAL);
                        }
                    }
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<SupernaturalSensesTrackPacket, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }

    /**
     * 客户端每帧调用，更新 CameraController 的追踪目标
     * 当服务端切换目标时，目标实体对象可能已过时，需要重新查找。
     */
    public static void clientTick() {
        if (!trackingActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        CameraController camController = CameraController.getInstance();

        // 如果目标实体已失效，重新查找
        if (targetEntity == null || !targetEntity.isAlive()) {
            if (targetEntityId != -1) {
                targetEntity = mc.level.getEntity(targetEntityId);
            }
            camController.setTrackingTarget(targetEntity);
        }
    }

    /**
     * 渲染暗色遮罩（暗角效果），在 RenderLevelStageEvent 中调用
     */
    public static void renderOverlay(RenderLevelStageEvent event) {
        if (!trackingActive) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 动态调整透明度：有目标时完全暗，无目标时稍亮（表示在搜索）
        float alpha = (targetEntity != null && targetEntity.isAlive()) ? OVERLAY_ALPHA : OVERLAY_ALPHA * 0.5f;

        renderVignette(alpha);
    }

    /**
     * 渲染暗角（类似 SupernaturalScanRenderer 的暗角效果）
     */
    private static void renderVignette(float alpha) {
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.getProjectionMatrix().set(new Matrix4f());

        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f idMat = new Matrix4f();
        Tesselator t = Tesselator.getInstance();
        BufferBuilder buf = t.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float edgeDist = 1.5f;
        for (int i = 0; i < VIGNETTE_SEGMENTS; i++) {
            double angle1 = 2.0 * Math.PI * i / VIGNETTE_SEGMENTS;
            double angle2 = 2.0 * Math.PI * (i + 1) / VIGNETTE_SEGMENTS;
            float x1 = (float) Math.cos(angle1) * edgeDist;
            float y1 = (float) Math.sin(angle1) * edgeDist;
            float x2 = (float) Math.cos(angle2) * edgeDist;
            float y2 = (float) Math.sin(angle2) * edgeDist;

            buf.vertex(idMat, 0, 0, 0).color(0, 0, 0, 0).endVertex();
            buf.vertex(idMat, x1, y1, 0).color(0, 0, 0, alpha).endVertex();
            buf.vertex(idMat, x2, y2, 0).color(0, 0, 0, alpha).endVertex();
        }

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.getProjectionMatrix().set(savedProjection);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * 停止追踪，清理状态
     */
    public static void stopTracking() {
        trackingActive = false;
        targetEntityId = -1;
        targetEntity = null;

        CameraController camController = CameraController.getInstance();
        if (camController.getCurrentMode() == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING
                || camController.getTargetMode() == CameraController.CameraMode.SUPERNATURAL_SENSES_TRACKING) {
            camController.setTrackingTarget(null);
            camController.requestMode(CameraController.CameraMode.NORMAL);
        }
    }

    public static boolean isTrackingActive() {
        return trackingActive;
    }
}