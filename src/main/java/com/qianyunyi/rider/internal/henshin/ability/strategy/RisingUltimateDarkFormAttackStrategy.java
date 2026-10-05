package com.qianyunyi.rider.internal.henshin.ability.strategy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class RisingUltimateDarkFormAttackStrategy extends UltimateDarkFormAttackStrategy {

    @Override
    protected void specialDamageHandling(ServerPlayer player, LivingHurtEvent event) {
        super.specialDamageHandling(player, event);
        if (event.getSource().getEntity() != null && event.getSource().getEntity().getUUID().equals(player.getUUID())) {
            LivingEntity target = event.getEntity();
            if (target != null) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 4, false, false));
            }
        }
    }
}
