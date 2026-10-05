package com.qianyunyi.rider.command;

import com.qianyunyi.rider.data.RiderPlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 描述数据模块中一个可调试字段的元信息。
 * 用于动态生成 /rider data <module> get/set <field> 指令树。
 */
public record DataField(
        /** 字段名称（小写，用作指令参数），如 "fusion"、"amadam" */
        String name,
        /** 字段数据类型 */
        DataType type,
        /** 从玩家数据中读取当前值的函数 */
        Function<RiderPlayerData, Number> getter,
        /** 将数值写入玩家数据的函数 */
        BiConsumer<RiderPlayerData, Number> setter,
        /** 最小值（含），仅对 INT/FLOAT 有效 */
        Number min,
        /** 最大值（含），仅对 INT/FLOAT 有效；null 表示无上限 */
        Number max
) {
    public enum DataType {
        BOOL,
        INT,
        FLOAT
    }

    // 便捷构造：无 min/max 限制
    public DataField(String name, DataType type, Function<RiderPlayerData, Number> getter,
                     BiConsumer<RiderPlayerData, Number> setter) {
        this(name, type, getter, setter, null, null);
    }

    /**
     * 在指令执行时根据玩家获取当前值（字符串形式）。
     */
    public String getDisplayValue(ServerPlayer player) {
        RiderPlayerData data = com.qianyunyi.rider.data.RiderPlayerVariablesProvider.get(player);
        Number val = getter.apply(data);
        if (type == DataType.BOOL) {
            return val.intValue() != 0 ? "true" : "false";
        }
        return val.toString();
    }

    /**
     * 在指令执行时将字符串值写入玩家数据。
     */
    public void applyValue(ServerPlayer player, Number value) {
        RiderPlayerData data = com.qianyunyi.rider.data.RiderPlayerVariablesProvider.get(player);
        setter.accept(data, value);
    }
}