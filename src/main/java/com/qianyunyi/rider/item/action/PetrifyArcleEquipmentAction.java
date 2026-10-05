package com.qianyunyi.rider.item.action;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.event.listener.rider.kuuga.GrowingKuugaHenshinListener;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PetrifyArcleEquipmentAction {
    // 玩家绑定监听器容器
    private static final Map<UUID, DeathListener> DEATH_LISTENERS = new ConcurrentHashMap<>();
    private static final Map<UUID, GrowingKuugaHenshinListener> GROWING_LISTENERS = new ConcurrentHashMap<>();
    // 全局延迟任务管理
    private static final Set<UUID> ACTIVE_TASK_IDS = ConcurrentHashMap.newKeySet();
    public static final Consumer<UUID> TASK_ADDER = ACTIVE_TASK_IDS::add;

    // 玩家退出统一清理（配套玩家登出事件调用）
    public static void clearPlayerResource(UUID playerUuid) {
        // 清理死亡监听
        DeathListener deathListener = DEATH_LISTENERS.remove(playerUuid);
        if (deathListener != null) deathListener.unregister();
        // 清理空我成长变身监听
        GrowingKuugaHenshinListener growListener = GROWING_LISTENERS.remove(playerUuid);
        if (growListener != null) growListener.unregister();
    }

    // 清空全部任务（模组重载/世界退出调用）
    public static void cancelAllTasks() {
        ACTIVE_TASK_IDS.forEach(DelayedTask::cancel);
        ACTIVE_TASK_IDS.clear();
    }

    // 死亡防护监听器
    private static class DeathListener {
        private final ServerPlayer player;
        private boolean registered = false;

        public DeathListener(ServerPlayer serverPlayer) {
            this.player = serverPlayer;
        }

        public void register() {
            if (!registered) {
                MinecraftForge.EVENT_BUS.register(this);
                registered = true;
                KamenRiderInterwovenWorldsMod.LOGGER.info("石化腰带死亡监听注册: {}", player.getUUID());
            }
        }

        public void unregister() {
            if (registered) {
                MinecraftForge.EVENT_BUS.unregister(this);
                registered = false;
                KamenRiderInterwovenWorldsMod.LOGGER.info("石化腰带死亡监听注销: {}", player.getUUID());
            }
        }

        @SubscribeEvent
        public void onPlayerDeath(LivingDeathEvent event) {
            if (!event.getEntity().getUUID().equals(player.getUUID())) return;
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            // 仅空我初生待机状态触发复活保护
            if (data.getCurrentState() != HenshinState.STANDBY
                    || !"Kuuga".equals(data.getActiveRiderName())
                    || !"base".equals(data.getActiveFormName())) {
                return;
            }

            event.setCanceled(true);
            switchToBaseForm(player);
            // 血量恢复一半 + 短暂无敌
            player.setHealth(player.getMaxHealth() / 2F);
            player.invulnerableTime = 20;
            // 销毁监听
            this.unregister();
            DEATH_LISTENERS.remove(player.getUUID());
            // 普通单行标题提示
            TitleUtils.sendTitleMessage(player, "亚玛达姆的力量保护了你", ChatFormatting.RED);
            TitleUtils.sendTitleMessage(player, "但你也因此感到虚弱", ChatFormatting.RED, 5, 30, 5);
        }
    }

    public static void execute(LevelAccessor world, Entity entity, ItemStack itemStack) {
        if (entity == null || world.isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;

        UUID playerUuid = player.getUUID();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 已解锁空我直接拦截
        if (data.getUnlockedRiders().contains("Kuuga")) {
            TitleUtils.sendTitleMessage(player, "你的体内已经拥有了", ChatFormatting.RED);
            TitleUtils.sendTitleMessage(player, "亚玛达姆的力量，无法再次吸收！", ChatFormatting.RED, 5, 40, 5);
            return;
        }

        // 腿部有装备拦截
        if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) return;

        // 添加物品冷却
        player.getCooldowns().addCooldown(itemStack.getItem(), 1200);
        data.setHenshining(true);

        // 播放吸收动画同步包
        NetworkHandler.sendToTrackingPlayers(
                new AnimationMessage("petrify_arcle_absorb", playerUuid, true),
                player
        );
        // 消耗手持腰带
        HenshinUtil.removeItemFromInventory(player, itemStack.getItem());
        // 解锁空我基础数据
        data.getKuugaData().setMightyKuuga(true);
        data.getKuugaData().addAmadamEnergy(400);

        // 初始化并注册监听器
        DEATH_LISTENERS.computeIfAbsent(playerUuid, k -> new DeathListener(player)).register();
        GrowingKuugaHenshinListener growListener = GROWING_LISTENERS.computeIfAbsent(playerUuid, k -> new GrowingKuugaHenshinListener());

        ItemStack originalLegging = player.getItemBySlot(EquipmentSlot.LEGS);

        // ========== 阶段1：20刻后临时装备石化腰带播放吸收动画 ==========
        TASK_ADDER.accept(DelayedTask.run((ServerLevel) world, 20, lvl -> {
            ItemStack arcleLeg = new ItemStack(KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get());
            player.setItemSlot(EquipmentSlot.LEGS, arcleLeg);
            // 1刻延迟设置护甲动画
            TASK_ADDER.accept(DelayedTask.run(lvl, 1, inner -> BaseAnimatedArmorItem.setAnim(arcleLeg, "absorb")));

            // ========== 阶段2：再等20刻（总计40刻）解锁骑士+第一组打字标题 ==========
            TASK_ADDER.accept(DelayedTask.run(lvl, 20, stage2Lvl -> {
                data.addUnlockedRider("Kuuga");
                KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}解锁Kuuga:{}", playerUuid, data.getUnlockedRiders());

                // 第一组分段打字标题：古老的遗物 带着痛苦
                MutableComponent t1 = Component.literal("古老的").withStyle(ChatFormatting.RED);
                MutableComponent t2 = Component.literal("遗物").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
                MutableComponent t3 = Component.literal("带着").withStyle(ChatFormatting.RED);
                MutableComponent t4 = Component.literal("痛苦").withStyle(ChatFormatting.DARK_RED, ChatFormatting.UNDERLINE);
                TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, t1, t2, t3, t4);

                // 计算本组标题总时长，延迟发送下一段
                int delay1 = calcTextDuration("古老的", "遗物", "带着", "痛苦");
                TASK_ADDER.accept(DelayedTask.run(stage2Lvl, delay1, innerLvl -> {
                    MutableComponent line = Component.literal("进入了你的身体").withStyle(ChatFormatting.RED);
                    TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, line);
                }));
            }));

            // ========== 阶段3：再等20刻（总计60刻）粒子 + 切换待机装备、第二大段标题 ==========
            TASK_ADDER.accept(DelayedTask.run(lvl, 20, stage3Lvl -> {
                // 闪光粒子
                ServerLevel serverLvl = (ServerLevel) stage3Lvl;
                serverLvl.sendParticles(ParticleTypes.FLASH,
                        player.getX(), player.getY() + 0.5D, player.getZ(),
                        10, 0.5D, 0.5D, 0.5D, 0.1D);

                // 保存胸甲待机装备并脱下胸甲、还原腿甲
                CompoundTag standbyGear = HenshinUtil.getStandbyGear(player);
                standbyGear.put("chest", player.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.LEGS, originalLegging);

                // 第二段多组分段标题
                MutableComponent p1 = Component.literal("内心平静 ").withStyle(ChatFormatting.AQUA);
                MutableComponent p2 = Component.literal("强壮身体 ").withStyle(ChatFormatting.DARK_GREEN);
                TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, p1, p2);

                int d1 = calcTextDuration("内心平静 ", "强壮身体 ");
                TASK_ADDER.accept(DelayedTask.run(stage3Lvl, d1, inner1 -> {
                    MutableComponent pp1 = Component.literal("将这 ").withStyle(ChatFormatting.YELLOW);
                    MutableComponent pp2 = Component.literal("掌握吧 ").withStyle(ChatFormatting.GOLD);
                    TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, pp1, pp2);

                    int d2 = calcTextDuration("将这 ", "掌握吧 ");
                    TASK_ADDER.accept(DelayedTask.run(inner1, d2, inner2 -> {
                        MutableComponent lp1 = Component.literal("那样 ").withStyle(ChatFormatting.WHITE);
                        MutableComponent lp2 = Component.literal("战士空我 ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                        MutableComponent lp3 = Component.literal("成为").withStyle(ChatFormatting.GRAY);
                        TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, lp1, lp2, lp3);
                    }));
                }));

                // 写入待机状态数据并保存
                data.setStandbyGear(standbyGear);
                data.setCurrentState(HenshinState.STANDBY);
                data.setActiveRiderName("Kuuga");
                data.setActiveFormName("base");
                data.setHenshining(false);
            }));

            // ========== 阶段4：等待30刻（总计90刻）注册成长监听 + 最终收尾标题 ==========
            TASK_ADDER.accept(DelayedTask.run(lvl, 30, stage4Lvl -> {
                growListener.initialize(player);
                growListener.register();

                MutableComponent s1 = Component.literal("拯救人类 ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                MutableComponent s2 = Component.literal("能掌握这点的话 ").withStyle(ChatFormatting.DARK_GREEN);
                TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, s1, s2);

                int delayA = calcTextDuration("拯救人类 ", "能掌握这点的话 ");
                TASK_ADDER.accept(DelayedTask.run(stage4Lvl, delayA, innerA -> {
                    MutableComponent tA1 = Component.literal("永远地 ").withStyle(ChatFormatting.GOLD);
                    MutableComponent tA2 = Component.literal("与你 ").withStyle(ChatFormatting.YELLOW);
                    TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, tA1, tA2);

                    int delayB = calcTextDuration("永远地 ", "与你 ");
                    TASK_ADDER.accept(DelayedTask.run(innerA, delayB, innerB -> {
                        MutableComponent e1 = Component.literal("同在 ").withStyle(ChatFormatting.LIGHT_PURPLE);
                        MutableComponent e2 = Component.literal("这力量 ").withStyle(ChatFormatting.RED);
                        MutableComponent e3 = Component.literal("成为你的").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
                        TitleUtils.sendPhasedTitleMessage(player, TASK_ADDER, e1, e2, e3);
                    }));
                }));
            }));
        }));
    }

    /**
     * 空我死亡保护切换初生形态
     */
    private static void switchToBaseForm(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        RiderConfig riderCfg = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElse(null);
        KamenRiderInterwovenWorldsMod.LOGGER.info("死亡保护切换初生形态:{}", data.getActiveRiderName());
        if (riderCfg == null) return;

        // 注销成长监听
        GrowingKuugaHenshinListener listener = GROWING_LISTENERS.remove(player.getUUID());
        if (listener != null) listener.unregister();

        // 解除当前形态
        FormConfig curForm = riderCfg.getForm(data.getActiveFormName());
        if (curForm != null) curForm.getBehavior().handleDeactivation(player, riderCfg, curForm);
        // 切初生
        FormConfig baseForm = riderCfg.getForm("base");
        if (baseForm != null) baseForm.getBehavior().handleQuickTransformation(player, riderCfg, baseForm);
    }

    /**
     * 基于TitleUtils常量计算分段标题总耗时，用于错开下一段文字
     */
    private static int calcTextDuration(String... phrases) {
        int typingTotal = 0;
        for (String str : phrases) {
            // 打字单字符延迟
            typingTotal += str.length() * TitleUtils.DEFAULT_TYPEWRITER_DELAY;
            // 词组间隔
            typingTotal += TitleUtils.DEFAULT_TYPEWRITER_DELAY * 3;
        }
        // 标题淡入-停留-淡出基础时间
        typingTotal += TitleUtils.DEFAULT_FADE_IN + TitleUtils.DEFAULT_STAY + TitleUtils.DEFAULT_FADE_OUT;
        return typingTotal;
    }
}