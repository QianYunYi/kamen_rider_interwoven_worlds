package com.qianyunyi.rider.internal.henshin.ability.strategy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public class UltimateFormAttackStrategy extends AbstractKuugaFormAttackStrategy {

    @Override
    public void onLivingAttack(ServerPlayer player, LivingAttackEvent event) {
        if (event.getEntity().getUUID().equals(player.getUUID())) {
            DamageSource source = event.getSource();
            Entity sourceEntity = source.getDirectEntity();
            if (sourceEntity instanceof Projectile projectile) {
                Vec3 lookVec = player.getLookAngle();
                Vec3 newMotion = lookVec.scale(projectile.getDeltaMovement().length());
                projectile.setDeltaMovement(newMotion);
                projectile.setYRot(player.getYRot());
                projectile.setYHeadRot(player.getYHeadRot());
                event.setCanceled(true);
                return;
            }
        }
        super.onLivingAttack(player, event);
    }
}