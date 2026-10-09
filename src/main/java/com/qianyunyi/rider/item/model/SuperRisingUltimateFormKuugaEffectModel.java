package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.SuperRisingUltimateFormKuugaEffectItem;

public class SuperRisingUltimateFormKuugaEffectModel extends GeoModel<SuperRisingUltimateFormKuugaEffectItem> {
	@Override
	public ResourceLocation getAnimationResource(SuperRisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/super_rising_ultimate_form_kuuga_effect.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(SuperRisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/super_rising_ultimate_form_kuuga_effect.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SuperRisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/armor/super_rising_ultimate_form_kuuga_effect.png");
	}
}
