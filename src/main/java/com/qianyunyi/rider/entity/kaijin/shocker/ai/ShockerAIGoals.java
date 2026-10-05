package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 修卡通用AI：所有子类共用，无冗余
public class ShockerAIGoals {

    // 被攻击 → 不反击友军 + 不攻击首领玩家
    public static class ShockerHurtByTargetGoal extends HurtByTargetGoal {
        private final ShockerBase shocker;

        public ShockerHurtByTargetGoal(ShockerBase mob) {
            super(mob);
            this.shocker = mob;
        }

        // 核心修复：必须重写 canAttack
        @Override
        protected boolean canAttack(@Nullable LivingEntity target, @NotNull TargetingConditions conditions) {
            // 玩家是首领 → 绝对不攻击
//            if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                return false;
//            }

            // 修卡友军 → 不攻击
            if (target instanceof ShockerBase other) {
                return !ShockerOrg.areFriendly(shocker, other);
            }

            return super.canAttack(target, conditions);
        }

        // 关键修复：被首领打了也不反击
        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
//            if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                this.mob.setTarget(null);
//                return false;
//            }
            return super.canContinueToUse();
        }
    }

    // 主动攻击 → 不打友军 + 不打首领玩家
    public static class ShockerAttackGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final ShockerBase shocker;

        public ShockerAttackGoal(ShockerBase mob, Class<T> cls) {
            super(mob, cls, true);
            this.shocker = mob;
        }

        // 核心修复
        @Override
        protected boolean canAttack(@Nullable LivingEntity target, @NotNull TargetingConditions conditions) {
            // 玩家是首领 → 不攻击
//            if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                return false;
//            }

            // 修卡友军 → 不攻击
            if (target instanceof ShockerBase other) {
                return !ShockerOrg.areFriendly(shocker, other);
            }

            return super.canAttack(target, conditions);
        }

        // 关键修复：持续目标检查
        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
//            if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                this.mob.setTarget(null);
//                return false;
//            }
            return super.canContinueToUse();
        }
    }
}