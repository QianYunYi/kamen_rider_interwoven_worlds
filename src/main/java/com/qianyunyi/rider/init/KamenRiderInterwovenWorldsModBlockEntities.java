
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;
import com.qianyunyi.rider.block.entity.KuugaSarcophagusTileEntity;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<BlockEntityType<ShockerOperatingTableTileEntity>> SHOCKER_OPERATING_TABLE = REGISTRY.register("shocker_operating_table",
			() -> BlockEntityType.Builder.of(ShockerOperatingTableTileEntity::new, KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.get()).build(null));
	public static final RegistryObject<BlockEntityType<KuugaSarcophagusTileEntity>> KUUGA_SARCOPHAGUS = REGISTRY.register("kuuga_sarcophagus",
			() -> BlockEntityType.Builder.of(KuugaSarcophagusTileEntity::new, KamenRiderInterwovenWorldsModBlocks.KUUGA_SARCOPHAGUS.get()).build(null));

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static RegistryObject<BlockEntityType<?>> register(String registryname, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}
