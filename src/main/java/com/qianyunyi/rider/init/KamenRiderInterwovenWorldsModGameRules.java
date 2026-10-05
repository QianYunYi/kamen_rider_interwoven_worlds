
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.qianyunyi.rider.init;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.GameRules;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class KamenRiderInterwovenWorldsModGameRules {
	public static final GameRules.Key<GameRules.BooleanValue> SKILL_EXPLOSION = GameRules.register("skillExplosion", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
}
