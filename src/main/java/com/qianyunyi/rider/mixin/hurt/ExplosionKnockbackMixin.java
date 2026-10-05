package com.qianyunyi.rider.mixin.hurt;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/** 取消爆炸击退效果
 * @author QianYunYi
 * @date 2023/7/14
 */

@Mixin(Explosion.class)
public class ExplosionKnockbackMixin {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Shadow
    private Map<Player, Vec3> hitPlayers;

    @Inject(method = "explode", at = @At("TAIL"))
    private void cancelExplosionKnockbackForSuperArmor(CallbackInfo ci) {
        if (hitPlayers == null) return;
        ArrayList<Player> toRemove = new ArrayList<>();
        for (Player player : hitPlayers.keySet()) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data != null && data.isHenshining()){
                Vec3 current = player.getDeltaMovement();
                player.setDeltaMovement(0, current.y > 0 ? 0 : current.y, 0);
                toRemove.add(player);
            }
            if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
                // 减免后的伤害 >5%（破防）时放行，保留爆炸击退
                if (data.getLastHurtFraction() <= 0.05f) {
                    Vec3 current = player.getDeltaMovement();
                    player.setDeltaMovement(0, current.y > 0 ? 0 : current.y, 0);
                    toRemove.add(player);
                }
            }
        }
        for (Player player : toRemove) {
            hitPlayers.remove(player);
        }
    }
}