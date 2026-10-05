package com.qianyunyi.rider.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义Boss血条渲染系统
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CustomBossBar {
    // 存储所有自定义Boss血条配置
    public static Map<UUID, CustomBossBar> customBossBars = new HashMap<>();

    // 血条纹理资源
    private final ResourceLocation baseTexture;       // 基础血条纹理
    private final ResourceLocation overlayTexture;    // 覆盖层纹理（可选）
    private final boolean hasOverlay;                 // 是否有覆盖层

    // 尺寸和位置参数
    private final int baseHeight;                     // 基础血条高度
    private final int baseTextureHeight;              // 基础纹理高度
    private final int baseOffsetX;                    // 基础血条X偏移
    private final int baseOffsetY;                    // 基础血条Y偏移
    private final int overlayOffsetX;                 // 覆盖层X偏移
    private final int overlayOffsetY;                 // 覆盖层Y偏移
    private final int overlayWidth;                   // 覆盖层宽度
    private final int overlayHeight;                  // 覆盖层高度

    // 其他参数
    private final int verticalIncrement;              // 垂直间距增量
    private final int progressWidth;                  // 进度条宽度
    private final ChatFormatting textColor;           // 文本颜色

    // 进度填充左移量（像素）
	private final int progressOffsetX;

	/**
	 * 自定义Boss血条构造函数
	 *
	 * @param baseTexture        基础血条纹理
	 * @param overlayTexture     覆盖层纹理（可为null）
	 * @param baseHeight         基础血条高度
	 * @param baseTextureHeight  基础纹理高度
	 * @param baseOffsetX        基础血条X偏移
	 * @param baseOffsetY        基础血条Y偏移
	 * @param overlayOffsetX     覆盖层X偏移
	 * @param overlayOffsetY     覆盖层Y偏移
	 * @param overlayWidth       覆盖层宽度
	 * @param overlayHeight      覆盖层高度
	 * @param verticalIncrement 垂直间距增量
	 * @param progressWidth      进度条宽度
	 * @param textColor         文本颜色
	 */
	public CustomBossBar(ResourceLocation baseTexture, ResourceLocation overlayTexture,
						 int baseHeight, int baseTextureHeight,
						 int baseOffsetX, int baseOffsetY,
						 int overlayOffsetX, int overlayOffsetY,
						 int overlayWidth, int overlayHeight,
						 int verticalIncrement, int progressWidth,
						 ChatFormatting textColor) {
		this(baseTexture, overlayTexture, baseHeight, baseTextureHeight,
			 baseOffsetX, baseOffsetY,
			 overlayOffsetX, overlayOffsetY,
			 overlayWidth, overlayHeight,
			 verticalIncrement, progressWidth, textColor,
			 0); // 默认进度不偏移
	}

	/**
	 * 自定义Boss血条构造函数（含进度偏移）
	 *
	 * @param baseTexture        基础血条纹理
	 * @param overlayTexture     覆盖层纹理（可为null）
	 * @param baseHeight         基础血条高度
	 * @param baseTextureHeight  基础纹理高度
	 * @param baseOffsetX        基础血条X偏移
	 * @param baseOffsetY        基础血条Y偏移
	 * @param overlayOffsetX     覆盖层X偏移
	 * @param overlayOffsetY     覆盖层Y偏移
	 * @param overlayWidth       覆盖层宽度
	 * @param overlayHeight      覆盖层高度
	 * @param verticalIncrement 垂直间距增量
	 * @param progressWidth      进度条宽度
	 * @param textColor         文本颜色
	 * @param progressOffsetX   进度填充左移量（正数左移）
	 */
	public CustomBossBar(ResourceLocation baseTexture, ResourceLocation overlayTexture,
						 int baseHeight, int baseTextureHeight,
						 int baseOffsetX, int baseOffsetY,
						 int overlayOffsetX, int overlayOffsetY,
						 int overlayWidth, int overlayHeight,
						 int verticalIncrement, int progressWidth,
						 ChatFormatting textColor,
						 int progressOffsetX) {
        this.baseTexture = baseTexture;
        this.overlayTexture = overlayTexture;
        this.hasOverlay = overlayTexture != null;
        this.baseHeight = baseHeight;
        this.baseTextureHeight = baseTextureHeight;
        this.baseOffsetX = baseOffsetX;
        this.baseOffsetY = baseOffsetY;
        this.overlayOffsetX = overlayOffsetX;
        this.overlayOffsetY = overlayOffsetY;
        this.overlayWidth = overlayWidth;
        this.overlayHeight = overlayHeight;
        this.verticalIncrement = verticalIncrement;
        this.progressWidth = progressWidth;
        this.textColor = textColor;
        this.progressOffsetX = progressOffsetX;
    }

    // ============== Getter方法 ==============
    public ResourceLocation getBaseTexture() {
        return baseTexture;
    }

    public ResourceLocation getOverlayTexture() {
        return overlayTexture;
    }

    public boolean hasOverlay() {
        return hasOverlay;
    }

    public int getBaseHeight() {
        return baseHeight;
    }

    public int getBaseTextureHeight() {
        return baseTextureHeight;
    }

    public int getBaseOffsetX() {
        return baseOffsetX;
    }

    public int getBaseOffsetY() {
        return baseOffsetY;
    }

    public int getOverlayOffsetX() {
        return overlayOffsetX;
    }

    public int getOverlayOffsetY() {
        return overlayOffsetY;
    }

    public int getOverlayWidth() {
        return overlayWidth;
    }

    public int getOverlayHeight() {
        return overlayHeight;
    }

    public int getProgressWidth() {
        return progressWidth;
    }

    public int getVerticalIncrement() {
        return verticalIncrement;
    }

    public ChatFormatting getTextColor() {
        return textColor;
    }

    public int getProgressOffsetX() {
        return progressOffsetX;
    }

    /**
     * 渲染自定义Boss血条
     */
    public void renderBossBar(GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textY = y - 9;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 渲染基础血条
        drawBar(guiGraphics, x + getBaseOffsetX(), y + getBaseOffsetY(), bossEvent);

        // 渲染Boss名称
        Component name = bossEvent.getName().copy().withStyle(getTextColor());
        Font font = mc.font;
        int nameWidth = font.width(name);
        int nameX = screenWidth / 2 - nameWidth / 2;
        guiGraphics.drawString(font, name, nameX, textY, 0xFFFFFF);

        // 渲染覆盖层
        if (hasOverlay()) {
            RenderSystem.setShaderTexture(0, getOverlayTexture());
            guiGraphics.blit(
                    getOverlayTexture(),
                    x + getBaseOffsetX() + getOverlayOffsetX() - progressOffsetX,
                    y + getBaseOffsetY() + getOverlayOffsetY(),
                    0, 0,
                    getOverlayWidth(), getOverlayHeight(),
                    getOverlayWidth(), getOverlayHeight()
            );
        }

        RenderSystem.disableBlend();
    }

    /**
     * 绘制血条进度
     */
    private void drawBar(GuiGraphics guiGraphics, int x, int y, BossEvent event) {
        // 绘制基础血条背景
        guiGraphics.blit(
                getBaseTexture(),
                x, y,
                0, 0,
                progressWidth, baseHeight,
                256, baseTextureHeight
        );

        // 计算血量进度
        int progress = (int)(event.getProgress() * (progressWidth + 1));
        if (progress > 0) {
            // 绘制血量进度条（左移 progressOffsetX 像素）
            guiGraphics.blit(
                    getBaseTexture(),
                    x - progressOffsetX, y,
                    0, baseHeight,
                    progress, baseHeight,
                    256, baseTextureHeight
            );
        }
    }

    // 反射访问BossHealthOverlay的events字段
    private static Field bossEventsField;
    private static boolean fieldAccessInitialized = false;

    @SuppressWarnings("unchecked")
    private static Map<UUID, ? extends BossEvent> getBossEventsFromOverlay(Object bossOverlay) {
        if (!fieldAccessInitialized) {
            try {
                Class<?> overlayClass = bossOverlay.getClass();
                // 尝试查找events字段（可能名称不同）
                for (Field field : overlayClass.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        bossEventsField = field;
                        break;
                    }
                }
            } catch (Exception e) {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("无法访问Boss血条字段: {}", e.getMessage());
            }
            fieldAccessInitialized = true;
        }

        if (bossEventsField != null) {
            try {
                return (Map<UUID, ? extends BossEvent>) bossEventsField.get(bossOverlay);
            } catch (Exception e) {
                // 忽略异常，返回空map
            }
        }
        return new HashMap<>();
    }

    // 事件处理器 - 取消原版Boss血条并渲染自定义血条
    @SubscribeEvent
    public static void onBossBarRenderPre(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;
        if (customBossBars.isEmpty()) return;

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.renderDebug) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();

        var bossOverlay = mc.gui.getBossOverlay();
        int yOffset = 12;

        Map<UUID, ? extends BossEvent> bossEvents = getBossEventsFromOverlay(bossOverlay);
        for (var entry : bossEvents.entrySet()) {
            UUID bossId = entry.getKey();
            BossEvent bossEvent = entry.getValue();
            CustomBossBar customBar = customBossBars.get(bossId);

            if (customBar != null) {
                int barX = screenWidth / 2 - customBar.getProgressWidth() / 2;
                customBar.renderBossBar(guiGraphics, barX, yOffset, bossEvent);
                yOffset += customBar.getBaseTextureHeight() + customBar.getVerticalIncrement();
            }
        }
    }
}