package com.qianyunyi.rider.item.renderer;

import com.qianyunyi.rider.api.RendersPlayerArms;
import com.qianyunyi.rider.item.RisingPegasusCrossbowItem;
import com.qianyunyi.rider.item.model.RisingPegasusCrossbowItemModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RisingPegasusCrossbowItemRenderer extends GeoItemRenderer<RisingPegasusCrossbowItem> implements RendersPlayerArms {

	public RisingPegasusCrossbowItemRenderer() {
		super(new RisingPegasusCrossbowItemModel());
	}

	@Override
	public RenderType getRenderType(RisingPegasusCrossbowItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
	protected boolean renderArms = false;
	protected MultiBufferSource currentBuffer;
	protected RenderType renderType;
	public ItemDisplayContext transformType;
	protected RisingPegasusCrossbowItem animatable;
	private final Set<String> hiddenBones = new HashSet<>();
	private final Set<String> suppressedBones = new HashSet<>();
	private final Map<String, Vector3f> queuedBoneSetMovements = new HashMap<>();
	private final Map<String, Vector3f> queuedBoneSetRotations = new HashMap<>();
	private final Map<String, Vector3f> queuedBoneAddRotations = new HashMap<>();

	@Override
	public void renderByItem(ItemStack itemStack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
		this.transformType = transformType;

		// 第一人称持握姿势调整（充能完成后瞄准姿态）
		if ((transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
				transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) &&
				itemStack.getItem() instanceof RisingPegasusCrossbowItem) {

			boolean isCharged = RisingPegasusCrossbowItem.isCharged(itemStack);

			if (isCharged) {
				matrixStack.translate(-13.5 * SCALE_RECIPROCAL, -8 * SCALE_RECIPROCAL, -1.25 * SCALE_RECIPROCAL);
				matrixStack.mulPose(Axis.XP.rotationDegrees(0));
				matrixStack.mulPose(Axis.YP.rotationDegrees(2));
				matrixStack.mulPose(Axis.ZP.rotationDegrees(-13.5f));
			}
		}

		super.renderByItem(itemStack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
	}

	@Override
	public void actuallyRender(PoseStack matrixStackIn, RisingPegasusCrossbowItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
							   int packedOverlayIn, float red, float green, float blue, float alpha) {
		this.currentBuffer = renderTypeBuffer;
		this.renderType = type;
		this.animatable = animatable;
		super.actuallyRender(matrixStackIn, animatable, model, type, renderTypeBuffer, vertexBuilder, isRenderer, partialTicks, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		if (this.renderArms) {
			this.renderArms = false;
		}
	}

	@Override
	public ResourceLocation getTextureLocation(RisingPegasusCrossbowItem instance) {
		return super.getTextureLocation(instance);
	}

	public void hideBone(String name, boolean hide) {
		if (hide) {
			this.hiddenBones.add(name);
		} else {
			this.hiddenBones.remove(name);
		}
	}

	@Override
	public void setRenderArms(boolean renderArms) {
		this.renderArms = renderArms;
	}

	public ItemDisplayContext getCurrentTransform() {
		return this.transformType;
	}

	public void suppressModification(String name) {
		this.suppressedBones.add(name);
	}

	public void allowModification(String name) {
		this.suppressedBones.remove(name);
	}

	public void setBonePosition(String name, float x, float y, float z) {
		this.queuedBoneSetMovements.put(name, new Vector3f(x, y, z));
	}

	public void addToBoneRotation(String name, float x, float y, float z) {
		this.queuedBoneAddRotations.put(name, new Vector3f(x, y, z));
	}

	public void setBoneRotation(String name, float x, float y, float z) {
		this.queuedBoneSetRotations.put(name, new Vector3f(x, y, z));
	}

	public ItemStack getCurrentItem() {
		return this.currentItemStack;
	}

	@Override
	public boolean shouldAllowHandRender(ItemStack mainhand, ItemStack offhand, InteractionHand renderingHand) {
		return renderingHand == InteractionHand.MAIN_HAND;
	}
}