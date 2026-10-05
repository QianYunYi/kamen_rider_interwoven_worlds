package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;
import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerCombatantBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.PlayMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 干部通用AI系统
 * 设计原则：
 * 1. 干部指挥"小队长"层级，不直接管理普通战斗员
 * 2. 小队长通过现有的 ShockerCombatantSquadAI 管理自己的队员
 * 3. 干部提供全局增益、战略指令、召唤增援
 */
public class ShockerCadreAI extends ShockerAIGoals {

    // ==================== 干部专用目标选择器 ====================

    /**
     * 干部被攻击时的目标选择
     * 与战斗员的区别：干部不会呼叫增援（那是战斗员的行为），而是触发指挥系统
     */
    public static class CadreHurtByTargetGoal extends ShockerHurtByTargetGoal {
        private final ShockerBase cadre;

        public CadreHurtByTargetGoal(ShockerBase mob) {
            super(mob);
            this.cadre = mob;
        }

        @Override
        public void start() {
            super.start();
            // 被攻击时自动指挥附近小队长集火攻击者
            if (cadre.getRank() == ShockerRank.Rank.CADRE) {
                CommandSystem.issueFocusFire(cadre, this.mob.getTarget());
            }
        }
    }

    /**
     * 干部主动攻击目标选择
     * 范围更大，优先选择玩家
     */
    public static class CadreAttackGoal<T extends LivingEntity> extends ShockerAttackGoal<T> {
        private final ShockerBase cadre;

        public CadreAttackGoal(ShockerBase mob, Class<T> cls) {
            super(mob, cls);
            this.cadre = mob;
        }

        @Override
        protected double getFollowDistance() {
            // 干部索敌范围比战斗员大50%
            return super.getFollowDistance() * 1.5;
        }
    }

    // ==================== 指挥系统 (CommandSystem) ====================
    /**
     * 为什么干部指挥小队长而不是战斗员？
     * 1. 层级清晰：干部→小队长→队员，符合组织架构
     * 2. 性能优化：干部只需扫描附近小队长（通常1-3个），而非所有战斗员（可能10+个）
     * 3. 职责分离：小队长负责战术执行（走位、阵型），干部负责战略目标
     * 4. 代码复用：直接复用 ShockerCombatantSquadAI 的小队管理逻辑
     */
    public static class CommandSystem {

        /**
         * 获取干部指挥范围内的所有小队长
         * @param cadre 干部实体
         * @param range 扫描范围（默认32格）
         */
        public static List<ShockerCombatantBase> getNearbySquadLeaders(ShockerBase cadre, double range) {
            List<ShockerCombatantBase> leaders = new ArrayList<>();
            AABB area = cadre.getBoundingBox().inflate(range);

            for (ShockerCombatantBase entity : cadre.level().getEntitiesOfClass(ShockerCombatantBase.class, area)) {
                if (entity != cadre
                        && entity.isAlive()
                        && entity.isSquadLeader()
                        && ShockerOrg.areFriendly(cadre, entity)) {
                    leaders.add(entity);
                }
            }
            return leaders;
        }

        /**
         * 获取干部指挥范围内的所有战斗员（包括小队长）
         * 用于全局增益类技能
         */
        public static List<ShockerCombatantBase> getAllSubordinates(ShockerBase cadre, double range) {
            List<ShockerCombatantBase> subordinates = new ArrayList<>();
            AABB area = cadre.getBoundingBox().inflate(range);

            for (ShockerCombatantBase entity : cadre.level().getEntitiesOfClass(ShockerCombatantBase.class, area)) {
                if (entity != cadre
                        && entity.isAlive()
                        && ShockerOrg.areFriendly(cadre, entity)) {
                    subordinates.add(entity);
                }
            }
            return subordinates;
        }

        /**
         * 指令1：集火目标
         * 干部发现敌人后，命令所有小队长集火该目标
         * 小队长会自动通过 SquadCommandGoal 将指令传递给队员
         */
        public static void issueFocusFire(ShockerBase cadre, LivingEntity target) {
            if (target == null) return;
//            if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                return; // 不攻击首领玩家
//            }

            List<ShockerCombatantBase> leaders = getNearbySquadLeaders(cadre, 48);
            for (ShockerCombatantBase leader : leaders) {
                // 小队长设置目标后，其 SquadCommandGoal 会自动同步给队员
                leader.setTarget(target);
            }

            cadre.playSound(SoundEvents.VILLAGER_YES, 1.2F, 0.7F);
        }

        /**
         * 指令2：全军突击
         * 提升所有下属的攻击欲望和移速
         */
        public static void issueAssault(ShockerBase cadre) {
            List<ShockerCombatantBase> subordinates = getAllSubordinates(cadre, 48);

            for (ShockerCombatantBase subordinate : subordinates) {
                subordinate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1, false, false));
                subordinate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, false));

                // 如果是小队长，额外激励其小队
                if (subordinate.isSquadLeader()) {
                    ShockerCombatantSquadAI.inspireSquad(subordinate);
                }
            }

            cadre.playSound(SoundEvents.RAID_HORN.get(), 1.5F, 1.0F);
        }

        /**
         * 指令3：撤退重整
         * 所有下属向干部靠拢并恢复少量生命
         */
        public static void issueRetreat(ShockerBase cadre) {
            Vec3 cadrePos = cadre.position();
            List<ShockerCombatantBase> subordinates = getAllSubordinates(cadre, 64);

            for (ShockerCombatantBase subordinate : subordinates) {
                // 向干部位置移动
                subordinate.getNavigation().moveTo(cadrePos.x, cadrePos.y, cadrePos.z, 1.2D);
                // 获得抗性提升
                subordinate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false));
            }

            cadre.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.8F);
        }

        /**
         * 指令4：建立防线
         * 小队长带领队员在干部周围形成防御圈
         */
        public static void issueDefensiveFormation(ShockerBase cadre) {
            Vec3 pos = cadre.position();
            List<ShockerCombatantBase> leaders = getNearbySquadLeaders(cadre, 32);

            for (int i = 0; i < leaders.size(); i++) {
                ShockerCombatantBase leader = leaders.get(i);
                double angle = Math.PI * 2 * i / Math.max(leaders.size(), 1);
                double x = pos.x + 5 * Math.cos(angle);
                double z = pos.z + 5 * Math.sin(angle);

                // 小队长移动到防御位置
                leader.getNavigation().moveTo(x, pos.y, z, 1.0D);

                // 小队长自动重整自己的小队阵型
                if (leader.getSquadSize() >= 2) {
                    ShockerCombatantSquadAI.reformFormation(leader);
                }
            }
        }
    }

    // ==================== 召唤系统 (SummonSystem) ====================
    /**
     * 干部的召唤能力比战斗员更强，可以召唤：
     * 1. 普通战斗员小队（带小队长）
     * 2. 特殊单位（如佐尔上校召唤军事战斗员）
     * 3. 通过重写实现自定义召唤逻辑
     */
    public static class SummonSystem {

        /**
         * 标准召唤：生成一个带小队长的战斗员小队
         * @param cadre 干部实体
         * @param minSize 最小队员数
         * @param maxSize 最大队员数
         */
        public static <T extends ShockerCombatantBase> void summonSquad(
                ShockerBase cadre,
                BiFunction<PlayMessages.SpawnEntity, Level, T> factory,
                int minSize, int maxSize) {

            // 这里需要通过实体注册获取EntityType，具体实现由各干部子类覆盖
            // 基类提供通用框架
        }

        /**
         * 紧急召唤：干部血量低于阈值时自动召唤援军
         */
        public static void tryEmergencySummon(ShockerBase cadre, float healthThreshold) {
            if (cadre.getHealth() <= cadre.getMaxHealth() * healthThreshold) {
                // 由各干部子类实现具体召唤逻辑
                // 佐尔上校：召唤军事战斗员
                // 军装干部：召唤军装战斗员
                // 死神博士：召唤改造人实验体
            }
        }
    }

    // ==================== 干部专属AI Goals ====================

    /**
     * 干部指挥Goal：定期扫描战场并下达指令
     * 优先级应高于普通攻击Goal
     */
    public static class CadreCommandGoal extends Goal {
        private final ShockerBase cadre;
        private int commandCooldown;
        private int scanCooldown;
        private LivingEntity lastTarget;

        public CadreCommandGoal(ShockerBase cadre) {
            this.cadre = cadre;
            this.commandCooldown = 0;
            this.scanCooldown = 0;
        }

        @Override
        public boolean canUse() {
            return cadre.getRank() == ShockerRank.Rank.CADRE && cadre.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return cadre.isAlive();
        }

        @Override
        public void tick() {
            // 每20tick扫描一次目标
            if (scanCooldown-- <= 0) {
                scanCooldown = 20;
                updateTarget();
            }

            // 每100-200tick下达一次指令
            if (commandCooldown-- <= 0) {
                commandCooldown = 100 + cadre.getRandom().nextInt(100);
                executeCommand();
            }
        }

        private void updateTarget() {
            LivingEntity target = cadre.getTarget();
            if (target != lastTarget && target != null) {
                // 目标切换时自动集火
                CommandSystem.issueFocusFire(cadre, target);
                lastTarget = target;
            }
        }

        private void executeCommand() {
            LivingEntity target = cadre.getTarget();

            if (target != null && cadre.getHealth() > cadre.getMaxHealth() * 0.3f) {
                // 血量健康 → 全军突击
                CommandSystem.issueAssault(cadre);
            } else if (target != null) {
                // 血量较低 → 建立防线
                CommandSystem.issueDefensiveFormation(cadre);
            } else {
                // 无目标 → 随机巡逻指令
                List<ShockerCombatantBase> leaders = CommandSystem.getNearbySquadLeaders(cadre, 48);
                for (ShockerCombatantBase leader : leaders) {
                    // 让小队长自主巡逻
                    if (leader.getTarget() == null) {
                        ShockerCombatantSquadAI.reformFormation(leader);
                    }
                }
            }
        }
    }

    /**
     * 干部护卫Goal：当干部被攻击时，召唤附近单位保护
     * 与战斗员的增援AI不同：干部是"被保护"，战斗员是"呼叫增援"
     */
    public static class CadreGuardGoal extends Goal {
        private final ShockerBase cadre;
        private int guardCooldown;
        private boolean wasHurt;

        public CadreGuardGoal(ShockerBase cadre) {
            this.cadre = cadre;
        }

        @Override
        public boolean canUse() {
            return cadre.getRank() == ShockerRank.Rank.CADRE
                    && cadre.getLastHurtByMob() != null
                    && cadre.tickCount - cadre.getLastHurtByMobTimestamp() < 100;
        }

        @Override
        public void start() {
            // 被攻击时触发防御阵型
            CommandSystem.issueDefensiveFormation(cadre);
            // 命令所有小队长集火攻击者
            CommandSystem.issueFocusFire(cadre, cadre.getLastHurtByMob());
            guardCooldown = 200;
        }

        @Override
        public boolean canContinueToUse() {
            return guardCooldown-- > 0 && cadre.isAlive();
        }
    }

    /**
     * 干部威慑Goal：附近无敌人时，让下属保持警戒阵型
     */
    public static class CadrePatrolGoal extends Goal {
        private final ShockerBase cadre;
        private int patrolCooldown;

        public CadrePatrolGoal(ShockerBase cadre) {
            this.cadre = cadre;
        }

        @Override
        public boolean canUse() {
            return cadre.getRank() == ShockerRank.Rank.CADRE
                    && cadre.getTarget() == null
                    && cadre.isAlive();
        }

        @Override
        public void tick() {
            if (patrolCooldown-- <= 0) {
                patrolCooldown = 300 + cadre.getRandom().nextInt(200);

                // 让下属在干部周围巡逻
                List<ShockerCombatantBase> subordinates = CommandSystem.getAllSubordinates(cadre, 64);
                Vec3 pos = cadre.position();

                for (ShockerCombatantBase sub : subordinates) {
                    if (sub.getTarget() == null && sub.getNavigation().isDone()) {
                        double angle = Math.PI * 2 * cadre.getRandom().nextDouble();
                        double dist = 8 + cadre.getRandom().nextInt(8);
                        double x = pos.x + dist * Math.cos(angle);
                        double z = pos.z + dist * Math.sin(angle);
                        sub.getNavigation().moveTo(x, pos.y, z, 0.8D);
                    }
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return cadre.getTarget() == null && cadre.isAlive();
        }
    }
}