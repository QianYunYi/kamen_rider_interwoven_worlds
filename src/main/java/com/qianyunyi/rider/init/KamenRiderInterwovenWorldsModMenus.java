
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import com.qianyunyi.rider.world.inventory.SurgicalConsoleInterfaceMenu;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<MenuType<SurgicalConsoleInterfaceMenu>> SURGICAL_CONSOLE_INTERFACE = REGISTRY.register("surgical_console_interface", () -> IForgeMenuType.create(SurgicalConsoleInterfaceMenu::new));
}
