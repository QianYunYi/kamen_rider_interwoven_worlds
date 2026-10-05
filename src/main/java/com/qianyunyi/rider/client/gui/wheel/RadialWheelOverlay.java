package com.qianyunyi.rider.client.gui.wheel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Vector4f;

import java.util.List;

public abstract class RadialWheelOverlay implements IGuiOverlay {

    private static final float RADIUS               = 48f;
    private static final float CENTER_RADIUS        = 22f;
    private static final float OPTION_RADIUS        = 20f;
    private static final int   TEXT_COLOR_SELECTED  = 0xFFF2C94C;
    private static final int   MAX_OPTIONS          = 8;

    private final double ringInnerEdge  = 18;
    private final double ringOuterEdge  = 70;

    private final Vector4f lineColor         = new Vector4f(1f, .85f, .7f, 1f);
    private final Vector4f radialButtonColor = new Vector4f(.04f, .03f, .01f, .6f);
    private final Vector4f highlightColor    = new Vector4f(.8f, .7f, .55f, .5f);

    // 平滑缓入缓出，零回弹
    private static final float LERP_SPEED       = 0.18f;
    private static final float SCALE_LERP_SPEED = 0.22f;
    private static final float MAX_SCALE        = 1.25f;

    protected boolean isOpen        = false;
    protected int     selectedIndex = -1;
    private   int     prevSelectedIndex = -1;

    private float openProgress = 0f;
    private final float[] optionScales = new float[MAX_OPTIONS];

    private double mouseX = 0, mouseY = 0;
    private long   openTime = 0;

    public abstract List<RadialOption> getOptions();

    public void open() {
        this.isOpen = true;
        this.selectedIndex = -1;
        this.prevSelectedIndex = -1;
        this.openTime = System.nanoTime();
        for (int i = 0; i < MAX_OPTIONS; i++) {
            optionScales[i] = 0f;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();
    }

    public void close() {
        List<RadialOption> opts = getOptions();
        if (!opts.isEmpty() && selectedIndex >= 0 && selectedIndex < opts.size()) {
            RadialOption opt = opts.get(selectedIndex);
            LivingEntity entity = Minecraft.getInstance().player;
            if (opt != null) opt.runAction(entity);
        }
        this.isOpen = false;
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
    }

    public boolean isOpen() { return isOpen; }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!isOpen && openProgress < 0.005f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float target = isOpen ? 1f : 0f;
        openProgress += (target - openProgress) * LERP_SPEED;
        if (!isOpen && openProgress < 0.005f) return;

        this.mouseX = mc.mouseHandler.xpos() * (double) screenWidth / mc.getWindow().getWidth();
        this.mouseY = mc.mouseHandler.ypos() * (double) screenHeight / mc.getWindow().getHeight();

        int centerX = screenWidth  / 2;
        int centerY = screenHeight / 2;

        List<RadialOption> options = getOptions();
        int optionCount = Math.min(options.size(), MAX_OPTIONS);
        updateSelectedIndex(centerX, centerY, optionCount);

        if (selectedIndex != prevSelectedIndex) {
            prevSelectedIndex = selectedIndex;
        }

        long elapsed = (System.nanoTime() - openTime) / 1_000_000;
        for (int i = 0; i < optionCount; i++) {
            float scaleTarget;
            if (i == selectedIndex) {
                scaleTarget = MAX_SCALE;
            } else if (isOpen && elapsed < i * 50L) {
                scaleTarget = 0f;
            } else {
                scaleTarget = 1f;
            }
            optionScales[i] += (scaleTarget - optionScales[i]) * SCALE_LERP_SPEED;
            if (optionScales[i] < 0.001f && scaleTarget == 0f) {
                optionScales[i] = 0f;
            }
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        drawRadialBackgrounds(centerX, centerY, optionCount);
        drawOptions(guiGraphics, centerX, centerY, options, optionCount, mc, elapsed);

        poseStack.popPose();
    }

    private void updateSelectedIndex(int centerX, int centerY, int optionCount) {
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        double dist   = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        selectedIndex = -1;
        if (optionCount <= 0) return;
        if (dist > CENTER_RADIUS && dist < (RADIUS + OPTION_RADIUS)) {
            double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
            if (angle < 0) angle += 360;
            double adjusted = (angle + 90) % 360;
            double sectorAngle = 360.0 / optionCount;
            adjusted = (adjusted + sectorAngle / 2) % 360;
            int idx = (int) (adjusted / sectorAngle);
            if (idx >= 0 && idx < optionCount) selectedIndex = idx;
        }
    }

    private void drawRadialBackgrounds(double centerX, double centerY, int totalOptions) {
        if (totalOptions <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        int segments = totalOptions < 2 ? 16 : Math.max(16, totalOptions * 4);
        double radiansPerSegment = 2 * Math.PI / segments;
        double radiansPerOption  = 2 * Math.PI / totalOptions;
        double quarterCircle = Math.PI / 2;

        float eased = easeOutCubic(openProgress);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double beginRadians = i * radiansPerSegment - (quarterCircle + (radiansPerOption / 2));
            double endRadians   = (i + 1) * radiansPerSegment - (quarterCircle + (radiansPerOption / 2));

            double innerR = ringInnerEdge * eased;
            double outerR = ringOuterEdge * eased;

            double x1i = Math.cos(beginRadians) * innerR;
            double x2i = Math.cos(endRadians)   * innerR;
            double y1i = Math.sin(beginRadians) * innerR;
            double y2i = Math.sin(endRadians)   * innerR;

            double x1o = Math.cos(beginRadians) * outerR;
            double x2o = Math.cos(endRadians)   * outerR;
            double y1o = Math.sin(beginRadians) * outerR;
            double y2o = Math.sin(endRadians)   * outerR;

            int optionIndex = (i * totalOptions) / segments;
            boolean highlighted = (optionIndex == selectedIndex);
            Vector4f color = highlighted ? highlightColor : radialButtonColor;

            buffer.vertex(centerX + x1i, centerY + y1i, 0)
                    .color(color.x(), color.y(), color.z(), color.w()).endVertex();
            buffer.vertex(centerX + x2i, centerY + y2i, 0)
                    .color(color.x(), color.y(), color.z(), color.w()).endVertex();
            buffer.vertex(centerX + x2o, centerY + y2o, 0)
                    .color(color.x(), color.y(), color.z(), 0).endVertex();
            buffer.vertex(centerX + x1o, centerY + y1o, 0)
                    .color(color.x(), color.y(), color.z(), 0).endVertex();
        }

        for (int i = 0; i < totalOptions; i++) {
            double angle = i * radiansPerOption - (quarterCircle + (radiansPerOption / 2));
            double cosA = Math.cos(angle);
            double sinA = Math.sin(angle);
            double innerR = ringInnerEdge * eased;
            double lineOuter = innerR + 2;

            buffer.vertex(centerX + cosA * innerR, centerY + sinA * innerR, 0)
                    .color(lineColor.x(), lineColor.y(), lineColor.z(), lineColor.w()).endVertex();
            buffer.vertex(centerX + cosA * (innerR + 0.5), centerY + sinA * (innerR + 0.5), 0)
                    .color(lineColor.x(), lineColor.y(), lineColor.z(), lineColor.w()).endVertex();
            buffer.vertex(centerX + cosA * lineOuter, centerY + sinA * lineOuter, 0)
                    .color(lineColor.x(), lineColor.y(), lineColor.z(), lineColor.w()).endVertex();
            buffer.vertex(centerX + cosA * (lineOuter + 0.5), centerY + sinA * (lineOuter + 0.5), 0)
                    .color(lineColor.x(), lineColor.y(), lineColor.z(), lineColor.w()).endVertex();
        }

        tesselator.end();
        RenderSystem.disableBlend();
    }

    private void drawOptions(GuiGraphics guiGraphics, int centerX, int centerY,
                             List<RadialOption> options, int optionCount,
                             Minecraft mc, long elapsed) {
        RenderSystem.enableBlend();
        PoseStack poseStack = guiGraphics.pose();

        float eased = easeOutCubic(openProgress);

        for (int i = 0; i < optionCount; i++) {
            if (i >= options.size()) break;
            RadialOption opt = options.get(i);
            if (opt == null) continue;

            float angle = (float) (2 * Math.PI * i / optionCount - Math.PI / 2);
            int optX = centerX + (int) (RADIUS * eased * Math.cos(angle));
            int optY = centerY + (int) (RADIUS * eased * Math.sin(angle));

            float scale = optionScales[i];
            if (scale < 0.01f) continue;

            poseStack.pushPose();
            poseStack.translate(optX, optY, 0);
            poseStack.scale(scale, scale, 1f);

            if (opt.texture() != null) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                guiGraphics.blit(opt.texture(), -8, -8, 0, 0, 16, 16, 16, 16);
            } else if (opt.icon() != null && !opt.icon().isEmpty()) {
                guiGraphics.renderItem(opt.icon(), -8, -8);
            }

            poseStack.popPose();

            if (i == selectedIndex && opt.name() != null) {
                String text = opt.name().getString();
                int w = mc.font.width(text);
                guiGraphics.drawString(mc.font, text,
                        optX - w / 2,
                        optY + (int) OPTION_RADIUS + 4,
                        TEXT_COLOR_SELECTED,
                        true);
            }
        }
        RenderSystem.disableBlend();
    }

    private static float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}