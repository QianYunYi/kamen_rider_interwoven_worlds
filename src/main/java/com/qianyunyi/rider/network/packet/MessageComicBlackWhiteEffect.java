package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.client.effect.ComicBlackWhiteEffectHandler;
import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;

public class MessageComicBlackWhiteEffect implements IMessage {
    private final boolean enable;
    private final int duration;
    private final float intensity;
    private final int entityId;
    private final String effectType;
    private final float invert; // 新增 0=正常 1=反转

    // 发送用构造器
    public MessageComicBlackWhiteEffect(boolean enable, int duration, float intensity, int entityId, String effectType, float invert) {
        this.enable = enable;
        this.duration = duration;
        this.intensity = intensity;
        this.entityId = entityId;
        this.effectType = effectType;
        this.invert = invert;
    }

    // 读取用构造器
    public MessageComicBlackWhiteEffect(FriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
        this.duration = buffer.readInt();
        this.intensity = buffer.readFloat();
        this.entityId = buffer.readInt();
        this.effectType = buffer.readUtf(32767);
        this.invert = buffer.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enable);
        buf.writeInt(duration);
        buf.writeFloat(intensity);
        buf.writeInt(entityId);
        buf.writeUtf(effectType);
        buf.writeFloat(invert);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<MessageComicBlackWhiteEffect, NetworkEvent.Context> handler = ComicBlackWhiteEffectHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    // GETTER
    public boolean isEnable() { return enable; }
    public int getDuration() { return duration; }
    public float getIntensity() { return intensity; }
    public int getEntityId() { return entityId; }
    public String getEffectType() { return effectType; }
    public float getInvert() { return invert; }
}