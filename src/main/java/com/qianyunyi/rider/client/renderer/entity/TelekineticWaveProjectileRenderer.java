package com.qianyunyi.rider.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianyunyi.rider.entity.projectile.TelekineticWaveProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 念力波弹射物渲染器
 * 纯渲染器效果，无贴图，渲染金色能量球体带闪电环绕效果
 */
@OnlyIn(Dist.CLIENT)
public class TelekineticWaveProjectileRenderer extends EntityRenderer<TelekineticWaveProjectile> {

	public TelekineticWaveProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0f; // 无阴影
	}

	@Override
	public void render(@NotNull TelekineticWaveProjectile entity, float entityYaw, float partialTick,
					   @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
		poseStack.pushPose();

		Vec3 motion = entity.getDeltaMovement();
		float scale = 0.6f;

		// 根据速度调整旋转
		if (motion.length() > 0.01) {
			float yaw = (float) Mth.atan2(motion.z, motion.x);
			float pitch = (float) Mth.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
			poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) (yaw * 180 / Math.PI) - 90));
			poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (pitch * 180 / Math.PI)));
		}

		float age = entity.tickCount + partialTick;

		// --- 核心球体 (金色) ---
		poseStack.pushPose();
		poseStack.scale(scale, scale, scale);
		renderEnergySphere(poseStack, bufferSource, packedLight, age, 0.0f,
				1.0f, 0.95f, 0.45f, 0.8f); // 金色
		poseStack.popPose();

		// --- 内层光晕 (绿色) ---
		poseStack.pushPose();
		poseStack.scale(scale * 1.4f, scale * 1.4f, scale * 1.4f);
		renderEnergySphere(poseStack, bufferSource, packedLight, age + 2.0f, 0.3f,
				0.3f, 0.9f, 0.2f, 0.3f); // 绿色半透明
		poseStack.popPose();

		// --- 外层光环 (金色淡) ---
		poseStack.pushPose();
		poseStack.scale(scale * 1.8f, scale * 1.8f, scale * 1.8f);
		renderEnergySphere(poseStack, bufferSource, packedLight, age + 4.0f, 0.6f,
				0.8f, 0.8f, 0.1f, 0.15f); // 淡金色
		poseStack.popPose();

		// --- 环绕闪电环 ---
		poseStack.pushPose();
		poseStack.scale(scale * 2.0f, scale * 2.0f, scale * 2.0f);
		renderLightningRing(poseStack, bufferSource, packedLight, age);
		poseStack.popPose();

		poseStack.popPose();

		super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	/**
	 * 渲染能量球体
	 */
	private void renderEnergySphere(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
									float age, float rotationOffset, float r, float g, float b, float alpha) {
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();

		int segments = 12;
		float radius = 0.8f;
		float rotation = age * 0.02f + rotationOffset;

		for (int i = 0; i < segments; i++) {
			float angle1 = (float) (2 * Math.PI * i / segments) + rotation;
			float angle2 = (float) (2 * Math.PI * (i + 1) / segments) + rotation;

			float x1 = Mth.cos(angle1) * radius;
			float z1 = Mth.sin(angle1) * radius;
			float x2 = Mth.cos(angle2) * radius;
			float z2 = Mth.sin(angle2) * radius;

			// 上弧
			vertexConsumer.vertex(matrix, x1, radius * 0.6f, z1).color(r, g, b, alpha).endVertex();
			vertexConsumer.vertex(matrix, x2, radius * 0.6f, z2).color(r, g, b, alpha).endVertex();
			vertexConsumer.vertex(matrix, x2, -radius * 0.6f, z2).color(r, g, b, alpha * 0.5f).endVertex();
			vertexConsumer.vertex(matrix, x1, -radius * 0.6f, z1).color(r, g, b, alpha * 0.5f).endVertex();

			// 侧弧 (旋转90度)
			float y1 = Mth.cos(angle1) * radius * 0.6f;
			float y2 = Mth.cos(angle2) * radius * 0.6f;
			float z1b = Mth.sin(angle1) * radius * 0.6f;
			float z2b = Mth.sin(angle2) * radius * 0.6f;

			vertexConsumer.vertex(matrix, 0, y1, z1b).color(r, g, b, alpha).endVertex();
			vertexConsumer.vertex(matrix, 0, y2, z2b).color(r, g, b, alpha).endVertex();
			vertexConsumer.vertex(matrix, radius * 0.6f, y2, z2b).color(r, g, b, alpha * 0.5f).endVertex();
			vertexConsumer.vertex(matrix, radius * 0.6f, y1, z1b).color(r, g, b, alpha * 0.5f).endVertex();
		}
	}

	/**
	 * 渲染环绕闪电环
	 */
	private void renderLightningRing(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float age) {
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();

		int bolts = 6;
		float ringRadius = 1.2f;
		float rotationSpeed = 0.05f;

		for (int b = 0; b < bolts; b++) {
			float baseAngle = (float) (2 * Math.PI * b / bolts) + age * rotationSpeed;
			float baseAngleNext = (float) (2 * Math.PI * (b + 0.5f) / bolts) + age * rotationSpeed;

			float x1 = Mth.cos(baseAngle) * ringRadius;
			float z1 = Mth.sin(baseAngle) * ringRadius;
			float x2 = Mth.cos(baseAngleNext) * ringRadius;
			float z2 = Mth.sin(baseAngleNext) * ringRadius;

			// 金色闪电弧
			float brightness = 0.8f + 0.2f * Mth.sin(age * 0.1f + b);
			vertexConsumer.vertex(matrix, x1, 0.05f, z1).color(brightness, brightness * 0.9f, 0.3f, 0.6f).endVertex();
			vertexConsumer.vertex(matrix, x2, 0.05f, z2).color(brightness, brightness * 0.9f, 0.3f, 0.6f).endVertex();
			vertexConsumer.vertex(matrix, x2, -0.05f, z2).color(brightness * 0.7f, brightness * 0.7f, 0.2f, 0.3f).endVertex();
			vertexConsumer.vertex(matrix, x1, -0.05f, z1).color(brightness * 0.7f, brightness * 0.7f, 0.2f, 0.3f).endVertex();

			// 垂直闪电束
			float flashX = Mth.cos(baseAngle + 0.2f) * ringRadius * 0.7f;
			float flashZ = Mth.sin(baseAngle + 0.2f) * ringRadius * 0.7f;
			float height = 0.2f + 0.1f * Mth.sin(age * 0.15f + b * 2);

			vertexConsumer.vertex(matrix, flashX, height, flashZ).color(0.3f, 0.9f, 0.2f, 0.5f).endVertex();
			vertexConsumer.vertex(matrix, flashX + 0.05f, height, flashZ + 0.05f).color(0.3f, 0.9f, 0.2f, 0.5f).endVertex();
			vertexConsumer.vertex(matrix, flashX + 0.05f, -height, flashZ + 0.05f).color(0.3f, 0.9f, 0.2f, 0.3f).endVertex();
			vertexConsumer.vertex(matrix, flashX, -height, flashZ).color(0.3f, 0.9f, 0.2f, 0.3f).endVertex();
		}
	}

	@Override
	public @NotNull ResourceLocation getTextureLocation(@NotNull TelekineticWaveProjectile entity) {
		// 无贴图，返回空资源
		return ResourceLocation.withDefaultNamespace("empty");
	}
}