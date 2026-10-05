package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.riderKickUtil.JumpUtil;
import com.qianyunyi.rider.util.riderKickUtil.KickMotionUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AmazingMightyFormKuugaRiderKick extends KuugaBaseRiderKick {
    // 双踢动画常量
    protected static final String DOUBLE_KICK_UP_ANIM = "kuuga_rider_double_kick_up";
    protected static final String DOUBLE_KICK_FULL_CHARGE_ANIM = "kuuga_rider_double_kick_full_charge";
    protected static final String DOUBLE_KICK_ANIM = "kuuga_rider_double_kick_idle";
    protected static final String DOUBLE_LAND_ANIM = "kuuga_rider_double_kick_land";

    // 伤害分次施加参数
    private final Random random = new Random();
    private static final int DAMAGE_HITS = 5;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;
    private static final float HIT_MAGNIFICATION = 0.2f;

    @Override
    public String getDisplayName() {
        return "惊异全能踢";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    /* ---------- 主入口 ---------- */
    @Override
    public void skill(ServerPlayer serverPlayer) {
        super.skill(serverPlayer);
    }

    /* ---------- 蓄力阶段 ---------- */
    @Override
    protected void startCharging(ServerPlayer serverPlayer, KickState s) {
        super.startCharging(serverPlayer, s);
    }

    @Override
    protected void performRelease(ServerPlayer serverPlayer, KickState s) {
        serverPlayer.removeEffect(KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get());

        GroundShatterEffect.createCrackGroundEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                5.0, 100, 1.0f, 0
        );

        s.chargeEndTick = serverPlayer.level().getGameTime();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        data.setSkillActive(false);

        if (data.getKuugaData().isSomersault()) {
            HenshinUtil.sendAnimationMessage(serverPlayer, DOUBLE_KICK_FULL_CHARGE_ANIM);
            JumpUtil.jump(serverPlayer, 0, 30, 40, () -> {});
            DelayedTask.run(serverPlayer.level(), 30, () -> {
                performAirDash(serverPlayer, s);
                registerEvents(s);
            });
        } else {
            HenshinUtil.sendAnimationMessage(serverPlayer, DOUBLE_KICK_UP_ANIM);
            JumpUtil.jump(serverPlayer, 0, 30, 40, () -> {});
            DelayedTask.run(serverPlayer.level(), 22, () -> {
                performAirDash(serverPlayer, s);
                registerEvents(s);
            });
        }
    }

    @Override
    protected void performAirDash(ServerPlayer serverPlayer, KickState s) {
        double dashSpeed = calculateDashSpeed(s);
        HenshinUtil.sendAnimationMessage(serverPlayer, DOUBLE_KICK_ANIM);
        KickMotionUtil.dash(serverPlayer, dashSpeed, 200.0, 30.0, 300, () -> triggerLanding(serverPlayer, s));
    }

    @Override
    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {
        if (s.hasTriggeredLanding) return;
        HenshinUtil.sendAnimationMessage(serverPlayer, DOUBLE_LAND_ANIM);
        KickMotionUtil.stopMotion(serverPlayer);
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

    /** 消耗能量 */
    protected boolean deductEnergy(RiderPlayerData data, float amount, ServerPlayer serverPlayer) {
        float currentEnergy = data.getKuugaData().getSuperEnergy();
        if (currentEnergy < amount) {
            data.getKuugaData().removeSuperEnergy(currentEnergy);
            return false;
        }
        data.getKuugaData().removeSuperEnergy(amount);
        return true;
    }

    /* ---------- 目标查找 ---------- */
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

    /* ---------- 伤害计算 ---------- */
    @Override
    protected float calculateTotalDamage(ServerPlayer serverPlayer, Boolean isSomersault, KickState s) {
        float energyCost = calculateEnergyCost(s);
        float playerAttack = (float) serverPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float playerLevel = serverPlayer.experienceLevel;
        if (isSomersault) {
            return (playerLevel * 5.0f + energyCost * playerAttack * 0.1f) * 1.25f;
        }
        return playerLevel * 5.0f + energyCost * playerAttack * 0.1f;
    }



    /** 应用多段伤害 */
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

                // 每11次伤害播放爆炸音效
                if (finalI % 5== 0) {
                    serverPlayer.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                            0.5f, 0.9f + random.nextFloat() * 0.2f);
                }
            });
        }
    }

    /* ---------- Tick事件 ---------- */
    @Override
    public void onServerPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer sp)) return;
        KickState s = getState(sp);
        if (s == null || !s.isCharging) return;
        super.onServerPlayerTick(event);
    }

    /* ---------- 冷却检查 ---------- */
    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        return super.isAvailable(serverPlayer);
    }
}