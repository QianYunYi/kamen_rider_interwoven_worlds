package com.qianyunyi.rider.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 书本生成工具类（所有书本通用）
 */
public class StoryBookUtil {

    /**
     * 通用生成成书方法
     * @param title 书本正式标题（NBT）
     * @param author 作者
     * @param displayName 物品显示名称（带颜色）
     * @param pages 书页内容
     * @return 成书 ItemStack
     */
    public static ItemStack createWrittenBook(String title, String author, String displayName, ListTag pages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();

        // NBT 基础信息
        tag.putString("title", title);
        tag.putString("author", author);
        tag.put("pages", pages);

        // 物品悬浮显示名称
        book.setHoverName(Component.translatable(displayName));

        return book;
    }

    /**
     * 快速创建一页文字
     */
    public static StringTag page(String text) {
        return StringTag.valueOf(text);
    }
}