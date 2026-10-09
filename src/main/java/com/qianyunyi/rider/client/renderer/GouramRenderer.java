
package com.qianyunyi.rider.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

import com.qianyunyi.rider.entity.model.GouramModel;
import com.qianyunyi.rider.entity.layer.GouramLayer;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramEntity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class GouramRenderer extends GeoEntityRenderer<GouramEntity> {
	public GouramRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new GouramModel());
		this.shadowRadius = 0.5f;
		this.addRenderLayer(new GouramLayer(this));
	}

	@Override
	public RenderType getRenderType(GouramEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public void preRender(PoseStack poseStack, GouramEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 1f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		if (!isReRender) {
			float lerpPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
			float lerpYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
			float pivotY = (float) entity.getPassengersRidingOffset();

			if (Math.abs(lerpPitch) > 0.1f) {
				float yawRad = (float) Math.toRadians(lerpYaw);
				float pitchAxisX = Mth.cos(yawRad);
				float pitchAxisZ = Mth.sin(yawRad);
				float pitchRad = (float) Math.toRadians(-lerpPitch);
				poseStack.translate(0, pivotY, 0);
				poseStack.mulPose(new Quaternionf(new AxisAngle4f(pitchRad, pitchAxisX, 0, pitchAxisZ)));
				poseStack.translate(0, -pivotY, 0);
			}

			float lean = entity.getLeanAmount();
			if (Math.abs(lean) > 0.1f) {
				float yawRad = (float) Math.toRadians(lerpYaw);
				float axisX = -(float) Math.sin(yawRad);
				float axisZ = (float) Math.cos(yawRad);
				float leanRad = (float) Math.toRadians(lean);

				poseStack.translate(0, pivotY, 0);
				poseStack.mulPose(new Quaternionf(new AxisAngle4f(leanRad, axisX, 0, axisZ)));
				poseStack.translate(0, -pivotY, 0);
			}
		}
	}
}