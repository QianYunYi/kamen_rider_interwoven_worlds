package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class UpdateItemDataPacket implements IMessage {
    private final int slot;
    private final CompoundTag tagData;

    public UpdateItemDataPacket(int slot, CompoundTag tagData) {
        this.slot = slot;
        this.tagData = tagData;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(slot);
        buffer.writeNbt(tagData);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ItemStack stack = player.getInventory().getItem(slot);
               /* if (stack.getItem() instanceof PegasusCrossbowItem) {
                    stack.setTag(tagData);
                }*/
            }
        });
        ctx.setPacketHandled(true);
    }

    public static UpdateItemDataPacket decode(FriendlyByteBuf buffer) {
        return new UpdateItemDataPacket(buffer.readInt(), buffer.readNbt());
    }
}