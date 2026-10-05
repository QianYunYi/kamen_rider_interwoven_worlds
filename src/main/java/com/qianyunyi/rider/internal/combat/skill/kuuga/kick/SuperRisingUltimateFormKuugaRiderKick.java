package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.entity.effect.TransformationEffectEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.YellowLightningSyncMessage;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 超升华究极空我精准飞踢 - 继承基类实现特殊效果
 */
public class SuperRisingUltimateFormKuugaRiderKick extends KuugaBaseRiderKick {
    @Override
    public String getDisplayName() {
        return "超升华究极空我精准飞踢";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    private static final int DAMAGE_HITS = 33;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;
    private static final float HIT_MAGNIFICATION = 0.03f;

    // 天气控制相关
    private boolean isStormActive = false;
    private int stormDuration = 0;
    private final int MAX_STORM_DURATION = 300;

    // 闪电相关
    private final Random random = new Random();
    private static final SoundEvent currentSound = KamenRiderInterwovenWorldsModSounds.CURRENT.get();

    // 黄色闪电渲染状态同步
    private int lightningSyncCounter = 0;
    private static final int LIGHTNING_SYNC_INTERVAL = 3;

    // 绑定特效实体
    private TransformationEffectEntity boundEffectEntity = null;

    // 蓄力阶段冲击波任务
    private UUID shockwaveTaskId = null;
    private static final int SHOCKWAVE_INTERVAL = 40;

    /* ---------- 主入口 ---------- */
    @Override
    public void skill(ServerPlayer serverPlayer) {
        super.skill(serverPlayer);
    }

    /* ---------- 蓄力阶段（添加天气和效果） ---------- */
    @Override
    protected void startCharging(ServerPlayer serverPlayer, KickState s) {
        super.startCharging(serverPlayer, s);

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            startStorm(serverLevel);
            startChargingShockwave(serverLevel, serverPlayer, s);
        }

        sendLightningSync(serverPlayer, 0, 0);

        serverPlayer.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.ULTIMATE_POWER.get(),
                200, 1, false, false
        ));
    }

    private void startChargingShockwave(ServerLevel serverLevel, ServerPlayer serverPlayer, KickState s) {
        shockwaveTaskId = DelayedTask.repeat(serverLevel, SHOCKWAVE_INTERVAL, (l) -> {
            if (!s.isCharging || !serverPlayer.isAlive()) {
                cancelShockwaveTask();
                return;
            }
            SpecialEffectsUtil.createCircularShockwave(
                    serverLevel,
                    serverPlayer,
                    15.0,
                    1.5,
                    50,
                    30
            );
        });
    }

    /** 取消冲击波任务 */
    private void cancelShockwaveTask() {
        if (shockwaveTaskId != null) {
            DelayedTask.cancel(shockwaveTaskId);
            shockwaveTaskId = null;
        }
    }

    /* ---------- 重写释放阶段（添加特效） ---------- */
    @Override
    protected void performRelease(ServerPlayer serverPlayer, KickState s) {
        serverPlayer.removeEffect(KamenRiderInterwovenWorldsModMobEffects.ULTIMATE_POWER.get());

        GroundShatterEffect.createCrackGroundEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                5.0, 100, 1.0f, 0
        );

        super.performRelease(serverPlayer, s);
    }

    @Override
    protected void performAirDash(ServerPlayer serverPlayer, KickState s) {
        spawnSpecialEffects(serverPlayer);

        sendLightningSync(serverPlayer, 1, 0);

        super.performAirDash(serverPlayer, s);
    }

    @Override
    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {

        cancelShockwaveTask();

        cleanupBoundEffect();

        sendLightningSync(serverPlayer, 2, 60);

        createSupermagneticStorm(serverPlayer);

        createShockwaveEffects(serverPlayer, s);

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            endStorm(serverLevel);
        }

        super.triggerLanding(serverPlayer, s);
    }

    /* ---------- 重写目标查找（扩大范围） ---------- */
    @Override
    protected List<LivingEntity> findTargetEntities(ServerPlayer serverPlayer) {
        TargetingConditions cond = TargetingConditions.forNonCombat()
                .range(15.0f)
                .selector(e -> e != serverPlayer && !isPlayerPet(e, serverPlayer));

        Vec3 pos = serverPlayer.position();
        AABB area = new AABB(
                pos.x - 15.0, pos.y - 7.5, pos.z - 15.0,
                pos.x + 15.0, pos.y + 7.5, pos.z + 15.0
        );

        return new ArrayList<>(serverPlayer.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> cond.test(serverPlayer, e)));
    }

    @Override
    protected boolean deductEnergy(RiderPlayerData data, float amount, ServerPlayer serverPlayer) {
        float currentEnergy = data.getKuugaData().getSuperEnergy();
        if (currentEnergy < amount) {
            data.getKuugaData().removeSuperEnergy(currentEnergy);
            return false;
        }
        data.getKuugaData().removeSuperEnergy(amount);
        return true;
    }

    /* ---------- 伤害计算（新公式） ---------- */
    @Override
    protected float calculateTotalDamage(ServerPlayer serverPlayer, Boolean isSomersault, KickState s) {
        float energyCost = calculateEnergyCost(serverPlayer, s);
        float playerAttack = (float) serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float playerLevel = serverPlayer.experienceLevel;
        if (isSomersault) {
            return (playerLevel * 10.0f + energyCost * playerAttack * 0.1f) * 1.25f;
        }
        return playerLevel * 10.0f + energyCost * playerAttack * 0.1f;
    }

    @Override
    protected void applyMultiHitDamage(ServerPlayer serverPlayer, LivingEntity target,
                                       float totalDamage, float distanceFactor, float suppressEnergy, String formName) {
        markAsExplosive(target, serverPlayer, formName);
        float baseDamagePerHit = (totalDamage + suppressEnergy) * HIT_MAGNIFICATION * distanceFactor;

        for (int i = 0; i < DAMAGE_HITS; i++) {
            if (!target.isAlive()) break;

            int finalI = i;
            DelayedTask.run(serverPlayer.level(), HIT_DELAY_TICKS * i, () -> {
                if (!target.isAlive()) return;

                float damageVariation = 1.0f + (random.nextFloat() - 0.5f) * (2 * DAMAGE_VARIATION);
                float finalDamage = baseDamagePerHit * damageVariation;

                applyKickDamage(serverPlayer, target, finalDamage);

                if (finalI % 5 == 0) {
                    serverPlayer.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                            0.5f, 0.9f + random.nextFloat() * 0.2f);
                }
            });
        }
    }

    /* ---------- 重写Tick事件处理 ---------- */
    @Override
    public void onServerPlayerTick(TickEvent.PlayerTickEvent event) {

        if (!(event.player instanceof ServerPlayer sp)) return;
        KickState s = getState(sp);

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                currentSound, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (isStormActive) {
            stormDuration++;
            if (stormDuration >= MAX_STORM_DURATION) {
                if (sp.level() instanceof ServerLevel serverLevel) {
                    endStorm(serverLevel);
                }
            }
        }

        lightningSyncCounter++;
        if (lightningSyncCounter >= LIGHTNING_SYNC_INTERVAL) {
            lightningSyncCounter = 0;
            if (s != null && s.isCharging) {
                sendLightningSync(sp, 0, 0);
            } else if (!sp.onGround() && s != null && !s.hasTriggeredLanding) {
                sendLightningSync(sp, 1, 0);
            }
        }

        if (!sp.onGround() && isStormActive) {
            if (sp.level() instanceof ServerLevel serverLevel) {
                spawnRandomLightning(serverLevel, sp);
            }
        }
        super.onServerPlayerTick(event);
    }

    /* ---------- 超升华究极特有方法 ---------- */

    private void sendLightningSync(ServerPlayer serverPlayer, int phase, int duration) {
        Vec3 pos = serverPlayer.position();
        YellowLightningSyncMessage msg = new YellowLightningSyncMessage(
                serverPlayer.getUUID(),
                phase,
                pos.x, pos.y, pos.z,
                0, 0, 0,
                duration,
                serverPlayer.level().getGameTime()
        );
        NetworkHandler.sendToTrackingPlayers(msg, serverPlayer);
    }

    private void spawnRandomLightning(ServerLevel level, ServerPlayer serverPlayer) {
        if (random.nextDouble() > 0.2) return;

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * 15;
        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;
        Vec3 pos = serverPlayer.position().add(offsetX, 0, offsetZ);

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) pos.x, (int) pos.z);

        Entity lightningBolt = EntityType.LIGHTNING_BOLT.create(level);
        if (lightningBolt != null) {
            lightningBolt.moveTo(pos.x, y, pos.z);
            level.addFreshEntity(lightningBolt);
            level.playSound(null, pos.x, y, pos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 1.0F);
        }
    }

    private void createSupermagneticStorm(ServerPlayer serverPlayer) {

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            TransformationEffectEntity effectEntity = new TransformationEffectEntity(
                    EntityRegistrationHandler.TRANS_FORMATION_EFFECT,
                    serverLevel
            );

            effectEntity.moveTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYRot(), 0);

            effectEntity.setModelLocation("supermagnetic_storm");
            effectEntity.setAnimationName("idle");
            effectEntity.setTextureLocation("supermagnetic_storm");
            effectEntity.setDying(true);
            effectEntity.setDeadTime(468);
            effectEntity.setAutoClear(false);

            effectEntity.getPersistentData().putBoolean("henshin", true);

            serverLevel.addFreshEntity(effectEntity);
            DelayedTask.run(serverPlayer.level(), 220, () ->
                    effectEntity.setAnimationName("henshin")
            );

            DelayedTask.run(serverPlayer.level(), 40, () ->
                    SpecialEffectsUtil.triggerComicBlackWhiteEffectInRadius(serverPlayer, 3.0f, 1.0f, 10.0f, true)
            );

            DelayedTask.run(serverPlayer.level(), 55, effectEntity::discard);
        }
    }

    private void createShockwaveEffects(ServerPlayer serverPlayer, KickState s) {
        float chargeSeconds = (s.chargeEndTick - s.chargeStartTick) / 20.0f;

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            SpecialEffectsUtil.createCircularShockwave(
                    serverLevel,
                    serverPlayer,
                    5.0f + chargeSeconds,
                    1.5f,
                    100,
                    30
            );
        }

        GroundShatterEffect.createCrackGroundEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                chargeSeconds,
                100,
                1.0f,
                0
        );

        DelayedTask.run(serverPlayer.level(), 20, () -> GroundShatterEffect.createFlyUpEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                5.0f + chargeSeconds,
                40,
                0.3f,
                0,
                serverPlayer
        ));
    }

    @Override
    protected float calculateDistanceFactor(float distance) {
        if (distance <= 5.0f) return 1.0f;
        if (distance <= 12.0f) {
            float t = (distance - 3.0f) / 7.0f;
            return 1.0f - t * t;
        }
        if (distance <= 15.0f) {
            return 0.3f * (1.0f - (distance - 10.0f) / 5.0f);
        }
        return 0.0f;
    }

    @Override
    protected float calculateEnergyCost(KickState s) {
        float chargeSeconds = (s.chargeEndTick - s.chargeStartTick) / 20.0f;
        return Math.min(chargeSeconds * 200.0f, 2000.0f);
    }

    private float calculateEnergyCost(ServerPlayer serverPlayer, KickState s) {
        float chargeSeconds = (s.chargeEndTick - s.chargeStartTick) / 20.0f;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        float superEnergyMax = data != null ? data.getKuugaData().getSuperEnergyMax() : 2000.0f;
        float maxCost = superEnergyMax / 4.0f;
        return Math.min(chargeSeconds * 200.0f, maxCost);
    }

    private void spawnSpecialEffects(ServerPlayer serverPlayer) {
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) return;

        // 先清理旧的特效实体，防止孤儿实体累积
        cleanupBoundEffect();

        TransformationEffectEntity effectEntity = new TransformationEffectEntity(
                EntityRegistrationHandler.TRANS_FORMATION_EFFECT,
                serverLevel
        );

        effectEntity.moveTo(
                serverPlayer.getX() + Math.sin(Math.toRadians(serverPlayer.getYRot())) * 0.5,
                serverPlayer.getY(),
                serverPlayer.getZ() - Math.cos(Math.toRadians(serverPlayer.getYRot())) * 0.5,
                serverPlayer.getYRot(), 0
        );

        effectEntity.setModelLocation("thunder_yellow_ver_pro_special_effects");
        effectEntity.setAnimationName("idle");
        effectEntity.setTextureLocation("thunder_yellow_ver_pro_special_effects");

        effectEntity.setOwner(serverPlayer);

        // 设置兜底存活时间（600 ticks = 30秒），防止异常情况下实体永存
        effectEntity.setDeadTime(600);
        effectEntity.setDying(true);

        serverLevel.addFreshEntity(effectEntity);

        boundEffectEntity = effectEntity;
    }

    /** 清理绑定的特效实体 */
    private void cleanupBoundEffect() {
        if (boundEffectEntity != null) {
            if (boundEffectEntity.isAlive()) {
                boundEffectEntity.remove(Entity.RemovalReason.DISCARDED);
            }
            boundEffectEntity = null;
        }
    }

    private void startStorm(ServerLevel level) {
        level.setWeatherParameters(0, MAX_STORM_DURATION, true, true);
        isStormActive = true;
        stormDuration = 0;
    }

    private void endStorm(ServerLevel level) {
        level.setWeatherParameters(MAX_STORM_DURATION, 0, false, false);
        isStormActive = false;
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        return super.isAvailable(serverPlayer);
    }

    /* ---------- 状态重置 ---------- */
    @Override
    protected void resetSkillState(ServerPlayer player, KickState s) {
        cancelShockwaveTask();
        cleanupBoundEffect();
        super.resetSkillState(player, s);
    }
}