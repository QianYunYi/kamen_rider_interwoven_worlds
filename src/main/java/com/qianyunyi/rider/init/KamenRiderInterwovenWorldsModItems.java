package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;

import com.qianyunyi.rider.item.WhiteScarfItem;
import com.qianyunyi.rider.item.UltraSonarItem;
import com.qianyunyi.rider.item.UltimateTitanSwordItem;
import com.qianyunyi.rider.item.UltimatePegasusCrossbowItem;
import com.qianyunyi.rider.item.UltimateFormKuugaItem;
import com.qianyunyi.rider.item.UltimateFormKuugaEffectItem;
import com.qianyunyi.rider.item.UltimateDragonWandItem;
import com.qianyunyi.rider.item.TyphoonOldModeItem;
import com.qianyunyi.rider.item.TyphoonModifiedPartItem;
import com.qianyunyi.rider.item.TitanSwordItem;
import com.qianyunyi.rider.item.TitanFormKuugaItem;
import com.qianyunyi.rider.item.SuperconductingCoilItem;
import com.qianyunyi.rider.item.SuperRisingUltimateFormKuugaItem;
import com.qianyunyi.rider.item.SuperRisingUltimateFormKuugaEffectItem;
import com.qianyunyi.rider.item.SuperAlloySkeletonItem;
import com.qianyunyi.rider.item.SignalReceiverItem;
import com.qianyunyi.rider.item.ShockerSteelItem;
import com.qianyunyi.rider.item.ShockerSaberItem;
import com.qianyunyi.rider.item.ShockerCombatStaffItem;
import com.qianyunyi.rider.item.ShockerBadgeItem;
import com.qianyunyi.rider.item.RisingUltimateFormKuugaItem;
import com.qianyunyi.rider.item.RisingUltimateFormKuugaEffectItem;
import com.qianyunyi.rider.item.RisingTitanSwordItem;
import com.qianyunyi.rider.item.RisingTitanFormKuugaItem;
import com.qianyunyi.rider.item.RisingPegasusFormKuugaItem;
import com.qianyunyi.rider.item.RisingPegasusCrossbowItem;
import com.qianyunyi.rider.item.RisingMightyFormKuugaItem;
import com.qianyunyi.rider.item.RisingDragonWandItem;
import com.qianyunyi.rider.item.RisingDragonFormKuugaItem;
import com.qianyunyi.rider.item.RiderPowerSystemItem;
import com.qianyunyi.rider.item.RiderGripItem;
import com.qianyunyi.rider.item.RiderFootItem;
import com.qianyunyi.rider.item.RiderArmItem;
import com.qianyunyi.rider.item.RedScarfItem;
import com.qianyunyi.rider.item.ReconstructedHeartItem;
import com.qianyunyi.rider.item.PrototypeEyeItem;
import com.qianyunyi.rider.item.PlasticFibrousPepticItem;
import com.qianyunyi.rider.item.PetrifyArcleItemItem;
import com.qianyunyi.rider.item.PegasusFormKuugaItem;
import com.qianyunyi.rider.item.PegasusCrossbowItem;
import com.qianyunyi.rider.item.OSignalItem;
import com.qianyunyi.rider.item.ModifiedFanItem;
import com.qianyunyi.rider.item.MightyFormKuugaItem;
import com.qianyunyi.rider.item.MicroNuclearReactorItem;
import com.qianyunyi.rider.item.MicroCircuitBoardItem;
import com.qianyunyi.rider.item.MaskRider1OldModifiedDrawingsItem;
import com.qianyunyi.rider.item.MaskRider1OldItem;
import com.qianyunyi.rider.item.MaskRider1OldHelmetModifiedPartItem;
import com.qianyunyi.rider.item.MaskRider1OldChestplateModifiedPartItem;
import com.qianyunyi.rider.item.MaskRider1OldBootsModifiedPartItem;
import com.qianyunyi.rider.item.LocustCellsItem;
import com.qianyunyi.rider.item.JumpShoesItem;
import com.qianyunyi.rider.item.HyperTentacleAntennaItem;
import com.qianyunyi.rider.item.GrowingFormKuugaItemItem;
import com.qianyunyi.rider.item.GovernmentOfDarknessBadgeItem;
import com.qianyunyi.rider.item.GelShockeBadgeItem;
import com.qianyunyi.rider.item.GaranderBadgeItem;
import com.qianyunyi.rider.item.FullBodyRestraintsItem;
import com.qianyunyi.rider.item.EnhancedArtificialMuscleItem;
import com.qianyunyi.rider.item.EnergyTankItem;
import com.qianyunyi.rider.item.EnergyConverterItem;
import com.qianyunyi.rider.item.ElectronicBrainItem;
import com.qianyunyi.rider.item.DynamicMicrophoneItem;
import com.qianyunyi.rider.item.DragonWandItem;
import com.qianyunyi.rider.item.DragonFormKuugaItem;
import com.qianyunyi.rider.item.DestronBadgeItem;
import com.qianyunyi.rider.item.DelzerBadgeItem;
import com.qianyunyi.rider.item.CrusherItem;
import com.qianyunyi.rider.item.ConverterLungItem;
import com.qianyunyi.rider.item.CompoundEyeLensItem;
import com.qianyunyi.rider.item.CarbonFiberTowItem;
import com.qianyunyi.rider.item.CEyeItem;
import com.qianyunyi.rider.item.BiogelItem;
import com.qianyunyi.rider.item.AtomicEnergyFurnaceItem;
import com.qianyunyi.rider.item.BeatChaser2000RightHandlebarItem;
import com.qianyunyi.rider.item.ArtificialMuscleItem;
import com.qianyunyi.rider.item.ArtificialMuscleFiberItem;
import com.qianyunyi.rider.item.ArtificialLungItem;
import com.qianyunyi.rider.item.TattooParchmentPaperItem;
import com.qianyunyi.rider.item.BagundadaItem;
import com.qianyunyi.rider.item.GuzepaItem;
import com.qianyunyi.rider.item.DodozoItem;
import com.qianyunyi.rider.item.ArcleItem;
import com.qianyunyi.rider.item.AmazingMightyFormKuugaItem;
import com.qianyunyi.rider.block.display.ShockerOperatingTableDisplayItem;
import com.qianyunyi.rider.block.display.KuugaSarcophagusDisplayItem;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<Item> SHOCKER_BADGE = REGISTRY.register("shocker_badge", () -> new ShockerBadgeItem());
	public static final RegistryObject<Item> GEL_SHOCKE_BADGE = REGISTRY.register("gel_shocke_badge", () -> new GelShockeBadgeItem());
	public static final RegistryObject<Item> DESTRON_BADGE = REGISTRY.register("destron_badge", () -> new DestronBadgeItem());
	public static final RegistryObject<Item> GOVERNMENT_OF_DARKNESS_BADGE = REGISTRY.register("government_of_darkness_badge", () -> new GovernmentOfDarknessBadgeItem());
	public static final RegistryObject<Item> GARANDER_BADGE = REGISTRY.register("garander_badge", () -> new GaranderBadgeItem());
	public static final RegistryObject<Item> DELZER_BADGE = REGISTRY.register("delzer_badge", () -> new DelzerBadgeItem());
	public static final RegistryObject<Item> PETRIFY_ARCLE_ITEM = REGISTRY.register("petrify_arcle_item", () -> new PetrifyArcleItemItem());
	public static final RegistryObject<ArcleItem> ARCLE_LEGGINGS = REGISTRY.register("arcle_leggings", () -> new ArcleItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<Item> SHOCKER_COMBATANT_SPAWN_EGG = REGISTRY.register("shocker_combatant_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.SHOCKER_COMBATANT, -1, -1, new Item.Properties()));
	public static final RegistryObject<Item> SHOCKER_SABER = REGISTRY.register("shocker_saber", () -> new ShockerSaberItem());
	public static final RegistryObject<Item> SHOCKER_COMBAT_STAFF = REGISTRY.register("shocker_combat_staff", () -> new ShockerCombatStaffItem());
	public static final RegistryObject<Item> SHOCKER_OPERATING_TABLE = REGISTRY.register(KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.getId().getPath(),
			() -> new ShockerOperatingTableDisplayItem(KamenRiderInterwovenWorldsModBlocks.SHOCKER_OPERATING_TABLE.get(), new Item.Properties()));
	public static final RegistryObject<FullBodyRestraintsItem> FULL_BODY_RESTRAINTS_CHESTPLATE = REGISTRY.register("full_body_restraints_chestplate", () -> new FullBodyRestraintsItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<FullBodyRestraintsItem> FULL_BODY_RESTRAINTS_LEGGINGS = REGISTRY.register("full_body_restraints_leggings", () -> new FullBodyRestraintsItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<Item> LOCUST_CELLS = REGISTRY.register("locust_cells", () -> new LocustCellsItem());
	public static final RegistryObject<Item> SHOCKER_STEEL = REGISTRY.register("shocker_steel", () -> new ShockerSteelItem());
	public static final RegistryObject<Item> ATOMIC_ENERGY_FURNACE = REGISTRY.register("atomic_energy_furnace", () -> new AtomicEnergyFurnaceItem());
	public static final RegistryObject<Item> SUPER_ALLOY_SKELETON = REGISTRY.register("super_alloy_skeleton", () -> new SuperAlloySkeletonItem());
	public static final RegistryObject<Item> ELECTRONIC_BRAIN = REGISTRY.register("electronic_brain", () -> new ElectronicBrainItem());
	public static final RegistryObject<Item> CONVERTER_LUNG = REGISTRY.register("converter_lung", () -> new ConverterLungItem());
	public static final RegistryObject<Item> ARTIFICIAL_MUSCLE = REGISTRY.register("artificial_muscle", () -> new ArtificialMuscleItem());
	public static final RegistryObject<Item> HYPER_TENTACLE_ANTENNA = REGISTRY.register("hyper_tentacle_antenna", () -> new HyperTentacleAntennaItem());
	public static final RegistryObject<Item> MODIFIED_FAN = REGISTRY.register("modified_fan", () -> new ModifiedFanItem());
	public static final RegistryObject<Item> COMPOUND_EYE_LENS = REGISTRY.register("compound_eye_lens", () -> new CompoundEyeLensItem());
	public static final RegistryObject<Item> ULTRA_SONAR = REGISTRY.register("ultra_sonar", () -> new UltraSonarItem());
	public static final RegistryObject<Item> RECONSTRUCTED_HEART = REGISTRY.register("reconstructed_heart", () -> new ReconstructedHeartItem());
	public static final RegistryObject<Item> PLASTIC_FIBROUS_PEPTIC = REGISTRY.register("plastic_fibrous_peptic", () -> new PlasticFibrousPepticItem());
	public static final RegistryObject<Item> ENERGY_CONVERTER = REGISTRY.register("energy_converter", () -> new EnergyConverterItem());
	public static final RegistryObject<Item> SUPERCONDUCTING_COIL = REGISTRY.register("superconducting_coil", () -> new SuperconductingCoilItem());
	public static final RegistryObject<Item> MICRO_CIRCUIT_BOARD = REGISTRY.register("micro_circuit_board", () -> new MicroCircuitBoardItem());
	public static final RegistryObject<Item> CARBON_FIBER_TOW = REGISTRY.register("carbon_fiber_tow", () -> new CarbonFiberTowItem());
	public static final RegistryObject<Item> ENHANCED_ARTIFICIAL_MUSCLE = REGISTRY.register("enhanced_artificial_muscle", () -> new EnhancedArtificialMuscleItem());
	public static final RegistryObject<Item> ARTIFICIAL_MUSCLE_FIBER = REGISTRY.register("artificial_muscle_fiber", () -> new ArtificialMuscleFiberItem());
	public static final RegistryObject<Item> BIOGEL = REGISTRY.register("biogel", () -> new BiogelItem());
	public static final RegistryObject<Item> SIGNAL_RECEIVER = REGISTRY.register("signal_receiver", () -> new SignalReceiverItem());
	public static final RegistryObject<Item> MICRO_NUCLEAR_REACTOR = REGISTRY.register("micro_nuclear_reactor", () -> new MicroNuclearReactorItem());
	public static final RegistryObject<Item> ENERGY_TANK = REGISTRY.register("energy_tank", () -> new EnergyTankItem());
	public static final RegistryObject<Item> RIDER_ARM = REGISTRY.register("rider_arm", () -> new RiderArmItem());
	public static final RegistryObject<Item> RIDER_FOOT = REGISTRY.register("rider_foot", () -> new RiderFootItem());
	public static final RegistryObject<Item> JUMP_SHOES = REGISTRY.register("jump_shoes", () -> new JumpShoesItem());
	public static final RegistryObject<Item> RIDER_POWER_SYSTEM = REGISTRY.register("rider_power_system", () -> new RiderPowerSystemItem());
	public static final RegistryObject<Item> O_SIGNAL = REGISTRY.register("o_signal", () -> new OSignalItem());
	public static final RegistryObject<Item> RIDER_GRIP = REGISTRY.register("rider_grip", () -> new RiderGripItem());
	public static final RegistryObject<Item> ARTIFICIAL_LUNG = REGISTRY.register("artificial_lung", () -> new ArtificialLungItem());
	public static final RegistryObject<Item> DYNAMIC_MICROPHONE = REGISTRY.register("dynamic_microphone", () -> new DynamicMicrophoneItem());
	public static final RegistryObject<Item> PROTOTYPE_EYE = REGISTRY.register("prototype_eye", () -> new PrototypeEyeItem());
	public static final RegistryObject<Item> C_EYE = REGISTRY.register("c_eye", () -> new CEyeItem());
	public static final RegistryObject<Item> WHITE_SCARF = REGISTRY.register("white_scarf", () -> new WhiteScarfItem());
	public static final RegistryObject<Item> RED_SCARF = REGISTRY.register("red_scarf", () -> new RedScarfItem());
	public static final RegistryObject<Item> CRUSHER = REGISTRY.register("crusher", () -> new CrusherItem());
	public static final RegistryObject<Item> MASK_RIDER_1_OLD_MODIFIED_DRAWINGS = REGISTRY.register("mask_rider_1_old_modified_drawings", () -> new MaskRider1OldModifiedDrawingsItem());
	public static final RegistryObject<Item> MASK_RIDER_1_OLD_HELMET_MODIFIED_PART = REGISTRY.register("mask_rider_1_old_helmet_modified_part", () -> new MaskRider1OldHelmetModifiedPartItem());
	public static final RegistryObject<Item> MASK_RIDER_1_OLD_CHESTPLATE_MODIFIED_PART = REGISTRY.register("mask_rider_1_old_chestplate_modified_part", () -> new MaskRider1OldChestplateModifiedPartItem());
	public static final RegistryObject<Item> TYPHOON_MODIFIED_PART = REGISTRY.register("typhoon_modified_part", () -> new TyphoonModifiedPartItem());
	public static final RegistryObject<Item> MASK_RIDER_1_OLD_BOOTS_MODIFIED_PART = REGISTRY.register("mask_rider_1_old_boots_modified_part", () -> new MaskRider1OldBootsModifiedPartItem());
	public static final RegistryObject<TyphoonOldModeItem> TYPHOON_OLD_MODE_LEGGINGS = REGISTRY.register("typhoon_old_mode_leggings", () -> new TyphoonOldModeItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<MaskRider1OldItem> MASK_RIDER_1_OLD_HELMET = REGISTRY.register("mask_rider_1_old_helmet", () -> new MaskRider1OldItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<MaskRider1OldItem> MASK_RIDER_1_OLD_CHESTPLATE = REGISTRY.register("mask_rider_1_old_chestplate", () -> new MaskRider1OldItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<MaskRider1OldItem> MASK_RIDER_1_OLD_BOOTS = REGISTRY.register("mask_rider_1_old_boots", () -> new MaskRider1OldItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<GrowingFormKuugaItemItem> GROWING_FORM_KUUGA_ITEM_HELMET = REGISTRY.register("growing_form_kuuga_item_helmet", () -> new GrowingFormKuugaItemItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<GrowingFormKuugaItemItem> GROWING_FORM_KUUGA_ITEM_CHESTPLATE = REGISTRY.register("growing_form_kuuga_item_chestplate", () -> new GrowingFormKuugaItemItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<GrowingFormKuugaItemItem> GROWING_FORM_KUUGA_ITEM_BOOTS = REGISTRY.register("growing_form_kuuga_item_boots", () -> new GrowingFormKuugaItemItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<Item> KUUGA_SARCOPHAGUS = REGISTRY.register(KamenRiderInterwovenWorldsModBlocks.KUUGA_SARCOPHAGUS.getId().getPath(),
			() -> new KuugaSarcophagusDisplayItem(KamenRiderInterwovenWorldsModBlocks.KUUGA_SARCOPHAGUS.get(), new Item.Properties()));
	public static final RegistryObject<MightyFormKuugaItem> MIGHTY_FORM_KUUGA_HELMET = REGISTRY.register("mighty_form_kuuga_helmet", () -> new MightyFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<MightyFormKuugaItem> MIGHTY_FORM_KUUGA_CHESTPLATE = REGISTRY.register("mighty_form_kuuga_chestplate", () -> new MightyFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<MightyFormKuugaItem> MIGHTY_FORM_KUUGA_BOOTS = REGISTRY.register("mighty_form_kuuga_boots", () -> new MightyFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<DragonFormKuugaItem> DRAGON_FORM_KUUGA_HELMET = REGISTRY.register("dragon_form_kuuga_helmet", () -> new DragonFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<DragonFormKuugaItem> DRAGON_FORM_KUUGA_CHESTPLATE = REGISTRY.register("dragon_form_kuuga_chestplate", () -> new DragonFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<DragonFormKuugaItem> DRAGON_FORM_KUUGA_BOOTS = REGISTRY.register("dragon_form_kuuga_boots", () -> new DragonFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<PegasusFormKuugaItem> PEGASUS_FORM_KUUGA_HELMET = REGISTRY.register("pegasus_form_kuuga_helmet", () -> new PegasusFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<PegasusFormKuugaItem> PEGASUS_FORM_KUUGA_CHESTPLATE = REGISTRY.register("pegasus_form_kuuga_chestplate", () -> new PegasusFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<PegasusFormKuugaItem> PEGASUS_FORM_KUUGA_BOOTS = REGISTRY.register("pegasus_form_kuuga_boots", () -> new PegasusFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<TitanFormKuugaItem> TITAN_FORM_KUUGA_HELMET = REGISTRY.register("titan_form_kuuga_helmet", () -> new TitanFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<TitanFormKuugaItem> TITAN_FORM_KUUGA_CHESTPLATE = REGISTRY.register("titan_form_kuuga_chestplate", () -> new TitanFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<TitanFormKuugaItem> TITAN_FORM_KUUGA_BOOTS = REGISTRY.register("titan_form_kuuga_boots", () -> new TitanFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<RisingMightyFormKuugaItem> RISING_MIGHTY_FORM_KUUGA_HELMET = REGISTRY.register("rising_mighty_form_kuuga_helmet", () -> new RisingMightyFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<RisingMightyFormKuugaItem> RISING_MIGHTY_FORM_KUUGA_CHESTPLATE = REGISTRY.register("rising_mighty_form_kuuga_chestplate", () -> new RisingMightyFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<RisingMightyFormKuugaItem> RISING_MIGHTY_FORM_KUUGA_BOOTS = REGISTRY.register("rising_mighty_form_kuuga_boots", () -> new RisingMightyFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<RisingDragonFormKuugaItem> RISING_DRAGON_FORM_KUUGA_HELMET = REGISTRY.register("rising_dragon_form_kuuga_helmet", () -> new RisingDragonFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<RisingDragonFormKuugaItem> RISING_DRAGON_FORM_KUUGA_CHESTPLATE = REGISTRY.register("rising_dragon_form_kuuga_chestplate", () -> new RisingDragonFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<RisingDragonFormKuugaItem> RISING_DRAGON_FORM_KUUGA_BOOTS = REGISTRY.register("rising_dragon_form_kuuga_boots", () -> new RisingDragonFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<RisingPegasusFormKuugaItem> RISING_PEGASUS_FORM_KUUGA_HELMET = REGISTRY.register("rising_pegasus_form_kuuga_helmet", () -> new RisingPegasusFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<RisingPegasusFormKuugaItem> RISING_PEGASUS_FORM_KUUGA_CHESTPLATE = REGISTRY.register("rising_pegasus_form_kuuga_chestplate",
			() -> new RisingPegasusFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<RisingPegasusFormKuugaItem> RISING_PEGASUS_FORM_KUUGA_BOOTS = REGISTRY.register("rising_pegasus_form_kuuga_boots", () -> new RisingPegasusFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<RisingTitanFormKuugaItem> RISING_TITAN_FORM_KUUGA_HELMET = REGISTRY.register("rising_titan_form_kuuga_helmet", () -> new RisingTitanFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<RisingTitanFormKuugaItem> RISING_TITAN_FORM_KUUGA_CHESTPLATE = REGISTRY.register("rising_titan_form_kuuga_chestplate", () -> new RisingTitanFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<RisingTitanFormKuugaItem> RISING_TITAN_FORM_KUUGA_BOOTS = REGISTRY.register("rising_titan_form_kuuga_boots", () -> new RisingTitanFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<AmazingMightyFormKuugaItem> AMAZING_MIGHTY_FORM_KUUGA_HELMET = REGISTRY.register("amazing_mighty_form_kuuga_helmet", () -> new AmazingMightyFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<AmazingMightyFormKuugaItem> AMAZING_MIGHTY_FORM_KUUGA_CHESTPLATE = REGISTRY.register("amazing_mighty_form_kuuga_chestplate",
			() -> new AmazingMightyFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<AmazingMightyFormKuugaItem> AMAZING_MIGHTY_FORM_KUUGA_BOOTS = REGISTRY.register("amazing_mighty_form_kuuga_boots", () -> new AmazingMightyFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<UltimateFormKuugaEffectItem> ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS = REGISTRY.register("ultimate_form_kuuga_effect_leggings", () -> new UltimateFormKuugaEffectItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<UltimateFormKuugaItem> ULTIMATE_FORM_KUUGA_HELMET = REGISTRY.register("ultimate_form_kuuga_helmet", () -> new UltimateFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<UltimateFormKuugaItem> ULTIMATE_FORM_KUUGA_CHESTPLATE = REGISTRY.register("ultimate_form_kuuga_chestplate", () -> new UltimateFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<UltimateFormKuugaItem> ULTIMATE_FORM_KUUGA_BOOTS = REGISTRY.register("ultimate_form_kuuga_boots", () -> new UltimateFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<RisingUltimateFormKuugaEffectItem> RISING_ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS = REGISTRY.register("rising_ultimate_form_kuuga_effect_leggings",
			() -> new RisingUltimateFormKuugaEffectItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<RisingUltimateFormKuugaItem> RISING_ULTIMATE_FORM_KUUGA_HELMET = REGISTRY.register("rising_ultimate_form_kuuga_helmet", () -> new RisingUltimateFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<RisingUltimateFormKuugaItem> RISING_ULTIMATE_FORM_KUUGA_CHESTPLATE = REGISTRY.register("rising_ultimate_form_kuuga_chestplate",
			() -> new RisingUltimateFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<RisingUltimateFormKuugaItem> RISING_ULTIMATE_FORM_KUUGA_BOOTS = REGISTRY.register("rising_ultimate_form_kuuga_boots", () -> new RisingUltimateFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<SuperRisingUltimateFormKuugaEffectItem> SUPER_RISING_ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS = REGISTRY.register("super_rising_ultimate_form_kuuga_effect_leggings",
			() -> new SuperRisingUltimateFormKuugaEffectItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<SuperRisingUltimateFormKuugaItem> SUPER_RISING_ULTIMATE_FORM_KUUGA_HELMET = REGISTRY.register("super_rising_ultimate_form_kuuga_helmet",
			() -> new SuperRisingUltimateFormKuugaItem(ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<SuperRisingUltimateFormKuugaItem> SUPER_RISING_ULTIMATE_FORM_KUUGA_CHESTPLATE = REGISTRY.register("super_rising_ultimate_form_kuuga_chestplate",
			() -> new SuperRisingUltimateFormKuugaItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<SuperRisingUltimateFormKuugaItem> SUPER_RISING_ULTIMATE_FORM_KUUGA_BOOTS = REGISTRY.register("super_rising_ultimate_form_kuuga_boots",
			() -> new SuperRisingUltimateFormKuugaItem(ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<Item> DRAGON_WAND = REGISTRY.register("dragon_wand", () -> new DragonWandItem());
	public static final RegistryObject<Item> PEGASUS_CROSSBOW = REGISTRY.register("pegasus_crossbow", () -> new PegasusCrossbowItem());
	public static final RegistryObject<Item> TITAN_SWORD = REGISTRY.register("titan_sword", () -> new TitanSwordItem());
	public static final RegistryObject<Item> RISING_DRAGON_WAND = REGISTRY.register("rising_dragon_wand", () -> new RisingDragonWandItem());
	public static final RegistryObject<Item> RISING_PEGASUS_CROSSBOW = REGISTRY.register("rising_pegasus_crossbow", () -> new RisingPegasusCrossbowItem());
	public static final RegistryObject<Item> RISING_TITAN_SWORD = REGISTRY.register("rising_titan_sword", () -> new RisingTitanSwordItem());
	public static final RegistryObject<Item> ULTIMATE_DRAGON_WAND = REGISTRY.register("ultimate_dragon_wand", () -> new UltimateDragonWandItem());
	public static final RegistryObject<Item> ULTIMATE_PEGASUS_CROSSBOW = REGISTRY.register("ultimate_pegasus_crossbow", () -> new UltimatePegasusCrossbowItem());
	public static final RegistryObject<Item> ULTIMATE_TITAN_SWORD = REGISTRY.register("ultimate_titan_sword", () -> new UltimateTitanSwordItem());
	public static final RegistryObject<Item> N_DAGUBA_ZEBA_SPAWN_EGG = REGISTRY.register("n_daguba_zeba_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.N_DAGUBA_ZEBA, 0xFFFFFF, 0xFFD700, new Item.Properties()));
	public static final RegistryObject<Item> N_GAMIO_ZEDA_SPAWN_EGG = REGISTRY.register("n_gamio_zeda_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.N_GAMIO_ZEDA, 0xCC0000, 0xFFD700, new Item.Properties()));
	public static final RegistryObject<Item> GOURAM_SPAWN_EGG = REGISTRY.register("gouram_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.GOURAM, -13312, -13434829, new Item.Properties()));
	public static final RegistryObject<Item> SHOCKER_SCIENTIST_SPAWN_EGG = REGISTRY.register("shocker_scientist_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.SHOCKER_SCIENTIST, -3355444, -16777216, new Item.Properties()));
	public static final RegistryObject<Item> SHOCKER_MILITARY_COMBATANT_SPAWN_EGG = REGISTRY.register("shocker_military_combatant_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.SHOCKER_MILITARY_COMBATANT, -16751053, -1, new Item.Properties()));
	public static final RegistryObject<Item> SHOCKER_MILITARY_CADRE_SPAWN_EGG = REGISTRY.register("shocker_military_cadre_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.SHOCKER_MILITARY_CADRE, -3355444, -10066432, new Item.Properties()));
	public static final RegistryObject<Item> COLONEL_SOL_SPAWN_EGG = REGISTRY.register("colonel_sol_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.COLONEL_SOL, -1, -16737895, new Item.Properties()));
	public static final RegistryObject<Item> BEAT_CHASER_2000_SPAWN_EGG = REGISTRY.register("beat_chaser_2000_spawn_egg", () -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.BEAT_CHASER_2000, -1, -1, new Item.Properties()));
	public static final RegistryObject<Item> BEAT_CHASER_2000_RIGHT_HANDLEBAR = REGISTRY.register("beat_chaser_2000_right_handlebar", () -> new BeatChaser2000RightHandlebarItem());
	public static final RegistryObject<Item> ZU_GOOMA_GU_SPAWN_EGG = REGISTRY.register("zu_gooma_gu_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU, 0x4E3524, 0x3E2723, new Item.Properties()));
	public static final RegistryObject<Item> ZU_GUMUN_BA_SPAWN_EGG = REGISTRY.register("zu_gumun_ba_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_GUMUN_BA, 0x556B2F, 0x9ACD32, new Item.Properties()));
	public static final RegistryObject<Item> ZU_MEBIO_DA_SPAWN_EGG = REGISTRY.register("zu_mebio_da_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_MEBIO_DA, 0x006400, 0x8B0000, new Item.Properties()));
	public static final RegistryObject<Item> ZU_BADUU_BA_SPAWN_EGG = REGISTRY.register("zu_baduu_ba_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_BADUU_BA, 0xBC8F4B, 0x8B7355, new Item.Properties()));
	public static final RegistryObject<Item> GO_BADAA_BA_SPAWN_EGG = REGISTRY.register("go_badaa_ba_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.GO_BADAA_BA, 0x006400, 0xB8860B, new Item.Properties()));
	public static final RegistryObject<Item> GO_GADORU_BA_SPAWN_EGG = REGISTRY.register("go_gadoru_ba_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.GO_GADORU_BA, 0x9ACD32, 0xDC143C, new Item.Properties()));
	public static final RegistryObject<Item> ZU_GOMA_GU_ENHANCED_SPAWN_EGG = REGISTRY.register("zu_goma_gu_enhanced_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_GOMA_GU_ENHANCED, 0x3E2723, 0x5D4037, new Item.Properties()));
	public static final RegistryObject<Item> ZU_GOMA_GU_ULTIMATE_SPAWN_EGG = REGISTRY.register("zu_goma_gu_ultimate_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_GOMA_GU_ULTIMATE, 0x1A1A1A, 0xE8E8E8, new Item.Properties()));
	public static final RegistryObject<Item> GARUME_RE_SPAWN_EGG = REGISTRY.register("garume_re_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.GARUME_RE, 0x4CAF50, 0xFFEB3B, new Item.Properties()));
	public static final RegistryObject<Item> ZU_ZAIN_DA_SPAWN_EGG = REGISTRY.register("zu_zain_da_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.ZU_ZAIN_DA, 0x9E9E9E, 0x757575, new Item.Properties()));
	public static final RegistryObject<Item> BE_JIMIN_BA_SPAWN_EGG = REGISTRY.register("be_jimin_ba_spawn_egg",
			() -> new ForgeSpawnEggItem(KamenRiderInterwovenWorldsModEntities.BE_JIMIN_BA, 0xA8E4A0, 0xC8F7C4, new Item.Properties()));
	public static final RegistryObject<Item> TATTOO_PARCHMENT_PAPER = REGISTRY.register("tattoo_parchment_paper", () -> new TattooParchmentPaperItem());
	public static final RegistryObject<Item> BAGUNDADA = REGISTRY.register("bagundada", () -> new BagundadaItem());
	public static final RegistryObject<Item> GUZEPA = REGISTRY.register("guzepa", () -> new GuzepaItem());
	public static final RegistryObject<Item> DODOZO = REGISTRY.register("dodozo", () -> new DodozoItem());
}