package com.qianyunyi.rider.internal.surgery;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class SurgeryRecipe implements Recipe<net.minecraft.world.Container> {
    private final ResourceLocation id;
    private final Ingredient[] materials;
    private final Ingredient blueprint;
    private final ItemStack result;
    private final SurgeryType surgeryType;
    private final int duration;
    private final int[] materialCosts;
    private final boolean ordered;

    public SurgeryRecipe(ResourceLocation id, Ingredient[] materials, Ingredient blueprint,
                         ItemStack result, SurgeryType surgeryType, int duration,
                         int[] materialCosts, boolean ordered) {
        this.id = id;
        this.materials = materials;
        this.blueprint = blueprint;
        this.result = result;
        this.surgeryType = surgeryType;
        this.duration = duration;
        this.materialCosts = materialCosts;
        this.ordered = ordered;
    }

    // ===== Recipe 接口实现 =====

    @Override
    public boolean matches(net.minecraft.world.Container container, Level level) {
        // 从容器中提取输入（前9个槽为材料，第10个槽为图纸）
        ItemStack[] inputMaterials = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            inputMaterials[i] = container.getItem(i);
        }
        ItemStack inputBlueprint = container.getItem(9);

        // 检查图纸
        if (!blueprint.test(inputBlueprint)) {
            return false;
        }

        // 检查材料
        return ordered ? matchesOrdered(inputMaterials) : matchesUnordered(inputMaterials);
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SurgeryRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SURGERY.get();
    }

    // ===== 原有匹配逻辑 =====

    /**
     * 旧版匹配方法 - 保持兼容
     */
    public boolean matches(ItemStack[] inputMaterials, ItemStack inputBlueprint) {
        if (!blueprint.test(inputBlueprint)) {
            return false;
        }
        return ordered ? matchesOrdered(inputMaterials) : matchesUnordered(inputMaterials);
    }

    private boolean matchesOrdered(ItemStack[] inputMaterials) {
        for (int i = 0; i < 9; i++) {
            ItemStack input = inputMaterials[i];
            Ingredient required = materials[i];
            int requiredCount = materialCosts[i];

            if (required == null || required.isEmpty()) {
                if (!input.isEmpty()) return false;
            } else {
                if (!required.test(input)) return false;
                if (input.getCount() < requiredCount) return false;
            }
        }
        return true;
    }

    private boolean matchesUnordered(ItemStack[] inputMaterials) {
        boolean[] used = new boolean[9];
        for (int reqIdx = 0; reqIdx < 9; reqIdx++) {
            if (materials[reqIdx] == null || materials[reqIdx].isEmpty()) continue;
            boolean found = false;
            for (int inpIdx = 0; inpIdx < 9; inpIdx++) {
                if (used[inpIdx]) continue;
                if (inputMaterials[inpIdx].isEmpty()) continue;
                if (materials[reqIdx].test(inputMaterials[inpIdx])
                        && inputMaterials[inpIdx].getCount() >= materialCosts[reqIdx]) {
                    used[inpIdx] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        for (int i = 0; i < 9; i++) {
            if (!used[i] && !inputMaterials[i].isEmpty()) return false;
        }
        return true;
    }

    // ===== Getter =====

    public SurgeryType getSurgeryType() { return surgeryType; }
    public int getDuration() { return duration; }

    /**
     * 便捷方法：获取产物（无注册表上下文时使用）
     */
    public ItemStack getResultItem() {
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    public int getMaterialCost(int slot) {
        if (slot < 0 || slot >= materialCosts.length) return 0;
        return materialCosts[slot];
    }
    public boolean isOrdered() { return ordered; }

    public Ingredient getMaterialIngredient(int slot) {
        if (slot < 0 || slot >= materials.length) return Ingredient.EMPTY;
        return materials[slot] == null ? Ingredient.EMPTY : materials[slot];
    }

    public Ingredient getBlueprintIngredient() {
        return blueprint == null ? Ingredient.EMPTY : blueprint;
    }
}