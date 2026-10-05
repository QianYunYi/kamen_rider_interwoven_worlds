package com.qianyunyi.rider.internal.henshin.ability.strategy;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class SuperRisingUltimateFormAttackStrategy extends AbstractKuugaFormAttackStrategy {

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

            if (isLightningDamage(source)) {
                event.setCanceled(true);
                float damageAmount = event.getAmount();
                RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
                data.getKuugaData().addSuperEnergy(damageAmount);
                data.getKuugaData().setSuperEnergyMax(data.getKuugaData().getSuperEnergyMax() + 1.0f);
                KamenRiderInterwovenWorldsMod.LOGGER.debug("玩家 {} 吸收雷电伤害，获得 {} 能量", player.getName().getString(), damageAmount);
                return;
            }

            if (isFireDamage(source)) {
                event.setCanceled(true);
                float damageAmount = event.getAmount();
                RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
                data.getKuugaData().addSuperEnergy(damageAmount);
                KamenRiderInterwovenWorldsMod.LOGGER.debug("玩家 {} 吸收火焰伤害，获得 {} 能量", player.getName().getString(), damageAmount);
            }
        }
    }

    @Override
    public void onLivingHurt(ServerPlayer player, LivingHurtEvent event) {
        super.onLivingHurt(player, event);
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity != null && sourceEntity.getUUID().equals(player.getUUID())) {
            LivingEntity target = event.getEntity();
            if (Math.random() < 0.1) {
                target.addEffect(new MobEffectInstance(
                        KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                        20, 8, false, false));
            }
        }
    }
}
