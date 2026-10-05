package com.qianyunyi.rider.client.gui;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KuugaEnergyHUD implements IHUDManager {

    // ===== 常量定义 =====
    private static final class Textures {
        static final ResourceLocation MIGHTY_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/arcle_red_energy_bar.png");
        static final ResourceLocation GROWING_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/arcle_orange_energy_bar.png");
        static final ResourceLocation DRAGON_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/arcle_blue_energy_bar.png");
        static final ResourceLocation PEGASUS_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/arcle_green_energy_bar.png");
        static final ResourceLocation TITAN_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/arcle_purple_energy_bar.png");
        static final ResourceLocation RISING_GOLD_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/rising_arcle_gold_energy_bar.png");
        static final ResourceLocation RISING_DARK_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/rising_arcle_dark_energy_bar.png");
        static final ResourceLocation ULTIMATE_ENERGY_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/ultimate_arcle_energy_bar.png");
        static final ResourceLocation ULTIMATE_ENERGY_GOLD_BAR =
                ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/ultimate_arcle_gold_energy_bar.png");
    }

    private static final class Dimensions {
        static final int TEXTURE_WIDTH = 128;
        static final int TEXTURE_HEIGHT = 32;
        static final int OUTLINE_WIDTH = 128;
        static final int OUTLINE_HEIGHT = 16;
        static final int ENERGY_BAR_WIDTH = 115;
        static final int ENERGY_BAR_HEIGHT = 9;
        static final int BAR_OFFSET_X = 6;
        static final int BAR_OFFSET_Y = 15;
        static final int HUD_MARGIN_RIGHT = 10;
        static final int HUD_MARGIN_BOTTOM = 10;
        static final int ENERGY_BAR_VISUAL_OFFSET_X = 6;
        static final int ENERGY_BAR_VISUAL_OFFSET_Y = 3;
        static final int RISING_BAR_OFFSET_Y = -16;
    }

    private static final class Colors {
        static final int HIGH_ENERGY_COLOR = 0x00FF00;
        static final int MEDIUM_ENERGY_COLOR = 0xFFFF00;
        static final int LOW_ENERGY_COLOR = 0xFF0000;
        static final int TEXT_SHADOW_COLOR = 0x000000;
    }

    private static final class Thresholds {
        static final float HIGH_ENERGY_THRESHOLD = 0.7f;
        static final float MEDIUM_ENERGY_THRESHOLD = 0.3f;
        static final float DEFAULT_MAX_ENERGY = 50.0f;
    }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final KuugaEnergyHUD INSTANCE = new KuugaEnergyHUD();

    private static boolean isRendering = false;
    private static float currentEnergy = 0;
    private static float maxEnergy = Thresholds.DEFAULT_MAX_ENERGY;
    private static float currentAmadamEnergy = 0;
    private static float maxAmadamEnergy = 100.0f;

    // ===== 生命周期 =====
    /**
     * 显示HUD（由外部网络包或变身事件调用）
     */
    public static void register() {
        if (!isRendering) {
            isRendering = true;
            updateEnergyData();
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[KuugaEnergyHUD] HUD已显示");
        }
    }

    /**
     * 隐藏HUD（由外部网络包或解除变身事件调用）
     */
    public static void unregister() {
        if (isRendering) {
            isRendering = false;
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[KuugaEnergyHUD] HUD已隐藏");
        }
    }

    public static KuugaEnergyHUD getInstance() {
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

    public static void updateEnergy(float energy, float max) {
        currentEnergy = energy;
        maxEnergy = max > 0 ? max : Thresholds.DEFAULT_MAX_ENERGY;
    }

    public static void updateEnergyData() {
        Player player = MC.player;
        if (player != null) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null) {
                KuugaDataModule kuugaData = data.getModule(KuugaDataModule.class);
                if (kuugaData != null) {
                    currentEnergy = kuugaData.getSuperEnergy();
                    maxEnergy = kuugaData.getSuperEnergyMax() > 0
                            ? kuugaData.getSuperEnergyMax()
                            : Thresholds.DEFAULT_MAX_ENERGY;

                    currentAmadamEnergy = kuugaData.getAmadamEnergy();
                    maxAmadamEnergy = kuugaData.getAmadamEnergyMax() > 0
                            ? kuugaData.getAmadamEnergyMax()
                            : 100.0f;
                }
            }
        }
    }
    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (MC.options.renderDebug) return;
        if (MC.player == null) return;
        if (!isRendering) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(MC.player);
        if (data == null) return;

        boolean isKuugaTransformed = "Kuuga".equals(data.getActiveRiderName())
                && data.getActiveFormName() != null
                && !data.getActiveFormName().isEmpty();
        if (!isKuugaTransformed) {
            return;
        }

        updateEnergyData();

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        renderEnergyHUD(guiGraphics, screenWidth, screenHeight, data, MC.player);
    }

    private static void renderEnergyHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight, RiderPlayerData data, Player player) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        HUDLayout layout = calculateHUDLayout(screenWidth, screenHeight);
        float energyRatio = calculateEnergyRatio();
        float amadamEnergyRatio = calculateAmadamEnergyRatio();

        KuugaDataModule kuugaData = data.getModule(KuugaDataModule.class);
        if (kuugaData == null) return;

        String formName = RiderPlayerVariablesProvider.get(player).getActiveFormName();
        boolean hasGoldPower = kuugaData.isHasGoldPower();

        renderAmadamEnergyBar(guiGraphics, layout, amadamEnergyRatio, formName);

        if (hasGoldPower) {
            renderRisingEnergyBar(guiGraphics, layout, energyRatio, formName);
            renderSuperChargeSaturationBar(guiGraphics, layout, formName);
        }

        RenderSystem.disableBlend();
    }

    private static void renderRisingEnergyBar(GuiGraphics guiGraphics, HUDLayout layout, float energyRatio, String formName) {
        ResourceLocation texture = getRisingFormTexture(formName);
        if (texture == null) return;

        int risingY = layout.hudY + Dimensions.RISING_BAR_OFFSET_Y;

        guiGraphics.blit(texture, layout.hudX, risingY, 0, 0,
                Dimensions.OUTLINE_WIDTH, Dimensions.OUTLINE_HEIGHT,
                Dimensions.TEXTURE_WIDTH, Dimensions.TEXTURE_HEIGHT);

        int fillWidth = (int) (Dimensions.ENERGY_BAR_WIDTH * energyRatio);
        if (fillWidth > 0) {
            int x = layout.hudX + Dimensions.ENERGY_BAR_VISUAL_OFFSET_X;
            int y = risingY + Dimensions.ENERGY_BAR_VISUAL_OFFSET_Y;
            guiGraphics.blit(texture, x, y,
                    Dimensions.BAR_OFFSET_X, Dimensions.BAR_OFFSET_Y,
                    fillWidth, Dimensions.ENERGY_BAR_HEIGHT,
                    Dimensions.TEXTURE_WIDTH, Dimensions.TEXTURE_HEIGHT);
        }

        renderRisingEnergyText(guiGraphics, layout, energyRatio, risingY);
    }

    private static void renderSuperChargeSaturationBar(GuiGraphics guiGraphics, HUDLayout layout, String formName) {
        Player player = MC.player;
        if (player == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getModule(KuugaDataModule.class);
        if (kuugaData == null) return;

        float current = kuugaData.getSuperChargeSaturation();
        float max = kuugaData.getSuperChargeSaturationMax();
        if (max <= 0 || current <= 0) return;

        float ratio = Math.min(1.0f, Math.max(0.0f, current / max));
        ResourceLocation texture = getRisingFormTexture(formName);
        if (texture == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        int risingY = layout.hudY + Dimensions.RISING_BAR_OFFSET_Y;
        int fillWidth = (int) (Dimensions.ENERGY_BAR_WIDTH * ratio);

        if (fillWidth > 0) {
            int x = layout.hudX + Dimensions.ENERGY_BAR_VISUAL_OFFSET_X;
            int y = risingY + Dimensions.ENERGY_BAR_VISUAL_OFFSET_Y;
            guiGraphics.blit(texture, x, y + 1,
                    Dimensions.BAR_OFFSET_X, Dimensions.BAR_OFFSET_Y + 7,
                    fillWidth, Dimensions.ENERGY_BAR_HEIGHT - 1,
                    Dimensions.TEXTURE_WIDTH, Dimensions.TEXTURE_HEIGHT);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderAmadamEnergyBar(GuiGraphics guiGraphics, HUDLayout layout, float energyRatio, String formName) {
        ResourceLocation texture = getAmadamFormTexture(formName);

        guiGraphics.blit(texture, layout.hudX, layout.hudY, 0, 0,
                Dimensions.OUTLINE_WIDTH, Dimensions.OUTLINE_HEIGHT,
                Dimensions.TEXTURE_WIDTH, Dimensions.TEXTURE_HEIGHT);

        int fillWidth = (int) (Dimensions.ENERGY_BAR_WIDTH * energyRatio);
        if (fillWidth > 0) {
            int x = layout.hudX + Dimensions.ENERGY_BAR_VISUAL_OFFSET_X;
            int y = layout.hudY + Dimensions.ENERGY_BAR_VISUAL_OFFSET_Y;
            guiGraphics.blit(texture, x, y,
                    Dimensions.BAR_OFFSET_X, Dimensions.BAR_OFFSET_Y,
                    fillWidth, Dimensions.ENERGY_BAR_HEIGHT,
                    Dimensions.TEXTURE_WIDTH, Dimensions.TEXTURE_HEIGHT);
        }

        renderAmadamEnergyText(guiGraphics, layout, energyRatio);
    }

    // ===== 文字渲染 =====
    private static void renderAmadamEnergyText(GuiGraphics guiGraphics, HUDLayout layout, float energyRatio) {
        String energyText = formatAmadamEnergyText();
        TextPosition position = calculateTextPosition(layout, energyText);
        int textColor = getTextColor(energyRatio);
        Font font = MC.font;
        guiGraphics.drawString(font, energyText, position.x, position.y, textColor);
    }

    private static void renderRisingEnergyText(GuiGraphics guiGraphics, HUDLayout layout, float energyRatio, int risingY) {
        String energyText = formatRisingEnergyText();
        Font font = MC.font;
        int textWidth = font.width(energyText);
        int textX = layout.hudX + (Dimensions.OUTLINE_WIDTH - textWidth) / 2;
        int textY = risingY + (Dimensions.OUTLINE_HEIGHT - font.lineHeight) / 2;
        int textColor = getTextColor(energyRatio);
        guiGraphics.drawString(font, energyText, textX, textY, textColor);
    }

    // ===== 工具方法 =====
    private static int getTextColor(float energyRatio) {
        if (energyRatio >= Thresholds.HIGH_ENERGY_THRESHOLD) return Colors.HIGH_ENERGY_COLOR;
        if (energyRatio >= Thresholds.MEDIUM_ENERGY_THRESHOLD) return Colors.MEDIUM_ENERGY_COLOR;
        return Colors.LOW_ENERGY_COLOR;
    }

    private static HUDLayout calculateHUDLayout(int screenWidth, int screenHeight) {
        int hudX = screenWidth - Dimensions.OUTLINE_WIDTH - Dimensions.HUD_MARGIN_RIGHT;
        int hudY = screenHeight - Dimensions.OUTLINE_HEIGHT - Dimensions.HUD_MARGIN_BOTTOM;
        return new HUDLayout(hudX, hudY);
    }

    private static float calculateEnergyRatio() {
        return maxEnergy > 0 ? Math.min(1.0f, Math.max(0.0f, currentEnergy / maxEnergy)) : 0;
    }

    private static float calculateAmadamEnergyRatio() {
        return maxAmadamEnergy > 0 ? Math.min(1.0f, Math.max(0.0f, currentAmadamEnergy / maxAmadamEnergy)) : 0;
    }

    private static boolean isAmazingOrAboveForm(String formName) {
        return formName.equals("amazing_mighty_form") ||
                formName.equals("ultimate_form") ||
                formName.equals("ultimate_form_dark_eye") ||
                formName.equals("rising_ultimate_form") ||
                formName.equals("rising_ultimate_form_dark_eye") ||
                formName.equals("super_rising_ultimate_form");
    }

    private static ResourceLocation getAmadamFormTexture(String formName) {
        if (formName.equals("rising_mighty_form")) {
            return Textures.MIGHTY_ENERGY_BAR;
        } else if (formName.equals("rising_dragon_form")) {
            return Textures.DRAGON_ENERGY_BAR;
        } else if (formName.equals("rising_pegasus_form")) {
            return Textures.PEGASUS_ENERGY_BAR;
        } else if (formName.equals("rising_titan_form")) {
            return Textures.TITAN_ENERGY_BAR;
        } else if (isAmazingOrAboveForm(formName)) {
            return Textures.MIGHTY_ENERGY_BAR;
        } else {
            return switch (formName) {
                case "might_form" -> Textures.MIGHTY_ENERGY_BAR;
                case "dragon_form" -> Textures.DRAGON_ENERGY_BAR;
                case "pegasus_form" -> Textures.PEGASUS_ENERGY_BAR;
                case "titan_form" -> Textures.TITAN_ENERGY_BAR;
                default -> Textures.GROWING_ENERGY_BAR;
            };
        }
    }

    private static ResourceLocation getRisingFormTexture(String formName) {
        return switch (formName) {
            case "rising_mighty_form", "rising_dragon_form", "rising_pegasus_form", "rising_titan_form" ->
                    Textures.RISING_GOLD_ENERGY_BAR;
            case "amazing_mighty_form" -> Textures.RISING_DARK_ENERGY_BAR;
            case "ultimate_form", "ultimate_form_dark_eye", "rising_ultimate_form", "rising_ultimate_form_dark_eye" ->
                    Textures.ULTIMATE_ENERGY_BAR;
            case "super_rising_ultimate_form" -> Textures.ULTIMATE_ENERGY_GOLD_BAR;
            default -> null;
        };
    }

    private static String formatAmadamEnergyText() {
        return String.format("%.0f/%.0f", currentAmadamEnergy, maxAmadamEnergy);
    }

    private static String formatRisingEnergyText() {
        return String.format("%.0f/%.0f", currentEnergy, maxEnergy);
    }

    private static TextPosition calculateTextPosition(HUDLayout layout, String energyText) {
        Font font = MC.font;
        int textWidth = font.width(energyText);
        int textX = layout.hudX + (Dimensions.OUTLINE_WIDTH - textWidth) / 2;
        int textY = layout.hudY + (Dimensions.OUTLINE_HEIGHT - font.lineHeight) / 2;
        return new TextPosition(textX, textY);
    }

    private record HUDLayout(int hudX, int hudY) {}
    private record TextPosition(int x, int y) {}
}