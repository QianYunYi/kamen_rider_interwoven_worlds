package com.qianyunyi.rider.internal.surgery;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 改造手术配方管理器
 * <p>
 * 配方现在以 JSON 格式定义在 data/kamen_rider_interwoven_worlds/recipes/surgery/ 目录下，
 * 通过 Minecraft 的 RecipeManager 自动加载，支持 /reload 热重载。
 * </p>
 * 此类作为便捷封装，提供从 RecipeManager 查询配方的辅助方法。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SurgeryRecipeManager {

    /**
     * 检查材料+图纸是否能匹配任意一个手术配方
     */
    public static boolean matches(Level level, ItemStack[] materials, ItemStack blueprint) {
        return getMatchingRecipe(level, materials, blueprint) != null;
    }

    /**
     * 获取匹配的配方（从 RecipeManager 查询）
     */
    public static SurgeryRecipe getMatchingRecipe(Level level, ItemStack[] materials, ItemStack blueprint) {
        if (level == null) return null;
        RecipeManager manager = level.getRecipeManager();
        RecipeType<?> recipeType = ModRecipeTypes.SURGERY.get();

        // 使用原始类型遍历配方列表，避免泛型推断问题
        for (Object obj : manager.getAllRecipesFor((RecipeType) recipeType)) {
            if (obj instanceof SurgeryRecipe sr) {
                SurgeryRecipeInput input = new SurgeryRecipeInput(materials, blueprint);
                if (sr.matches(input, level)) {
                    return sr;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有已注册的手术配方
     */
    public static List<SurgeryRecipe> getAllRecipes(Level level) {
        if (level == null) return Collections.emptyList();
        RecipeManager manager = level.getRecipeManager();
        RecipeType<?> recipeType = ModRecipeTypes.SURGERY.get();

        List<SurgeryRecipe> result = new ArrayList<>();
        for (Object obj : manager.getAllRecipesFor((RecipeType) recipeType)) {
            if (obj instanceof SurgeryRecipe sr) {
                result.add(sr);
            }
        }
        return result;
    }
}