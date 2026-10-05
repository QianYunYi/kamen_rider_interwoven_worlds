package com.qianyunyi.rider.data.module;

import com.qianyunyi.rider.command.DataField;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 骑士数据模块接口 - 1.20.1重构版
 * 每个骑士系统（Kuuga/Faiz/Blade等）实现此接口
 */
public interface IRiderDataModule {

    /**
     * 模块唯一标识名，如 "kuuga", "faiz"
     */
    String getModuleName();

    /**
     * 序列化为NBT
     */
    CompoundTag serializeNBT();

    /**
     * 从NBT反序列化
     */
    void deserializeNBT(CompoundTag nbt);

    /**
     * 返回该模块的可调试字段列表，用于动态生成 /rider data 调试指令。
     * 默认返回空列表，不生成任何调试指令。
     */
    default List<DataField> getDebugFields() {
        return List.of();
    }

    /**
     * 每Tick更新
     */
    default void tick(Player player) {}

    /**
     * 重置为默认值
     */
    void resetToDefault();

    /**
     * 标记为脏数据（需要同步）
     */
    void markDirty();

    /**
     * 是否脏数据
     */
    boolean isDirty();

    /**
     * 清除脏标记
     */
    void clearDirty();

    /**
     * 玩家登录时调用
     */
    default void onPlayerLogin(Player player) {}

    /**
     * 玩家登出时调用
     */
    default void onPlayerLogout(Player player) {}
}