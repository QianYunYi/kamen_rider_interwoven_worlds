package com.qianyunyi.rider.item;

import com.qianyunyi.rider.entity.projectile.DragonWandEntity;
import com.qianyunyi.rider.entity.projectile.RisingDragonWandEntity;
import com.qianyunyi.rider.item.base.BaseTridentAnimationItem;
import com.qianyunyi.rider.item.renderer.RisingDragonWandItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class RisingDragonWandItem extends BaseTridentAnimationItem {

    public RisingDragonWandItem() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.RARE)
                .durability(800));
    }

    @Override
    protected DragonWandEntity createThrownEntity(Level level, LivingEntity shooter, ItemStack stack) {
        return new RisingDragonWandEntity(level, shooter, stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new RisingDragonWandItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}