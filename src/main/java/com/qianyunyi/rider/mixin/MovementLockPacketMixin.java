package com.qianyunyi.rider.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffectInstance;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class MovementLockPacketMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(
            method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHandleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        MobEffectInstance lock = player.getEffect(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get()
        );

        if (lock != null) {
            ci.cancel();
        }
    }
}