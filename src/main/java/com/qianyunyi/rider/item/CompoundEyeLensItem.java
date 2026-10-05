
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class CompoundEyeLensItem extends Item {
	public CompoundEyeLensItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
