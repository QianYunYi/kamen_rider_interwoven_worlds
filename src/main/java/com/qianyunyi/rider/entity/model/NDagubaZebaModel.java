package com.qianyunyi.rider.entity.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;

public class NDagubaZebaModel extends GeoModel<NDagubaZebaEntity> {
	@Override
	public ResourceLocation getAnimationResource(NDagubaZebaEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/n_daguba_zeba.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(NDagubaZebaEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/n_daguba_zeba.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(NDagubaZebaEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + entity.getTexture() + ".png");
	}

}
