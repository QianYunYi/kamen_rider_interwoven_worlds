package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;
import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerScientistBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;

/**
 * 科学家AI系统
 * 设计原则：
 * 1. 科学家是辅助单位，不具备战斗能力
 * 2. 发现威胁时优先逃跑，而非反击
 * 3. 安全时给友军上增益效果（治疗、强化）
 * 4. 类似于女巫的行为模式，但辅助对象是修卡友军
 */
public class ShockerScientistAI extends ShockerAIGoals {

    // ==================== 科学家专用目标选择器 ====================

    /**
     * 科学家被攻击时的反应：不反击，而是标记为"受惊"状态
     * 由 FleeGoal 接管逃跑行为
     */
    public static class ScientistHurtGoal extends HurtByTargetGoal {
        private final ShockerScientistBase scientist;

        public ScientistHurtGoal(ShockerScientistBase mob) {
            super(mob);
            this.scientist = mob;
            // 科学家不反击，所以设置目标为null
            this.setAlertOthers((Class<? extends LivingEntity>) null);
        }

        @Override
        public void start() {
            // 不设置反击目标，只标记受惊
            scientist.setPanicked(true);
            scientist.setPanicTarget(this.mob.getLastHurtByMob());
        }

        @Override
        public boolean canContinueToUse() {
            // 科学家不会持续反击
            return false;
        }
    }

    // ==================== 逃跑系统 (FleeSystem) ====================

    /**
     * 逃跑Goal：发现威胁时远离
     * 优先级最高，科学家第一要务是保命
     */
    public static class FleeGoal extends Goal {
        private final ShockerScientistBase scientist;
        private LivingEntity threat;
        private int fleeTimer;
        private int cooldown;

        public FleeGoal(ShockerScientistBase scientist) {
            this.scientist = scientist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0) return false;

            // 检查是否受惊
            if (scientist.isPanicked()) {
                threat = scientist.getPanicTarget();
                if (threat != null && threat.isAlive()) return true;
                scientist.setPanicked(false);
            }

            // 扫描附近威胁（玩家、非修卡生物）
            threat = findNearestThreat();
            return threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            return threat != null
                    && threat.isAlive()
                    && fleeTimer > 0
                    && scientist.distanceToSqr(threat) < 400; // 20格内持续逃跑
        }

        @Override
        public void start() {
            fleeTimer = 100 + scientist.getRandom().nextInt(100);
            scientist.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, fleeTimer, 1, false, false));
            fleeFromThreat();
        }

        @Override
        public void tick() {
            fleeTimer--;

            // 每10tick重新计算逃跑方向
            if (fleeTimer % 10 == 0) {
                fleeFromThreat();
            }

            // 逃跑时持续看向威胁方向（监视敌人）
            scientist.getLookControl().setLookAt(threat, 30, 30);
        }

        @Override
        public void stop() {
            threat = null;
            cooldown = 60; // 3秒冷却后再评估
            scientist.setPanicked(false);
            scientist.setPanicTarget(null);
        }

        private LivingEntity findNearestThreat() {
            AABB area = scientist.getBoundingBox().inflate(12);
            double closestDist = Double.MAX_VALUE;
            LivingEntity closest = null;

            // 优先检测玩家
            for (Player player : scientist.level().getEntitiesOfClass(Player.class, area)) {
                // 首领玩家不算威胁
                //if (JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) continue;

                double dist = scientist.distanceToSqr(player);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = player;
                }
            }

            // 也可以检测其他敌对生物（如村民铁傀儡等）
            // 这里简化，主要处理玩家威胁

            return closest;
        }

        private void fleeFromThreat() {
            if (threat == null) return;

            Vec3 scientistPos = scientist.position();
            Vec3 threatPos = threat.position();

            // 计算远离威胁的方向
            Vec3 fleeDir = scientistPos.subtract(threatPos).normalize();

            // 随机偏移，避免直线逃跑
            double offsetX = (scientist.getRandom().nextDouble() - 0.5) * 4;
            double offsetZ = (scientist.getRandom().nextDouble() - 0.5) * 4;

            double targetX = scientistPos.x + fleeDir.x * 12 + offsetX;
            double targetZ = scientistPos.z + fleeDir.z * 12 + offsetZ;
            double targetY = scientistPos.y;

            scientist.getNavigation().moveTo(targetX, targetY, targetZ, 1.5D); // 逃跑时加速
        }
    }

    // ==================== 辅助系统 (SupportSystem) ====================

    /**
     * 辅助Goal：安全时给友军上增益
     * 类似于女巫给同伴治疗/强化
     */
    public static class SupportGoal extends Goal {
        private final ShockerScientistBase scientist;
        private int cooldown;
        private int castTimer;
        private ShockerBase targetAlly;
        private SupportType nextSpell;

        public SupportGoal(ShockerScientistBase scientist) {
            this.scientist = scientist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0) return false;
            if (scientist.isPanicked()) return false; // 受惊时不辅助

            // 寻找需要辅助的友军
            targetAlly = findSupportTarget();
            return targetAlly != null;
        }

        @Override
        public boolean canContinueToUse() {
            return targetAlly != null
                    && targetAlly.isAlive()
                    && !scientist.isPanicked()
                    && castTimer > 0;
        }

        @Override
        public void start() {
            castTimer = 40; // 2秒施法时间
            nextSpell = determineSpell();

            // 移动到合适距离（4-8格）
            scientist.getNavigation().moveTo(targetAlly, 0.6D);
        }

        @Override
        public void tick() {
            castTimer--;

            if (targetAlly != null) {
                scientist.getLookControl().setLookAt(targetAlly, 30, 30);

                // 施法完成
                if (castTimer <= 0) {
                    castSpell();
                    cooldown = 60 + scientist.getRandom().nextInt(40);
                }
            }
        }

        @Override
        public void stop() {
            targetAlly = null;
            castTimer = 0;
        }

        private ShockerBase findSupportTarget() {
            AABB area = scientist.getBoundingBox().inflate(16);
            ShockerBase mostNeedy = null;
            double lowestHealthRatio = 1.0;

            for (ShockerBase ally : scientist.level().getEntitiesOfClass(ShockerBase.class, area)) {
                if (ally == scientist || !ally.isAlive()) continue;
                if (!ShockerOrg.areFriendly(scientist, ally)) continue;

                double ratio = ally.getHealth() / ally.getMaxHealth();

                // 优先治疗血量低的
                if (ratio < 0.8 && ratio < lowestHealthRatio) {
                    lowestHealthRatio = ratio;
                    mostNeedy = ally;
                }
            }

            return mostNeedy;
        }

        private SupportType determineSpell() {
            if (targetAlly == null) return SupportType.HEAL;

            double healthRatio = targetAlly.getHealth() / targetAlly.getMaxHealth();

            if (healthRatio < 0.4) return SupportType.HEAL;
            if (healthRatio < 0.7) return SupportType.REGENERATION;
            if (!targetAlly.hasEffect(MobEffects.MOVEMENT_SPEED)) return SupportType.SPEED;
            if (!targetAlly.hasEffect(MobEffects.DAMAGE_BOOST)) return SupportType.STRENGTH;

            return SupportType.RESISTANCE;
        }

        private void castSpell() {
            if (targetAlly == null || !targetAlly.isAlive()) return;

            switch (nextSpell) {
                case HEAL -> {
                    targetAlly.heal(8);
                    targetAlly.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0));
                    spawnCastParticles(targetAlly, 0xFF0000); // 红色
                }
                case REGENERATION -> {
                    targetAlly.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                    spawnCastParticles(targetAlly, 0xFF69B4); // 粉色
                }
                case SPEED -> {
                    targetAlly.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
                    spawnCastParticles(targetAlly, 0x00FFFF); // 青色
                }
                case STRENGTH -> {
                    targetAlly.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
                    spawnCastParticles(targetAlly, 0xFF4500); // 橙红
                }
                case RESISTANCE -> {
                    targetAlly.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                    spawnCastParticles(targetAlly, 0x808080); // 灰色
                }
            }

            // 播放施法音效
            scientist.level().playSound(null, scientist.getX(), scientist.getY(), scientist.getZ(),
                    SoundEvents.WITCH_DRINK, SoundSource.NEUTRAL, 0.5F, 1.0F);
        }

        private void spawnCastParticles(ShockerBase target, int color) {
            // 这里可以添加粒子效果
            // 例如：target.level.addParticle(ParticleTypes.ENTITY_EFFECT, ...)
        }

        private enum SupportType {
            HEAL, REGENERATION, SPEED, STRENGTH, RESISTANCE
        }
    }

    // ==================== 研究系统 (ResearchSystem) ====================

    /**
     * 研究Goal：科学家在安全区域进行"研究"行为
     * 表现为原地不动、播放粒子效果、偶尔自言自语
     * 被攻击或发现威胁时中断
     */
    public static class ResearchGoal extends Goal {
        private final ShockerScientistBase scientist;
        private int researchTimer;
        private int cooldown;
        private Vec3 researchPos;

        public ResearchGoal(ShockerScientistBase scientist) {
            this.scientist = scientist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0) return false;
            if (scientist.isPanicked()) return false;

            // 附近有威胁时不研究
            if (findNearestThreat() != null) return false;

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return researchTimer > 0
                    && !scientist.isPanicked()
                    && findNearestThreat() == null;
        }

        @Override
        public void start() {
            researchTimer = 200 + scientist.getRandom().nextInt(200); // 10-20秒
            researchPos = scientist.position();

            // 停止移动
            scientist.getNavigation().stop();
        }

        @Override
        public void tick() {
            researchTimer--;

            // 保持在原地小幅移动（模拟研究时的踱步）
            if (researchTimer % 40 == 0 && scientist.getRandom().nextFloat() < 0.3f) {
                double offsetX = (scientist.getRandom().nextDouble() - 0.5) * 2;
                double offsetZ = (scientist.getRandom().nextDouble() - 0.5) * 2;
                scientist.getNavigation().moveTo(
                        researchPos.x + offsetX,
                        researchPos.y,
                        researchPos.z + offsetZ,
                        0.3D
                );
            }

            // 播放研究粒子（绿色药水效果）
            if (researchTimer % 20 == 0) {
                // scientist.level.addParticle(ParticleTypes.WITCH, ...)
            }
        }

        @Override
        public void stop() {
            cooldown = 100 + scientist.getRandom().nextInt(100);
        }

        private LivingEntity findNearestThreat() {
            AABB area = scientist.getBoundingBox().inflate(10);

            for (Player player : scientist.level().getEntitiesOfClass(Player.class, area)) {
//                if (!JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                    return player;
//                }
            }
            return null;
        }
    }

    // ==================== 跟随系统 (FollowSystem) ====================

    /**
     * 跟随Goal：科学家跟随附近的干部或高级单位
     * 类似于女巫跟随灾厄村民
     */
    public static class FollowCadreGoal extends Goal {
        private final ShockerScientistBase scientist;
        private ShockerBase leader;
        private int cooldown;

        public FollowCadreGoal(ShockerScientistBase scientist) {
            this.scientist = scientist;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cooldown-- > 0) return false;
            if (scientist.isPanicked()) return false;
            if (scientist.getTarget() != null) return false; // 有目标时不跟随

            leader = findNearbyCadre();
            return leader != null && scientist.distanceToSqr(leader) > 64; // 8格外才跟随
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null
                    && leader.isAlive()
                    && !scientist.isPanicked()
                    && scientist.distanceToSqr(leader) < 900; // 30格内持续跟随
        }

        @Override
        public void tick() {
            if (leader != null) {
                scientist.getNavigation().moveTo(leader, 0.6D);

                // 保持在干部侧后方
                if (scientist.distanceToSqr(leader) < 16) {
                    scientist.getNavigation().stop();
                }
            }
        }

        @Override
        public void stop() {
            leader = null;
            cooldown = 40;
        }

        private ShockerBase findNearbyCadre() {
            AABB area = scientist.getBoundingBox().inflate(32);
            ShockerBase closest = null;
            double closestDist = Double.MAX_VALUE;

            for (ShockerBase entity : scientist.level().getEntitiesOfClass(ShockerBase.class, area)) {
                if (entity == scientist || !entity.isAlive()) continue;
                if (!ShockerOrg.areFriendly(scientist, entity)) continue;

                // 优先跟随干部/首领
                int priority = getFollowPriority(entity);
                if (priority <= 0) continue;

                double dist = scientist.distanceToSqr(entity);
                double score = dist / priority; // 优先级高的单位距离权重更大

                if (score < closestDist) {
                    closestDist = score;
                    closest = entity;
                }
            }

            return closest;
        }

        private int getFollowPriority(ShockerBase entity) {
            return switch (entity.getRank()) {
                case LEADER, SHADOW_LEADER -> 5;
                case CADRE -> 4;
                case KAIJIN -> 2;
                case SQUAD_LEADER -> 1;
                default -> 0;
            };
        }
    }
}