package com.qianyunyi.rider.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class TattooParchmentPaperItem extends Item {
    public TattooParchmentPaperItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}