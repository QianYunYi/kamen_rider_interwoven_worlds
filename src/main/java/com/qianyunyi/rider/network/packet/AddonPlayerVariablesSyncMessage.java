package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;


/**
 * 玩家变量同步消息
 *
 * @author QianYunYi
 *
 */
public class AddonPlayerVariablesSyncMessage implements IMessage {
    // 玩家变量数据
    private final CompoundTag data;
    // 玩家实体ID
    private final int entityId;

    /**
     * 玩家变量同步消息
     * @param data
     * @param entityId
     */
    public AddonPlayerVariablesSyncMessage(CompoundTag data, int entityId) {
        this.data = data;
        this.entityId = entityId;
    }

    /**
     * 从字节缓冲中读取玩家变量同步消息
     * @param buf 字节缓冲
     */
    public AddonPlayerVariablesSyncMessage(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
        this.entityId = buf.readInt();
    }

    /**
     * 编码玩家变量同步消息
     * @param buf 字节缓冲
     */
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
        buf.writeInt(entityId);
    }

    /**
     * 处理玩家变量同步消息
     * @param ctx 网络事件上下文
     */
    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.getId() == entityId) {
                    RiderPlayerVariablesProvider.get(player).deserializeNBT(data);
                }
            });
        }
        ctx.setPacketHandled(true);
    }
}