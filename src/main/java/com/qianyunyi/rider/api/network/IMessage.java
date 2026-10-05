package com.qianyunyi.rider.api.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface IMessage {
    /**
     * 编码消息到字节缓冲区
     */
    void encode(FriendlyByteBuf buf);

    /**
     * 处理收到的消息
     */
    void handle(NetworkEvent.Context ctx);

    /**
     * 默认的消息处理包装方法
     */
    default void handleMessage(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.enqueueWork(() -> handle(context));
        context.setPacketHandled(true);
    }
}