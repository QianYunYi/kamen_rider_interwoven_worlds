package com.qianyunyi.rider.api.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * 玩家数据模块接口
 */
public interface IPlayerDataModule extends INBTSerializable<CompoundTag> {
    String getModuleName();
    void tick(Player player);
    boolean isActive();
    void resetToDefault();
    default void onPlayerLogin(Player player) {}
    default void onPlayerLogout(Player player) {}

    /** 新增：脏标记 */
    boolean isDirty();
    void clearDirty();
}

