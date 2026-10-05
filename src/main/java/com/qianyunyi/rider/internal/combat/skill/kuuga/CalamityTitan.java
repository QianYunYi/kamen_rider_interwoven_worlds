package com.qianyunyi.rider.internal.combat.skill.kuuga;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.RiderCompanionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CalamityTitan implements IRiderSkill {

    private static final String CALAMITY_TITAN = "kuuga_titan_from_calamity_titan";
    private static final String CALAMITY_TITAN_TWO = "kuuga_titan_from_two_calamity_titan";
    // 技能配置参数
    // 攻击范围（格数）
    private static final double ATTACK_RANGE = 3.5;
    // 攻击角度
    private static final float ATTACK_ANGLE = 90.0f;
    // 双持攻击角度（正面180度）
    private static final float DUAL_ATTACK_ANGLE = 180.0f;
    // 镇压标记持续时间（秒）
    private static final int SUPPRESS_DURATION = 1;
    // 镇压标记延迟（1秒 = 20刻）
    private static final int SUPPRESS_DELAY = 20;
    // 冷却时间（秒）
    private static final int COOLDOWN_SECONDS = 8;
    // 伤害倍率
    private static final float DAMAGE_MULTIPLIER = 5.0f;
    // 双持灾厄泰坦伤害倍率（原本灾厄泰坦的2倍）
    private static final float DUAL_DAMAGE_MULTIPLIER = 10.0f;
    // 双持出伤延迟（0.5秒 = 10刻）
    private static final int DUAL_DAMAGE_DELAY = 10;
    private long cooldownStartTick = 0;

    private static boolean isTitanSwordItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().equals(KamenRiderInterwovenWorldsModItems.TITAN_SWORD.get())
                || stack.getItem().equals(KamenRiderInterwovenWorldsModItems.RISING_TITAN_SWORD.get())
                || stack.getItem().equals(KamenRiderInterwovenWorldsModItems.ULTIMATE_TITAN_SWORD.get());
    }

    @Override
    public String getDisplayName() {
        return "泰坦斩击";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        // 判断玩家当前主手是否为泰坦剑
        ItemStack mainHandItem = serverPlayer.getMainHandItem();
        if (!isTitanSwordItem(mainHandItem)) {
            return;
        }
        // 判断副手：为空则为单手模式，为泰坦剑则为双持模式，否则拒绝
        ItemStack offHandItem = serverPlayer.getOffhandItem();
        boolean dualWield;
        if (offHandItem.isEmpty()) {
            dualWield = false;
        } else if (isTitanSwordItem(offHandItem)) {
            dualWield = true;
        } else {
            return;
        }

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;
        if (data.isSkillActive()) return;

        data.setSkillActive(true);
        cooldownStartTick = serverPlayer.level().getGameTime();

        String animation = dualWield ? CALAMITY_TITAN_TWO : CALAMITY_TITAN;
        int damageDelay = dualWield ? DUAL_DAMAGE_DELAY : 15;
        final float damageMultiplier = dualWield ? DUAL_DAMAGE_MULTIPLIER : DAMAGE_MULTIPLIER;
        final float attackAngle = dualWield ? DUAL_ATTACK_ANGLE : ATTACK_ANGLE;

        HenshinUtil.sendAnimationMessage(serverPlayer, animation);

        serverPlayer.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.7F);
        serverPlayer.playSound(SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.5F);

        if (!dualWield) {
            HenshinUtil.equipWeaponAndPlayAnimation(serverPlayer, EquipmentSlot.MAINHAND, "calamity_titan");
        }

        // 延迟指定刻执行斩击攻击
        final boolean isDualWield = dualWield;
        DelayedTask.run(serverPlayer.level(), damageDelay, () -> {
            performSlashAttack(serverPlayer, damageMultiplier, attackAngle, isDualWield);
        });

        // 延迟31刻恢复默认动作
        DelayedTask.run(serverPlayer.level(), 31, () -> {
            String defaultAnim = BaseSwordAnimationItem.getDefaultAnim(serverPlayer.getMainHandItem());
            HenshinUtil.equipWeaponAndPlayAnimation(serverPlayer, EquipmentSlot.MAINHAND, serverPlayer.getMainHandItem(), defaultAnim);
            data.setSkillActive(false);
        });
    }

    private void performSlashAttack(ServerPlayer serverPlayer, float damageMultiplier, float attackAngle, boolean dualWield) {
        Vec3 playerPos = serverPlayer.getEyePosition();
        Vec3 lookVec = serverPlayer.getLookAngle().normalize();

        // 计算攻击方向的前方向量
        Vec3 forward = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        if (forward.lengthSqr() < 0.0001) {
            forward = new Vec3(0, 0, 1);
        }

        // 计算攻击区域的AABB（以玩家为中心，覆盖整个攻击范围）
        AABB attackBox = new AABB(
                playerPos.x - ATTACK_RANGE,
                playerPos.y - 1.0,
                playerPos.z - ATTACK_RANGE,
                playerPos.x + ATTACK_RANGE,
                playerPos.y + 2.0,
                playerPos.z + ATTACK_RANGE
        );

        // 获取攻击范围内的所有实体
        Vec3 finalForward = forward;
        List<LivingEntity> targets = serverPlayer.level().getEntitiesOfClass(
                LivingEntity.class,
                attackBox,
                entity -> isInAttackCone(entity, playerPos, finalForward, attackAngle, ATTACK_RANGE)
                        && serverPlayer.hasLineOfSight(entity)
                        && !RiderCompanionUtil.isPlayerCompanion(entity, serverPlayer.getUUID())
        );

        // 获取玩家当前属性攻击力
        double baseAttack = serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        // 计算技能伤害 = 基础攻击力 * 伤害倍率
        float skillDamage = (float) (baseAttack * damageMultiplier);

        boolean hitAnyTarget = false;

        for (LivingEntity target : targets) {

            // 标记为爆炸性实体
            markAsExplosive(target, serverPlayer);

            // 延迟1秒给予镇压标记
            final LivingEntity suppressTarget = target;
            DelayedTask.run(serverPlayer.level(), SUPPRESS_DELAY, () -> {
                suppressTarget.addEffect(new MobEffectInstance(
                        KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                        SUPPRESS_DURATION * 20,
                        0,
                        false,
                        false
                ));
            });

            // 造成伤害，使用玩家属性攻击力*5
            DamageSources damageSources = serverPlayer.level().damageSources();
            DamageSource damageSource = damageSources.playerAttack(serverPlayer);
            target.hurt(damageSource, skillDamage);

            target.playSound(SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.8F);
            hitAnyTarget = true;

            // 如果目标是玩家，给予击退效果
            if (target instanceof Player) {
                Vec3 knockbackDir = target.position().subtract(serverPlayer.position()).normalize();
                target.push(knockbackDir.x * 0.8, 0.3, knockbackDir.z * 0.8);
            }
        }

        if (hitAnyTarget) {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    dualWield ? KamenRiderInterwovenWorldsModSounds.RISING_CALAMITI_STEP.get()
                            : KamenRiderInterwovenWorldsModSounds.TITAN_CALAMITI.get(),
                    SoundSource.PLAYERS, 1.5F, 1.0F);
        }
    }

    /**
     * 判断目标是否在攻击锥形范围内
     * @param entity 目标实体
     * @param playerPos 玩家位置
     * @param forward 攻击方向
     * @param angle 攻击角度
     * @param range 攻击范围
     * @return 是否在攻击范围内
     */
    private boolean isInAttackCone(LivingEntity entity, Vec3 playerPos, Vec3 forward, float angle, double range) {
        Vec3 toEntity = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(playerPos);
        double distance = toEntity.horizontalDistance();

        if (distance > range) return false;

        Vec3 toEntityXZ = new Vec3(toEntity.x, 0, toEntity.z).normalize();
        double dot = forward.dot(toEntityXZ);
        double maxAngle = Math.cos(Math.toRadians(angle * 0.5));

        return dot > maxAngle;
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        // 技能释放时立即执行，不需要额外处理
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        long currentTick = serverPlayer.level().getGameTime();
        long elapsed = (currentTick - cooldownStartTick) / 20;
        long remaining = COOLDOWN_SECONDS - elapsed;
        if (remaining > 0) {
            HenshinUtil.sendMessage(serverPlayer, "技能冷却中，剩余时间：" + remaining + "秒", ChatFormatting.BLUE);
            return false;
        }
        return true;
    }

    /**
     * 标记为爆炸性实体
     * @param target 目标实体
     * @param serverPlayer 施放技能的玩家
     */
    private void markAsExplosive(LivingEntity target, ServerPlayer serverPlayer) {
        target.getPersistentData().putBoolean("is_explosive", true);
        // 标记为灾厄泰坦触发的爆炸，用于区分其他爆炸源
        target.getPersistentData().putBoolean("is_calamity_titan_explosion", true);
        // 标记给予爆炸标记的玩家UUID
        target.getPersistentData().putString("explosion_player", serverPlayer.getUUID().toString());
    }
}