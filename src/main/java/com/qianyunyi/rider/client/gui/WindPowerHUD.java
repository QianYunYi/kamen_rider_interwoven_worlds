package com.qianyunyi.rider.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qianyunyi.rider.data.module.ShowaData;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WindPowerHUD implements IHUDManager {

    // ===== 单一风力值纹理 =====
    private static final ResourceLocation WIND_POWER_BAR =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/wind_power_bar.png");

    // ===== 尺寸配置 =====
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 32;
    private static final int OUTLINE_WIDTH = 128;
    private static final int OUTLINE_HEIGHT = 16;
    private static final int ENERGY_BAR_WIDTH = 115;
    private static final int ENERGY_BAR_HEIGHT = 9;
    private static final int BAR_OFFSET_X = 6;
    private static final int BAR_OFFSET_Y = 15;

    // ===== 颜色 =====
    private static final int HIGH_COLOR = 0x00FF00;
    private static final int MEDIUM_COLOR = 0xFFFF00;
    private static final int LOW_COLOR = 0xFF0000;
    private static final int SHADOW_COLOR = 0x000000;

    // ===== 默认值 =====
    private static final float DEFAULT_MAX = 50.0f;

    // ===== 实例 =====
    private static final Minecraft MC = Minecraft.getInstance();
    private static final WindPowerHUD INSTANCE = new WindPowerHUD();

    private static boolean isRendering = false;
    private static boolean isRegistered = false;
    private static float currentWind = 0;
    private static float maxWind = DEFAULT_MAX;

    // ==============================================
    // 注册 / 注销
    // ==============================================
    public static void register() {
        if (!isRegistered) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
            isRegistered = true;
            isRendering = true;
            updateData();
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[WindPowerHUD] 已注册");
        }
    }

    public static void unregister() {
        if (isRegistered) {
            MinecraftForge.EVENT_BUS.unregister(INSTANCE);
            isRegistered = false;
            isRendering = false;
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[WindPowerHUD] 已注销");
        }
    }

    public static WindPowerHUD getInstance() {
        return INSTANCE;
    }

    @Override
    public void show() {
        register();
    }

    @Override
    public void hide() {
        unregister();
    }

    // ==============================================
    // 数据更新
    // ==============================================
    public static void setWindPower(float current, float max) {
        currentWind = Math.max(0, current);
        maxWind = Math.max(1, max);
    }

    public static void updateData() {
        Player player = MC.player;
        if (player == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;

        // 通过模块系统获取 ShowaData
        ShowaData showaData = data.getModule(ShowaData.class);
        if (showaData != null) {
            currentWind = showaData.getWindPower();
            maxWind = Math.max(1, showaData.getMaxWindPower());
        }
    }

    // ==============================================
    // 渲染事件（修复后）
    // ==============================================
    @SubscribeEvent
    public void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (!isRendering || MC.options.renderDebug) return;
        if (MC.player == null) return;

        updateData();

        // 获取 GuiGraphics 和窗口尺寸
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        renderWindBar(guiGraphics, screenWidth, screenHeight);
    }

    // ==============================================
    // 渲染风力条（修复后）
    // ==============================================
    private void renderWindBar(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 位置：右下角
        int x = screenWidth - OUTLINE_WIDTH - 10;
        int y = screenHeight - OUTLINE_HEIGHT - 10;

        // 1. 渲染背景框
        RenderSystem.setShaderTexture(0, WIND_POWER_BAR);
        guiGraphics.blit(WIND_POWER_BAR, x, y, 0, 0, OUTLINE_WIDTH, OUTLINE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 2. 渲染填充条
        float ratio = Math.min(1.0f, currentWind / maxWind);
        int fillWidth = (int) (ENERGY_BAR_WIDTH * ratio);

        if (fillWidth > 0) {
            guiGraphics.blit(WIND_POWER_BAR,
                    x + BAR_OFFSET_X,
                    y + 3,
                    BAR_OFFSET_X,
                    BAR_OFFSET_Y,
                    fillWidth,
                    ENERGY_BAR_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        }

        // 3. 渲染文字：当前 / 最大值
        String text = String.format("%.0f/%.0f", currentWind, maxWind);
        Font font = MC.font;
        int textX = x + (OUTLINE_WIDTH - font.width(text)) / 2;
        int textY = y + (OUTLINE_HEIGHT - font.lineHeight) / 2;

        // 使用 GuiGraphics 渲染文字
        guiGraphics.drawString(font, text, textX, textY, getColor(ratio));

        RenderSystem.disableBlend();
    }

    // ==============================================
    // 颜色根据比例自动变化
    // ==============================================
    private int getColor(float ratio) {
        if (ratio > 0.7f) return HIGH_COLOR;
        if (ratio > 0.3f) return MEDIUM_COLOR;
        return LOW_COLOR;
    }
}