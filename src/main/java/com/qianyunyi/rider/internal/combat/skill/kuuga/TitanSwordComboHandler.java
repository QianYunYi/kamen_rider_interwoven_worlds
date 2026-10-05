package com.qianyunyi.rider.internal.combat.skill.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TitanSwordComboHandler {

    private static final String[] COMBO_ANIMS = {
            "kuuga_titan_attack_1",
            "kuuga_titan_attack_2",
            "kuuga_titan_attack_3",
            "kuuga_titan_attack_4"
    };

    private static final String[] DUAL_COMBO_ANIMS = {
            "kuuga_two_titan_attack_1",
            "kuuga_two_titan_attack_2",
            "kuuga_two_titan_attack_3"
    };

    // 双持第三段伤害倍率
    private static final float DUAL_FINISHER_MULTIPLIER = 1.5F;

    private static final int ANIM_DURATION = 15;
    private static final int COMBO_WINDOW = 20;
    private static final int DAMAGE_DELAY = 7;
    // 双持出伤延迟（0.5秒 = 10刻）
    private static final int DUAL_DAMAGE_DELAY = 10;
    // 平A音效延迟（0.5秒 = 10刻）
    private static final int SWING_SOUND_DELAY = 10;

    private static final double SWEEP_RANGE = 3.0;
    private static final double CONE_RANGE = 3.5;
    private static final float SWEEP_ANGLE = 180.0F;
    private static final float CONE_ANGLE = 90.0F;
    private static final float KNOCKBACK_STRENGTH = 0.6F;

    private static final Map<UUID, Integer> comboState = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAttackTick = new ConcurrentHashMap<>();

    private static boolean isTitanSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().equals(KamenRiderInterwovenWorldsModItems.TITAN_SWORD.get())
                || stack.getItem().equals(KamenRiderInterwovenWorldsModItems.RISING_TITAN_SWORD.get())
                || stack.getItem().equals(KamenRiderInterwovenWorldsModItems.ULTIMATE_TITAN_SWORD.get());
    }

    public static boolean isDualWielding(LivingEntity player) {
        return isTitanSword(player.getMainHandItem()) && isTitanSword(player.getOffhandItem());
    }

    public static boolean isHoldingTitanSword(LivingEntity player) {
        return isTitanSword(player.getMainHandItem());
    }

    public static boolean canExecute(ServerPlayer player) {
        if (!isHoldingTitanSword(player)) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: NOT holding TitanSword");
            return false;
        }
        // 副手非空时必须也是泰坦剑（双持模式），否则拒绝执行
        if (!player.getOffhandItem().isEmpty() && !isTitanSword(player.getOffhandItem())) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: offhand is not a TitanSword");
            return false;
        }
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: player data is null");
            return false;
        }
        if (data.isSkillActive()) {
            long currentTick = player.level().getGameTime();
            Long lastTick = lastAttackTick.get(player.getUUID());
            if (lastTick == null || (currentTick - lastTick) > ANIM_DURATION + COMBO_WINDOW) {
                KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: skillActive was stuck, resetting");
                data.setSkillActive(false);
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: skill is active");
                return false;
            }
        }
        long currentTick = player.level().getGameTime();
        Long lastTick = lastAttackTick.get(player.getUUID());
        boolean canExec = lastTick == null || (currentTick - lastTick) >= ANIM_DURATION;
        KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] canExecute: currentTick={}, lastTick={}, canExec={}",
                currentTick, lastTick, canExec);
        return canExec;
    }

    public static void executeCombo(ServerPlayer serverPlayer) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] executeCombo called for player {}", serverPlayer.getName().getString());
        if (!canExecute(serverPlayer)) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] executeCombo: canExecute returned false");
            return;
        }

        ItemStack mainHandItem = serverPlayer.getMainHandItem();
        if (!isTitanSword(mainHandItem)) {
            return;
        }

        ItemStack offHandItem = serverPlayer.getOffhandItem();
        if (!offHandItem.isEmpty() && !isTitanSword(offHandItem)) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;

        boolean dualWield = isDualWielding(serverPlayer);
        String[] anims = dualWield ? DUAL_COMBO_ANIMS : COMBO_ANIMS;
        int animLength = anims.length;

        long currentTick = serverPlayer.level().getGameTime();
        UUID playerId = serverPlayer.getUUID();

        Long lastTick = lastAttackTick.get(playerId);
        if (lastTick != null && (currentTick - lastTick) < ANIM_DURATION) return;

        int currentCombo = comboState.getOrDefault(playerId, 0);
        if (lastTick != null && (currentTick - lastTick) > ANIM_DURATION + COMBO_WINDOW) {
            currentCombo = 0;
        }

        if (currentCombo >= animLength) {
            currentCombo = 0;
        }

        String animName = anims[currentCombo];
        int comboIndex = currentCombo;

        comboState.put(playerId, currentCombo + 1);
        lastAttackTick.put(playerId, currentTick);

        data.setSkillActive(true);

        HenshinUtil.sendAnimationMessage(serverPlayer, animName);

        serverPlayer.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);

        int damageDelay = dualWield ? DUAL_DAMAGE_DELAY : DAMAGE_DELAY;
        DelayedTask.run(serverPlayer.level(), damageDelay, () -> applyComboDamage(serverPlayer, comboIndex, dualWield));

        // 平A音效在0.5秒后播放
        DelayedTask.run(serverPlayer.level(), SWING_SOUND_DELAY, () -> serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                KamenRiderInterwovenWorldsModSounds.ABILITY_CHANGE_OR_LIGHT_ATTAK.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F));

        DelayedTask.run(serverPlayer.level(), ANIM_DURATION, () -> {
            String defaultAnim = BaseSwordAnimationItem.getDefaultAnim(serverPlayer.getMainHandItem());
            HenshinUtil.equipWeaponAndPlayAnimation(serverPlayer, EquipmentSlot.MAINHAND, serverPlayer.getMainHandItem(), defaultAnim);
            if (dualWield) {
                String offhandDefaultAnim = BaseSwordAnimationItem.getDefaultAnim(serverPlayer.getOffhandItem());
                HenshinUtil.equipWeaponAndPlayAnimation(serverPlayer, EquipmentSlot.OFFHAND, serverPlayer.getOffhandItem(), offhandDefaultAnim);
            }
            data.setSkillActive(false);
        });
    }

    private static void applyComboDamage(ServerPlayer serverPlayer, int comboIndex, boolean dualWield) {
        double baseAttack = serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        DamageSources damageSources = serverPlayer.level().damageSources();
        DamageSource damageSource = damageSources.playerAttack(serverPlayer);

        Vec3 playerPos = serverPlayer.getEyePosition();
        Vec3 lookVec = serverPlayer.getLookAngle();
        Vec3 forward = new Vec3(lookVec.x, 0, lookVec.z).normalize();

        if (forward.lengthSqr() < 0.0001) {
            forward = new Vec3(0, 0, 1);
        }

        if (dualWield) {
            switch (comboIndex) {
                case 0:
                    applySweepDamage(serverPlayer, playerPos, forward, baseAttack, damageSource);
                    break;
                case 1:
                    applySweepDamage(serverPlayer, playerPos, forward, baseAttack, damageSource);
                    break;
                case 2:
                    // 双持第三段造成1.5倍伤害，前方180度全部命中
                    applyConeDamage(serverPlayer, playerPos, forward,
                            baseAttack * DUAL_FINISHER_MULTIPLIER, damageSource, true, 180.0F);
                    break;
            }
            return;
        }

        switch (comboIndex) {
            case 0:
            case 2:
                applySweepDamage(serverPlayer, playerPos, forward, baseAttack, damageSource);
                break;
            case 1:
            case 3:
                applyConeDamage(serverPlayer, playerPos, forward, baseAttack * 1.2F, damageSource, true);
                break;
        }
    }

    private static void applySweepDamage(ServerPlayer serverPlayer, Vec3 playerPos, Vec3 forward,
                                         double baseAttack, DamageSource damageSource) {
        AABB sweepBox = new AABB(
                playerPos.x - SWEEP_RANGE, playerPos.y - 1.0, playerPos.z - SWEEP_RANGE,
                playerPos.x + SWEEP_RANGE, playerPos.y + 2.0, playerPos.z + SWEEP_RANGE
        );

        List<LivingEntity> targets = serverPlayer.level().getEntitiesOfClass(
                LivingEntity.class,
                sweepBox,
                entity -> entity != serverPlayer
                        && isInAngleCone(entity, playerPos, forward, SWEEP_ANGLE, SWEEP_RANGE)
                        && serverPlayer.hasLineOfSight(entity)
        );

        float damage = (float) (baseAttack * 1.0F);
        for (LivingEntity target : targets) {
            target.hurt(damageSource, damage);
            target.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.9F);
        }

        if (!targets.isEmpty()) {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.TITAN_SWORD_STANDARD_ATTACK.get(),
                    SoundSource.PLAYERS, 1.0F, 0.9F + serverPlayer.level().random.nextFloat() * 0.2F);
        }
    }

    private static void applyConeDamage(ServerPlayer serverPlayer, Vec3 playerPos, Vec3 forward,
                                        double baseAttack, DamageSource damageSource, boolean withKnockback) {
        applyConeDamage(serverPlayer, playerPos, forward, baseAttack, damageSource, withKnockback, CONE_ANGLE);
    }

    private static void applyConeDamage(ServerPlayer serverPlayer, Vec3 playerPos, Vec3 forward,
                                        double baseAttack, DamageSource damageSource, boolean withKnockback, float coneAngle) {
        AABB coneBox = new AABB(
                playerPos.x - CONE_RANGE, playerPos.y - 1.0, playerPos.z - CONE_RANGE,
                playerPos.x + CONE_RANGE, playerPos.y + 2.0, playerPos.z + CONE_RANGE
        );

        List<LivingEntity> targets = serverPlayer.level().getEntitiesOfClass(
                LivingEntity.class,
                coneBox,
                entity -> entity != serverPlayer
                        && isInAngleCone(entity, playerPos, forward, coneAngle, CONE_RANGE)
                        && serverPlayer.hasLineOfSight(entity)
        );

        float damage = (float) baseAttack;
        for (LivingEntity target : targets) {
            target.hurt(damageSource, damage);
            target.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 0.9F);

            if (withKnockback) {
                Vec3 knockbackDir = target.position().subtract(serverPlayer.position()).normalize();
                target.push(knockbackDir.x * KNOCKBACK_STRENGTH, 0.2, knockbackDir.z * KNOCKBACK_STRENGTH);
            }
        }

        if (!targets.isEmpty()) {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.TITAN_SWORD_STANDARD_ATTACK.get(),
                    SoundSource.PLAYERS, 1.0F, 0.9F + serverPlayer.level().random.nextFloat() * 0.2F);
            serverPlayer.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        }
    }

    private static boolean isInAngleCone(LivingEntity entity, Vec3 playerPos, Vec3 forward,
                                         float angle, double range) {
        Vec3 toEntity = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(playerPos);
        double distance = toEntity.horizontalDistance();
        if (distance > range) return false;

        Vec3 toEntityXZ = new Vec3(toEntity.x, 0, toEntity.z).normalize();
        double dot = forward.dot(toEntityXZ);
        double maxAngle = Math.cos(Math.toRadians(angle * 0.5));
        return dot > maxAngle;
    }

    public static int getCurrentCombo(UUID playerId) {
        return comboState.getOrDefault(playerId, 0);
    }
}