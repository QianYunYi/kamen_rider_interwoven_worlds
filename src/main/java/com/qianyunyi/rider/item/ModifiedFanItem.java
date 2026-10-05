
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class ModifiedFanItem extends Item {
	public ModifiedFanItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
