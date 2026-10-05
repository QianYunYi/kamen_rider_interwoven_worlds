package com.qianyunyi.rider.network.handler;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.IRiderDataModule;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SyncRiderDataPacket;
import com.qianyunyi.rider.data.RiderPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * 骑士数据同步处理器 - 1.20.1 Capability 版本
 *
 * 职责：
 * - 服务端Tick时检测脏数据并发送差异同步
 * - 处理登录/重生时的全量同步
 * - 支持追踪玩家同步（给其他玩家看）
 */
public class RiderDataSyncHandler {

    // ========== 性能配置 ==========
    private static final int SYNC_INTERVAL = 5;           // 每5tick检查一次
    private static final int MODULE_SYNC_INTERVAL = 2;    // 模块差异每2tick
    private static final int FORCE_FULL_SYNC_INTERVAL = 600; // 30秒强制全量同步

    // ========== 同步状态 ==========
    private int tickCounter = 0;
    private int lastFullSyncTick = 0;

    /**
     * 服务端每Tick调用
     */
    public void serverTick(ServerPlayer player) {
        tickCounter++;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (!data.isDirty()) return;

        boolean coreSynced = false;

        // 1. 高频模块差异同步（能量值等）
        if (tickCounter % MODULE_SYNC_INTERVAL == 0) {
            var dirtyModules = data.getDirtyModules();
            if (!dirtyModules.isEmpty()) {
                NetworkHandler.sendToPlayer(
                        SyncRiderDataPacket.modules(data, player.getUUID()),
                        player
                );
                // 只清模块脏标记，保留核心脏标记
                dirtyModules.forEach(IRiderDataModule::clearDirty);
            }
        }

        // 2. 核心字段同步（状态变化）
        if (tickCounter % SYNC_INTERVAL == 0) {
            NetworkHandler.sendToPlayer(
                    SyncRiderDataPacket.core(data, player.getUUID()),
                    player
            );
            coreSynced = true;
        }

        // 3. 定期全量同步（兜底）
        if (tickCounter - lastFullSyncTick >= FORCE_FULL_SYNC_INTERVAL) {
            forceFullSync(player);
            coreSynced = true;
        }

        // 只有核心同步成功发送后才清除核心脏标记，避免丢失状态变更
        if (coreSynced) {
            data.clearDirty();
        }
    }

    /**
     * 强制全量同步（登录、重生、手动调用）
     */
    public void forceFullSync(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        NetworkHandler.sendToPlayer(
                new SyncRiderDataPacket(data, player.getUUID()),
                player
        );

        lastFullSyncTick = tickCounter;
        data.clearDirty();

        KamenRiderInterwovenWorldsMod.LOGGER.debug("Full sync sent to {}", player.getScoreboardName());
    }

    /**
     * 同步给追踪该玩家的其他玩家
     * 用于显示其他玩家的骑士形态等
     */
    public void syncToTrackingPlayers(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 只同步核心字段给追踪玩家（减少带宽）
        NetworkHandler.sendToTrackingPlayers(
                SyncRiderDataPacket.core(data, player.getUUID()),
                player
        );
    }

    /**
     * 玩家登录时调用 - 发送全量同步
     */
    public void onPlayerLogin(ServerPlayer player) {
        forceFullSync(player);
    }

    /**
     * 玩家重生时调用 - 发送全量同步
     */
    public void onPlayerRespawn(ServerPlayer player) {
        forceFullSync(player);
    }

    /**
     * 维度切换时调用
     */
    public void onChangedDimension(ServerPlayer player) {
        forceFullSync(player);
    }
}