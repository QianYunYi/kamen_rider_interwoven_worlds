package com.qianyunyi.rider.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuzepaItem extends Item {

    private static final String TAG_GAME_COUNT = "GurongiGameCount";

    public GuzepaItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = getGameCount(stack);
        tooltip.add(Component.translatable("tooltip.kamen_rider_interwoven_worlds.guzepa.game_count", count));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static int getGameCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_GAME_COUNT)) {
            return tag.getInt(TAG_GAME_COUNT);
        }
        return 0;
    }

    public static void setGameCount(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_GAME_COUNT, count);
    }
}