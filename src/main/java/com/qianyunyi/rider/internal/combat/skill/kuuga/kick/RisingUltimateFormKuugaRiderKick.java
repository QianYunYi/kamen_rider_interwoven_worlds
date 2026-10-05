package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.entity.effect.TransformationEffectEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RisingUltimateFormKuugaRiderKick extends KuugaBaseRiderKick {

    // 伤害分次施加参数
    private final Random random = new Random();
    private static final int DAMAGE_HITS = 15;
    private static final float DAMAGE_VARIATION = 0.15f;
    private static final int HIT_DELAY_TICKS = 1;
    private static final float HIT_MAGNIFICATION = 0.06f;

    private final SimpleParticleType lightningParticle = (SimpleParticleType) KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get();
    private static final SoundEvent currentSound = KamenRiderInterwovenWorldsModSounds.CURRENT.get();
    // 绑定特效实体
    private TransformationEffectEntity boundEffectEntity = null;

    @Override
    public String getDisplayName() {
        return "升华究极踢";
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
        spawnSpecialEffects(serverPlayer);
        super.performAirDash(serverPlayer, s);
    }

    @Override
    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {
        cleanupBoundEffect();

        createShockwaveEffects(serverPlayer, s);

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

    /** 消耗能量 */
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
                if (finalI % 2 == 0) {
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

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                currentSound, SoundSource.PLAYERS, 1.0F, 1.0F);

        sp.level().addParticle(
                lightningParticle,
                sp.getX(), sp.getY() - 0.5, sp.getZ(),
                1, 0, 0.02
        );
        super.onServerPlayerTick(event);
    }


    /* ---------- 升华究极特有方法 ---------- */


    /** 生成绑定特效实体 */
    private void spawnSpecialEffects(ServerPlayer serverPlayer) {
        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) return;

        // 先清理旧的特效实体，防止孤儿实体累积
        cleanupBoundEffect();

        // 获取 EntityType
        TransformationEffectEntity effectEntity = new TransformationEffectEntity(
                EntityRegistrationHandler.TRANS_FORMATION_EFFECT,
                serverLevel
        );

        // 设置位置为玩家位置
        effectEntity.moveTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYRot(), 0);

        // 设置特效属性
        effectEntity.setModelLocation("thunder_yellow_ver_pro_special_effects"); // 设置模型路径
        effectEntity.setAnimationName("idle"); // 设置要播放的动画名称
        effectEntity.setTextureLocation("thunder_yellow_ver_pro_special_effects"); // 设置纹理路径

        // 绑定玩家
        effectEntity.setOwner(serverPlayer);

        // 设置兜底存活时间（600 ticks = 30秒），防止异常情况下实体永存
        effectEntity.setDeadTime(600);
        effectEntity.setDying(true);

        // 添加实体到世界
        serverLevel.addFreshEntity(effectEntity);

        // 保存引用以便后续移除
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

    /** 创建冲击波特效 */
    private void createShockwaveEffects(ServerPlayer serverPlayer, KickState s) {
        float chargeSeconds = (s.chargeEndTick - s.chargeStartTick) / 20.0f;

        SpecialEffectsUtil.createCircularShockwave(
                (ServerLevel) serverPlayer.level(),
                serverPlayer,
                5.0f + chargeSeconds,
                1.5f,
                100,
                30
        );

        GroundShatterEffect.createCrackGroundEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                chargeSeconds,
                100,
                1.0f,
                0
        );

        DelayedTask.run(serverPlayer.level(), 20, () -> {
            GroundShatterEffect.createFlyUpEffect(
                    serverPlayer.level(),
                    serverPlayer.position(),
                    5.0f + chargeSeconds,
                    40,
                    0.3f,
                    0,
                    serverPlayer
            );
        });
    }




    /* ---------- 冷却检查 ---------- */
    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        return super.isAvailable(serverPlayer);
    }

    /* ---------- 状态重置 ---------- */
    @Override
    protected void resetSkillState(ServerPlayer player, KickState s) {
        cleanupBoundEffect();
        super.resetSkillState(player, s);
    }
}