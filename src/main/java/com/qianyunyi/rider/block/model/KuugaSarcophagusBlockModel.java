package com.qianyunyi.rider.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.block.entity.KuugaSarcophagusTileEntity;

public class KuugaSarcophagusBlockModel extends GeoModel<KuugaSarcophagusTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(KuugaSarcophagusTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "animations/kuuga_sarcophagus.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(KuugaSarcophagusTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "geo/kuuga_sarcophagus.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(KuugaSarcophagusTileEntity animatable) {
		return new ResourceLocation("kamen_rider_interwoven_worlds", "textures/block/kuuga_sarcophagus.png");
	}
}
