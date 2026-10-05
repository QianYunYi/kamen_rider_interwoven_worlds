package com.qianyunyi.rider.item.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.item.FullBodyRestraintsItem;

public class FullBodyRestraintsModel extends GeoModel<FullBodyRestraintsItem> {
	@Override
	public ResourceLocation getAnimationResource(FullBodyRestraintsItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/full_body_restraints.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(FullBodyRestraintsItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/full_body_restraints.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(FullBodyRestraintsItem object) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/item/full_body_restraints.png");
	}
}
