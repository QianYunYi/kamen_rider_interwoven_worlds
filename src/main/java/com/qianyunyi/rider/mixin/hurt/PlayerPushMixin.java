package com.qianyunyi.rider.mixin.hurt;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/** 取消推搡效果
 * @author QianYunYi
 */

@Mixin(Entity.class)
public class PlayerPushMixin {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void cancelPushVelocity(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null && data.isHenshining()){
                ci.cancel();
            }
            if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
                // 减免后的伤害 >5%（破防）时放行，保留推搡效果
                if (data.getLastHurtFraction() <= 0.05f) {
                    ci.cancel();
                }
            }
        }
    }
}