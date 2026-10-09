package com.qianyunyi.rider.client.renderer;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import com.qianyunyi.rider.entity.model.BeatChaser2000Model;
import com.qianyunyi.rider.entity.layer.BeatChaser2000Layer;
import com.qianyunyi.rider.entity.motoycle.BeatChaser2000Entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

import org.joml.Matrix4f;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class BeatChaser2000Renderer extends GeoEntityRenderer<BeatChaser2000Entity> {
	public BeatChaser2000Renderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new BeatChaser2000Model());
		this.shadowRadius = 0.5f;
		this.addRenderLayer(new BeatChaser2000Layer(this));
	}

	@Override
	public RenderType getRenderType(BeatChaser2000Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public void render(BeatChaser2000Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		renderHeadlight(entity, partialTick, poseStack, bufferSource);
	}

	private void renderHeadlight(BeatChaser2000Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
		if (!entity.isVehicle() || !(entity.getControllingPassenger() instanceof Player)) return;

		float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
		float yawRad = (float) Math.toRadians(yaw);
		float sin = -(float) Math.sin(yawRad);
		float cos = (float) Math.cos(yawRad);

		double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
		double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) + 0.55;
		double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

		poseStack.pushPose();
		poseStack.translate(x, y, z);

		VertexConsumer vc = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f mat = poseStack.last().pose();

		float beamNear = 1.5f;
		float beamFar = 5.0f;
		int strips = 6;

		for (int i = 0; i < strips; i++) {
			float s1 = (float) i / strips * 2.0f - 1.0f;
			float s2 = (float) (i + 1) / strips * 2.0f - 1.0f;

			float nearX1 = sin * beamNear + cos * s1 * 0.4f;
			float nearZ1 = cos * beamNear - sin * s1 * 0.4f;
			float nearX2 = sin * beamNear + cos * s2 * 0.4f;
			float nearZ2 = cos * beamNear - sin * s2 * 0.4f;

			float farX1 = sin * beamFar + cos * s1 * 1.2f;
			float farZ1 = cos * beamFar - sin * s1 * 1.2f;
			float farX2 = sin * beamFar + cos * s2 * 1.2f;
			float farZ2 = cos * beamFar - sin * s2 * 1.2f;

			vc.vertex(mat, nearX1, 0.02f, nearZ1).color(255, 240, 180, 100).endVertex();
			vc.vertex(mat, farX1, 0.02f, farZ1).color(255, 240, 180, 5).endVertex();
			vc.vertex(mat, farX2, 0.02f, farZ2).color(255, 240, 180, 5).endVertex();
			vc.vertex(mat, nearX2, 0.02f, nearZ2).color(255, 240, 180, 100).endVertex();
		}

		poseStack.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(BeatChaser2000Entity animatable) {
		return this.model.getTextureResource(animatable);
	}

	@Override
	public void preRender(PoseStack poseStack, BeatChaser2000Entity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green,
			float blue, float alpha) {
		float scale = 1f;
		this.scaleHeight = scale;
		this.scaleWidth = scale;
		super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

		if (!isReRender) {
			float lean = entity.getLeanAmount();
			if (Math.abs(lean) > 0.1f) {
				float lerpYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
				float yawRad = (float) Math.toRadians(lerpYaw);
				float axisX = -(float) Math.sin(yawRad);
				float axisZ = (float) Math.cos(yawRad);
				float leanRad = (float) Math.toRadians(lean);
				float pivotY = entity.getBbHeight() / 2.0f;

				poseStack.translate(0, pivotY, 0);
				poseStack.mulPose(new Quaternionf(new AxisAngle4f(leanRad, axisX, 0, axisZ)));
				poseStack.translate(0, -pivotY, 0);
			}
		}
	}
}