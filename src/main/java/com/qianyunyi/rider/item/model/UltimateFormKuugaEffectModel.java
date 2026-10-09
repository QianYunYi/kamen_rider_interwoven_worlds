package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.UltimateFormKuugaEffectItem;

public class UltimateFormKuugaEffectModel extends GeoModel<UltimateFormKuugaEffectItem> {
	@Override
	public ResourceLocation getAnimationResource(UltimateFormKuugaEffectItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/ultimate_form_kuuga_henshin_effect.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(UltimateFormKuugaEffectItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/ultimate_form_kuuga_henshin_effect.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(UltimateFormKuugaEffectItem object) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/armor/ultimate_form_kuuga_henshin_effect.png");
	}
}
