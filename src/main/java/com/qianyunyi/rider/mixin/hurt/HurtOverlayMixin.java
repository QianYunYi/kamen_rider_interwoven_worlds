package com.qianyunyi.rider.mixin.hurt;

import com.qianyunyi.rider.client.hurt.ClientHurtBreakTracker;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Gui.class)
public class HurtOverlayMixin {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Unique
    private int savedHurtTime = -1;

    @Inject(method = "render", at = @At("HEAD"))
    private void cancelHurtOverlay(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data != null && data.isHenshining()){
            ci.cancel();
        }
        if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
            // 破防（实际扣血 > 5%）时放行，保留红色受击遮罩；否则格挡成功，隐藏遮罩
            if (!ClientHurtBreakTracker.isBrokenHurt(player)) {
                savedHurtTime = player.hurtTime;
                player.hurtTime = 0;
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void restoreHurtTime(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (savedHurtTime >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.hurtTime = savedHurtTime;
            }
            savedHurtTime = -1;
        }
    }
}