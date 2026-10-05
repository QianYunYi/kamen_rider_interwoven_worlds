package com.qianyunyi.rider.mixin.hurt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qianyunyi.rider.client.hurt.ClientHurtBreakTracker;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(GameRenderer.class)
public class HurtCameraMixin {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void cancelHurtCameraShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data != null && data.isHenshining()){
            ci.cancel();
        }
        if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
            // 破防（实际扣血 > 5%）时放行，保留镜头摇晃；否则格挡成功，取消镜头摇晃
            if (!ClientHurtBreakTracker.isBrokenHurt(player)) {
                ci.cancel();
            }
        }
    }
}