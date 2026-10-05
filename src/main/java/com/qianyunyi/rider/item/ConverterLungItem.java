
package com.qianyunyi.rider.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class ConverterLungItem extends Item {
	public ConverterLungItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}
