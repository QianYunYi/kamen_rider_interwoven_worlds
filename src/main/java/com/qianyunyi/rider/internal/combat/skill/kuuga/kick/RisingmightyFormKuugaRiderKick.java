package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RisingmightyFormKuugaRiderKick extends KuugaBaseRiderKick {

    private final Random random = new Random();
    private static final int DAMAGE_HITS = 5;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;
    private static final float HIT_MAGNIFICATION = 0.2f;

    @Override
    public String getDisplayName() {
        return "升华全能踢";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }


    /* ---------- 主入口 ---------- */
    @Override
    public void skill(ServerPlayer serverPlayer) {
        // 开始蓄力
        super.skill(serverPlayer); // 调用父类基础蓄力逻辑
    }

    /* ---------- 蓄力阶段 ---------- */
    @Override
    protected void startCharging(ServerPlayer serverPlayer, KickState s) {
        super.startCharging(serverPlayer, s);

        serverPlayer.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(),
                200, 1, false, false
        ));
    }

    @Override
    protected void performRelease(ServerPlayer serverPlayer, KickState s) {
        serverPlayer.removeEffect(KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get());

        GroundShatterEffect.createCrackGroundEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                5.0, 100, 1.0f, 0
        );

        super.performRelease(serverPlayer, s);
    }

    @Override
    protected void performAirDash(ServerPlayer serverPlayer, KickState s) {
        super.performAirDash(serverPlayer, s);
    }

    @Override
    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {
        super.triggerLanding(serverPlayer, s);
    }

    /* ---------- 目标查找 ---------- */
    @Override
    protected List<LivingEntity> findTargetEntities(ServerPlayer serverPlayer) {
        TargetingConditions cond = TargetingConditions.forNonCombat()
                .range(15.0f)
                .selector(e -> e != serverPlayer && !isPlayerPet(e, serverPlayer) );

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
            return ( playerLevel * 5.0f + energyCost * playerAttack * 0.1f)*1.25f;
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
                if (finalI % 11 == 0) {
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