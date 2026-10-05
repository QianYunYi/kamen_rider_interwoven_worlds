package com.qianyunyi.rider.network;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.henshin.SummonDriverManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端选中驱动器轮盘的选项后，向服务端发送的消息。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public record SummonDriverSelectMessage(String riderName) {

    public SummonDriverSelectMessage(String riderName) {
        this.riderName = riderName == null ? "" : riderName;
    }

    public static void buffer(SummonDriverSelectMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.riderName);
    }

    public static SummonDriverSelectMessage decode(FriendlyByteBuf buf) {
        return new SummonDriverSelectMessage(buf.readUtf());
    }

    public static void handler(SummonDriverSelectMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player sender = ctx.get().getSender();
            // 直接走严格模式，不回退
            if (sender instanceof ServerPlayer serverPlayer) {
                SummonDriverManager.executeForRider(serverPlayer, msg.riderName);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        KamenRiderInterwovenWorldsMod.addNetworkMessage(
                SummonDriverSelectMessage.class,
                SummonDriverSelectMessage::buffer,
                SummonDriverSelectMessage::decode,
                SummonDriverSelectMessage::handler
        );
    }
}