package com.qianyunyi.rider.entity.kaijin.gurongi.daguba;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import com.qianyunyi.rider.util.TeleportUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public class NDagubaZebaAttackGoal extends MeleeAttackGoal {
    private final NDagubaZebaEntity entity;

    private static final double TELEPORT_DISTANCE = 10.0;
    private static final double ATTACK_DISTANCE = 3.0;
    private static final double MIN_DISTANCE = 1.5;

    private static final int TELEPORT_COOLDOWN = 60;
    private static final int SKILL_CHECK_INTERVAL = 40;

    private int fastBoxingAttackCount = 0;
    private int lastAttackTick = 0;
    private int teleportCooldown = 0;
    private int skillCheckCooldown = 0;

    public NDagubaZebaAttackGoal(NDagubaZebaEntity entity) {
        super(entity, 1.2, false);
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!super.canUse() || entity.isStunned() || entity.isCastingSkill()) {
            return false;
        }
        LivingEntity target = entity.getTarget();
        return target != null && target.isAlive() && entity.canAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        if (entity.isStunned()) return false;
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return false;
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        fastBoxingAttackCount = 0;
        teleportCooldown = 0;
        skillCheckCooldown = 0;
        entity.setBossState(NDagubaZebaEntity.BossState.COMBAT);
    }

    @Override
    public void stop() {
        super.stop();
        fastBoxingAttackCount = 0;
    }

    @Override
    public void tick() {
        // 安全检查：确保实体在世界中，防止在客户端或实体移除后执行AI逻辑
        if (entity.isStunned() || entity.level() == null) return;

        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) return;

        updateRotationToTarget(target);

        if (entity.isCastingSkill()) return;

        // 冷却递减
        if (teleportCooldown > 0) teleportCooldown--;
        if (skillCheckCooldown > 0) skillCheckCooldown--;

        double distanceSqr = entity.distanceToSqr(target);
        double distance = Math.sqrt(distanceSqr);

        // 远程传送：距离过远时瞬移到目标附近
        if (distanceSqr > TELEPORT_DISTANCE * TELEPORT_DISTANCE && teleportCooldown <= 0) {
            teleportCooldown = TELEPORT_COOLDOWN;
            // 记录传送前位置，用于生成黑雾
            Vec3 oldPos = entity.position();
            entity.setAnimation("teleport");
            TeleportUtil.teleportTowardsTarget(entity, target);
            // 在原地生成黑雾效果
            if (entity.level() instanceof ServerLevel serverLevel) {
                SpecialEffectsUtil.spawnBlackMist(serverLevel, oldPos);
            }
            return; // 瞬移后本 tick 不再处理其他逻辑
        }

        // 在近战范围内，让父类 MeleeAttackGoal 处理寻路、攻击和跳跃
        if (distance <= ATTACK_DISTANCE + 2.0) {
            super.tick();
        } else if (distance <= TELEPORT_DISTANCE) {
            // 中等距离：使用导航逼近目标，寻路系统会自动处理跳跃
            var navigation = entity.getNavigation();
            if (navigation != null) {
                navigation.moveTo(target, 1.2);
            }
        }

        // 处理快速拳击连招的过期重置
        handleFastBoxingCombo(target);

        // 技能检查：不在近战范围内时，每间隔一段时间检查一次
        if (distance > ATTACK_DISTANCE + 1.0 && skillCheckCooldown <= 0) {
            skillCheckCooldown = SKILL_CHECK_INTERVAL;
            if (entity.getSkillManager().tryAutoExecute(target, distance)) {
                return;
            }
        }
    }

    @Override
    protected void checkAndPerformAttack(@Nonnull LivingEntity target, double distanceSqr) {
        if (entity.isCastingSkill()) return;
        if (!isFacingTarget(target)) return;

        double squaredAttackDistance = ATTACK_DISTANCE * ATTACK_DISTANCE;
        if (distanceSqr <= squaredAttackDistance && this.getTicksUntilNextAttack() <= 0) {
            executeRandomAttack(target);
            this.resetAttackCooldown();
        }
    }

    @Override
    protected double getAttackReachSqr(@Nonnull LivingEntity entity) {
        return ATTACK_DISTANCE * ATTACK_DISTANCE;
    }

    private void executeRandomAttack(LivingEntity target) {
        double distance = entity.distanceTo(target);

        // 20%概率触发快速拳击连招（仅当不在冷却中且距离较近时）
        if (fastBoxingAttackCount == 0 && distance <= ATTACK_DISTANCE && entity.getRandom().nextFloat() < 0.2f) {
            startFastBoxingCombo(target);
            return;
        }

        if (distance <= MIN_DISTANCE) {
            if (entity.getRandom().nextBoolean()) {
                executeKickAttack(target);
            } else {
                executePunchAttack(target);
            }
        } else if (distance <= ATTACK_DISTANCE) {
            if (entity.getRandom().nextFloat() < 0.3f) {
                executeKickAttack(target);
            } else {
                executePunchAttack(target);
            }
        }
    }

    private void startFastBoxingCombo(LivingEntity target) {
        entity.setAnimation("fast_boxing");
        fastBoxingAttackCount = 0;
        lastAttackTick = entity.tickCount;
        entity.setUsingSkill(true);
        applyMovementLock();
        scheduleFastBoxingAttack(target, 0);
    }

    private void handleFastBoxingCombo(LivingEntity target) {
        if (fastBoxingAttackCount == 0) return;
        if (entity.tickCount - lastAttackTick > 20) {
            fastBoxingAttackCount = 0;
            entity.setUsingSkill(false);
        }
    }

    private void scheduleFastBoxingAttack(LivingEntity target, int hitIndex) {
        int[] attackDelays = {5, 8, 10, 13};

        if (hitIndex >= attackDelays.length) {
            DelayedTask.run(entity.level(), 5, () -> {
                if (isTargetValid(target)) executeKickAttack(target);
                fastBoxingAttackCount = 0;
                entity.setUsingSkill(false);
            });
            return;
        }

        int delay = attackDelays[hitIndex];
        DelayedTask.run(entity.level(), delay, () -> {
            if (!isTargetValid(target)) {
                fastBoxingAttackCount = 0;
                entity.setUsingSkill(false);
                return;
            }
            updateRotationToTarget(target);
            target.hurt(entity.damageSources().mobAttack(entity), (float) (entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2));
            playStrikeSound();
            addParticleEffect(ParticleTypes.SWEEP_ATTACK, target);
            reduceArmorDurability(target);
            fastBoxingAttackCount = hitIndex + 1;
            lastAttackTick = entity.tickCount;
            scheduleFastBoxingAttack(target, hitIndex + 1);
        });
    }

    public void executeBackTeleportAndFastBoxing(LivingEntity target) {
        teleportBehindTarget(target);
        updateRotationToTarget(target);
        startFastBoxingCombo(target);
    }

    private void executePunchAttack(LivingEntity target) {
        entity.setAnimation("attack");
        applyMovementLock();

        DelayedTask.run(entity.level(), 10, () -> {
            if (isTargetValid(target)) {
                updateRotationToTarget(target);
                target.hurt(entity.damageSources().mobAttack(entity), (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
                playStrikeSound();
                addParticleEffect(ParticleTypes.CRIT, target);
            }
        });

        DelayedTask.run(entity.level(), 15, () -> {
            if (isTargetValid(target)) {
                updateRotationToTarget(target);
                target.hurt(entity.damageSources().mobAttack(entity), (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE));
                playStrikeSound();
                addParticleEffect(ParticleTypes.CRIT, target);
            }
        });
    }

    private void executeKickAttack(LivingEntity target) {
        entity.setAnimation("attack_kick");
        applyMovementLock();

        DelayedTask.run(entity.level(), 10, () -> {
            if (isTargetValid(target)) {
                updateRotationToTarget(target);
                target.hurt(entity.damageSources().mobAttack(entity), (float) (entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5));
                playStrikeSound();
                addParticleEffect(ParticleTypes.EXPLOSION, target);
                if (target.isAlive()) {
                    Vec3 knockback = entity.getLookAngle().normalize().scale(2.0);
                    target.push(knockback.x, 0.5, knockback.z);
                }
            }
        });
    }

    private boolean isTargetValid(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && entity.isAlive()
                && !entity.isStunned()
                && entity.distanceToSqr(target) <= (ATTACK_DISTANCE + 2.0) * (ATTACK_DISTANCE + 2.0);
    }

    private void playStrikeSound() {
        float pitch = 1.0F + (entity.level().getRandom().nextFloat() - 0.5F) * 0.2F;
        entity.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, pitch);
    }

    private void addParticleEffect(net.minecraft.core.particles.ParticleOptions particle, LivingEntity target) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);

        if (particle == ParticleTypes.SWEEP_ATTACK) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double offsetX = Math.cos(angle) * 0.5;
                double offsetZ = Math.sin(angle) * 0.5;
                serverLevel.sendParticles(particle, pos.x + offsetX, pos.y, pos.z + offsetZ, 1, 0.1, 0.1, 0.1, 0.05);
            }
        } else {
            serverLevel.sendParticles(particle, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void reduceArmorDurability(LivingEntity target) {
        if (target instanceof Player player) {
            for (ItemStack stack : player.getInventory().armor) {
                if (!stack.isEmpty() && stack.isDamageableItem()) {
                    stack.hurt(100, entity.getRandom(), null);
                }
            }
        }
    }

    private void updateRotationToTarget(LivingEntity target) {
        if (target == null) return;
        entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void teleportBehindTarget(LivingEntity target) {
        if (target == null) return;
        // 记录传送前位置
        Vec3 oldPos = entity.position();
        Vec3 lookVec = target.getLookAngle().normalize();
        Vec3 behindPos = target.position().subtract(lookVec.scale(2));
        behindPos = new Vec3(behindPos.x, target.getY(), behindPos.z);
        entity.setAnimation("teleport");
        TeleportUtil.teleportTo(entity, behindPos.x, behindPos.y, behindPos.z);
        // 在原地生成黑雾效果
        if (entity.level() instanceof ServerLevel serverLevel) {
            SpecialEffectsUtil.spawnBlackMist(serverLevel, oldPos);
        }
    }

    private boolean isFacingTarget(LivingEntity target) {
        Vec3 lookVec = entity.getLookAngle().normalize();
        Vec3 toTarget = target.position().subtract(entity.position()).normalize();
        return lookVec.dot(toTarget) > 0;
    }

    private void applyMovementLock() {
        entity.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.MOVEMENT_LOCK.get(), 20, 0, false, false));
    }
}