package com.qianyunyi.rider.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class DodozoItem extends Item {
    public DodozoItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}