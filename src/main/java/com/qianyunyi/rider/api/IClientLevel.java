package com.qianyunyi.rider.api;

import net.minecraft.world.entity.Entity;

public interface IClientLevel {
    void rider_adventure_affiliated$tickWhiteListEntities();

    void rider_adventure_affiliated$addTickWhiteListEntities(Entity var1);

    void rider_adventure_affiliated$removeTickWhiteListEntities(Entity var1);

    void rider_adventure_affiliated$removeAll();

    boolean rider_adventure_affiliated$isInList(Entity var1);
}
