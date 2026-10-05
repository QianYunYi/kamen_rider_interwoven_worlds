package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.effect.SupernaturalKillingIntentEffectHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;

public class MessageSupernaturalKillingIntent implements IMessage {
    private final boolean enable;
    private final int duration;
    private final float intensity;

    public MessageSupernaturalKillingIntent(boolean enable, int duration, float intensity) {
        this.enable = enable;
        this.duration = duration;
        this.intensity = intensity;
    }

    public MessageSupernaturalKillingIntent(FriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
        this.duration = buffer.readInt();
        this.intensity = buffer.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enable);
        buf.writeInt(duration);
        buf.writeFloat(intensity);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<MessageSupernaturalKillingIntent, NetworkEvent.Context> handler =
                SupernaturalKillingIntentEffectHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    public boolean isEnable() { return enable; }
    public int getDuration() { return duration; }
    public float getIntensity() { return intensity; }
}