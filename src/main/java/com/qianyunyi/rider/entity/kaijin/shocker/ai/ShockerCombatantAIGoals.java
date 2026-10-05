package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerCombatantBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

public class ShockerCombatantAIGoals extends ShockerAIGoals {

    // 跟随队长
    public static class FollowLeaderGoal extends Goal {
        private final ShockerCombatantBase follower;
        private ShockerCombatantBase leader;
        private int cooldown;

        public FollowLeaderGoal(ShockerCombatantBase mob) {
            this.follower = mob;
        }

        /**
         * 检查是否可以使用该目标
         * @return 如果可以使用该目标，则返回true；否则返回false
         */
        @Override
        public boolean canUse() {
            if (follower.getRank() == ShockerRank.Rank.SQUAD_LEADER) return false;
            if (follower.getTarget() != null) return false;
            // 检查是否有队长
            List<ShockerCombatantBase> leaders = follower.level().getEntitiesOfClass(
                    ShockerCombatantBase.class, follower.getBoundingBox().inflate(30),
                    e -> e != follower && e.isAlive() && e.isSquadLeader()
                            && e.getSquadMembers().contains(follower.getUUID())
            );

            if (leaders.isEmpty()) return false;
            leader = leaders.get(0);
            return leader.distanceToSqr(follower) > 16;
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && follower.getTarget() == null
                    && leader.distanceToSqr(follower) > 9;
        }

        @Override
        public void tick() {
            if (cooldown-- <= 0 && leader != null) {
                follower.getNavigation().moveTo(leader, 1.0D);
                cooldown = 20 + follower.getRandom().nextInt(20);
            }
        }
    }

    // 指令
    public static class SquadCommandGoal extends Goal {
        private final ShockerCombatantBase leader;
        private int cooldown;

        public SquadCommandGoal(ShockerCombatantBase leader) {
            this.leader = leader;
        }

        @Override
        public boolean canUse() {
            return cooldown <= 0 && leader.isSquadCommander();
        }

        @Override
        public void start() {
            ShockerCombatantSquadAI.issueCommand(leader);
            cooldown = 100 + leader.getRandom().nextInt(100);
        }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
    }

    // 激励
    public static class SquadInspireGoal extends Goal {
        private final ShockerCombatantBase leader;
        private int cooldown;

        public SquadInspireGoal(ShockerCombatantBase leader) {
            this.leader = leader;
        }

        @Override
        public boolean canUse() {
            return cooldown <= 0 && leader.isSquadCommander();
        }

        @Override
        public void start() {
            ShockerCombatantSquadAI.inspireSquad(leader);
            cooldown = 300 + leader.getRandom().nextInt(200);
        }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
    }

    // 阵型
    public static class SquadReformGoal extends Goal {
        private final ShockerCombatantBase leader;
        private int cooldown;

        public SquadReformGoal(ShockerCombatantBase leader) {
            this.leader = leader;
        }

        @Override
        public boolean canUse() {
            return cooldown <= 0 && leader.isSquadCommander() && leader.getSquadSize() >= 3;
        }

        @Override
        public void start() {
            ShockerCombatantSquadAI.reformFormation(leader);
            cooldown = 400 + leader.getRandom().nextInt(200);
        }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
        }
    }
}