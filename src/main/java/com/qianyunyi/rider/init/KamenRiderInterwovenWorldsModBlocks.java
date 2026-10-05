
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import com.qianyunyi.rider.block.ShockerOperatingTableBlock;
import com.qianyunyi.rider.block.KuugaSarcophagusBlock;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<Block> SHOCKER_OPERATING_TABLE = REGISTRY.register("shocker_operating_table", () -> new ShockerOperatingTableBlock());
	public static final RegistryObject<Block> KUUGA_SARCOPHAGUS = REGISTRY.register("kuuga_sarcophagus", () -> new KuugaSarcophagusBlock());

}
