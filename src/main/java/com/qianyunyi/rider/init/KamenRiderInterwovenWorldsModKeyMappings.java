package com.qianyunyi.rider.init;

import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import com.qianyunyi.rider.client.gui.wheel.WheelManager;
import com.qianyunyi.rider.network.SummonDriverMessage;
import com.qianyunyi.rider.network.RisingMessage;
import com.qianyunyi.rider.network.RiderFinishingMoveMessage;
import com.qianyunyi.rider.network.HenshinMessage;
import com.qianyunyi.rider.network.AbilityMessage;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class KamenRiderInterwovenWorldsModKeyMappings {
	public static final KeyMapping HENSHIN = new KeyMapping("key.kamen_rider_interwoven_worlds.henshin", GLFW.GLFW_KEY_G, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new HenshinMessage(0, 0));
				HenshinMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping SUMMON_DRIVER = new KeyMapping("key.kamen_rider_interwoven_worlds.summon_driver", GLFW.GLFW_KEY_C, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new SummonDriverMessage(0, 0));
				SummonDriverMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping RISING = new KeyMapping("key.kamen_rider_interwoven_worlds.rising", GLFW.GLFW_KEY_V, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown) {
				if (isDown) {
					KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new RisingMessage(0, 0));
					RisingMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				} else {
					WheelManager.INSTANCE.closeCurrent();
				}
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping ABILITY = new KeyMapping("key.kamen_rider_interwoven_worlds.ability", GLFW.GLFW_KEY_Z, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown) {
				if (isDown) {
					KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new AbilityMessage(0, 0));
					AbilityMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				} else {
					WheelManager.INSTANCE.closeCurrent();
				}
			}
			isDownOld = isDown;
		}
	};
	private static long RIDER_FINISHING_MOVE_LASTPRESS = 0;

	public static final KeyMapping RIDER_FINISHING_MOVE = new KeyMapping("key.kamen_rider_interwoven_worlds.rider_finishing_move", GLFW.GLFW_KEY_X, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown) {
				if (isDown) {
					RIDER_FINISHING_MOVE_LASTPRESS = System.currentTimeMillis();
					KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new RiderFinishingMoveMessage(0, 0));
					RiderFinishingMoveMessage.pressAction(Minecraft.getInstance().player, 0, 0);
				} else {
					int dt = (int) (System.currentTimeMillis() - RIDER_FINISHING_MOVE_LASTPRESS);
					KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new RiderFinishingMoveMessage(1, dt));
					RiderFinishingMoveMessage.pressAction(Minecraft.getInstance().player, 1, dt);
				}
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(HENSHIN);
		event.register(SUMMON_DRIVER);
		event.register(RISING);
		event.register(ABILITY);
		event.register(RIDER_FINISHING_MOVE);
	}

	@Mod.EventBusSubscriber({Dist.CLIENT})
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (Minecraft.getInstance().screen == null) {
				HENSHIN.consumeClick();
				SUMMON_DRIVER.consumeClick();
				RISING.consumeClick();
				ABILITY.consumeClick();
				RIDER_FINISHING_MOVE.consumeClick();
			}
		}
	}
}