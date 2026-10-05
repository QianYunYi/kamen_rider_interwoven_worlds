package com.qianyunyi.rider.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class SummonEntityUtil {

    // 物品使用（有 hand）
    public static InteractionResultHolder<ItemStack> summon(
            Level world,
            Player player,
            InteractionHand hand,
            RegistryObject<? extends EntityType<?>> entityType,
            int consumeCount,
            int cooldown
    ) {
        return summon(world, player, hand, entityType, consumeCount, cooldown, true);
    }

    // 物品使用（无消耗）
    public static InteractionResultHolder<ItemStack> summonNoConsume(
            Level world,
            Player player,
            InteractionHand hand,
            RegistryObject<? extends EntityType<?>> entityType,
            int cooldown
    ) {
        // 修复：自动处理null手
        InteractionHand safeHand = hand == null ? InteractionHand.MAIN_HAND : hand;
        return summon(world, player, safeHand, entityType, 0, cooldown, false);
    }

    // 剧情 / 程序召唤（不需要 hand！）
    public static void summonDirect(
            Level world,
            Player player,
            RegistryObject<? extends EntityType<?>> entityType
    ) {
        if (world.isClientSide()) return;

        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double spawnX = playerPos.x + lookVec.x * 3.0D;
        double spawnY = playerPos.y + 1.0D;
        double spawnZ = playerPos.z + lookVec.z * 3.0D;

        BlockPos spawnBlockPos = BlockPos.containing(spawnX, spawnY, spawnZ);
        BlockPos posAbove = spawnBlockPos.above();

        if (!world.isEmptyBlock(spawnBlockPos) || !world.isEmptyBlock(posAbove)) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        Entity entity = entityType.get().create(world);
        if (entity == null) return;

        entity.setPos(spawnX, spawnY, spawnZ);
        entity.setYRot(player.getYRot());
        world.addFreshEntity(entity);

        world.playSound(null, spawnX, spawnY, spawnZ,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, spawnX, spawnY, spawnZ,
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // 主方法
    public static InteractionResultHolder<ItemStack> summon(
            Level world,
            Player player,
            InteractionHand hand,
            RegistryObject<? extends EntityType<?>> entityType,
            int consumeCount,
            int cooldown,
            boolean consumeItem
    ) {
        // 双重保险：再次确保 hand 不为 null
        InteractionHand safeHand = (hand == null) ? InteractionHand.MAIN_HAND : hand;
        ItemStack itemstack = player.getItemInHand(safeHand);

        if (world.isClientSide())
            return InteractionResultHolder.pass(itemstack);

        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double spawnX = playerPos.x + lookVec.x * 3.0D;
        double spawnY = playerPos.y + 1.0D;
        double spawnZ = playerPos.z + lookVec.z * 3.0D;

        BlockPos spawnBlockPos = BlockPos.containing(spawnX, spawnY, spawnZ);
        BlockPos posAbove = spawnBlockPos.above();

        if (!world.isEmptyBlock(spawnBlockPos) || !world.isEmptyBlock(posAbove)) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.pass(itemstack);
        }

        Entity entity = entityType.get().create(world);
        if (entity == null)
            return InteractionResultHolder.pass(itemstack);

        entity.setPos(spawnX, spawnY, spawnZ);
        entity.setYRot(player.getYRot());
        world.addFreshEntity(entity);

        world.playSound(null, spawnX, spawnY, spawnZ,
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, spawnX, spawnY, spawnZ,
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.8F, 1.2F);

        if (consumeItem && !player.isCreative()) {
            itemstack.shrink(consumeCount);
        }

        if (cooldown > 0) {
            player.getCooldowns().addCooldown(itemstack.getItem(), cooldown);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
    }
}