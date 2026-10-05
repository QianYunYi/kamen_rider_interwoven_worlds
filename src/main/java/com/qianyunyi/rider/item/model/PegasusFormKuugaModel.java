package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.PegasusFormKuugaItem;

public class PegasusFormKuugaModel extends GeoModel<PegasusFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(PegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/pegasus_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(PegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/pegasus_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(PegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/pegasus_form_kuuga.png");
	}
}
