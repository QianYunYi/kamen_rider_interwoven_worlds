package com.qianyunyi.rider.internal.surgery;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 改造手术配方输入容器
 * <p>
 * 类似工作台的 Container 包装，包含 9 个材料槽 + 1 个图纸槽。
 * 使用 SimpleContainer 而非 RecipeInput，兼容 Minecraft 1.20.1 的配方 API。
 * </p>
 */
public class SurgeryRecipeInput extends SimpleContainer {

    public static final int MATERIAL_SLOTS = 9;
    public static final int BLUEPRINT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    /**
     * 从材料数组和图纸创建输入
     */
    public SurgeryRecipeInput(ItemStack[] materials, ItemStack blueprint) {
        super(TOTAL_SLOTS);
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            if (i < materials.length) {
                setItem(i, materials[i] == null ? ItemStack.EMPTY : materials[i]);
            }
        }
        setItem(BLUEPRINT_SLOT, blueprint == null ? ItemStack.EMPTY : blueprint);
    }

    /**
     * 从菜单创建输入
     */
    public static SurgeryRecipeInput fromMenu(AbstractContainerMenu menu) {
        ItemStack[] mats = new ItemStack[MATERIAL_SLOTS];
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            mats[i] = menu.getSlot(i).getItem();
        }
        ItemStack bp = menu.getSlot(BLUEPRINT_SLOT).getItem();
        return new SurgeryRecipeInput(mats, bp);
    }

    public ItemStack[] getMaterials() {
        ItemStack[] result = new ItemStack[MATERIAL_SLOTS];
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            result[i] = getItem(i);
        }
        return result;
    }

    public ItemStack getBlueprint() {
        return getItem(BLUEPRINT_SLOT);
    }
}