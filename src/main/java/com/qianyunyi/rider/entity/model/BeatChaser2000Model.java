package com.qianyunyi.rider.entity.model;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import net.minecraft.resources.ResourceLocation;

import com.qianyunyi.rider.entity.BeatChaser2000Entity;

public class BeatChaser2000Model extends GeoModel<BeatChaser2000Entity> {
	@Override
	public ResourceLocation getAnimationResource(BeatChaser2000Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "animations/beat_chaser_2000.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BeatChaser2000Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/beat_chaser_2000.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BeatChaser2000Entity entity) {
		return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(BeatChaser2000Entity animatable, long instanceId, AnimationState<BeatChaser2000Entity> animationState) {
		GeoBone helmet = (GeoBone) getAnimationProcessor().getBone("motorcycleHelmet");
		if (helmet != null) {
			float turnInput = animatable.getLastTurnInput();
			float targetRotY = -turnInput * 0.4f;
			helmet.setRotY(helmet.getRotY() + (targetRotY - helmet.getRotY()) * 0.3f);
		}
		GeoBone helmetBone = (GeoBone) getAnimationProcessor().getBone("helmet");
		if (helmetBone != null) {
			float turnInput = animatable.getLastTurnInput();
			float targetRotY = -turnInput * 0.4f;
			helmetBone.setRotY(helmetBone.getRotY() + (targetRotY - helmetBone.getRotY()) * 0.3f);
		}
		GeoBone gouram = (GeoBone) getAnimationProcessor().getBone("gouram");
		if (gouram != null) {
			gouram.setHidden(animatable.shouldHideGouram());
		}
		GeoBone rightHandlebar = (GeoBone) getAnimationProcessor().getBone("rightHandlebar");
		if (rightHandlebar != null) {
			rightHandlebar.setHidden(animatable.isRightHandlebarRemoved());
		}
		float wheelRot = animatable.getWheelRotation();
		GeoBone frontWheel = (GeoBone) getAnimationProcessor().getBone("frontWheel");
		if (frontWheel != null) {
			frontWheel.setRotX(wheelRot);
		}
		GeoBone rearWheel = (GeoBone) getAnimationProcessor().getBone("rearWheel");
		if (rearWheel != null) {
			rearWheel.setRotX(wheelRot);
		}
	}
}