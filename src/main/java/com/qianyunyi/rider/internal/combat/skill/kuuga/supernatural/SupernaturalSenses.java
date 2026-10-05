package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SupernaturalSensesTrackPacket;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.MusicHandler;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class SupernaturalSenses implements IRiderSkill {

    private final SoundEvent PEGASUS_ABILITY = KamenRiderInterwovenWorldsModSounds.FRONT_PART_PEGASUS_ABILITY.get();
    private static final Map<UUID, Long> PLAYER_COOLDOWN = new HashMap<>();
    private static final Map<UUID, SneakTrackingState> SNEAK_TRACKING_STATES = new HashMap<>();

    private static final int SENSE_DURATION_TICKS = 500;
    private static final float SENSE_SCOPE = 30.0F;
    private static final int COOLDOWN_TICKS = 20;
    private static final float ENERGY_COST_PER_SECOND = 2.0f;

    // 潜行追踪模式参数
    private static final long SNEAK_ACTIVATION_TICKS = 20;   // 潜行1秒（20 tick）激活追踪
    private static final long TARGET_SWITCH_INTERVAL = 20;   // 每1秒（20 tick）切换目标

    /**
     * 每个玩家的潜行追踪状态
     */
    private static class SneakTrackingState {
        long sneakStartTick = -1;            // 开始潜行的 tick
        boolean isTracking = false;          // 是否处于追踪模式
        int currentTargetIndex = 0;          // 当前目标在排序列表中的索引
        List<Integer> sortedEntityIds = new ArrayList<>();  // 按距离排序的实体ID列表
        long lastSwitchTick = 0;             // 上次切换目标的 tick
    }

    @Override
    public String getDisplayName() {
        return "超自然感官";
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/supernatural_senses.png");
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        UUID playerUuid = serverPlayer.getUUID();

        if (!isAvailable(serverPlayer)) {
            return;
        }

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        if (kuugaData.isSupernaturalSensesActive()) {
            stopSenses(serverPlayer);
        } else {
            startSenses(serverPlayer);
        }

        PLAYER_COOLDOWN.put(playerUuid, serverPlayer.level().getGameTime());
    }

    private void startSenses(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        kuugaData.setSupernaturalSensesActive(true);
        MusicHandler.playLoopingSound(serverPlayer, PEGASUS_ABILITY, SENSE_DURATION_TICKS);

        SpecialEffectsUtil.highlightNearbyEnemies(SENSE_SCOPE, serverPlayer, SENSE_DURATION_TICKS);

        UUID taskId = DelayedTask.repeat(serverPlayer.level(), 20, level -> {
            if (level.getPlayerByUUID(serverPlayer.getUUID()) instanceof ServerPlayer player) {
                RiderPlayerData playerData = RiderPlayerVariablesProvider.get(player);
                if (playerData == null) return;

                KuugaDataModule playerKuugaData = playerData.getKuugaData();
                if (playerKuugaData == null) return;

                if (!playerKuugaData.isSupernaturalSensesActive()) {
                    return;
                }

                if (playerKuugaData.getAmadamEnergy() < ENERGY_COST_PER_SECOND) {
                    stopSenses(player);
                    return;
                }

                playerKuugaData.removeAmadamEnergy(ENERGY_COST_PER_SECOND);
                SpecialEffectsUtil.highlightNearbyEnemies(SENSE_SCOPE, player, SENSE_DURATION_TICKS);

                // === 潜行追踪模式逻辑 ===
                updateSneakTracking(player, level.getGameTime());
            }
        });

        if (taskId != null) {
            kuugaData.setSupernaturalSensesTaskId(taskId);
        }
    }

    /**
     * 更新潜行追踪状态：检测潜行时长、切换目标、发送数据包
     */
    private void updateSneakTracking(ServerPlayer player, long currentTick) {
        UUID uuid = player.getUUID();
        SneakTrackingState state = SNEAK_TRACKING_STATES.computeIfAbsent(uuid, k -> new SneakTrackingState());

        boolean isSneaking = player.isCrouching();

        if (isSneaking) {
            // 记录开始潜行的 tick
            if (state.sneakStartTick == -1) {
                state.sneakStartTick = currentTick;
            }

            long sneakElapsed = currentTick - state.sneakStartTick;

            if (sneakElapsed >= SNEAK_ACTIVATION_TICKS && !state.isTracking) {
                // 潜行超过1秒，激活追踪模式
                startTrackingMode(player, state, currentTick);
            }

            if (state.isTracking) {
                // 已激活追踪模式：检查是否需要切换目标
                long timeSinceLastSwitch = currentTick - state.lastSwitchTick;
                if (timeSinceLastSwitch >= TARGET_SWITCH_INTERVAL) {
                    advanceToNextTarget(player, state, currentTick);
                }

                // 每 tick 都发送当前目标给客户端（确保客户端始终有最新目标）
                sendTrackPacket(player, state);
            }
        } else {
            // 玩家已停止潜行
            if (state.isTracking) {
                // 退出追踪模式
                stopTrackingMode(player, state);
            }
            state.sneakStartTick = -1;
        }
    }

    /**
     * 启动追踪模式：先进入自视模式（-1），5秒后才开始扫描实体
     */
    private void startTrackingMode(ServerPlayer player, SneakTrackingState state, long currentTick) {
        state.isTracking = true;
        state.currentTargetIndex = -1;     // -1 表示自视模式：摄像机俯视玩家自身
        state.lastSwitchTick = currentTick; // 5秒后 currentTick - lastSwitchTick >= 100 时切换到第一个实体

        // 不立即扫描实体，先给予5秒自视时间
        sendTrackPacket(player, state);
    }

    /**
     * 刷新附近实体列表并按距离排序。
     * 注意：不重置 currentTargetIndex，由调用方按需管理。
     */
    private void refreshEntityList(ServerPlayer player, SneakTrackingState state) {
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SENSE_SCOPE),
                entity ->
                        entity != player &&
                                entity.isAlive() &&
                                !entity.isSpectator()
        );

        // 按距离从近到远排序
        nearbyEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        state.sortedEntityIds = new ArrayList<>();
        for (LivingEntity entity : nearbyEntities) {
            state.sortedEntityIds.add(entity.getId());
        }

        // 处理索引越界：自视模式(-1)保持不变；非自视模式越界则重置到0或回到自视
        if (state.currentTargetIndex != -1) {
            if (state.sortedEntityIds.isEmpty()) {
                state.currentTargetIndex = -1;  // 没有实体时回到自视模式
            } else if (state.currentTargetIndex >= state.sortedEntityIds.size()) {
                state.currentTargetIndex = 0;
            }
        }
    }

    /**
     * 切换到下一个目标（循环切换）。
     * 第一次调用时（currentTargetIndex == -1）从自视模式切换到第一个实体。
     */
    private void advanceToNextTarget(ServerPlayer player, SneakTrackingState state, long currentTick) {
        // 刷新实体列表（可能有新实体进入或旧实体离开/死亡）
        refreshEntityList(player, state);

        if (state.sortedEntityIds.isEmpty()) {
            // 没有实体可追踪，回到自视模式
            state.currentTargetIndex = -1;
            NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(true, -1), player);
            state.lastSwitchTick = currentTick;
            return;
        }

        if (state.currentTargetIndex == -1) {
            // 第一次切换：从自视模式切换到最近的第一个实体
            state.currentTargetIndex = 0;
        } else {
            // 正常循环切换：移到下一个实体
            state.currentTargetIndex = (state.currentTargetIndex + 1) % state.sortedEntityIds.size();
        }
        state.lastSwitchTick = currentTick;

        sendTrackPacket(player, state);
    }

    /**
     * 发送追踪数据包给客户端。
     * - currentTargetIndex == -1：发送自视模式（active=true, 无目标实体）
     * - 否则：发送当前追踪的实体 ID
     */
    private void sendTrackPacket(ServerPlayer player, SneakTrackingState state) {
        if (state.currentTargetIndex == -1) {
            // 自视模式：追踪激活，但无目标实体（客户端显示自视角）
            NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(true, -1), player);
            return;
        }

        if (state.sortedEntityIds.isEmpty()) {
            NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(true, -1), player);
            return;
        }

        int targetId;
        if (state.currentTargetIndex >= 0 && state.currentTargetIndex < state.sortedEntityIds.size()) {
            targetId = state.sortedEntityIds.get(state.currentTargetIndex);
        } else {
            targetId = state.sortedEntityIds.get(0);
            state.currentTargetIndex = 0;
        }

        NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(true, targetId), player);
    }

    /**
     * 停止追踪模式，清理状态并通知客户端
     */
    private void stopTrackingMode(ServerPlayer player, SneakTrackingState state) {
        state.isTracking = false;
        state.sortedEntityIds.clear();
        state.currentTargetIndex = 0;
        state.lastSwitchTick = 0;

        // 通知客户端停止追踪
        NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(false, -1), player);
    }

    private void stopSenses(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        kuugaData.setSupernaturalSensesActive(false);
        MusicHandler.stopLoopingSound(serverPlayer);

        SpecialEffectsUtil.clearHighlightNearbyEnemies(SENSE_SCOPE, serverPlayer);

        UUID taskId = kuugaData.getSupernaturalSensesTaskId();
        if (taskId != null) {
            DelayedTask.cancel(taskId);
            kuugaData.setSupernaturalSensesTaskId(null);
        }

        // 清理潜行追踪状态
        cleanupTrackingState(serverPlayer);
    }

    /**
     * 完全清理玩家的潜行追踪状态（技能关闭时调用）
     */
    private void cleanupTrackingState(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SneakTrackingState state = SNEAK_TRACKING_STATES.remove(uuid);
        if (state != null && state.isTracking) {
            NetworkHandler.sendToPlayer(new SupernaturalSensesTrackPacket(false, -1), player);
        }
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        if (isAutoRelease) {
            stopSenses(serverPlayer);
        }
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        UUID playerUuid = serverPlayer.getUUID();
        Long lastUseTime = PLAYER_COOLDOWN.get(playerUuid);

        if (lastUseTime == null) {
            return true;
        }

        long currentTick = serverPlayer.level().getGameTime();
        long elapsedTicks = currentTick - lastUseTime;

        return elapsedTicks >= COOLDOWN_TICKS;
    }
}