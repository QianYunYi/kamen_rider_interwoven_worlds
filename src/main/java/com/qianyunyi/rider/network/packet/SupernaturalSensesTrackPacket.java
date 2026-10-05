package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.client.effect.SupernaturalSensesTrackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;

/**
 * 超自然感官——潜行追踪模式数据包（Server -> Client）
 * 当空我玩家开启超自然感官并潜行超过1秒后，服务端周期发送此包，
 * 通知客户端当前追踪的实体ID，客户端据此渲染暗色遮罩和镜头转向。
 */
public class SupernaturalSensesTrackPacket implements IMessage {
    private final boolean active;
    private final int targetEntityId;

    /**
     * @param active         是否处于追踪模式
     * @param targetEntityId 当前追踪的实体ID（无目标时为 -1）
     */
    public SupernaturalSensesTrackPacket(boolean active, int targetEntityId) {
        this.active = active;
        this.targetEntityId = targetEntityId;
    }

    public SupernaturalSensesTrackPacket(FriendlyByteBuf buffer) {
        this.active = buffer.readBoolean();
        this.targetEntityId = buffer.readInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeInt(targetEntityId);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        BiConsumer<SupernaturalSensesTrackPacket, NetworkEvent.Context> handler =
                SupernaturalSensesTrackHandler.getHandler();
        if (handler != null) {
            handler.accept(this, ctx);
        }
        ctx.setPacketHandled(true);
    }

    public boolean isActive() {
        return active;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }
}