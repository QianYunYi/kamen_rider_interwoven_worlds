package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingUltimateFormKuugaEffectItem;

public class RisingUltimateFormKuugaEffectModel extends GeoModel<RisingUltimateFormKuugaEffectItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_ultimate_form_kuuga_effect.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_ultimate_form_kuuga_effect.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingUltimateFormKuugaEffectItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_ultimate_form_kuuga_effect.png");
	}
}
