package com.qianyunyi.rider.client.gui.wheel;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 轮盘选项数据记录。
 * <p>
 * 支持两种图标模式：
 * <ul>
 *   <li>物品图标：通过 {@link #of(ItemStack, Component, Consumer)} 构造，使用 {@link #icon()} 获取</li>
 *   <li>纹理图标：通过 {@link #ofTexture(ResourceLocation, Component, Consumer)} 构造，使用 {@link #texture()} 获取</li>
 * </ul>
 * 渲染时优先使用纹理模式，若无纹理则回退到物品图标。
 */
public record RadialOption(
        /** 物品图标，用于在轮盘上渲染（纹理模式为 null 时生效） */
        ItemStack icon,
        /** 纹理图标路径，优先于物品图标渲染；为 null 时使用物品图标 */
        @Nullable ResourceLocation texture,
        /** 选项名称，选中时显示在图标下方 */
        Component name,
        /** 选项选择后执行的动作 */
        Consumer<LivingEntity> action
) {

    /**
     * 构造一个物品图标轮盘选项
     *
     * @param icon    物品图标
     * @param name    名称
     * @param action  选中后的动作
     */
    public static RadialOption of(ItemStack icon, Component name, Consumer<LivingEntity> action) {
        return new RadialOption(
                icon == null ? ItemStack.EMPTY : icon,
                null,
                name == null ? Component.empty() : name,
                action
        );
    }

    /**
     * 构造一个纹理图标轮盘选项
     *
     * @param texture 纹理路径
     * @param name    名称
     * @param action  选中后的动作
     */
    public static RadialOption ofTexture(ResourceLocation texture, Component name, Consumer<LivingEntity> action) {
        return new RadialOption(
                ItemStack.EMPTY,
                texture,
                name == null ? Component.empty() : name,
                action
        );
    }

    /**
     * 执行选中动作。
     * 当 action 为 null 时什么也不做。
     */
    public void runAction(LivingEntity entity) {
        if (action != null) action.accept(entity);
    }
}