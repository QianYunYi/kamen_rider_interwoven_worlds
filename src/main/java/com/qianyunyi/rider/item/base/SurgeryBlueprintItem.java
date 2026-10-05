package com.qianyunyi.rider.item.base;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * 改造图纸基础类 - 所有改造图纸继承此类，自动被标记为图纸
 */
public class SurgeryBlueprintItem extends Item {

    public SurgeryBlueprintItem() {
        super(new Item.Properties()
                .stacksTo(1)  // 图纸不可堆叠
                .rarity(Rarity.EPIC)
        );
    }
}