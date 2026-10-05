package com.qianyunyi.rider.entity.kaijin.gurongi.base;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.registry.GurongiSpawnRegistry;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 古朗基全局监听管理器 — 集中控制所有古朗基相关的 Forge 事件总线处理器。
 * <p>
 * <h3>设计意图</h3>
 * 古朗基的生成、基基鲁游戏等系统不应在非活跃状态下占用事件总线资源。
 * 本管理器通过读取 {@link AddonWorldData} 中的 {@code gurongiSpawn} 和
 * {@code gameStage} 字段，动态注册或注销 Forge 事件处理器：
 * <ul>
 *   <li><b>活跃</b> — 当任意维度的 {@code gurongiSpawn > 0 || gameStage != NONE}
 *       时，注册所有处理器，驱动生成去重清理和游戏管理器 tick。</li>
 *   <li><b>休眠</b> — 当所有维度 {@code gurongiSpawn == 0 && gameStage == NONE}
 *       时，从总线注销所有处理器，零开销。</li>
 * </ul>
 * <p>
 * 玩家通过刷怪蛋召唤古朗基不依赖事件总线处理器（直接执行实体创建逻辑），
 * 因此不受管理器休眠状态影响。
 *
 * @author QianYunYi
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GurongiGlobalManager {

    private static boolean registered = false;

    // ==================== 处理器实例（对象引用，支持 unregister） ====================

    private static final GameTickHandler GAME_TICK_HANDLER = new GameTickHandler();
    private static final SpawnCleanupHandler SPAWN_CLEANUP_HANDLER = new SpawnCleanupHandler();

    // ==================== 状态判定 ====================

    /**
     * 判断是否有任意维度需要古朗基事件监听处于活跃状态。
     * 只要有一个维度的 {@code gurongiSpawn > 0} 或 {@code gameStage != NONE}，
     * 即认为需要活跃。
     */
    public static boolean isActive() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        for (ServerLevel level : server.getAllLevels()) {
            AddonWorldData data = AddonWorldData.get(level);
            if (data.gurongiSpawn() > 0 || data.getGameStage() != GurongiGameStage.NONE) {
                return true;
            }
        }
        return false;
    }

    // ==================== 注册 / 注销 ====================

    /**
     * 注册所有古朗基 Forge 事件处理器到 {@link MinecraftForge#EVENT_BUS}。
     * 幂等：已注册时直接返回。
     */
    public static synchronized void enable() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.register(GAME_TICK_HANDLER);
        MinecraftForge.EVENT_BUS.register(SPAWN_CLEANUP_HANDLER);
        registered = true;
        KamenRiderInterwovenWorldsMod.LOGGER.info("[GurongiGlobalManager] 古朗基监听已启用");
    }

    /**
     * 从 {@link MinecraftForge#EVENT_BUS} 注销所有古朗基事件处理器。
     * 幂等：未注册时直接返回。
     */
    public static synchronized void disable() {
        if (!registered) return;
        MinecraftForge.EVENT_BUS.unregister(GAME_TICK_HANDLER);
        MinecraftForge.EVENT_BUS.unregister(SPAWN_CLEANUP_HANDLER);
        registered = false;
        KamenRiderInterwovenWorldsMod.LOGGER.info("[GurongiGlobalManager] 古朗基监听已禁用");
    }

    /**
     * 评估当前世界状态，自动启用或禁用古朗基事件监听。
     * 每次 {@link AddonWorldData} 中关键字段变化后应调用此方法。
     */
    public static void checkAndUpdate() {
        if (isActive()) {
            enable();
        } else {
            disable();
        }
    }

    // ==================== 内部处理器 ====================

    /**
     * 每 tick 结束时，驱动所有维度的 {@link GurongiGameManager#tick(ServerLevel)}。
     */
    private static final class GameTickHandler {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            for (ServerLevel level : server.getAllLevels()) {
                GurongiGameManager manager = GurongiGameManager.get(level);
                manager.tick(level);
            }
        }
    }

    /**
     * 每 tick 结束时，清理 {@link GurongiSpawnRegistry} 的去重记录，
     * 确保下一个 tick 的自然生成尝试不受前一个 tick 影响。
     */
    private static final class SpawnCleanupHandler {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            GurongiSpawnRegistry.clearSpawnedThisTick();
        }
    }

    // ====================================================================
    //  Forge 事件 — 自动检测世界状态变化
    // ====================================================================

    /**
     * 服务端启动完成后，检查一次是否应该启用古朗基监听。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        checkAndUpdate();
    }

    /**
     * 世界加载时，检查一次是否应该启用古朗基监听。
     * 覆盖维度加载/重新加载的场景。
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel) {
            checkAndUpdate();
        }
    }
}