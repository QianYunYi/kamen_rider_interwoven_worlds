package com.qianyunyi.rider.network;

import com.qianyunyi.rider.client.gui.wheel.AbilityWheel;
import com.qianyunyi.rider.client.gui.wheel.WheelManager;
import com.qianyunyi.rider.internal.henshin.ability.AbilityManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;

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
public class AbilityMessage {
	int type, pressedms;

	public AbilityMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public AbilityMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(AbilityMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(AbilityMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			pressAction(context.getSender(), message.type, message.pressedms);
		});
		context.setPacketHandled(true);
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {
			if (world.isClientSide()) {
				RiderPlayerData data = RiderPlayerVariablesProvider.get(entity);
				if (data != null && data.getCurrentState() != HenshinState.INACTIVE) {
					AbilityWheel.INSTANCE.invalidateCache();
					WheelManager.INSTANCE.openAbilityWheel();
				}
			}
		} else if (type == 1) {
			AbilityManager.executeByIndex(entity, pressedms);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		KamenRiderInterwovenWorldsMod.addNetworkMessage(AbilityMessage.class, AbilityMessage::buffer, AbilityMessage::new, AbilityMessage::handler);
	}
}