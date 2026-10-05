package com.qianyunyi.rider.entity.kaijin.shocker.event;


import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import com.qianyunyi.rider.entity.kaijin.shocker.util.CadreRegistry;
import com.qianyunyi.rider.entity.kaijin.shocker.util.ShockerSpawnHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 修卡组织事件处理：友军伤害保护 + 干部注册
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ShockerEvents {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        CadreRegistry.registerAll();
        ShockerSpawnHelper.registerCadreTypes(); // 加这一行
    }

    // 原有友伤保护逻辑
    @SubscribeEvent
    public void onShockerAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ShockerBase victim))
            return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        // 饰品玩家 = 友好
        if (attacker instanceof Player player) {
//            if (victim.isPlayerSuperiorLeader(player)) {
//                event.setCanceled(true);
//                return;
//            }
        }

        // 修卡友伤免疫
        if (attacker instanceof ShockerBase attackerShock) {
            if (ShockerOrg.areFriendly(victim, attackerShock)) {
                event.setCanceled(true);
            }
        }
    }
}