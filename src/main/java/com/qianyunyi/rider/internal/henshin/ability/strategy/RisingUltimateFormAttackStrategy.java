package com.qianyunyi.rider.internal.henshin.ability.strategy;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RisingUltimateFormAttackStrategy extends AbstractKuugaFormAttackStrategy {

    private static final int LIGHTNING_STRIKES_REQUIRED = 100;
    private static final int TIME_LIMIT_TICKS = 1200;

    private static final Map<UUID, Integer> LIGHTNING_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> START_TIMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> RESET_TASK_IDS = new ConcurrentHashMap<>();

    @Override
    public void onFormEnter(ServerPlayer player) {
        super.onFormEnter(player);
        LIGHTNING_COUNTS.put(player.getUUID(), 0);
    }

    @Override
    public void onFormLeave(ServerPlayer player) {
        super.onFormLeave(player);
        UUID uuid = player.getUUID();
        LIGHTNING_COUNTS.remove(uuid);
        START_TIMES.remove(uuid);
        UUID taskId = RESET_TASK_IDS.remove(uuid);
        if (taskId != null) DelayedTask.cancel(taskId);
    }

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

    @Override
    public void onEntityStruckByLightning(ServerPlayer player, EntityStruckByLightningEvent event) {
        UUID uuid = player.getUUID();
        int count = LIGHTNING_COUNTS.getOrDefault(uuid, 0);

        if (count == 0) {
            START_TIMES.put(uuid, player.level().getGameTime());
            UUID taskId = DelayedTask.run(player.level(), TIME_LIMIT_TICKS, () -> {
                LIGHTNING_COUNTS.put(uuid, 0);
                START_TIMES.put(uuid, player.level().getGameTime());
            });
            RESET_TASK_IDS.put(uuid, taskId);
        }

        event.setCanceled(true);
        count++;
        LIGHTNING_COUNTS.put(uuid, count);

        if (count >= LIGHTNING_STRIKES_REQUIRED) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            data.getKuugaData().setSuperRisingUltimateForm(true);
            HenshinUtil.sendMessage(player, "短时间内受到强烈的电击效果，蜕变出超越升华究极的形态！", ChatFormatting.GOLD);
            var cfg = RiderRegistryHelper.getConfig("Kuuga");
            var superForm = cfg.orElseThrow().getForm("super_rising_ultimate_form");
            if (superForm != null) {
                cfg.orElseThrow().switchForm(player, superForm.getFormName());
            }
        }
    }
}