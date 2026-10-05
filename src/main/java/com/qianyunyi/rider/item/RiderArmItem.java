
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class RiderArmItem extends Item {
	public RiderArmItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
