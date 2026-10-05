package com.qianyunyi.rider.mixin;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当玩家处于骑士踢/必杀技状态时，取消原版准星渲染。
 * 自定义准星由 IGuiOverlay 实现 {@code KickDashCrosshairHUD} 负责渲染。
 */
@Mixin(Gui.class)
public class CrosshairMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void kamenRiderCrosshair$onRenderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || (!data.isKickDashing() && !data.isSupernaturalEarthActive())) return;

        // 骑士踢期间相机会切换为自定义第三人称视角，此时仍需取消原版准星、保留自定义准星；
        // 其余情况仅在第一次人称下取消。
        if (!mc.options.getCameraType().isFirstPerson() && !data.isKickDashing()) return;

        // 取消原版准星渲染，自定义准星由 KickDashCrosshairHUD Overlay 绘制
        ci.cancel();
    }
}