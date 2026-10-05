package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.RisingPegasusFormKuugaItem;

public class RisingPegasusFormKuugaModel extends GeoModel<RisingPegasusFormKuugaItem> {
	@Override
	public ResourceLocation getAnimationResource(RisingPegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/rising_pegasus_form_kuuga.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(RisingPegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/rising_pegasus_form_kuuga.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(RisingPegasusFormKuugaItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/rising_pegasus_form_kuuga.png");
	}
}
