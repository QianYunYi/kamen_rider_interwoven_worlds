package com.qianyunyi.rider.entity.kaijin.shocker.ai;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerCombatantBase;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public class ShockerCombatantSquadAI {

    // 队长下达指令：全员集火目标
    public static void issueCommand(ShockerCombatantBase leader) {
        if (!leader.isSquadCommander()) return;

        // 冷却已经在 Goal 里控制，这里不再判断 → 不重叠
        List<ShockerCombatantBase> squad = leader.getSquadCombatants();
        if (squad.isEmpty()) return;

        LivingEntity target = leader.getTarget();

        // 没有目标 → 寻找敌人
        if (target == null) {
            AABB box = leader.getBoundingBox().inflate(20);
            List<Player> players = leader.level().getEntitiesOfClass(Player.class, box);

            if (!players.isEmpty()) {
                target = players.get(0);
            } else {
                List<Villager> villagers = leader.level().getEntitiesOfClass(Villager.class, box);
                if (!villagers.isEmpty()) {
                    target = villagers.get(0);
                }
            }
        }

        if (target == null) return;
//        if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//            return; // 绝对不攻击首领玩家
//        }

        // 下达指令
        for (ShockerCombatantBase member : squad) {
            member.setTarget(target);
        }
        leader.playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.8F);
    }

    // 激励小队
    public static void inspireSquad(ShockerCombatantBase leader) {
        if (!leader.isSquadCommander()) return;

        for (ShockerCombatantBase member : leader.getSquadCombatants()) {
            member.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
            member.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   200, 0, false, false));
            member.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, false));
            member.playSound(SoundEvents.PLAYER_LEVELUP, 0.5F, 1.5F);
        }
        leader.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
    }

    // 重整阵型
    public static void reformFormation(ShockerCombatantBase leader) {
        if (!leader.isSquadCommander() || leader.getSquadSize() < 3) return;

        Vec3 pos = leader.position();
        List<ShockerCombatantBase> squad = leader.getSquadCombatants();

        for (int i = 0; i < squad.size(); i++) {
            ShockerCombatantBase member = squad.get(i);
            double angle = Math.PI * 2 * i / squad.size();
            double x = pos.x + 3 * Math.cos(angle);
            double z = pos.z + 3 * Math.sin(angle);
            member.getNavigation().moveTo(x, pos.y, z, 1.0D);
        }
    }
}