package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.item.base.BaseAnimationItem;
import com.qianyunyi.rider.item.base.BaseCrossbowAnimationItem;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端通知服务器：设置手持物品的动画
 */
public class SetItemAnimPacket implements IMessage {

    private final InteractionHand hand;
    private final String anim;
    private final int itemSlot; // 备用槽位，用于非手持情况

    /** 客户端构造：用于手持物品 */
    public SetItemAnimPacket(InteractionHand hand, String anim) {
        this.hand = hand;
        this.anim = anim;
        this.itemSlot = -1;
    }

    /** 客户端构造：用于指定槽位的物品 */
    public SetItemAnimPacket(int slot, String anim) {
        this.hand = null;
        this.anim = anim;
        this.itemSlot = slot;
    }

    /** 解码构造：由网络反射调用 */
    public SetItemAnimPacket(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            this.hand = buf.readEnum(InteractionHand.class);
            this.itemSlot = -1;
        } else {
            this.hand = null;
            this.itemSlot = buf.readInt();
        }
        this.anim = buf.readUtf(32);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        if (hand != null) {
            buf.writeBoolean(true);
            buf.writeEnum(hand);
        } else {
            buf.writeBoolean(false);
            buf.writeInt(itemSlot);
        }
        buf.writeUtf(anim, 32);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) return;

        ItemStack stack = null;

        if (hand != null) {
            // 手持物品
            stack = player.getItemInHand(hand);
        } else if (itemSlot >= 0) {
            // 背包槽位物品
            stack = player.getInventory().getItem(itemSlot);
        }

        // 在服务器端设置权威NBT
        // 同步给其他客户端
        if (stack != null && stack.getItem() instanceof BaseAnimationItem) {
            // 在服务器端设置权威NBT
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(BaseAnimationItem.TAG_ANIM, anim);

            // 同步给其他客户端
            syncToTrackingClients(player, stack, hand, itemSlot, anim);
        }else if (stack != null && stack.getItem() instanceof BaseSwordAnimationItem) {
            // 在服务器端设置权威NBT
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(BaseSwordAnimationItem.TAG_ANIM, anim);

            // 同步给其他客户端
            syncToTrackingClients(player, stack, hand, itemSlot, anim);
        } else if (stack != null && stack.getItem() instanceof BaseCrossbowAnimationItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(BaseCrossbowAnimationItem.TAG_ANIM, anim);
            syncToTrackingClients(player, stack, hand, itemSlot, anim);
        }
    }

    private void syncToTrackingClients(ServerPlayer player, ItemStack stack,
                                       InteractionHand hand, int slot, String anim) {
        // 这里可以添加同步给其他客户端的逻辑
        // 例如：player.server.getPlayerList().broadcast(...)
    }
}