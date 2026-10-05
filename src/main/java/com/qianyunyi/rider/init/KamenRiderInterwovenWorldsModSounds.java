package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

public class KamenRiderInterwovenWorldsModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<SoundEvent> STANDBY_ARCLE = REGISTRY.register("standby_arcle", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "standby_arcle")));
	public static final RegistryObject<SoundEvent> SHOCKER_CRY = REGISTRY.register("shocker_cry", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "shocker_cry")));
	public static final RegistryObject<SoundEvent> PEGASUS_BOWGUN_SHOOT = REGISTRY.register("pegasus_bowgun_shoot", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "pegasus_bowgun_shoot")));
	public static final RegistryObject<SoundEvent> PEGASUS_BOWGUN_LOAD = REGISTRY.register("pegasus_bowgun_load", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "pegasus_bowgun_load")));
	public static final RegistryObject<SoundEvent> PEGASUS_BOWGUN_CHARGE = REGISTRY.register("pegasus_bowgun_charge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "pegasus_bowgun_charge")));
	public static final RegistryObject<SoundEvent> KUUGA_TITAN_FORM = REGISTRY.register("kuuga_titan_form", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_titan_form")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_ULTIMATE_DARK_EYES = REGISTRY.register("kuuga_rising_ultimate_dark_eyes",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "kuuga_rising_ultimate_dark_eyes")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_ULTIMATE = REGISTRY.register("kuuga_rising_ultimate", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_rising_ultimate")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_TITAN = REGISTRY.register("kuuga_rising_titan", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_rising_titan")));
	public static final RegistryObject<SoundEvent> KUUGA_PEGASUS_FORM = REGISTRY.register("kuuga_pegasus_form", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_pegasus_form")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_MIGHTY = REGISTRY.register("kuuga_rising_mighty", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_rising_mighty")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_DRAGON = REGISTRY.register("kuuga_rising_dragon", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_rising_dragon")));
	public static final RegistryObject<SoundEvent> KUUGA_MIGHTY_FORM = REGISTRY.register("kuuga_mighty_form", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_mighty_form")));
	public static final RegistryObject<SoundEvent> KUUGA_HENSHIN_KAIJO = REGISTRY.register("kuuga_henshin_kaijo", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_henshin_kaijo")));
	public static final RegistryObject<SoundEvent> KUUGA_DRAGON_FORM = REGISTRY.register("kuuga_dragon_form", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_dragon_form")));
	public static final RegistryObject<SoundEvent> KUUGA_AMAZING_MIGHTY = REGISTRY.register("kuuga_amazing_mighty", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_amazing_mighty")));
	public static final RegistryObject<SoundEvent> KILLED_BY_DAGUBA = REGISTRY.register("killed_by_daguba", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "killed_by_daguba")));
	public static final RegistryObject<SoundEvent> THE_JOKER_OF_THE_DEVIL = REGISTRY.register("the_joker_of_the_devil", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "the_joker_of_the_devil")));
	public static final RegistryObject<SoundEvent> SSS_SHOCK_SHOCKER_SHOC = REGISTRY.register("sss_shock_shocker_shoc", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "sss_shock_shocker_shoc")));
	public static final RegistryObject<SoundEvent> EARTHQUAKE = REGISTRY.register("earthquake", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "earthquake")));
	public static final RegistryObject<SoundEvent> DAGUBA_LAUGHTER = REGISTRY.register("daguba_laughter", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "daguba_laughter")));
	public static final RegistryObject<SoundEvent> DAGUBA_ARRIVAL = REGISTRY.register("daguba_arrival", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "daguba_arrival")));
	public static final RegistryObject<SoundEvent> CURRENT = REGISTRY.register("current", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "current")));
	public static final RegistryObject<SoundEvent> CRACKED_STONE = REGISTRY.register("cracked_stone", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "cracked_stone")));
	public static final RegistryObject<SoundEvent> CHANGE_ULTIMATE = REGISTRY.register("change_ultimate", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "change_ultimate")));
	public static final RegistryObject<SoundEvent> KUUGA_RISING_PEGASUS = REGISTRY.register("kuuga_rising_pegasus", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_rising_pegasus")));
	public static final RegistryObject<SoundEvent> FRONT_PART_PEGASUS_ABILITY = REGISTRY.register("front_part_pegasus_ability",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "front_part_pegasus_ability")));
	public static final RegistryObject<SoundEvent> GOURAM_FLY = REGISTRY.register("gouram_fly", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "gouram_fly")));
	public static final RegistryObject<SoundEvent> KUUGA_UNFORM = REGISTRY.register("kuuga_unform", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_unform")));
	public static final RegistryObject<SoundEvent> KUUGA_UNFORM_RISING = REGISTRY.register("kuuga_unform_rising", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_unform_rising")));
	public static final RegistryObject<SoundEvent> PUNCH = REGISTRY.register("punch", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "punch")));
	public static final RegistryObject<SoundEvent> PUNCH_HENSHIN_1 = REGISTRY.register("punch_henshin_1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "punch_henshin_1")));
	public static final RegistryObject<SoundEvent> PUNCH_HENSHIN_2 = REGISTRY.register("punch_henshin_2", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "punch_henshin_2")));
	public static final RegistryObject<SoundEvent> JUMP_DRAGON = REGISTRY.register("jump_dragon", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "jump_dragon")));
	public static final RegistryObject<SoundEvent> GROWING_HENSHIN = REGISTRY.register("growing_henshin", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "growing_henshin")));
	public static final RegistryObject<SoundEvent> FINAL_EXPLODE = REGISTRY.register("final_explode", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "final_explode")));
	public static final RegistryObject<SoundEvent> IMORTAL_GRONGI_GROWING_FORM = REGISTRY.register("imortal_grongi_growing_form",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "imortal_grongi_growing_form")));
	public static final RegistryObject<SoundEvent> NORMAL_EXPLOSION = REGISTRY.register("normal_explosion", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "normal_explosion")));
	public static final RegistryObject<SoundEvent> TITAN_ABILITY = REGISTRY.register("titan_ability", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "titan_ability")));
	public static final RegistryObject<SoundEvent> KUUGA_AFTER_HISATSU_EFFECT = REGISTRY.register("kuuga_after_hisatsu_effect",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "kuuga_after_hisatsu_effect")));
	public static final RegistryObject<SoundEvent> PEGASUS_HISATSU = REGISTRY.register("pegasus_hisatsu",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "pegasus_hisatsu")));
	public static final RegistryObject<SoundEvent> RIDER_KICK_START = REGISTRY.register("rider_kick_start",
			() -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "rider_kick_start")));
	public static final RegistryObject<SoundEvent> OPEN_TITAN_SWORD = REGISTRY.register("open_titan_sword", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "open_titan_sword")));
	public static final RegistryObject<SoundEvent> RISING_CALAMITI_STEP = REGISTRY.register("rising_calamiti_step", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "rising_calamiti_step")));
	public static final RegistryObject<SoundEvent> TITAN_CALAMITI = REGISTRY.register("titan_calamiti", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "titan_calamiti")));
	public static final RegistryObject<SoundEvent> TITAN_FOYLA = REGISTRY.register("titan_foyla", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "titan_foyla")));
	public static final RegistryObject<SoundEvent> ABILITY_CHANGE_OR_LIGHT_ATTAK = REGISTRY.register("ability_change_or_light_attak", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "ability_change_or_light_attak")));
	public static final RegistryObject<SoundEvent> TWIN_TITAN_RISING_SWORD = REGISTRY.register("twin_titan_rising_sword", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "twin_titan_rising_sword")));
	public static final RegistryObject<SoundEvent> DRAGON_ROD_FOYLA = REGISTRY.register("dragon_rod_foyla", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "dragon_rod_foyla")));
	public static final RegistryObject<SoundEvent> BOWGUN_ACTIFING = REGISTRY.register("bowgun_actifing", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "bowgun_actifing")));
	public static final RegistryObject<SoundEvent> DRAGON_ROD_OPEN = REGISTRY.register("dragon_rod_open", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "dragon_rod_open")));
	public static final RegistryObject<SoundEvent> ROD_DRAGON_STRIKE = REGISTRY.register("rod_dragon_strike", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "rod_dragon_strike")));
	public static final RegistryObject<SoundEvent> TITAN_SWORD_STANDARD_ATTACK = REGISTRY.register("titan_sword_standard_attack", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("kamen_rider_interwoven_worlds", "titan_sword_standard_attack")));
}