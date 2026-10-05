package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 同步装备槽位物品的数据包
 */
public class SyncEquipmentSlotPacket implements IMessage {
    private EquipmentSlot slot;
    private ItemStack stack;

    // 空构造函数（用于服务端解码）
    public SyncEquipmentSlotPacket() {}

    // 带参数构造函数（客户端发送时使用）
    public SyncEquipmentSlotPacket(EquipmentSlot slot, ItemStack stack) {
        this.slot = slot;
        this.stack = stack;
    }

    // 接受 FriendlyByteBuf 的构造函数（服务端解码时使用）
    public SyncEquipmentSlotPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(EquipmentSlot.class);
        this.stack = buf.readItem();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(slot);
        buf.writeItem(stack);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        // 在服务端主线程执行逻辑
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                // 更新服务端玩家的装备槽位
                player.setItemSlot(slot, stack);
                // 广播容器变更，确保服务端与客户端状态一致
                player.containerMenu.broadcastChanges();
            }
        });
        ctx.setPacketHandled(true);
    }

    // 解码方法（服务端从字节流读取数据包）
    public static SyncEquipmentSlotPacket decode(FriendlyByteBuf buf) {
        return new SyncEquipmentSlotPacket(buf);
    }
}