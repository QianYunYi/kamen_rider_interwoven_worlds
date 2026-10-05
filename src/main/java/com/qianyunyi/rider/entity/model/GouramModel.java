package com.qianyunyi.rider.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.entity.GouramEntity;

public class GouramModel extends GeoModel<GouramEntity> {
	@Override
	public ResourceLocation getAnimationResource(GouramEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/gouram.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GouramEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/gouram.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GouramEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/entities/" + entity.getTexture() + ".png");
	}

}
