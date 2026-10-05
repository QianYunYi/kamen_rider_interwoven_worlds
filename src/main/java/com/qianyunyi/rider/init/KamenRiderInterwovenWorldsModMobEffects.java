package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.effect.MobEffect;

import com.qianyunyi.rider.potion.UltimatePowerMobEffect;
import com.qianyunyi.rider.potion.SuppressEnergyMobEffect;
import com.qianyunyi.rider.potion.SupernaturalWaterMobEffect;
import com.qianyunyi.rider.potion.SupernaturalFireMobEffect;
import com.qianyunyi.rider.potion.MovementLockMobEffect;
import com.qianyunyi.rider.potion.HenshinCannotMobEffect;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<MobEffect> MOVEMENT_LOCK = REGISTRY.register("movement_lock", () -> new MovementLockMobEffect());
	public static final RegistryObject<MobEffect> HENSHIN_CANNOT = REGISTRY.register("henshin_cannot", () -> new HenshinCannotMobEffect());
	public static final RegistryObject<MobEffect> SUPPRESS_ENERGY = REGISTRY.register("suppress_energy", () -> new SuppressEnergyMobEffect());
	public static final RegistryObject<MobEffect> ULTIMATE_POWER = REGISTRY.register("ultimate_power", () -> new UltimatePowerMobEffect());
	public static final RegistryObject<MobEffect> SUPERNATURAL_FIRE = REGISTRY.register("supernatural_fire", () -> new SupernaturalFireMobEffect());
	public static final RegistryObject<MobEffect> SUPERNATURAL_WATER = REGISTRY.register("supernatural_water", () -> new SupernaturalWaterMobEffect());
}
