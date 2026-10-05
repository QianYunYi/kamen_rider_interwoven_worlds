package com.qianyunyi.rider.item;

import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.item.renderer.BeatChaser2000RightHandlebarItemRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class BeatChaser2000RightHandlebarItem extends BaseSwordAnimationItem {
	public BeatChaser2000RightHandlebarItem() {
		super(Tiers.IRON, 3, -2.4F, new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return false;
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final BlockEntityWithoutLevelRenderer renderer = new BeatChaser2000RightHandlebarItemRenderer();

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}
}