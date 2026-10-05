
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class CrusherItem extends Item {
	public CrusherItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
