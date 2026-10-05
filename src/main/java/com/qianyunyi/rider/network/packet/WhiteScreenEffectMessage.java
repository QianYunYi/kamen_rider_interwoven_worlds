package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.qianyunyi.rider.client.effect.ComicBlackWhiteRenderer;
import net.minecraft.client.Minecraft;

public class WhiteScreenEffectMessage implements IMessage {
    private final float duration; // 效果持续时间（秒）
    private final float intensity; // 效果强度（0-1）

    public WhiteScreenEffectMessage(float duration, float intensity) {
        this.duration = duration;
        this.intensity = intensity;
    }

    public WhiteScreenEffectMessage(FriendlyByteBuf buffer) {
        this.duration = buffer.readFloat();
        this.intensity = buffer.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(duration);
        buffer.writeFloat(intensity);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            // 在客户端触发白屏效果
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ComicBlackWhiteRenderer.start(mc.player, (int) (duration * 20), intensity, 0.0f);
            }
        });
        context.setPacketHandled(true);
    }
}