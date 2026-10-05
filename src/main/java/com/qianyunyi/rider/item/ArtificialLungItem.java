
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class ArtificialLungItem extends Item {
	public ArtificialLungItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
