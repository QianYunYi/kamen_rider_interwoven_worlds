package com.qianyunyi.rider.mixin.hurt;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/** 取消近战/命中击退效果
 * @author QianYunYi
 */

@Mixin(LivingEntity.class)
public class KnockbackMixin {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void cancelKnockbackForSuperArmor(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null && data.isHenshining()) {
                ci.cancel();
            }
            if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
                // 减免后的伤害 >5%（破防）时放行，保留近战命中击退
                if (data.getLastHurtFraction() <= 0.05f) {
                    ci.cancel();
                }
            }
        }
    }
}