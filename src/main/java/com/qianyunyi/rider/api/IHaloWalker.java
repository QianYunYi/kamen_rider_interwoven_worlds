package com.qianyunyi.rider.api;

import net.minecraft.world.level.material.FluidState;
// 1. 岩浆行走接口
public interface IHaloWalker {
    /** 是否允许在岩浆表面行走 */
    default boolean canWalkOnLava(FluidState state) {
        return state.is(net.minecraft.tags.FluidTags.LAVA);
    }
}