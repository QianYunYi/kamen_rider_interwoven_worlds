
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<CreativeModeTab> SHOWA_ITEMS = REGISTRY.register("showa_items", () -> CreativeModeTab.builder().title(Component.translatable("item_group.kamen_rider_interwoven_worlds.showa_items"))
			.icon(() -> new ItemStack(KamenRiderInterwovenWorldsModItems.SHOCKER_BADGE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GEL_SHOCKE_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.DESTRON_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GOVERNMENT_OF_DARKNESS_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GARANDER_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.DELZER_BADGE.get());
				tabData.accept(KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.get().asItem());
				tabData.accept(KamenRiderInterwovenWorldsModItems.LOCUST_CELLS.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_STEEL.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ATOMIC_ENERGY_FURNACE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SUPER_ALLOY_SKELETON.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ELECTRONIC_BRAIN.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.CONVERTER_LUNG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ARTIFICIAL_MUSCLE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.HYPER_TENTACLE_ANTENNA.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MODIFIED_FAN.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.COMPOUND_EYE_LENS.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ULTRA_SONAR.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RECONSTRUCTED_HEART.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.PLASTIC_FIBROUS_PEPTIC.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ENERGY_CONVERTER.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SUPERCONDUCTING_COIL.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MICRO_CIRCUIT_BOARD.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.CARBON_FIBER_TOW.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ENHANCED_ARTIFICIAL_MUSCLE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ARTIFICIAL_MUSCLE_FIBER.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.BIOGEL.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SIGNAL_RECEIVER.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MICRO_NUCLEAR_REACTOR.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ENERGY_TANK.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RIDER_ARM.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RIDER_FOOT.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.JUMP_SHOES.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RIDER_POWER_SYSTEM.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.O_SIGNAL.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RIDER_GRIP.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ARTIFICIAL_LUNG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.DYNAMIC_MICROPHONE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.PROTOTYPE_EYE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.C_EYE.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.WHITE_SCARF.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.RED_SCARF.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.CRUSHER.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_MODIFIED_DRAWINGS.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_HELMET_MODIFIED_PART.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_CHESTPLATE_MODIFIED_PART.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.TYPHOON_MODIFIED_PART.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_BOOTS_MODIFIED_PART.get());
			}).withSearchBar().build());
	public static final RegistryObject<CreativeModeTab> HEISEI_ITEMS = REGISTRY.register("heisei_items", () -> CreativeModeTab.builder().title(Component.translatable("item_group.kamen_rider_interwoven_worlds.heisei_items"))
			.icon(() -> new ItemStack(KamenRiderInterwovenWorldsModItems.PETRIFY_ARCLE_ITEM.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KamenRiderInterwovenWorldsModItems.PETRIFY_ARCLE_ITEM.get());
				tabData.accept(KamenRiderInterwovenWorldsModBlocks.KUUGA_SARCOPHAGUS.get().asItem());
				tabData.accept(KamenRiderInterwovenWorldsModItems.TATTOO_PARCHMENT_PAPER.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.BAGUNDADA.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GUZEPA.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.DODOZO.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.BEAT_CHASER_2000_RIGHT_HANDLEBAR.get());
			}).withTabsBefore(SHOWA_ITEMS.getId()).build());
	public static final RegistryObject<CreativeModeTab> SHOWA_ENTITY = REGISTRY.register("showa_entity", () -> CreativeModeTab.builder().title(Component.translatable("item_group.kamen_rider_interwoven_worlds.showa_entity"))
			.icon(() -> new ItemStack(KamenRiderInterwovenWorldsModItems.SHOCKER_BADGE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_COMBATANT_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_SCIENTIST_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_MILITARY_COMBATANT_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.SHOCKER_MILITARY_CADRE_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.COLONEL_SOL_SPAWN_EGG.get());
			}).withTabsBefore(HEISEI_ITEMS.getId()).build());
	public static final RegistryObject<CreativeModeTab> SHOWA_BLOCKS = REGISTRY.register("showa_blocks", () -> CreativeModeTab.builder().title(Component.translatable("item_group.kamen_rider_interwoven_worlds.showa_blocks"))
			.icon(() -> new ItemStack(KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.get().asItem());
			}).withTabsBefore(SHOWA_ENTITY.getId()).build());
	public static final RegistryObject<CreativeModeTab> HEISEI_ENTITY = REGISTRY.register("heisei_entity", () -> CreativeModeTab.builder().title(Component.translatable("item_group.kamen_rider_interwoven_worlds.heisei_entity"))
			.icon(() -> new ItemStack(KamenRiderInterwovenWorldsModItems.N_DAGUBA_ZEBA_SPAWN_EGG.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KamenRiderInterwovenWorldsModItems.GOURAM_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.BE_JIMIN_BA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_GUMUN_BA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_GOOMA_GU_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_MEBIO_DA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_BADUU_BA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_GOMA_GU_ENHANCED_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_GOMA_GU_ULTIMATE_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GARUME_RE_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.ZU_ZAIN_DA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GO_BADAA_BA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.GO_GADORU_BA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.N_DAGUBA_ZEBA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.N_GAMIO_ZEDA_SPAWN_EGG.get());
				tabData.accept(KamenRiderInterwovenWorldsModItems.BEAT_CHASER_2000_SPAWN_EGG.get());
			}).withTabsBefore(SHOWA_BLOCKS.getId()).build());
}