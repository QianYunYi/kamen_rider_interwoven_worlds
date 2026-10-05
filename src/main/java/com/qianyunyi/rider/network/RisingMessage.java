package com.qianyunyi.rider.network;

import com.qianyunyi.rider.client.gui.wheel.FormSwitchWheel;
import com.qianyunyi.rider.client.gui.wheel.WheelManager;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RisingMessage {
	int type, pressedms;

	public RisingMessage(int type, int pressedms) {
		this.type = type;
		this.pressedms = pressedms;
	}

	public RisingMessage(FriendlyByteBuf buffer) {
		this.type = buffer.readInt();
		this.pressedms = buffer.readInt();
	}

	public static void buffer(RisingMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.type);
		buffer.writeInt(message.pressedms);
	}

	public static void handler(RisingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
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
				// 形态切换冷却中禁止打开轮盘
				if (FormSwitchWheel.isOnFormSwitchCooldown()) {
					return;
				}
				boolean shiftDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
						|| GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
				if (shiftDown) {
					FormSwitchWheel.INSTANCE.setMode(FormSwitchWheel.Mode.CYCLE);
				} else {
					FormSwitchWheel.INSTANCE.setMode(FormSwitchWheel.Mode.ADVANCEMENT);
				}
				WheelManager.INSTANCE.openFormSwitch();
			}
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		KamenRiderInterwovenWorldsMod.addNetworkMessage(RisingMessage.class, RisingMessage::buffer, RisingMessage::new, RisingMessage::handler);
	}
}