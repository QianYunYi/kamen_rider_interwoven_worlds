package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.autoaim.ClientAutoAim;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class StartAutoAimPacket implements IMessage {
    private final int duration;

    public StartAutoAimPacket(int duration) {
        this.duration = duration;
    }

    public StartAutoAimPacket(FriendlyByteBuf buf) {
        this.duration = buf.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(duration);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        // 确保只在客户端执行
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (Minecraft.getInstance().player != null) {
                    ClientAutoAim.requestAutoAim(duration);
                }
            });
        }
    }
}