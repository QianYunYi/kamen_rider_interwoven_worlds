package com.qianyunyi.rider.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMovementLockMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void postTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        MobEffectInstance lock = player.getEffect(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get()
        );

        if (lock != null) {
            var data = player.getPersistentData();

            if (!data.contains("lock_pos")) {
                data.putDouble("lock_x", player.getX());
                data.putDouble("lock_y", player.getY());
                data.putDouble("lock_z", player.getZ());
                data.putFloat("lock_yrot", player.getYRot());
                data.putFloat("lock_xrot", player.getXRot());
                data.putBoolean("lock_pos", true);
            }

            double x = data.getDouble("lock_x");
            double y = data.getDouble("lock_y");
            double z = data.getDouble("lock_z");
            float yRot = data.getFloat("lock_yrot");
            float xRot = data.getFloat("lock_xrot");

            player.connection.teleport(x, y, z, yRot, xRot);
            player.setDeltaMovement(0, 0, 0);
            player.setYHeadRot(yRot);
            player.setYBodyRot(yRot);
            player.fallDistance = 0;
            player.hurtMarked = true;
        } else {
            var data = player.getPersistentData();
            data.remove("lock_pos");
            data.remove("lock_x");
            data.remove("lock_y");
            data.remove("lock_z");
            data.remove("lock_yrot");
            data.remove("lock_xrot");
        }
    }
}