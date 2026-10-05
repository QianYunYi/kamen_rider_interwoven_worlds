package com.qianyunyi.rider.item.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class BaseStoryItem extends Item {

    public BaseStoryItem(Properties props) {
        super(props.stacksTo(1)); // 剧情物品不可堆叠
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false; //使物品不可损坏
    }


    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            openStoryScreen(player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    protected abstract void openStoryScreen(Player player, ItemStack stack);

    @Override
    public @NotNull Component getName(ItemStack stack) {
        return Component.translatable(stack.getItem().getDescriptionId(stack)).withStyle(style -> style.withColor(0xFFFFFF));
    }
}