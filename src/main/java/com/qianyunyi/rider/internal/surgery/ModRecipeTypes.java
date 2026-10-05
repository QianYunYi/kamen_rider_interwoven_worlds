package com.qianyunyi.rider.internal.surgery;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义配方类型注册
 * <p>
 * 注意：使用原始类型 (RecipeType<?>) 避免 SurgeryRecipe ↔ ModRecipeTypes 之间的循环泛型引用。
 * 在 1.20.1 Forge 环境中，某些 Java 编译器版本无法解析这种循环引用。
 * </p>
 */
public class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, KamenRiderInterwovenWorldsMod.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, KamenRiderInterwovenWorldsMod.MODID);

    private static final ResourceLocation SURGERY_ID =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "surgery");

    public static final RegistryObject<RecipeType<?>> SURGERY =
            RECIPE_TYPES.register("surgery", () -> RecipeType.simple(SURGERY_ID));

    public static final RegistryObject<RecipeSerializer<?>> SURGERY_SERIALIZER =
            RECIPE_SERIALIZERS.register("surgery", () -> SurgeryRecipeSerializer.INSTANCE);
}