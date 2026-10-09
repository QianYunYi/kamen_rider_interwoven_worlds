package com.qianyunyi.rider.internal.henshin.ability.strategy;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class GrowingFormAttackStrategy extends AbstractKuugaFormAttackStrategy {

    @Override
    public void onLivingHurt(ServerPlayer player, LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity != null && sourceEntity.getUUID().equals(player.getUUID())) {
            if (!player.getMainHandItem().isEmpty()) return;
            if (RiderPlayerVariablesProvider.get(player).isSkillActive()) return;
            float p = 1 + (player.level().getRandom().nextFloat() - 0.5f) * 0.2f;
            player.level().playSound(null, player.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.PUNCH.get(), SoundSource.PLAYERS, 1, p);
            return;
        }
        super.onLivingHurt(player, event);
    }

    @Override
    public void onLivingDeath(ServerPlayer player, LivingDeathEvent event) {
        if (!event.getEntity().getUUID().equals(player.getUUID())) {
            super.onLivingDeath(player, event);
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth() / 2.0f);
        removeForm(player);
        player.invulnerableTime = 20;
    }

    private void removeForm(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        RiderConfig cfg = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElseThrow();
        FormConfig currentForm = cfg.getForm(data.getActiveFormName());
        if (currentForm != null) {
            currentForm.getBehavior().handleUnForm(player, cfg, currentForm);
        }
        player.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get(),
                12000, 0, false, false));
    }
}