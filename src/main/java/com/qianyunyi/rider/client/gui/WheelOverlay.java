package com.qianyunyi.rider.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 自定义轮盘叠加层 — 假面骑士风格渲染
 * 选项不显示文字，选中时在轮盘中央显示名称
 */
public class WheelOverlay implements IGuiOverlay {
    public static final WheelOverlay INSTANCE = new WheelOverlay();

    // ==================== 轮盘状态 ====================

    public boolean isOpen = false;
    private int selectedIndex = -1;
    private float openProgress = 0f;
    private double mouseX, mouseY;

    // ==================== 外观配置 ====================

    private static final float RADIUS = 60f;
    private static final float CENTER_RADIUS = 25f;
    private static final float OPTION_RADIUS = 20f;
    private final int MAX_OPTIONS;

    private static final double RING_INNER = 20;
    private static final double RING_OUTER = 80;

    // ==================== 配色（加亮版，看得见） ====================

    private static final Vector4f COLOR_RADIAL_BG   = new Vector4f(0.08f, 0.06f, 0.10f, 0.55f);
    private static final Vector4f COLOR_RADIAL_SEL  = new Vector4f(0.85f, 0.15f, 0.08f, 0.6f);
    private static final Vector4f COLOR_LINE        = new Vector4f(0.9f, 0.2f, 0.12f, 0.8f);
    private static final Vector4f COLOR_OUTER_RING  = new Vector4f(0.9f, 0.2f, 0.12f, 0.75f);
    private static final Vector4f COLOR_GLOW        = new Vector4f(0.9f, 0.25f, 0.15f, 0.3f);
    private static final Vector4f COLOR_CENTER_BG   = new Vector4f(0.06f, 0.04f, 0.07f, 0.7f);
    private static final Vector4f COLOR_CENTER_BRD  = new Vector4f(0.9f, 0.2f, 0.12f, 0.7f);

    // ==================== 选项数据 ====================

    private List<Radialoption> options;

    // ==================== 构造函数 ====================

    public WheelOverlay(int maxOptions, List<Radialoption> options) {
        MAX_OPTIONS = maxOptions;
        this.options = options;
    }

    private WheelOverlay() {
        this(8, List.of(
                Radialoption.add(new ItemStack(KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get()),
                        Component.literal("召唤亚古鲁"), (living) -> {
                            if (living instanceof Player player) {
                                // TODO: 实现召唤逻辑
                            }
                        })
        ));
    }

    // ==================== 公共控制方法 ====================

    public void open() {
        this.isOpen = true;
        this.selectedIndex = -1;
        Minecraft.getInstance().mouseHandler.releaseMouse();
    }

    public void close() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            options.get(selectedIndex).action().run(Minecraft.getInstance().player);
        }
        this.isOpen = false;
        Minecraft.getInstance().mouseHandler.grabMouse();
    }

    public void addOption(Radialoption option) {
        this.options.add(option);
    }

    public boolean hasOption(Radialoption option) {
        return this.options.contains(option);
    }

    public void removeOption(Radialoption option) {
        if (hasOption(option))
            this.options.remove(option);
    }

    public List<Radialoption> getOptions() {
        return options;
    }

    // ==================== IGuiOverlay 接口实现 ====================

    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isOpen && openProgress < 0.01f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (isOpen) {
            openProgress = Math.min(1f, openProgress + 0.15f);
        } else {
            openProgress = Math.max(0f, openProgress - 0.15f);
        }
        if (openProgress < 0.01f) return;

        long windowHandle = mc.getWindow().getWindow();
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, xPos, yPos);

        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();
        this.mouseX = xPos[0] * screenWidth / windowWidth;
        this.mouseY = yPos[0] * screenHeight / windowHeight;

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;
        double mouseDist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        double mouseAngle = Math.toDegrees(Math.atan2(deltaY, deltaX));
        if (mouseAngle < 0) mouseAngle += 360;

        int optionCount = Math.min(options.size(), MAX_OPTIONS);
        selectedIndex = -1;

        if (mouseDist > CENTER_RADIUS * openProgress
                && mouseDist < (RADIUS + OPTION_RADIUS) * openProgress) {
            float sectorAngle = 360f / optionCount;
            double adjusted = (mouseAngle + 90) % 360;
            adjusted = (adjusted + sectorAngle / 2) % 360;
            selectedIndex = (int) (adjusted / sectorAngle);
            if (selectedIndex < 0 || selectedIndex >= optionCount) {
                selectedIndex = -1;
            }
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 1. 先画背景（让它在下层）
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawRadialBackgrounds(buffer, centerX, centerY, selectedIndex);
        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // 2. 再画道具图标（在上层，不会被背景盖住）
        drawOption(guiGraphics, optionCount, centerX, centerY, mc);

        // 3. 最后画中央文字
        drawCenterText(guiGraphics, centerX, centerY, mc);

        poseStack.popPose();
    }

    // ==================== 绘图：扇形背景 + 外环 + 中心圆 ====================

    private void drawRadialBackgrounds(BufferBuilder buffer, double centerX, double centerY, int selectedIndex) {
        int totalOptions = options.size();
        if (totalOptions == 0) return;

        double radiansPerOption = 2 * Math.PI / totalOptions;
        int segmentsPerOption = 12;

        for (int opt = 0; opt < totalOptions; opt++) {
            double beginAngle = opt * radiansPerOption - Math.PI / 2 - radiansPerOption / 2;
            boolean isSelected = (opt == selectedIndex);

            for (int j = 0; j < segmentsPerOption; j++) {
                double a1 = beginAngle + j * radiansPerOption / segmentsPerOption;
                double a2 = beginAngle + (j + 1) * radiansPerOption / segmentsPerOption;

                double ix1 = Math.cos(a1) * RING_INNER;
                double iy1 = Math.sin(a1) * RING_INNER;
                double ix2 = Math.cos(a2) * RING_INNER;
                double iy2 = Math.sin(a2) * RING_INNER;
                double ox1 = Math.cos(a1) * RING_OUTER;
                double oy1 = Math.sin(a1) * RING_OUTER;
                double ox2 = Math.cos(a2) * RING_OUTER;
                double oy2 = Math.sin(a2) * RING_OUTER;

                Vector4f base = isSelected ? COLOR_RADIAL_SEL : COLOR_RADIAL_BG;

                buffer.vertex(centerX + ix1, centerY + iy1, 0).color(base.x(), base.y(), base.z(), base.w()).endVertex();
                buffer.vertex(centerX + ix2, centerY + iy2, 0).color(base.x(), base.y(), base.z(), base.w()).endVertex();
                buffer.vertex(centerX + ox2, centerY + oy2, 0).color(base.x(), base.y(), base.z(), 0.0f).endVertex();
                buffer.vertex(centerX + ox1, centerY + oy1, 0).color(base.x(), base.y(), base.z(), 0.0f).endVertex();
            }

            // 扇区分隔线
            double lx1 = Math.cos(beginAngle) * RING_INNER;
            double ly1 = Math.sin(beginAngle) * RING_INNER;
            double lx2 = Math.cos(beginAngle) * RING_OUTER;
            double ly2 = Math.sin(beginAngle) * RING_OUTER;
            double lw = 2.0;
            double lnx1 = Math.cos(beginAngle - 0.02) * RING_INNER;
            double lny1 = Math.sin(beginAngle - 0.02) * RING_INNER;
            double lnx2 = Math.cos(beginAngle - 0.02) * RING_OUTER;
            double lny2 = Math.sin(beginAngle - 0.02) * RING_OUTER;

            buffer.vertex(centerX + lx1, centerY + ly1, 0).color(COLOR_LINE.x(), COLOR_LINE.y(), COLOR_LINE.z(), 0.9f).endVertex();
            buffer.vertex(centerX + lnx1, centerY + lny1, 0).color(COLOR_LINE.x(), COLOR_LINE.y(), COLOR_LINE.z(), 0.9f).endVertex();
            buffer.vertex(centerX + lnx2, centerY + lny2, 0).color(COLOR_LINE.x(), COLOR_LINE.y(), COLOR_LINE.z(), 0.9f).endVertex();
            buffer.vertex(centerX + lx2, centerY + ly2, 0).color(COLOR_LINE.x(), COLOR_LINE.y(), COLOR_LINE.z(), 0.9f).endVertex();
        }

        drawOuterRing(buffer, centerX, centerY);
        drawCenterCircle(buffer, centerX, centerY);
    }

    private void drawOuterRing(BufferBuilder buffer, double centerX, double centerY) {
        double ringThickness = 3.5;
        double glowOuter = RING_OUTER + ringThickness + 5;
        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double a1 = 2 * Math.PI * i / segments;
            double a2 = 2 * Math.PI * (i + 1) / segments;

            double ix1 = Math.cos(a1) * RING_OUTER;
            double iy1 = Math.sin(a1) * RING_OUTER;
            double ix2 = Math.cos(a2) * RING_OUTER;
            double iy2 = Math.sin(a2) * RING_OUTER;
            double ox1 = Math.cos(a1) * (RING_OUTER + ringThickness);
            double oy1 = Math.sin(a1) * (RING_OUTER + ringThickness);
            double ox2 = Math.cos(a2) * (RING_OUTER + ringThickness);
            double oy2 = Math.sin(a2) * (RING_OUTER + ringThickness);

            buffer.vertex(centerX + ix1, centerY + iy1, 0).color(COLOR_OUTER_RING.x(), COLOR_OUTER_RING.y(), COLOR_OUTER_RING.z(), 0.9f).endVertex();
            buffer.vertex(centerX + ix2, centerY + iy2, 0).color(COLOR_OUTER_RING.x(), COLOR_OUTER_RING.y(), COLOR_OUTER_RING.z(), 0.9f).endVertex();
            buffer.vertex(centerX + ox2, centerY + oy2, 0).color(COLOR_OUTER_RING.x(), COLOR_OUTER_RING.y(), COLOR_OUTER_RING.z(), 0.0f).endVertex();
            buffer.vertex(centerX + ox1, centerY + oy1, 0).color(COLOR_OUTER_RING.x(), COLOR_OUTER_RING.y(), COLOR_OUTER_RING.z(), 0.0f).endVertex();

            // 外发光
            double gx1 = Math.cos(a1) * (RING_OUTER + ringThickness);
            double gy1 = Math.sin(a1) * (RING_OUTER + ringThickness);
            double gx2 = Math.cos(a2) * (RING_OUTER + ringThickness);
            double gy2 = Math.sin(a2) * (RING_OUTER + ringThickness);
            double gox1 = Math.cos(a1) * glowOuter;
            double goy1 = Math.sin(a1) * glowOuter;
            double gox2 = Math.cos(a2) * glowOuter;
            double goy2 = Math.sin(a2) * glowOuter;

            buffer.vertex(centerX + gx1, centerY + gy1, 0).color(COLOR_GLOW.x(), COLOR_GLOW.y(), COLOR_GLOW.z(), 0.4f).endVertex();
            buffer.vertex(centerX + gx2, centerY + gy2, 0).color(COLOR_GLOW.x(), COLOR_GLOW.y(), COLOR_GLOW.z(), 0.4f).endVertex();
            buffer.vertex(centerX + gox2, centerY + goy2, 0).color(COLOR_GLOW.x(), COLOR_GLOW.y(), COLOR_GLOW.z(), 0.0f).endVertex();
            buffer.vertex(centerX + gox1, centerY + goy1, 0).color(COLOR_GLOW.x(), COLOR_GLOW.y(), COLOR_GLOW.z(), 0.0f).endVertex();
        }
    }

    private void drawCenterCircle(BufferBuilder buffer, double centerX, double centerY) {
        int segments = 32;
        int rings = 4;
        double centerR = RING_INNER - 1;

        for (int ring = 0; ring < rings; ring++) {
            double innerR = centerR * ring / rings;
            double outerR = centerR * (ring + 1) / rings;
            float alpha = COLOR_CENTER_BG.w() - 0.08f * ring;

            for (int i = 0; i < segments; i++) {
                double a1 = 2 * Math.PI * i / segments;
                double a2 = 2 * Math.PI * (i + 1) / segments;

                double ix1 = Math.cos(a1) * innerR;
                double iy1 = Math.sin(a1) * innerR;
                double ix2 = Math.cos(a2) * innerR;
                double iy2 = Math.sin(a2) * innerR;
                double ox1 = Math.cos(a1) * outerR;
                double oy1 = Math.sin(a1) * outerR;
                double ox2 = Math.cos(a2) * outerR;
                double oy2 = Math.sin(a2) * outerR;

                buffer.vertex(centerX + ix1, centerY + iy1, 0).color(COLOR_CENTER_BG.x(), COLOR_CENTER_BG.y(), COLOR_CENTER_BG.z(), alpha).endVertex();
                buffer.vertex(centerX + ix2, centerY + iy2, 0).color(COLOR_CENTER_BG.x(), COLOR_CENTER_BG.y(), COLOR_CENTER_BG.z(), alpha).endVertex();
                buffer.vertex(centerX + ox2, centerY + oy2, 0).color(COLOR_CENTER_BG.x(), COLOR_CENTER_BG.y(), COLOR_CENTER_BG.z(), alpha * 0.5f).endVertex();
                buffer.vertex(centerX + ox1, centerY + oy1, 0).color(COLOR_CENTER_BG.x(), COLOR_CENTER_BG.y(), COLOR_CENTER_BG.z(), alpha * 0.5f).endVertex();
            }
        }

        // 中心圆边框
        double borderW = 2.0;
        for (int i = 0; i < segments; i++) {
            double a1 = 2 * Math.PI * i / segments;
            double a2 = 2 * Math.PI * (i + 1) / segments;

            double ix1 = Math.cos(a1) * centerR;
            double iy1 = Math.sin(a1) * centerR;
            double ix2 = Math.cos(a2) * centerR;
            double iy2 = Math.sin(a2) * centerR;
            double ox1 = Math.cos(a1) * (centerR + borderW);
            double oy1 = Math.sin(a1) * (centerR + borderW);
            double ox2 = Math.cos(a2) * (centerR + borderW);
            double oy2 = Math.sin(a2) * (centerR + borderW);

            buffer.vertex(centerX + ix1, centerY + iy1, 0).color(COLOR_CENTER_BRD.x(), COLOR_CENTER_BRD.y(), COLOR_CENTER_BRD.z(), 0.8f).endVertex();
            buffer.vertex(centerX + ix2, centerY + iy2, 0).color(COLOR_CENTER_BRD.x(), COLOR_CENTER_BRD.y(), COLOR_CENTER_BRD.z(), 0.8f).endVertex();
            buffer.vertex(centerX + ox2, centerY + oy2, 0).color(COLOR_CENTER_BRD.x(), COLOR_CENTER_BRD.y(), COLOR_CENTER_BRD.z(), 0.0f).endVertex();
            buffer.vertex(centerX + ox1, centerY + oy1, 0).color(COLOR_CENTER_BRD.x(), COLOR_CENTER_BRD.y(), COLOR_CENTER_BRD.z(), 0.0f).endVertex();
        }
    }

    // ==================== 绘图：中央文字 ====================

    private void drawCenterText(GuiGraphics guiGraphics, int centerX, int centerY, Minecraft mc) {
        String centerText = selectedIndex >= 0 && selectedIndex < options.size()
                ? options.get(selectedIndex).name().getString()
                : "菜单";

        int textWidth = mc.font.width(centerText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - mc.font.lineHeight / 2;

        // 阴影
        guiGraphics.drawString(mc.font, centerText,
                textX + 1, textY + 1, 0x330000, false);

        // 正文
        guiGraphics.drawString(mc.font, centerText,
                textX, textY,
                selectedIndex >= 0 ? 0xFFFF4444 : 0xFFEEEEEE,
                false);
    }

    // ==================== 绘图：道具图标（不显示文字） ====================

    private void drawOption(GuiGraphics guiGraphics, int optionCount, int centerX, int centerY, Minecraft mc) {
        PoseStack poseStack = guiGraphics.pose();

        for (int i = 0; i < optionCount; i++) {
            float angle = (float) (2 * Math.PI * i / optionCount - Math.PI / 2);
            int optX = centerX + (int) (RADIUS * openProgress * Math.cos(angle));
            int optY = centerY + (int) (RADIUS * openProgress * Math.sin(angle));

            boolean selected = (i == selectedIndex);

            if (i < options.size()) {
                float scale = selected ? 1.25f : 1.0f;

                poseStack.pushPose();
                poseStack.translate(optX, optY, 0);
                poseStack.scale(scale, scale, 1.0f);

                if (selected) {
                    guiGraphics.fill(-14, -14, 14, 14, 0x66FF3333);
                    guiGraphics.fill(-13, -13, 13, 13, 0x44FF2222);
                }

                guiGraphics.fill(-11, -11, 11, 11, 0xCC0A0A0A);
                guiGraphics.fill(-10, -10, 10, 10, 0x99181818);

                if (!options.get(i).icon().isEmpty()) {
                    guiGraphics.renderItem(options.get(i).icon(), -8, -8);
                }

                poseStack.popPose();
            }
        }
    }

    // ==================== 辅助方法 ====================

    private static void executeCommand(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand(cmd);
        }
    }

    // ==================== 内部类：选项数据 ====================

    public interface Radialoption {
        ItemStack icon();
        Component name();
        Run action();

        static Radialoption add(ItemStack icon, Component component, Run runnable) {
            return new Radialoption() {
                @Override
                public ItemStack icon() {
                    return icon;
                }

                @Override
                public Component name() {
                    return component;
                }

                @Override
                public Run action() {
                    return runnable;
                }
            };
        }

        @FunctionalInterface
        interface Run {
            void run(LivingEntity player);
        }
    }

    public record Center(double centerX, double centerY) {
    }
}