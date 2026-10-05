package com.qianyunyi.rider.network;

import com.qianyunyi.rider.client.gui.wheel.WheelManager;
import com.qianyunyi.rider.internal.henshin.RisingManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;

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
public class SummonDriverMessage {
	int type, pressedms;

	public SummonDriverMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public SummonDriverMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(SummonDriverMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(SummonDriverMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
				if (data == null) return;

				if (data.getCurrentState() == HenshinState.INACTIVE
						&& !data.isHenshining()
						&& entity.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get()) == null) {
					// 未变身：召唤驱动器轮盘
					WheelManager.INSTANCE.openDriverSelect();
				} else if (data.getCurrentState() == HenshinState.ACTIVE
						|| data.getCurrentState() == HenshinState.ACTIVE_AWAKENING) {
					// 已变身：执行同级形态轮询切换
					RisingManager.execute(entity);
				}
			}
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		KamenRiderInterwovenWorldsMod.addNetworkMessage(SummonDriverMessage.class, SummonDriverMessage::buffer, SummonDriverMessage::new, SummonDriverMessage::handler);
	}
}