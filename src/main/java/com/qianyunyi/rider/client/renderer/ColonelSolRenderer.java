
package com.qianyunyi.rider.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

import com.qianyunyi.rider.entity.kaijin.shocker.ColonelSolEntity;

public class ColonelSolRenderer extends HumanoidMobRenderer<ColonelSolEntity, HumanoidModel<ColonelSolEntity>> {
	public ColonelSolRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<ColonelSolEntity>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(ColonelSolEntity entity) {
		return new ResourceLocation("kamen_rider_interwoven_worlds:textures/entities/colonel_sol.png");
	}
}
