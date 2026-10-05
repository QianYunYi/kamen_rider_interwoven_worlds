package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.GrowingFormKuugaHenshinBehavior;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.network.packet.CameraPerspectiveSyncMessage;
import com.qianyunyi.rider.network.packet.ScreenShakeSyncMessage;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.potion.action.SuppressEnergyExplodeAction;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import com.qianyunyi.rider.util.riderKickUtil.JumpUtil;
import com.qianyunyi.rider.util.riderKickUtil.KickMotionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class KuugaBaseRiderKick implements IRiderSkill {
    protected static final String CHARGE_UP_ANIM = "kuuga_charge_up";
    protected static final String CHARGE_STOP_ANIM = "kuuga_charge_stop";
    protected static final String KICK_UP_ANIM = "kuuga_rider_kick_up";
    protected static final String KICK_FULL_CHARGE_ANIM = "kuuga_rider_kick_full_charge";
    protected static final String KICK_ANIM = "kuuga_rider_kick_idle";
    protected static final String LAND_ANIM = "kuuga_rider_kick_land";

    protected static final int MAX_CHARGE_TICKS = 200;
    protected static final int MIN_FULL_CHARGE_TICKS = 20;

    private final Random random = new Random();
    private static final int DAMAGE_HITS = 1;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;
    private static final float HIT_MAGNIFICATION = 1;

    protected static final int COOLDOWN_SECONDS = 10;

    private static final Map<UUID, KickState> STATE_MAP = new ConcurrentHashMap<>();

    protected static class KickState {
        boolean isCharging = false;
        boolean isFirstPress = true;
        boolean isFullCharge = false;
        boolean isChargeCancelled = false;
        long chargeStartTick = 0;
        long chargeEndTick = 0;
        long cooldownStartTick = 0;
        boolean isEventRegistered = false;
        boolean hasTriggeredLanding = false;
        UUID autoCancelTaskId;
        UUID fullChargeTaskId;
        UUID releaseTimeoutTaskId;
    }

    protected KickState state(ServerPlayer player) {
        return STATE_MAP.computeIfAbsent(player.getUUID(), k -> new KickState());
    }

    protected KickState getState(ServerPlayer player) {
        return STATE_MAP.get(player.getUUID());
    }

    protected void cleanupState(ServerPlayer player) {
        KickState s = STATE_MAP.get(player.getUUID());
        if (s != null) {
            if (s.autoCancelTaskId != null) DelayedTask.cancel(s.autoCancelTaskId);
            if (s.fullChargeTaskId != null) DelayedTask.cancel(s.fullChargeTaskId);
            if (s.releaseTimeoutTaskId != null) DelayedTask.cancel(s.releaseTimeoutTaskId);
        }
        STATE_MAP.remove(player.getUUID());
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        KickState s = state(serverPlayer);

        if (s.isCharging && !s.isChargeCancelled) {
            if (s.isFullCharge) {
                cancelAutoCancelTask(s);
                cancelReleaseTimeoutTask(s);
                s.isCharging = false;
                performRelease(serverPlayer, s);
            }
            return;
        }

        if (data.isSkillActive() && !s.isCharging) return;

        if (s.isFirstPress) {
            s.isFirstPress = false;
            s.isCharging = true;
            s.isChargeCancelled = false;
            data.setSkillActive(true);
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.RIDER_KICK_START.get(), SoundSource.PLAYERS, 1, 1);

            resetSkillState(serverPlayer, s);
            startCharging(serverPlayer, s);

            s.fullChargeTaskId = DelayedTask.run(serverPlayer.level(), MIN_FULL_CHARGE_TICKS, () -> {
                if (s.isCharging) {
                    s.isFullCharge = true;
                }
            });

            s.autoCancelTaskId = DelayedTask.run(serverPlayer.level(), MAX_CHARGE_TICKS, () -> {
                if (s.isCharging) {
                    cancelCharge(serverPlayer, s);
                }
            });
        }
    }

    protected void startCharging(ServerPlayer serverPlayer, KickState s) {
        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage(CHARGE_UP_ANIM, serverPlayer.getUUID(), true), serverPlayer);
        NetworkHandler.sendToPlayer(new CameraPerspectiveSyncMessage(1), serverPlayer);
        serverPlayer.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(),
                200, 1, false, false
        ));
        s.chargeStartTick = serverPlayer.level().getGameTime();
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        KickState s = STATE_MAP.get(serverPlayer.getUUID());
        if (s == null) return;
        if (!s.isCharging || s.hasTriggeredLanding || s.isChargeCancelled) return;
        serverPlayer.removeEffect(KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get());
        if (!s.isFullCharge) {
            cancelAutoCancelTask(s);
            s.isCharging = false;
            s.isFullCharge = false;
            performRelease(serverPlayer, s);
        } else {
            HenshinUtil.sendAnimationMessage(serverPlayer, CHARGE_STOP_ANIM);
        }
    }

    protected void cancelCharge(ServerPlayer serverPlayer, KickState s) {
        if (s.isCharging) {
            s.isFullCharge = true;
            serverPlayer.removeEffect(KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get());
            HenshinUtil.sendAnimationMessage(serverPlayer, CHARGE_STOP_ANIM);
            s.releaseTimeoutTaskId = DelayedTask.run(serverPlayer.level(), MAX_CHARGE_TICKS, () -> {
                if (s.isCharging && !s.hasTriggeredLanding) {
                    autoCancelSkill(serverPlayer, s);
                }
            });
        }
    }

    protected void autoCancelSkill(ServerPlayer serverPlayer, KickState s) {
        s.isCharging = false;
        s.isFullCharge = false;
        s.isChargeCancelled = true;
        NetworkHandler.sendToPlayer(new CameraPerspectiveSyncMessage(0), serverPlayer);
        resetSkillState(serverPlayer, s);
        unregisterEvent(s);
        s.isFirstPress = true;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data != null) {
            data.setSkillActive(false);
        }
    }

    protected void cancelAutoCancelTask(KickState s) {
        if (s.autoCancelTaskId != null) {
            DelayedTask.cancel(s.autoCancelTaskId);
            s.autoCancelTaskId = null;
        }
    }

    protected void cancelReleaseTimeoutTask(KickState s) {
        if (s.releaseTimeoutTaskId != null) {
            DelayedTask.cancel(s.releaseTimeoutTaskId);
            s.releaseTimeoutTaskId = null;
        }
    }

    protected void performRelease(ServerPlayer serverPlayer, KickState s) {
        s.chargeEndTick = serverPlayer.level().getGameTime();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        data.setSkillActive(false);
        if (data.getKuugaData().isSomersault()) {
            HenshinUtil.sendAnimationMessage(serverPlayer, KICK_FULL_CHARGE_ANIM);
            JumpUtil.jump(serverPlayer, 0, 30, 40, () -> {});
            DelayedTask.run(serverPlayer.level(), 30, () -> {
                performAirDash(serverPlayer, s);
                registerEvents(s);
            });
        } else {
            HenshinUtil.sendAnimationMessage(serverPlayer, KICK_UP_ANIM);
            JumpUtil.jump(serverPlayer, 0, 30, 40, () -> {});
            DelayedTask.run(serverPlayer.level(), 22, () -> {
                performAirDash(serverPlayer, s);
                registerEvents(s);
            });
        }
    }

    protected void registerEvents(KickState s) {
        if (!s.isEventRegistered) {
            MinecraftForge.EVENT_BUS.register(this);
            s.isEventRegistered = true;
        }
    }

    protected void performAirDash(ServerPlayer serverPlayer, KickState s) {
        double dashSpeed = calculateDashSpeed(s);
        HenshinUtil.sendAnimationMessage(serverPlayer, KICK_ANIM);
        KickMotionUtil.dash(serverPlayer, dashSpeed, 200.0, 30.0, 300, () -> triggerLanding(serverPlayer, s));
    }

    protected double calculateDashSpeed(KickState s) {
        double baseSpeed = 20.0;
        double maxSpeed = 40.0;
        double chargeRatio = Math.min((double)(s.chargeEndTick - s.chargeStartTick) / MAX_CHARGE_TICKS, 1.0);
        return baseSpeed + (maxSpeed - baseSpeed) * chargeRatio;
    }

    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {
        if (s.hasTriggeredLanding) return;
        HenshinUtil.sendAnimationMessage(serverPlayer, LAND_ANIM);
        KickMotionUtil.stopMotion(serverPlayer);
        NetworkHandler.sendToPlayer(new CameraPerspectiveSyncMessage(0), serverPlayer);
        NetworkHandler.sendToPlayer(new ScreenShakeSyncMessage(0.3f, 12), serverPlayer);
        s.hasTriggeredLanding = true;
        s.cooldownStartTick = serverPlayer.level().getGameTime();
        explodeAndSeal(serverPlayer, s);
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data != null) {
            data.setSkillActive(false);
            data.getKuugaData().addRiderKickCount(1);
        }
        unregisterEvent(s);
        resetSkillState(serverPlayer, s);
        s.isFirstPress = true;
    }

    /** 查找目标实体（排除玩家自身的宠物/伙伴） */
    protected List<LivingEntity> findTargetEntities(ServerPlayer serverPlayer) {
        Vec3 pos = serverPlayer.position();
        AABB area = new AABB(
                pos.x - 5.0, pos.y - 2.5, pos.z - 5.0,
                pos.x + 5.0, pos.y + 2.5, pos.z + 5.0
        );

        Entity vehicle = serverPlayer.getVehicle();
        return new ArrayList<>(serverPlayer.level().getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != vehicle && !RiderCompanionUtil.isPlayerCompanion(e, serverPlayer.getUUID())));
    }

    /** @deprecated 内部已使用 {@link RiderCompanionUtil}，保留是为了兼容子类调用 */
    @Deprecated
    protected boolean isPlayerPet(LivingEntity entity, ServerPlayer serverPlayer) {
        return RiderCompanionUtil.isPlayerCompanion(entity, serverPlayer.getUUID());
    }

    protected float calculateTotalDamage(ServerPlayer serverPlayer, Boolean isSomersault, KickState s) {
        float baseDamage = serverPlayer.experienceLevel * 5.0f;
        float chargeTime = (s.chargeEndTick - s.chargeStartTick) / 20.0f;
        float chargeBonus = chargeTime * 10.0f;
        if (isSomersault) {
            return (baseDamage + chargeBonus)*1.25f;
        }
        return baseDamage + chargeBonus;
    }

    protected void explodeAndSeal(ServerPlayer serverPlayer, KickState s) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        List<LivingEntity> targets = findTargetEntities(serverPlayer);
        if (targets.isEmpty()) return;

        float energyCost = calculateEnergyCost(s);
        if (!deductEnergy(data, energyCost, serverPlayer)) {
            scheduleDeactivation(serverPlayer, data);
            return;
        }

        float totalDamage = calculateTotalDamage(serverPlayer,data.getKuugaData().isSomersault(), s);
        float totalSealingPower = (energyCost / 2);

        String formName = data.getActiveFormName();
        targets.forEach(target -> {
            applySuppressEffect(target);
            // 落地时立即标记爆炸（前摄标记，覆盖前20 tick的无伤害期）
            markAsExplosive(target, serverPlayer, formName);

            float distance = target.distanceTo(serverPlayer);
            float distanceFactor = calculateDistanceFactor(distance);
            float suppressEnergy = totalSealingPower * distanceFactor;

            addSuppressEnergy(target, suppressEnergy, totalSealingPower);

            // 延迟施加多段伤害（标记爆炸与伤害在同一时刻给出，消除空窗期）
            DelayedTask.run(serverPlayer.level(), 20, () -> applyMultiHitDamage(serverPlayer, target, totalDamage, distanceFactor, suppressEnergy, formName));
        });
    }

    /** 应用多段伤害（爆炸标记与伤害同一时刻给出，消除死亡空窗期） */
    protected void applyMultiHitDamage(ServerPlayer serverPlayer, LivingEntity target,
                                       float totalDamage, float distanceFactor, float suppressEnergy, String formName) {
        // 在伤害前一刻给出爆炸标记，消除 is_explosive 设置到实际伤害之间的时间窗口
        markAsExplosive(target, serverPlayer, formName);
        float baseDamagePerHit = (totalDamage + suppressEnergy) * HIT_MAGNIFICATION * distanceFactor;

        for (int i = 0; i < DAMAGE_HITS; i++) {
            if (!target.isAlive()) break;

            int finalI = i;
            DelayedTask.run(serverPlayer.level(), HIT_DELAY_TICKS * i, () -> {
                if (!target.isAlive()) return;

                float damageVariation = 1.0f + (random.nextFloat() - 0.5f) * (2 * DAMAGE_VARIATION);
                float finalDamage = baseDamagePerHit * damageVariation;

                // 爆炸标记实体：限制伤害不致死，保留至少0.5血量
                // 不依赖 LivingDeathEvent 事件拦截（可能因优先级或模组兼容性失效）
                boolean isExplosive = target.getPersistentData().getBoolean("is_explosive");
                if (isExplosive) {
                    float healthAfterDamage = target.getHealth() - finalDamage;
                    if (healthAfterDamage <= 0) {
                        finalDamage = Math.max(target.getHealth() - 0.5f, 0.1f);
                    }
                }

                target.invulnerableTime = 0;
                target.hurt(new PlayerLightningDamageSource(serverPlayer), finalDamage);

                // 播放爆炸音效
                serverPlayer.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                        0.5f, 0.9f + random.nextFloat() * 0.2f);

                // 爆炸标记实体：直接触发爆炸流程（不依赖 LivingDeathEvent 事件拦截）
                // 理由：LivingDeathEvent 的安全网拦截可能因其他模组事件优先级问题失效，
                // 此处改为在伤害后直接调用 triggerExplosion，由 triggerExplosion 内部
                // 的 is_exploding 标记防止重复触发，同时 LivingDeathEvent 的安全网保留作冗余保护。
                if (isExplosive) {
                    if (!target.isAlive()) {
                        target.setHealth(0.5f);
                    }
                    SuppressEnergyExplodeAction.triggerExplosion(target.level(), target);
                }
            });
        }
    }

    /**
     * 安全施加骑士踢伤害，统一处理爆炸标记实体的保护逻辑
     * <p>
     * 所有形态子类在 applyMultiHitDamage() 中替换 {@code target.hurt(...)} 为此方法。
     * 消除各子类重复实现和维护成本。
     */
    protected void applyKickDamage(ServerPlayer serverPlayer, LivingEntity target, float finalDamage) {
        // 爆炸流程中的实体跳过后续伤害（PlayerLightningDamageSource 可能无视无敌）
        if (target.getPersistentData().getBoolean("is_exploding")) return;

        target.invulnerableTime = 0;
        float healthBefore = target.getHealth();
        boolean isExplosive = target.getPersistentData().getBoolean("is_explosive");

        if (isExplosive) {
            // 爆炸标记实体：第一刀即触发爆炸
            // 无论单刀伤害大小，防止多段小伤害累积致死
            if (finalDamage >= healthBefore) {
                finalDamage = Math.max(healthBefore - 0.5f, 0.1f);
            }
            target.hurt(new PlayerLightningDamageSource(serverPlayer), finalDamage);
            if (!target.isAlive()) {
                target.setHealth(0.5f);
            }
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.NORMAL_EXPLOSION.get(), SoundSource.PLAYERS, 1, 1);
            SuppressEnergyExplodeAction.triggerExplosion(target.level(), target);
        } else {
            target.hurt(new PlayerLightningDamageSource(serverPlayer), finalDamage);
        }
    }

    /** 伤害距离衰减 */
    protected float calculateDistanceFactor(float distance) {
        if (distance <= 3.0f) return 1.0f;
        if (distance <= 10.0f) {
            float t = (distance - 3.0f) / 7.0f;
            return 1.0f - t * t;
        }
        if (distance <= 15.0f) {
            return 0.3f * (1.0f - (distance - 10.0f) / 5.0f);
        }
        return 0.0f;
    }

    /** 添加镇压能量 */
    protected void addSuppressEnergy(LivingEntity target, float suppressEnergy, float totalSuppressEnergy) {
        double current = target.getPersistentData().getDouble("suppress_energy");
        double newValue = Math.min(current + suppressEnergy, 2000);
        target.getPersistentData().putDouble("suppress_energy", newValue);
    }

    /** 消耗能量 */
    protected boolean deductEnergy(RiderPlayerData data, float amount, ServerPlayer serverPlayer) {
        float currentEnergy = data.getKuugaData().getAmadamEnergy();
        if (currentEnergy < amount) {
            data.getKuugaData().removeAmadamEnergy(currentEnergy);
            return false;
        }
        data.getKuugaData().removeAmadamEnergy(amount);
        return true;
    }

    protected float calculateEnergyCost(KickState s) {
        float chargeSeconds = (s.chargeEndTick - s.chargeStartTick) / 20.0f;
        return Math.min(chargeSeconds * 50.0f, 500.0f);
    }

    /** 应用镇压效果（2秒 = 40 tick） */
    protected void applySuppressEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                40, 8, false, false));
    }

    /** 标记为爆炸性实体 */
    protected void markAsExplosive(LivingEntity target, ServerPlayer serverPlayer,String form) {
        target.getPersistentData().putBoolean("is_explosive", true);
        target.getPersistentData().putString("explosion_player", serverPlayer.getUUID().toString());
        target.getPersistentData().putString("form", form);
    }

    /** 能量不足时切换形态 */
    protected void scheduleDeactivation(ServerPlayer serverPlayer, RiderPlayerData data) {
        DelayedTask.run(serverPlayer.level(), 25, () -> {
                    GrowingFormKuugaHenshinBehavior.switchToBaseForm(serverPlayer);
                    serverPlayer.addEffect(new MobEffectInstance(
                            KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get(),
                            12000, 0, false, false));
                }
        );
    }
    protected void resetSkillState(ServerPlayer player, KickState s) {
        s.chargeStartTick = 0;
        s.chargeEndTick = 0;
        s.hasTriggeredLanding = false;
        s.isFullCharge = false;
        s.isChargeCancelled = false;

        if (s.fullChargeTaskId != null) {
            DelayedTask.cancel(s.fullChargeTaskId);
            s.fullChargeTaskId = null;
        }
        cancelAutoCancelTask(s);
        cancelReleaseTimeoutTask(s);
    }

    protected void unregisterEvent(KickState s) {
        if (s.isEventRegistered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            s.isEventRegistered = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onServerPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        KickState s = STATE_MAP.get(sp.getUUID());
        if (s == null || !s.isCharging) return;
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        KickState s = state(serverPlayer);
        long cooldownElapsed = (currentTick - s.cooldownStartTick) / 20;
        long remainingCooldown = COOLDOWN_SECONDS - cooldownElapsed;

        if (remainingCooldown > 0) {
            HenshinUtil.sendMessage(serverPlayer,
                    "技能冷却中，剩余时间：" + remainingCooldown + "秒",
                    ChatFormatting.RED);
            return false;
        }
        return true;
    }
}