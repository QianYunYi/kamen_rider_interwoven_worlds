package com.qianyunyi.rider.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 模组物品标签注册
 */
public class RiderAdventureAffiliatedModTags {

    /**
     * 改造图纸标签 - 可放入改造台图纸槽的物品
     */
    public static final TagKey<Item> SURGERY_BLUEPRINTS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("rider_adventure_affiliated", "surgery_blueprints")
    );

    /**
     * 修卡材料标签 - 可用于修卡改造的材料
     */
    public static final TagKey<Item> SHOCKER_MATERIALS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("rider_adventure_affiliated", "shocker_materials")
    );

    /**
     * 骑士核心标签 - 骑士系统的核心组件
     */
    public static final TagKey<Item> RIDER_CORES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("rider_adventure_affiliated", "rider_cores")
    );

    /**
     * 生物组织标签 - 用于生物改造的材料
     */
    public static final TagKey<Item> BIO_MATERIALS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("rider_adventure_affiliated", "bio_materials")
    );

    // ========== 便捷判断方法 ==========

    /**
     * 判断物品是否为改造图纸
     */
    public static boolean isSurgeryBlueprint(ItemStack stack) {
        return stack.is(SURGERY_BLUEPRINTS);
    }

    /**
     * 判断物品是否为修卡材料
     */
    public static boolean isShockerMaterial(ItemStack stack) {
        return stack.is(SHOCKER_MATERIALS);
    }

    /**
     * 判断物品是否为骑士核心
     */
    public static boolean isRiderCore(ItemStack stack) {
        return stack.is(RIDER_CORES);
    }

    /**
     * 判断物品是否为生物材料
     */
    public static boolean isBioMaterial(ItemStack stack) {
        return stack.is(BIO_MATERIALS);
    }
}