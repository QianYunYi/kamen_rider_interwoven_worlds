package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.effect.CameraPerspectiveHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;

/**
 * 同步相机视角切换消息（Server -> Client）
 * 0 = 恢复之前保存的视角
 * 1 = 保存当前视角并切换到第三人称背面（越肩视角）
 * 2 = 救援越肩视角（镜头拉远）
 */
public class CameraPerspectiveSyncMessage implements IMessage {
    private final int perspective;

    public CameraPerspectiveSyncMessage(int perspective) {
        this.perspective = perspective;
    }

    public CameraPerspectiveSyncMessage(FriendlyByteBuf buffer) {
        this.perspective = buffer.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(perspective);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<CameraPerspectiveSyncMessage, NetworkEvent.Context> handler =
                CameraPerspectiveHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    public int getPerspective() {
        return perspective;
    }
}