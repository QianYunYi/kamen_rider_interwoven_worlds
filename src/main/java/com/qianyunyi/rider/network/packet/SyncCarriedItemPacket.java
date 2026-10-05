package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 同步客户端手持物品到服务端的数据包
 */
public class SyncCarriedItemPacket implements IMessage {
    private ItemStack carriedItem; // 手持物品

    // 空构造函数（用于服务端解码）
    public SyncCarriedItemPacket() {}

    // 带参数构造函数（客户端发送时使用）
    public SyncCarriedItemPacket(ItemStack carriedItem) {
        this.carriedItem = carriedItem;
    }

    // 接受 FriendlyByteBuf 的构造函数（服务端解码时使用）
    public SyncCarriedItemPacket(FriendlyByteBuf buf) {
        this.carriedItem = buf.readItem();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(carriedItem); // 将手持物品写入字节流
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        // 在服务端主线程执行逻辑
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                // 更新服务端玩家容器菜单的手持物品
                player.containerMenu.setCarried(carriedItem);
                // 广播容器变更，确保服务端与客户端状态一致
                player.containerMenu.broadcastChanges();
            }
        });
        ctx.setPacketHandled(true);
    }

    // 解码方法（服务端从字节流读取数据包）
    public static SyncCarriedItemPacket decode(FriendlyByteBuf buf) {
        return new SyncCarriedItemPacket(buf);
    }
}