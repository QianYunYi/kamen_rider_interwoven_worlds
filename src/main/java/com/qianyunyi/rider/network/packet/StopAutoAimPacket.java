package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.autoaim.ClientAutoAim;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端→客户端：停止自动瞄准
 */
public class StopAutoAimPacket implements IMessage {

    public StopAutoAimPacket() {}

    public StopAutoAimPacket(FriendlyByteBuf buf) {}

    @Override
    public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().player != null) {
                    ClientAutoAim.cancelAutoAim();
                }
            });
        }
    }
}