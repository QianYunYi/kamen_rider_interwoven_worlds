package com.qianyunyi.rider.internal.henshin.system;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 首次触发管理系统
 * 负责管理所有需要在玩家首次进入世界时触发的逻辑
 */
@Mod.EventBusSubscriber
public class FirstTimeTriggerSystem {

    // 注册表：存储所有首次触发处理器
    private static final Map<String, FirstTimeTriggerHandler> handlers = new ConcurrentHashMap<>();

    // 玩家触发状态缓存（避免频繁访问持久化存储）
    private static final Map<UUID, Set<String>> playerTriggerStatus = new ConcurrentHashMap<>();

    /**
     * 注册首次触发处理器
     * @param id 处理器唯一标识
     * @param handler 处理器实例
     */
    public static void registerHandler(String id, FirstTimeTriggerHandler handler) {
        handlers.put(id, handler);
    }

    /**
     * 注销首次触发处理器
     * @param id 处理器唯一标识
     */
    public static void unregisterHandler(String id) {
        handlers.remove(id);
    }

    /**
     * 玩家登录事件处理
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 初始化玩家触发状态缓存
        UUID playerId = player.getUUID();
        playerTriggerStatus.computeIfAbsent(playerId, k -> new HashSet<>());

        // 检查并执行所有首次触发逻辑
        handlers.forEach((id, handler) -> {
            if (!hasTriggered(player, id) && handler.shouldTrigger(player)) {
                handler.execute(player);
                markTriggered(player, id);
            }
        });
    }

    /**
     * 检查是否已触发
     * @param player 玩家
     * @param handlerId 处理器ID
     * @return 是否已触发
     */
    public static boolean hasTriggered(ServerPlayer player, String handlerId) {
        UUID playerId = player.getUUID();

        // 首先检查内存缓存
        if (playerTriggerStatus.containsKey(playerId) &&
                playerTriggerStatus.get(playerId).contains(handlerId)) {
            return true;
        }

        // 如果缓存中没有，查询持久化存储
        boolean triggered = handlers.get(handlerId).hasTriggered(player);

        // 更新缓存
        if (triggered) {
            playerTriggerStatus.computeIfAbsent(playerId, k -> new HashSet<>()).add(handlerId);
        }

        return triggered;
    }

    /**
     * 标记为已触发
     * @param player 玩家
     * @param handlerId 处理器ID
     */
    public static void markTriggered(ServerPlayer player, String handlerId) {
        UUID playerId = player.getUUID();

        // 更新内存缓存
        playerTriggerStatus.computeIfAbsent(playerId, k -> new HashSet<>()).add(handlerId);

        // 更新持久化存储
        handlers.get(handlerId).markTriggered(player);
    }

    /**
     * 首次触发处理器接口
     */
    public interface FirstTimeTriggerHandler {
        /**
         * 检查是否应该触发
         * @param player 玩家
         * @return 是否应该触发
         */
        boolean shouldTrigger(ServerPlayer player);

        /**
         * 执行触发逻辑
         * @param player 玩家
         */
        void execute(ServerPlayer player);

        /**
         * 检查是否已触发
         * @param player 玩家
         * @return 是否已触发
         */
        boolean hasTriggered(ServerPlayer player);

        /**
         * 标记为已触发
         * @param player 玩家
         */
        void markTriggered(ServerPlayer player);
    }
}