package com.qianyunyi.rider.network;

import com.qianyunyi.rider.internal.combat.skill.RiderFinishingMoveManager;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RiderFinishingMoveMessage {
	int type, pressedms;

	public RiderFinishingMoveMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public RiderFinishingMoveMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(RiderFinishingMoveMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(RiderFinishingMoveMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			RiderFinishingMoveManager.execute(entity);
		}
		if (type == 1) {
			RiderFinishingMoveManager.releaseSkill(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		KamenRiderInterwovenWorldsMod.addNetworkMessage(RiderFinishingMoveMessage.class, RiderFinishingMoveMessage::buffer, RiderFinishingMoveMessage::new, RiderFinishingMoveMessage::handler);
	}
}