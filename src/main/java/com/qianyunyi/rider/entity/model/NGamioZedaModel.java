package com.qianyunyi.rider.entity.model;

import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

public class NGamioZedaModel extends GeoModel<NGamioZedaEntity> {

	@Override
	public ResourceLocation getAnimationResource(NGamioZedaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/n_gamio_zeda.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(NGamioZedaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/n_gamio_zeda.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(NGamioZedaEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gurongi/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(NGamioZedaEntity entity, long uniqueID, software.bernie.geckolib.core.animation.AnimationState<NGamioZedaEntity> customPredicate) {
		super.setCustomAnimations(entity, uniqueID, customPredicate);

		// 控制tail骨骼组可见性
		CoreGeoBone tail = this.getAnimationProcessor().getBone("tail");
		if (tail != null) {
			tail.setHidden(!entity.isTailVisible());
		}

		// 控制green_lightning骨骼组可见性
		CoreGeoBone greenLightning = this.getAnimationProcessor().getBone("green_lightning");
		if (greenLightning != null) {
			greenLightning.setHidden(!entity.isGreenLightningVisible());
		}

		// 控制green_lightning子骨骼
		for (int i = 1; i <= 3; i++) {
			CoreGeoBone glChild = this.getAnimationProcessor().getBone("green_lightning_" + i);
			if (glChild != null) {
				glChild.setHidden(!entity.isGreenLightningVisible());
			}
		}
	}
}