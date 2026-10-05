
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import com.qianyunyi.rider.client.gui.SurgicalConsoleInterfaceScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KamenRiderInterwovenWorldsModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(KamenRiderInterwovenWorldsModMenus.SURGICAL_CONSOLE_INTERFACE.get(), SurgicalConsoleInterfaceScreen::new);
		});
	}
}
