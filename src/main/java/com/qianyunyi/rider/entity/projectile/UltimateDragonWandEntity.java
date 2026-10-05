package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class UltimateDragonWandEntity extends DragonWandEntity {

    public UltimateDragonWandEntity(EntityType<? extends UltimateDragonWandEntity> type, Level level) {
        super(type, level);
        this.wandItem = new ItemStack(KamenRiderInterwovenWorldsModItems.ULTIMATE_DRAGON_WAND.get());
    }

    public UltimateDragonWandEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(EntityRegistrationHandler.ULTIMATE_DRAGON_WAND_ENTITY, level, shooter, stack);
    }
}