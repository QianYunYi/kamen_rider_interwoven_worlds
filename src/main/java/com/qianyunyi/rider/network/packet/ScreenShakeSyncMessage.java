package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.effect.ScreenShakeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;

/**
 * 屏幕震动消息（Server -> Client）
 * 可复用：骑士踢落地、超自然之土等技能均可调用
 */
public class ScreenShakeSyncMessage implements IMessage {
    private final float intensity;
    private final int durationTicks;

    /**
     * @param intensity     震动强度（最大偏移量，单位：方块，建议 0.1~0.5）
     * @param durationTicks 震动持续时间（tick，建议 5~20）
     */
    public ScreenShakeSyncMessage(float intensity, int durationTicks) {
        this.intensity = intensity;
        this.durationTicks = durationTicks;
    }

    public ScreenShakeSyncMessage(FriendlyByteBuf buffer) {
        this.intensity = buffer.readFloat();
        this.durationTicks = buffer.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeInt(durationTicks);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<ScreenShakeSyncMessage, NetworkEvent.Context> handler =
                ScreenShakeHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    public float getIntensity() { return intensity; }
    public int getDurationTicks() { return durationTicks; }
}