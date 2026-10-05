package com.qianyunyi.rider.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

public class WaterRippleRenderer {

    private static final double RENDER_RANGE = 8.0;
    private static final float SPEED = 0.025f;

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
        if (kd == null || !kd.isSupernaturalWaterActive()) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float tick = (level.getGameTime() + event.getPartialTick()) * SPEED;

        AABB area = new AABB(player.blockPosition()).inflate(RENDER_RANGE);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        if (entities.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (LivingEntity ent : entities) {
            double groundY = findGroundY(level, ent);
            if (groundY == Double.NEGATIVE_INFINITY) continue;

            float dx = (float)(ent.getX() - camPos.x);
            float dy = (float)(groundY - camPos.y);
            float dz = (float)(ent.getZ() - camPos.z);

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            Matrix4f mat = poseStack.last().pose();

            float discRadius = ent.getBbWidth() * 0.9f + 0.25f;
            drawWaterSurface(buf, mat, tick, discRadius);

            poseStack.popPose();
        }

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static double findGroundY(Level level, LivingEntity ent) {
        BlockPos below = ent.blockPosition();
        for (int i = 0; i < 6; i++) {
            below = below.below();
            if (below.getY() < level.getMinBuildHeight()) break;
            if (!level.getBlockState(below).isAir()) {
                return below.getY() + 1.005;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    private static float waveHeight(float cx, float cz, float tick, float discRadius) {
        float dist2 = cx * cx + cz * cz;
        float r2 = discRadius * discRadius;
        if (dist2 >= r2) return 0;

        float sx1 = discRadius * 0.30f, sz1 = discRadius * 0.20f;
        float sx2 = -discRadius * 0.25f, sz2 = -discRadius * 0.35f;

        float dx1 = cx - sx1, dz1 = cz - sz1;
        float dx2 = cx - sx2, dz2 = cz - sz2;
        float d1 = (float)Math.sqrt(dx1 * dx1 + dz1 * dz1);
        float d2 = (float)Math.sqrt(dx2 * dx2 + dz2 * dz2);

        float h = 0;
        h += (float)Math.sin(d1 * 5.5f - tick * 2.0f) * 0.035f;
        h += (float)Math.sin(d2 * 5.0f + tick * 1.8f) * 0.032f;
        h += (float)Math.sin((d1 + d2) * 3.0f + tick * 1.4f) * 0.018f;

        float edge = 1.0f - (float)Math.sqrt(dist2) / discRadius;
        return h * edge;
    }

    private static void drawWaterSurface(BufferBuilder buf, Matrix4f mat, float tick, float discRadius) {
        int gridRes = Math.max(10, (int)(discRadius * 14));
        int ringSegments = Math.max(20, (int)(discRadius * 28));

        for (int ring = 0; ring < gridRes; ring++) {
            float ir = (float)ring / gridRes * discRadius;
            float or = (float)(ring + 1) / gridRes * discRadius;
            float alphaInner = fadeAlpha(ir, discRadius);
            float alphaOuter = fadeAlpha(or, discRadius);

            for (int seg = 0; seg < ringSegments; seg++) {
                double a1 = 2.0 * Math.PI * seg / ringSegments;
                double a2 = 2.0 * Math.PI * (seg + 1) / ringSegments;
                float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
                float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

                float x1 = ir * c1, z1 = ir * s1;
                float x2 = ir * c2, z2 = ir * s2;
                float x3 = or * c1, z3 = or * s1;
                float x4 = or * c2, z4 = or * s2;

                float h1 = waveHeight(x1, z1, tick, discRadius);
                float h2 = waveHeight(x2, z2, tick, discRadius);
                float h3 = waveHeight(x3, z3, tick, discRadius);
                float h4 = waveHeight(x4, z4, tick, discRadius);

                float r1, g1, b1, r2, g2, b2, r3, g3, b3, r4, g4, b4;
                r1 = waterR(h1); g1 = waterG(h1); b1 = waterB(h1);
                r2 = waterR(h2); g2 = waterG(h2); b2 = waterB(h2);
                r3 = waterR(h3); g3 = waterG(h3); b3 = waterB(h3);
                r4 = waterR(h4); g4 = waterG(h4); b4 = waterB(h4);

                float baseZ = 0.001f;

                buf.vertex(mat, x1, z1, baseZ + h1).color(r1, g1, b1, alphaInner).endVertex();
                buf.vertex(mat, x3, z3, baseZ + h3).color(r3, g3, b3, alphaOuter).endVertex();
                buf.vertex(mat, x2, z2, baseZ + h2).color(r2, g2, b2, alphaInner).endVertex();

                buf.vertex(mat, x2, z2, baseZ + h2).color(r2, g2, b2, alphaInner).endVertex();
                buf.vertex(mat, x3, z3, baseZ + h3).color(r3, g3, b3, alphaOuter).endVertex();
                buf.vertex(mat, x4, z4, baseZ + h4).color(r4, g4, b4, alphaOuter).endVertex();
            }
        }
    }

    private static float fadeAlpha(float dist, float discRadius) {
        if (dist >= discRadius) return 0;
        float t = dist / discRadius;
        return 0.55f * (1.0f - t * t);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float waterR(float h) {
        float spec = specular(h);
        float bright = clamp(0.5f + h * 12.0f, 0.05f, 1.0f);
        float base = 0.02f + bright * 0.16f;
        return base * (1.0f - spec) + 0.90f * spec;
    }

    private static float waterG(float h) {
        float spec = specular(h);
        float bright = clamp(0.5f + h * 12.0f, 0.05f, 1.0f);
        float base = 0.14f + bright * 0.38f;
        return base * (1.0f - spec) + 0.95f * spec;
    }

    private static float waterB(float h) {
        float spec = specular(h);
        float bright = clamp(0.5f + h * 12.0f, 0.05f, 1.0f);
        float base = 0.38f + bright * 0.55f;
        return base * (1.0f - spec) + 1.0f * spec;
    }

    private static float specular(float h) {
        if (h <= 0) return 0;
        float s = clamp(h * 6.0f, 0, 1);
        return s * s * 2.0f;
    }
}