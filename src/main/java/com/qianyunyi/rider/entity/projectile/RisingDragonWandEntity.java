package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RisingDragonWandEntity extends DragonWandEntity {

    public RisingDragonWandEntity(EntityType<? extends RisingDragonWandEntity> type, Level level) {
        super(type, level);
        this.wandItem = new ItemStack(KamenRiderInterwovenWorldsModItems.RISING_DRAGON_WAND.get());
    }

    public RisingDragonWandEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(EntityRegistrationHandler.RISING_DRAGON_WAND_ENTITY, level, shooter, stack);
    }
}