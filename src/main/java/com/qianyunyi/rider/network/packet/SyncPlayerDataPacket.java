package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/**
 * 同步玩家数据包
 */
public final class SyncPlayerDataPacket implements IMessage {
    private final CompoundTag nbt;
    private final UUID target;

    public SyncPlayerDataPacket(CompoundTag nbt, UUID target) {
        this.nbt = nbt;
        this.target = target != null ? target : Util.NIL_UUID;
    }

    public SyncPlayerDataPacket(FriendlyByteBuf buf) {
        this(buf.readNbt(), buf.readUUID());
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
        buf.writeUUID(target);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                Player clientPlayer = getClientPlayer();
                if (clientPlayer != null &&
                        (target.equals(Util.NIL_UUID) || clientPlayer.getUUID().equals(target))) {
                    RiderPlayerVariablesProvider.get(clientPlayer).deserializeNBT(nbt);
                }
            }
        }));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static Player getClientPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }
}