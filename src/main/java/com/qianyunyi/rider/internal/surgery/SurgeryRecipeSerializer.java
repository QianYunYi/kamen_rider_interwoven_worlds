package com.qianyunyi.rider.internal.surgery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 改造手术配方 JSON 序列化器
 * JSON 格式类似工作台配方，但增加了手术专用字段
 *
 * 示例:
 * {
 *   "type": "kamen_rider_interwoven_worlds:surgery",
 *   "surgery_type": "rider_system",
 *   "duration": 400,
 *   "ordered": true,
 *   "blueprint": {
 *     "item": "kamen_rider_interwoven_worlds:mask_rider_1_old_modified_drawings"
 *   },
 *   "materials": [
 *     { "slot": 0, "item": "kamen_rider_interwoven_worlds:mask_rider_1_old_helmet_modified_part", "count": 1 },
 *     { "slot": 1, "item": "...", "count": 1 }
 *   ],
 *   "result": {
 *     "item": "kamen_rider_interwoven_worlds:some_result"
 *   }
 * }
 */
public class SurgeryRecipeSerializer implements RecipeSerializer<SurgeryRecipe> {

    public static final SurgeryRecipeSerializer INSTANCE = new SurgeryRecipeSerializer();

    private SurgeryRecipeSerializer() {}

    @Override
    public SurgeryRecipe fromJson(ResourceLocation id, JsonObject json) {
        // 解析手术类型
        SurgeryType surgeryType = SurgeryType.valueOf(
                GsonHelper.getAsString(json, "surgery_type", "RIDER_SYSTEM").toUpperCase()
        );

        // 解析持续时间
        int duration = GsonHelper.getAsInt(json, "duration", 200);

        // 解析是否有序
        boolean ordered = GsonHelper.getAsBoolean(json, "ordered", true);

        // 解析图纸
        JsonObject blueprintJson = GsonHelper.getAsJsonObject(json, "blueprint");
        Ingredient blueprint = Ingredient.fromJson(blueprintJson);

        // 解析材料 (9槽, 默认空)
        Ingredient[] materials = new Ingredient[9];
        int[] materialCosts = new int[9];
        for (int i = 0; i < 9; i++) {
            materials[i] = Ingredient.EMPTY;
            materialCosts[i] = 0;
        }

        JsonArray materialsArray = GsonHelper.getAsJsonArray(json, "materials", new JsonArray());
        for (JsonElement elem : materialsArray) {
            JsonObject matObj = elem.getAsJsonObject();
            int slot = GsonHelper.getAsInt(matObj, "slot");
            if (slot < 0 || slot >= 9) {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("[手术配方] 槽位 {} 超出范围 (0-8)，跳过", slot);
                continue;
            }
            int count = GsonHelper.getAsInt(matObj, "count", 1);
            materials[slot] = Ingredient.fromJson(matObj);
            materialCosts[slot] = count;
        }

        // 解析产物
        JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
        ItemStack result = readItemStack(resultJson);

        return new SurgeryRecipe(id, materials, blueprint, result, surgeryType, duration, materialCosts, ordered);
    }

    /**
     * 从 JSON 对象读取 ItemStack
     */
    private ItemStack readItemStack(JsonObject json) {
        String itemId = GsonHelper.getAsString(json, "item");
        int count = GsonHelper.getAsInt(json, "count", 1);
        ResourceLocation id = ResourceLocation.parse(itemId);
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn("[手术配方] 无法识别的物品: {}", itemId);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    @Override
    public @Nullable SurgeryRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        try {
            SurgeryType surgeryType = buf.readEnum(SurgeryType.class);
            int duration = buf.readVarInt();
            boolean ordered = buf.readBoolean();

            // 读图纸
            Ingredient blueprint = Ingredient.fromNetwork(buf);

            // 读材料
            Ingredient[] materials = new Ingredient[9];
            int[] costs = new int[9];
            for (int i = 0; i < 9; i++) {
                materials[i] = Ingredient.fromNetwork(buf);
                costs[i] = buf.readVarInt();
            }

            // 读产物
            ItemStack result = buf.readItem();

            return new SurgeryRecipe(id, materials, blueprint, result, surgeryType, duration, costs, ordered);
        } catch (Exception e) {
            KamenRiderInterwovenWorldsMod.LOGGER.error("[手术配方] 从网络读取配方失败: {}", id, e);
            return null;
        }
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, SurgeryRecipe recipe) {
        buf.writeEnum(recipe.getSurgeryType());
        buf.writeVarInt(recipe.getDuration());
        buf.writeBoolean(recipe.isOrdered());

        // 写图纸
        recipe.getBlueprintIngredient().toNetwork(buf);

        // 写材料
        for (int i = 0; i < 9; i++) {
            recipe.getMaterialIngredient(i).toNetwork(buf);
            buf.writeVarInt(recipe.getMaterialCost(i));
        }

        // 写产物
        buf.writeItem(recipe.getResultItem());
    }
}