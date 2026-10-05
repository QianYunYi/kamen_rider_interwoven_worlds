package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerCombatantBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public class ShockerCombatantReinforcementAI {
    public static void tryCallReinforcements(ShockerCombatantBase shocker, LivingEntity attacker) {
        if (shocker.level().isClientSide() || !canCall(shocker)) return;

        long now = shocker.level().getGameTime();
        if (now - shocker.getLastReinforcementCallTime() < 150 || shocker.hasCalledReinforcements())
            return;

        shocker.setCalledReinforcements(true);
        shocker.setLastReinforcementCallTime(now);

        // 小队长范围扩大到40格，普通25格
        double range = shocker.getRank() == ShockerRank.Rank.SQUAD_LEADER ? 40 : 25;
        AABB area = shocker.getBoundingBox().inflate(range);

        List<ShockerCombatantBase> nearby = shocker.level().getEntitiesOfClass(ShockerCombatantBase.class, area,
                e -> e != shocker && e.isAlive());

        // 小队长最多呼叫8人，普通4人
        int maxResponders = shocker.getRank() == ShockerRank.Rank.SQUAD_LEADER ? 8 : 4;
        int count = 0;

        for (ShockerCombatantBase nearbyShocker : nearby) {
            // 同组织+未进入战斗→响应
            if (ShockerOrg.areFriendly(shocker, nearbyShocker) && !nearbyShocker.isAggressive()) {
                nearbyShocker.setTarget(attacker);
                nearbyShocker.playSound(SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
                if (++count >= maxResponders) break;
            }
        }

        // 12秒冷却后可再次呼叫
        DelayedTask.run(shocker.level(), 240, () -> {
            if (shocker.isAlive()) shocker.setCalledReinforcements(false);
        });
    }

    // 允许小队长呼叫增援，仅首领/干部/幕后首领不能呼叫
    private static boolean canCall(ShockerCombatantBase shocker) {
        ShockerRank.Rank r = shocker.getRank();
        return r != ShockerRank.Rank.LEADER && r != ShockerRank.Rank.CADRE && r != ShockerRank.Rank.SHADOW_LEADER;
    }
}