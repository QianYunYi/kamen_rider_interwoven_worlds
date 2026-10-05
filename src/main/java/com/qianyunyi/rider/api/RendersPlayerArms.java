package com.qianyunyi.rider.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface RendersPlayerArms {
	void setRenderArms(boolean renderArms);

	boolean shouldAllowHandRender(ItemStack mainhand, ItemStack offhand, InteractionHand renderingHand);
}
