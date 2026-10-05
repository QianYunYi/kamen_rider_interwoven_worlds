package com.qianyunyi.rider.item;

import com.qianyunyi.rider.internal.combat.skill.kuuga.TitanSwordComboHandler;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.item.renderer.UltimateTitanSwordItemRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import java.util.function.Consumer;

public class UltimateTitanSwordItem extends BaseSwordAnimationItem {

	public UltimateTitanSwordItem() {
		super(
				Tiers.NETHERITE,
				15,
				-2.4F,
				new Item.Properties()
						.stacksTo(1)
						.rarity(Rarity.EPIC)
		);
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof ServerPlayer sp) {
			TitanSwordComboHandler.executeCombo(sp);
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final BlockEntityWithoutLevelRenderer renderer = new UltimateTitanSwordItemRenderer();

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}
}