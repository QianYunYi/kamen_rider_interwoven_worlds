package com.qianyunyi.rider.entity.kaijin.shocker.ai;

import com.qianyunyi.rider.util.TeleportUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

// 通用瞬移AI —— 所有怪物都能用
public class TeleportAttackAI extends Goal {
    private final Mob mob;
    private LivingEntity target;
    private int teleportCooldown; // 瞬移冷却
    private static final int COOLDOWN = 300; // 每15秒最多瞬移1次（300tick）

    public TeleportAttackAI(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); // 占用移动、看向AI
    }

    @Override
    public boolean canUse() {
        target = mob.getTarget();
        // 条件：有目标 + 活着 + 冷却结束 + 距离太远/太近
        return target != null && target.isAlive() && mob.isAlive()
                && teleportCooldown <= 0
                && (mob.distanceTo(target) > 8 || mob.distanceTo(target) < 2);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // 只执行一次瞬移
    }

    @Override
    public void start() {
        if (target == null) return;

        double dist = mob.distanceTo(target);

        // 距离太远 → 瞬移到目标身边
        if (dist > 8) {
            TeleportUtil.teleportTowardsTarget(mob, target);
        }
        // 距离太近（贴脸）→ 瞬移到背后拉开距离
        else if (dist < 2) {
            TeleportUtil.teleportBehindTarget(mob, target);
        }

        teleportCooldown = COOLDOWN; // 进入冷却
    }

    @Override
    public void tick() {
        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
    }
}