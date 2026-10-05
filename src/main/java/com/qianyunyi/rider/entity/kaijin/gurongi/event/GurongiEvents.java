package com.qianyunyi.rider.entity.kaijin.gurongi.event;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiHuntingLaw;
import com.qianyunyi.rider.entity.kaijin.gurongi.GoGadoruBaEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.item.GuzepaItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;

/**
 * 古朗基事件处理：
 * <ol>
 *   <li>基基鲁游戏计数 - 古朗基按游戏目标条件击杀时才计数，
 *       且受 {@link com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiHuntingLaw 狩猎法则} 约束</li>
 *   <li>村庄灾厄效果 - 村民对古朗基的恐惧行为</li>
 * </ol>
 */
@Mod.EventBusSubscriber(
        modid = KamenRiderInterwovenWorldsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class GurongiEvents {

    /**
     * 当古朗基杀死目标时：
     * <ol>
     *   <li>首先通过 {@link GurongiHuntingLaw} 检查合法性</li>
     *   <li>匹配游戏目标条件才计入基基鲁游戏计数</li>
     * </ol>
     * <p>
     * 玩家、村民、灾厄村民统一视为"人类"目标，不区分具体类型。
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // 获取杀手，判断是否为古朗基
        if (!(event.getSource().getEntity() instanceof Gurongi gurongi)) {
            return;
        }

        // 只处理人类类型的目标
        if (!GurongiHuntingLaw.isHuman(victim)) {
            return;
        }

        // ★ 狩猎法则事前检查：如果会导致 PENALTY，先执行惩罚
        if (!gurongi.level().isClientSide()) {
            GurongiHuntingLaw.CheckResult lawCheck = GurongiHuntingLaw.canKill(gurongi, victim);
            if (lawCheck == GurongiHuntingLaw.CheckResult.PENALTY) {
                GurongiHuntingLaw.applyPenalty(gurongi,
                        "在非游戏期间杀死了 " + victim.getName().getString());
                // 即使受罚，本次击杀仍然发生（已死亡），但不计入游戏计数
                logKill(gurongi, victim, false);
                return;
            }

            if (lawCheck == GurongiHuntingLaw.CheckResult.DENY) {
                // 法则禁止此击杀，但仍记录
                logKill(gurongi, victim, false);
                return;
            }
        }

        // ===== 通过法则检查：判断是否匹配游戏目标 =====

        // 获取目标的具体分类
        String targetTypeName = GurongiHuntingLaw.isLinto(victim) ? "林多" : "灾厄村民";

        // 判断此次击杀是否匹配游戏目标（林多和灾厄村民均匹配）
        if (gurongi.isEntityValidObjectiveTarget(victim)) {
            // ★ 使用新的 onObjectiveKill(LivingEntity) 方法
            boolean completed = gurongi.onObjectiveKill(victim);

            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[Gurongi] {} 完成了基基鲁游戏目标击杀！(目标类型: {}), 进度: {}/{}",
                    gurongi.getName().getString(),
                    targetTypeName,
                    completed ? " ✓ 目标完成!" : "",
                    gurongi.getObjectiveProgress(),
                    gurongi.getGameObjective() != null ? gurongi.getGameObjective().getTargetCount() : "?"
            );
        } else {
            // 不匹配目标条件 → 不计入
            KamenRiderInterwovenWorldsMod.LOGGER.debug(
                    "[Gurongi] {} 杀死了 {}，但不符合当前游戏目标，不计入计数",
                    gurongi.getName().getString(),
                    victim.getName().getString()
            );
        }
    }

    /**
     * 古朗基掉落物处理：
     * 1. 算珠手环仅由me集团（升阶变色龙：zu_goma_gu_enhanced/ultimate）必定掉落（战利品表），同时设置游戏计数
     * 2. 板由zu集团掉落（战利品表），但全玩家共享数量限制——首次掉落后不再掉落
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // 只处理古朗基被击杀的情况
        if (!(event.getEntity() instanceof Gurongi gurongi)) {
            return;
        }

        // 获取实体注册名，判断是否为me集团（升阶变色龙）
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(gurongi.getType());
        if (entityId == null) return;
        String entityPath = entityId.getPath();
        boolean isEvolvedChameleon = entityPath.equals("zu_goma_gu_enhanced") || entityPath.equals("zu_goma_gu_ultimate");

        int gameCount = gurongi.getObjectiveProgress();

        // 获取击杀者（玩家）
        Player killer = null;
        if (event.getSource().getEntity() instanceof Player player) {
            killer = player;
        }

        // 检查本次掉落中是否有板
        boolean hasDodozoDrop = false;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(KamenRiderInterwovenWorldsModItems.DODOZO.get())) {
                hasDodozoDrop = true;
                break;
            }
        }

        // 检查玩家是否已获得过板（共享数量限制）
        String dodozoKey = KamenRiderInterwovenWorldsMod.MODID + ".has_obtained_dodozo";
        boolean alreadyHasDodozo = killer != null && hasDodozoDrop
                && killer.getPersistentData().getBoolean(dodozoKey);

        // 遍历并处理掉落物（使用Iterator安全移除）
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity drop = iterator.next();
            var stack = drop.getItem();

            // 算珠手环：仅me集团才设置游戏计数
            if (isEvolvedChameleon && stack.getItem() instanceof GuzepaItem) {
                GuzepaItem.setGameCount(stack, gameCount);
            }

            // 板：如果已拥有则移除本次掉落
            if (stack.is(KamenRiderInterwovenWorldsModItems.DODOZO.get()) && alreadyHasDodozo) {
                iterator.remove();
                drop.discard();
            }
        }

        // 首次获得板，标记玩家
        if (killer != null && hasDodozoDrop && !alreadyHasDodozo) {
            killer.getPersistentData().putBoolean(dodozoKey, true);
        }
    }

    /**
     * 记录击杀日志
     */
    private static void logKill(Gurongi gurongi, LivingEntity victim, boolean counted) {
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[Gurongi] {} {} {} (状态: {}, 游戏阶段: {})",
                gurongi.getName().getString(),
                counted ? "✓ 有效击杀" : "✗ 无效击杀",
                victim.getName().getString(),
                gurongi.getGameStatus(),
                gurongi.getGameStage()
        );
    }

    /**
     * 葛·伽多鲁·巴：被闪电击中后解锁电击体。
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onEntityStruckByLightning(net.minecraftforge.event.entity.EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof GoGadoruBaEntity gadoru) {
            if (!gadoru.level().isClientSide() && !gadoru.isShockerUnlocked()) {
                gadoru.unlockShocker();
                // 解锁后立即切换为电击体，让玩家立刻看到解锁效果
                gadoru.setForm(GoGadoruBaEntity.FormType.SHOCKER);
            }
        }
    }
}