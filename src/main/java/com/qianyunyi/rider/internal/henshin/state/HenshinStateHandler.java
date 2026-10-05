package com.qianyunyi.rider.internal.henshin.state;

import com.qianyunyi.rider.internal.henshin.HenshinManager;
import net.minecraft.world.entity.LivingEntity;

public interface HenshinStateHandler {
    void handle(LivingEntity entity, boolean isCrouching, HenshinManager manager);
}