package com.qianyunyi.rider.entity.kaijin.gurongi.daguba;

import com.qianyunyi.rider.util.SpecialEffectsUtil;
import com.qianyunyi.rider.util.TeleportUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

public class DagubaDamageHandler {

    private static final int BASE_DAMAGE_TELEPORT_COOLDOWN = 120;
    private static final int MIN_DAMAGE_TELEPORT_COOLDOWN = 60;
    private static final int MAX_DAMAGE_TELEPORT_COOLDOWN = 300;
    private static final int DAMAGE_THRESHOLD = 60;
    private static final int BREAK_DURATION = 40;

    public static boolean handleDamage(NDagubaZebaEntity entity, DamageSource source, float amount) {
        if (entity.isStunned()) {
            return entity.superHurt(source, amount);
        }

        float minDamage = entity.getMaxHealth() * 0.005f;
        if (amount < minDamage) {
            return false;
        }

        if (source.is(DamageTypeTags.IS_FALL) ||
                source.is(DamageTypeTags.IS_DROWNING) ||
                source.is(DamageTypeTags.IS_FREEZING)) {
            return false;
        }

        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return entity.superHurt(source, amount);
        }

        float maxDamage = entity.getMaxHealth() * 0.08f;
        if (amount > maxDamage) {
            amount = maxDamage;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 0.5f;
        }

        entity.incrementDamageCount();

        if (entity.getDamageCount() >= DAMAGE_THRESHOLD) {
            triggerBreakThrough(entity);
            return false;
        }

        if (source.getDirectEntity() instanceof AbstractArrow) {
            if (entity.getDamageTeleportCooldown() <= 0) {
                int cooldown = calculateTeleportCooldown(entity, source);
                entity.setDamageTeleportCooldown(cooldown);
                Vec3 oldPos = entity.position();
                entity.setAnimation("teleport");
                TeleportUtil.teleportShortDistance(entity);
                spawnBlackMistAt(entity, oldPos);
            }
            return false;
        }

        if (entity.getDamageTeleportCooldown() <= 0) {
            int cooldown = calculateTeleportCooldown(entity, source);
            entity.setDamageTeleportCooldown(cooldown);
            Vec3 oldPos = entity.position();
            entity.setAnimation("teleport");
            TeleportUtil.teleportShortDistance(entity);
            spawnBlackMistAt(entity, oldPos);
            return false;
        }

        return entity.superHurt(source, amount);
    }

    private static void triggerBreakThrough(NDagubaZebaEntity entity) {
        entity.resetDamageCount();
        entity.getSkillManager().resetAllCooldowns();
        entity.setAnimation("break_through");
        entity.setBossState(NDagubaZebaEntity.BossState.STUNNED);
        entity.setBreakDuration(BREAK_DURATION);
    }

    /**
     * 在指定位置生成黑雾效果（瞬移离场特效）
     */
    private static void spawnBlackMistAt(NDagubaZebaEntity entity, Vec3 pos) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            SpecialEffectsUtil.spawnBlackMist(serverLevel, pos);
        }
    }

    private static int calculateTeleportCooldown(NDagubaZebaEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        double entitySpeed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double attackerSpeed = 0.0;

        if (attacker instanceof LivingEntity) {
            attackerSpeed = ((LivingEntity) attacker).getAttributeValue(Attributes.MOVEMENT_SPEED);
        }

        double speedRatio = attackerSpeed > 0 ? entitySpeed / attackerSpeed : 1.0;

        if (speedRatio > 1.5) {
            return MIN_DAMAGE_TELEPORT_COOLDOWN;
        } else if (speedRatio > 1.0) {
            return (int) (BASE_DAMAGE_TELEPORT_COOLDOWN * 0.5);
        } else if (speedRatio > 0.5) {
            return BASE_DAMAGE_TELEPORT_COOLDOWN;
        } else {
            return MAX_DAMAGE_TELEPORT_COOLDOWN;
        }
    }

}