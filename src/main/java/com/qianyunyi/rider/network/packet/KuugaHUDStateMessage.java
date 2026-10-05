package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.gui.KuugaEnergyHUD;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class KuugaHUDStateMessage implements IMessage {
    private final boolean shouldShow;

    // 主构造函数
    public KuugaHUDStateMessage(boolean shouldShow) {
        this.shouldShow = shouldShow;
    }

    // 必须添加的解码构造函数
    public KuugaHUDStateMessage(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    // 保持decode方法作为静态工厂方法
    public static KuugaHUDStateMessage decode(FriendlyByteBuf buf) {
        return new KuugaHUDStateMessage(buf);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(shouldShow);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (shouldShow) {
                KuugaEnergyHUD.register();
            } else {
               KuugaEnergyHUD.unregister();
            }
        });
        ctx.setPacketHandled(true);
    }
}