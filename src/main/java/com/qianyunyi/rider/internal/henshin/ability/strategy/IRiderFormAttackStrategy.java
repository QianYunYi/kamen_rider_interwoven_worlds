package com.qianyunyi.rider.internal.henshin.ability.strategy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public interface IRiderFormAttackStrategy {

    void onLivingAttack(ServerPlayer player, LivingAttackEvent event);

    void onLivingHurt(ServerPlayer player, LivingHurtEvent event);

    void onLivingDeath(ServerPlayer player, LivingDeathEvent event);

    void onPlayerTick(ServerPlayer player, TickEvent.PlayerTickEvent event);

    void onRightClick(ServerPlayer player, PlayerInteractEvent.RightClickItem event);

    void onLivingJump(ServerPlayer player, LivingEvent.LivingJumpEvent event);

    default void onLeftClickEmpty(ServerPlayer player, PlayerInteractEvent.LeftClickEmpty event) {}

    default void onLeftClickBlock(ServerPlayer player, PlayerInteractEvent.LeftClickBlock event) {}

    default void onEntityStruckByLightning(ServerPlayer player, EntityStruckByLightningEvent event) {}

    default void onFormEnter(ServerPlayer player) {}

    default void onFormLeave(ServerPlayer player) {}
}