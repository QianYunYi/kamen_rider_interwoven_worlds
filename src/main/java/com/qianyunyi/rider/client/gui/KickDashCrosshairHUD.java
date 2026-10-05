package com.qianyunyi.rider.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 空我骑士踢/必杀技自定义准星 Overlay
 * 通过注册自定义 Overlay 在准星位置渲染骑士踢专用准星纹理
 */
@OnlyIn(Dist.CLIENT)
public class KickDashCrosshairHUD implements IGuiOverlay {

    public static final KickDashCrosshairHUD INSTANCE = new KickDashCrosshairHUD();

    private static final ResourceLocation CROSSHAIR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "textures/gui/crosshair/kuuga_rider_kick_crosshair.png");

    private static final int CROSSHAIR_WIDTH  = 20;
    private static final int CROSSHAIR_HEIGHT = 22;
    private static final int TEXTURE_WIDTH  = 25;
    private static final int TEXTURE_HEIGHT = 27;

    private KickDashCrosshairHUD() {}

    /**
     * 在 MOD 总线上注册自定义 Overlay，置于原版准星上方
     */
    @Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(
                    VanillaGuiOverlay.CROSSHAIR.id(),
                    "kick_dash_crosshair",
                    KickDashCrosshairHUD.INSTANCE
            );
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || (!data.isKickDashing() && !data.isSupernaturalEarthActive())) return;

        // 骑士踢期间相机会切换为自定义第三人称视角，此时仍需显示准星；
        // 其余情况仅在第一次人称下显示。
        if (!mc.options.getCameraType().isFirstPerson() && !data.isKickDashing()) return;

        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        // 根据移动速度调整透明度
        double speed = player.getDeltaMovement().length();
        float alpha = (float) Mth.clamp(speed / 0.3, 0.3, 1.0);

        int halfW = CROSSHAIR_WIDTH / 2;
        int halfH = CROSSHAIR_HEIGHT / 2;
        int x = cx - halfW;
        int y = cy - halfH;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        guiGraphics.blit(
                CROSSHAIR_TEXTURE,
                x, y,
                CROSSHAIR_WIDTH, CROSSHAIR_HEIGHT,
                0, 0,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}