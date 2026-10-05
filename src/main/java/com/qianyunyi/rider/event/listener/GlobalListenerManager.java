package com.qianyunyi.rider.event.listener;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.internal.henshin.armor.HenshinArmorGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.time.StopWatch;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 全局玩家监听器管理器
 *
 * 职责：
 * 1. 管理玩家的所有事件监听器
 * 2. 监听玩家登录、登出、重生事件
 * 3. 动态激活/停用监听器
 * 4. 提供线程安全的监听器管理
 */
@Mod.EventBusSubscriber
public class GlobalListenerManager {

    // 玩家ID到监听器集合的映射
    private static final Map<UUID, PlayerListenerSet> playerListeners = new ConcurrentHashMap<>();

    // 监听器类型的构造器缓存
    private static final List<Supplier<IPlayerEventListener>> listenerConstructors = new ArrayList<>();

    // 处理中的玩家集合（防止重复处理）
    private static final Set<UUID> processingPlayers = Collections.synchronizedSet(new HashSet<>());

    // 监听器构造器缓存
    private static final Map<Class<? extends IPlayerEventListener>, Constructor<? extends IPlayerEventListener>> constructorCache =
            new ConcurrentHashMap<>();

    static {
        initializeListenerConstructors();
    }

    /**
     * 初始化所有监听器构造器
     */
    private static void initializeListenerConstructors() {
       registerListenerConstructor(HenshinArmorGuard::new);
    }

    /**
     * 注册监听器构造器
     * @param constructor 监听器构造器
     */
    public static void registerListenerConstructor(Supplier<IPlayerEventListener> constructor) {
        if (constructor != null) {
            listenerConstructors.add(constructor);
        }
    }

    /**
     * 玩家登录事件处理
     * @param event 玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        String playerName = serverPlayer.getScoreboardName();

        if (tryBeginProcessing(playerId, "登录")) {
            return;
        }

        try {
            StopWatch stopWatch = StopWatch.createStarted();
            KamenRiderInterwovenWorldsMod.LOGGER.info("开始处理玩家 {} ({}) 的登录事件", playerName, playerId);

            // 触发玩家数据管理器的登录逻辑
            //PlayerDataManager.onPlayerLogin(serverPlayer);

            // 清理并重新初始化监听器
            cleanupPlayerListeners(playerId);
            Set<IPlayerEventListener> listeners = initializePlayerListeners(serverPlayer);
            playerListeners.put(playerId, new PlayerListenerSet(listeners));

            // 更新监听器状态
            updateListenerState(serverPlayer);

            stopWatch.stop();
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "玩家 {} 登录事件处理完成，初始化了 {} 个监听器，耗时 {}ms",
                    playerName, listeners.size(), stopWatch.getTime(TimeUnit.MILLISECONDS)
            );
        } finally {
            processingPlayers.remove(playerId);
        }
    }

    /**
     * 玩家重生事件处理
     * @param event 玩家重生事件
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        String playerName = serverPlayer.getScoreboardName();

        if (tryBeginProcessing(playerId, "重生")) {
            return;
        }

        try {
            KamenRiderInterwovenWorldsMod.LOGGER.info("处理玩家 {} 的重生事件", playerName);

            // 清理并重新初始化监听器
            cleanupPlayerListeners(playerId);
            Set<IPlayerEventListener> listeners = initializePlayerListeners(serverPlayer);
            playerListeners.put(playerId, new PlayerListenerSet(listeners));

            // 更新监听器状态
            updateListenerState(serverPlayer);

            KamenRiderInterwovenWorldsMod.LOGGER.debug("玩家重生后重新初始化了 {} 个监听器", listeners.size());
        } finally {
            processingPlayers.remove(playerId);
        }
    }

    /**
     * 玩家登出事件处理
     * @param event 玩家登出事件
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        String playerName = serverPlayer.getScoreboardName();

        KamenRiderInterwovenWorldsMod.LOGGER.info("处理玩家 {} 的登出事件", playerName);

        // 触发离线处理逻辑
        //PlayerDataManager.onPlayerRespawn(serverPlayer);

        // 清理监听器
        cleanupPlayerListeners(playerId);

        KamenRiderInterwovenWorldsMod.LOGGER.debug("为玩家 {} 清理了所有监听器", playerName);
    }

    /**
     * 开始处理玩家事件（防止重复处理）
     * @param playerId 玩家ID
     * @param eventType 事件类型
     * @return 是否可以开始处理
     */
    private static boolean tryBeginProcessing(UUID playerId, String eventType) {
        if (processingPlayers.contains(playerId)) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "玩家 {} 的{}事件已被处理，跳过重复处理",
                    playerId, eventType
            );
            return true;
        }

        processingPlayers.add(playerId);
        return false;
    }

    /**
     * 清理玩家的所有监听器
     * @param playerId 玩家ID
     */
    private static void cleanupPlayerListeners(UUID playerId) {
        PlayerListenerSet listenerSet = playerListeners.remove(playerId);
        if (listenerSet != null) {
            listenerSet.unregisterAll();
        }
    }

    /**
     * 为玩家初始化监听器
     * @param serverPlayer 服务器玩家实例
     * @return 监听器集合
     */
    private static Set<IPlayerEventListener> initializePlayerListeners(ServerPlayer serverPlayer) {
        Set<IPlayerEventListener> listeners = new HashSet<>();
        UUID playerId = serverPlayer.getUUID();

        for (Supplier<IPlayerEventListener> constructor : listenerConstructors) {
            try {
                IPlayerEventListener listener = constructor.get();
                listener.initialize(serverPlayer);

                if (!listener.isPermanentlyDisabled(serverPlayer)) {
                    listeners.add(listener);
                    KamenRiderInterwovenWorldsMod.LOGGER.debug(
                            "为玩家 {} 创建监听器: {}",
                            playerId, listener.getType()
                    );
                }
            } catch (Exception e) {
                KamenRiderInterwovenWorldsMod.LOGGER.error(
                        "创建监听器实例失败: {}",
                        e.getMessage(), e
                );
            }
        }

        return listeners;
    }

    /**
     * 根据条件动态激活/停用监听器
     * @param serverPlayer 服务器玩家实例
     */
    public static void updateListenerState(ServerPlayer serverPlayer) {
        UUID playerId = serverPlayer.getUUID();
        PlayerListenerSet listenerSet = playerListeners.get(playerId);

        if (listenerSet == null) {
            return;
        }

        listenerSet.updateState(serverPlayer);
    }

    /**
     * 获取玩家的监听器集合
     * @param playerId 玩家ID
     * @return 监听器集合，如果不存在则返回空集合
     */
    public static Set<IPlayerEventListener> getPlayerListeners(UUID playerId) {
        PlayerListenerSet listenerSet = playerListeners.get(playerId);
        return listenerSet != null ? listenerSet.listeners() : Collections.emptySet();
    }

    /**
     * 玩家监听器集合包装类
     */
    private record PlayerListenerSet(Set<IPlayerEventListener> listeners) {
        private PlayerListenerSet(Set<IPlayerEventListener> listeners) {
            this.listeners = listeners != null ? listeners : new HashSet<>();
        }

        @Override
        public Set<IPlayerEventListener> listeners() {
            return Collections.unmodifiableSet(listeners);
        }

        /**
         * 注销所有监听器
         */
        public void unregisterAll() {
            for (IPlayerEventListener listener : listeners) {
                try {
                    listener.unregister();
                } catch (Exception e) {
                    KamenRiderInterwovenWorldsMod.LOGGER.error(
                            "注销监听器失败: {}",
                            listener.getType(), e
                    );
                }
            }
            listeners.clear();
        }

        /**
         * 更新监听器状态
         *
         * @param serverPlayer 服务器玩家实例
         */
        public void updateState(ServerPlayer serverPlayer) {
            for (IPlayerEventListener listener : listeners) {
                try {
                    boolean shouldBeActive = listener.shouldBeActive(serverPlayer);
                    boolean isRegistered = listener.isRegistered();

                    if (shouldBeActive && !isRegistered) {
                        listener.register();
                        KamenRiderInterwovenWorldsMod.LOGGER.debug(
                                "激活玩家 {} 的监听器: {}",
                                serverPlayer.getUUID(), listener.getType()
                        );
                    } else if (!shouldBeActive && isRegistered) {
                        listener.unregister();
                        KamenRiderInterwovenWorldsMod.LOGGER.debug(
                                "停用玩家 {} 的监听器: {}",
                                serverPlayer.getUUID(), listener.getType()
                        );
                    }
                } catch (Exception e) {
                    KamenRiderInterwovenWorldsMod.LOGGER.error(
                            "更新监听器状态失败: {}",
                            listener.getType(), e
                    );
                }
            }
        }
    }
}