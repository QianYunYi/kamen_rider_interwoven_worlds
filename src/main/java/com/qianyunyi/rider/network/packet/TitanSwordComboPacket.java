package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.network.IMessage;
import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class TitanSwordComboPacket implements IMessage {

    public TitanSwordComboPacket() {
    }

    @SuppressWarnings("unused")
    public TitanSwordComboPacket(FriendlyByteBuf buf) {
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] Packet received, direction={}", ctx.getDirection());
        if (ctx.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn("[TitanSword] Wrong direction, ignoring");
            return;
        }
        ServerPlayer player = ctx.getSender();
        KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] Packet sender: {}", player);
        if (player == null) return;
        RiderAttackEventDispatcher.handleTitanSwordComboPacket(player);
    }
}