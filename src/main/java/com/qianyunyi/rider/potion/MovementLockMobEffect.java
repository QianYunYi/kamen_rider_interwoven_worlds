package com.qianyunyi.rider.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class MovementLockMobEffect extends MobEffect {

	private static final UUID MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");

	public MovementLockMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return new ArrayList<>();
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// Mixin 在 ServerPlayer#tick 和 handleMovePlayer 中处理完全锁定
		// 这里不需要额外逻辑，但方法必须存在以触发每 tick 效果
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		var attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr != null) attr.removeModifier(MODIFIER_UUID);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		var attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attr != null && attr.getModifier(MODIFIER_UUID) == null) {
			attr.addTransientModifier(getAttributeModifier());
		}
	}

	private AttributeModifier getAttributeModifier() {
		return new AttributeModifier(
				MODIFIER_UUID,
				this.getDescriptionId(),
				-1.0D,
				AttributeModifier.Operation.MULTIPLY_BASE
		);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void initializeClient(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
		consumer.accept(new IClientMobEffectExtensions() {
			@Override
			public boolean isVisibleInInventory(MobEffectInstance effect) {
				return false;
			}

			@Override
			public boolean renderInventoryText(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
				return false;
			}

			@Override
			public boolean isVisibleInGui(MobEffectInstance effect) {
				return false;
			}
		});
	}
}