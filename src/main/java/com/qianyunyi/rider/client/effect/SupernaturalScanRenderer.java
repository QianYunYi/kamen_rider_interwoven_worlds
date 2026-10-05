package com.qianyunyi.rider.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupernaturalScanRenderer {

    private static final double SCAN_MAX_RADIUS = 22.0;
    private static final float INTRO_DURATION = 2.5f;
    private static final float OVERLAY_MAX_ALPHA = 0.55f;
    private static final int RING_SEGMENTS = 128;
    private static final float RING_THICKNESS = 0.35f;
    private static final int TERRAIN_SAMPLE_SKIP = 2;
    private static final int VIGNETTE_SEGMENTS = 64;

    private static long startTick = 0;
    private static boolean wasActive = false;
    private static final Set<Integer> scannedEntities = new HashSet<>();

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Level level = player.level();
        if (level == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;
        KuugaDataModule kd = data.getKuugaData();
        if (kd == null) return;

        boolean active = kd.isSupernaturalSensesActive();

        if (active) {
            if (!wasActive) {
                startTick = level.getGameTime();
                scannedEntities.clear();
            }

            float elapsed = (level.getGameTime() - startTick + event.getPartialTick()) * 0.05f;

            if (elapsed < INTRO_DURATION) {
                renderIntroPhase(event, player, level, elapsed);
            } else {
                renderActivePhase(event, player, level, elapsed);
            }
        } else {
            if (wasActive) {
                scannedEntities.clear();
            }
        }

        wasActive = active;
    }

    private static void renderIntroPhase(RenderLevelStageEvent event, Player player, Level level, float elapsed) {
        float introProgress = elapsed / INTRO_DURATION;

        float overlayAlpha;
        if (introProgress < 0.4f) {
            overlayAlpha = OVERLAY_MAX_ALPHA * (introProgress / 0.4f);
        } else {
            overlayAlpha = OVERLAY_MAX_ALPHA * (1.0f - (introProgress - 0.4f) / 0.6f);
        }

        float introRadius = (float) (SCAN_MAX_RADIUS * introProgress);

        if (overlayAlpha > 0.01f) {
            renderFullScreenQuad(overlayAlpha);
        }

        renderScanRing(event, player, level, introRadius, 0.85f);

        updateScannedEntities(player, level, introRadius);
        renderScannedEntityOutlines(event, player, level);
    }

    private static void renderActivePhase(RenderLevelStageEvent event, Player player, Level level, float elapsed) {
        float activeTime = elapsed - INTRO_DURATION;

        float breathe = (float) Math.sin(activeTime * 1.2f) * 0.5f + 0.5f;
        float vignetteAlpha = 0.25f + breathe * 0.15f;

        renderVignette(vignetteAlpha);

        float fullRange = (float) SCAN_MAX_RADIUS;
        updateScannedEntities(player, level, fullRange);
        renderScannedEntityOutlines(event, player, level);
    }

    private static void updateScannedEntities(Player player, Level level, float currentRadius) {
        AABB scanArea = player.getBoundingBox().inflate(currentRadius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, scanArea,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator());

        for (LivingEntity entity : entities) {
            double dist = entity.position().distanceTo(player.position());
            if (dist <= currentRadius) {
                scannedEntities.add(entity.getId());
            }
        }
    }

    private static void renderScanRing(RenderLevelStageEvent event, Player player, Level level,
                                       float currentRadius, float alpha) {
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        float innerR = Math.max(0, currentRadius - RING_THICKNESS);
        float outerR = currentRadius + RING_THICKNESS;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f mat = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float rC = 0.05f, gC = 0.92f, bC = 0.28f;
        float yOffset = 0.02f;

        int step = TERRAIN_SAMPLE_SKIP;
        float[] innerHeights = new float[RING_SEGMENTS];
        float[] outerHeights = new float[RING_SEGMENTS];
        for (int seg = 0; seg < RING_SEGMENTS; seg += step) {
            double angle = 2.0 * Math.PI * seg / RING_SEGMENTS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            double ix = px + innerR * cos;
            double iz = pz + innerR * sin;
            double ox = px + outerR * cos;
            double oz = pz + outerR * sin;
            float ih = (float) (findSurfaceY(level, ix, py, iz) + yOffset);
            float oh = (float) (findSurfaceY(level, ox, py, oz) + yOffset);
            for (int j = 0; j < step && seg + j < RING_SEGMENTS; j++) {
                innerHeights[seg + j] = ih;
                outerHeights[seg + j] = oh;
            }
        }

        for (int seg = 0; seg < RING_SEGMENTS; seg++) {
            double a1 = 2.0 * Math.PI * seg / RING_SEGMENTS;
            double a2 = 2.0 * Math.PI * (seg + 1) / RING_SEGMENTS;
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);

            float ix1 = (float) px + innerR * c1, iz1 = (float) pz + innerR * s1;
            float ix2 = (float) px + innerR * c2, iz2 = (float) pz + innerR * s2;
            float ox1 = (float) px + outerR * c1, oz1 = (float) pz + outerR * s1;
            float ox2 = (float) px + outerR * c2, oz2 = (float) pz + outerR * s2;

            float iy1 = innerHeights[seg];
            float iy2 = innerHeights[(seg + 1) % RING_SEGMENTS];
            float oy1 = outerHeights[seg];
            float oy2 = outerHeights[(seg + 1) % RING_SEGMENTS];

            buf.vertex(mat, ix1, iy1, iz1).color(rC, gC, bC, alpha).endVertex();
            buf.vertex(mat, ox1, oy1, oz1).color(rC, gC, bC, alpha * 0.3f).endVertex();
            buf.vertex(mat, ix2, iy2, iz2).color(rC, gC, bC, alpha).endVertex();

            buf.vertex(mat, ix2, iy2, iz2).color(rC, gC, bC, alpha).endVertex();
            buf.vertex(mat, ox1, oy1, oz1).color(rC, gC, bC, alpha * 0.3f).endVertex();
            buf.vertex(mat, ox2, oy2, oz2).color(rC, gC, bC, alpha * 0.3f).endVertex();
        }

        BufferUploader.drawWithShader(buf.end());

        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static double findSurfaceY(Level level, double x, double playerY, double z) {
        int maxY = Math.min((int) playerY + 5, level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                (int) Math.floor(x), maxY, (int) Math.floor(z));
        for (int i = 0; i < 15; i++) {
            if (pos.getY() < level.getMinBuildHeight()) break;
            if (!level.getBlockState(pos).isAir()) {
                return pos.getY() + 1.005;
            }
            pos.setY(pos.getY() - 1);
        }
        return playerY - 0.005;
    }

    private static void renderFullScreenQuad(float alpha) {
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
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(idMat, -1, -1, 0).color(0, 0, 0, alpha).endVertex();
        buf.vertex(idMat, 1, -1, 0).color(0, 0, 0, alpha).endVertex();
        buf.vertex(idMat, 1, 1, 0).color(0, 0, 0, alpha).endVertex();
        buf.vertex(idMat, -1, 1, 0).color(0, 0, 0, alpha).endVertex();
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.getProjectionMatrix().set(savedProjection);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

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

    private static void renderScannedEntityOutlines(RenderLevelStageEvent event, Player player, Level level) {
        if (scannedEntities.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        float r = 0.1f, g = 0.95f, b = 0.35f, a = 0.75f;

        scannedEntities.removeIf(id -> {
            Entity entity = level.getEntity(id);
            if (entity == null || !entity.isAlive()) return true;

            AABB bb = entity.getBoundingBox();
            double ex = entity.getX() - camPos.x;
            double ey = bb.minY - camPos.y;
            double ez = entity.getZ() - camPos.z;
            float hw = (float) ((bb.maxX - bb.minX) / 2.0);
            float hh = (float) (bb.maxY - bb.minY);
            float hd = (float) ((bb.maxZ - bb.minZ) / 2.0);

            poseStack.pushPose();
            poseStack.translate(ex, ey, ez);
            Matrix4f mat = poseStack.last().pose();

            buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

            float nx = -hw, px = hw;
            float ny = 0, py = hh;
            float nz = -hd, pz = hd;

            drawLine(buf, mat, nx, ny, nz, px, ny, nz, r, g, b, a);
            drawLine(buf, mat, nx, ny, pz, px, ny, pz, r, g, b, a);
            drawLine(buf, mat, nx, py, nz, px, py, nz, r, g, b, a);
            drawLine(buf, mat, nx, py, pz, px, py, pz, r, g, b, a);
            drawLine(buf, mat, nx, ny, nz, nx, ny, pz, r, g, b, a);
            drawLine(buf, mat, px, ny, nz, px, ny, pz, r, g, b, a);
            drawLine(buf, mat, nx, py, nz, nx, py, pz, r, g, b, a);
            drawLine(buf, mat, px, py, nz, px, py, pz, r, g, b, a);
            drawLine(buf, mat, nx, ny, nz, nx, py, nz, r, g, b, a);
            drawLine(buf, mat, px, ny, nz, px, py, nz, r, g, b, a);
            drawLine(buf, mat, nx, ny, pz, nx, py, pz, r, g, b, a);
            drawLine(buf, mat, px, ny, pz, px, py, pz, r, g, b, a);

            BufferUploader.drawWithShader(buf.end());

            poseStack.popPose();
            return false;
        });

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawLine(BufferBuilder buf, Matrix4f mat,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}