package com.qianyunyi.rider.event;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FORGE总线事件处理器
 * 处理游戏运行时事件
 */
@Mod.EventBusSubscriber(
        modid = KamenRiderInterwovenWorldsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class ForgeEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeEventHandler.class);

    /**
     * 服务器即将启动事件
     */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        LOGGER.info("[Kamen Rider] 服务器启动中，注册骑士配置...");
        long startTime = System.currentTimeMillis();

        // 注册骑士配置
        RiderRegistryHelper.autoRegisterAll();
        LOGGER.info("[Kamen Rider] 服务器端骑士配置已注册");

        // 初始化服务器专用内容
        //ServerHandler.onServerStarting(event.getServer());

        long endTime = System.currentTimeMillis();
        LOGGER.info("[Kamen Rider] 服务器配置注册完成，耗时 {} ms", endTime - startTime);
    }

    // ========== 死亡缓存：解决 PlayerEvent.Clone 不触发时的数据丢失问题 ==========

    /**
     * 玩家死亡时，将 Capability 数据保存到缓存中
     * 注意：必须使用 getCapability().ifPresent() 而非 get()，
     * 确保只保存真实 Capability 数据，避免保存 fallback 空数据
     */
    private static final Map<UUID, CompoundTag> DEATH_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(data -> {
                CompoundTag nbt = data.serializeNBT();
                DEATH_CACHE.put(player.getUUID(), nbt);
                LOGGER.debug("[Kamen Rider] 玩家 {} 死亡，数据已缓存", player.getName().getString());
            });
        }
    }

    /**
     * 玩家重生事件：从死亡缓存恢复数据
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            UUID uuid = serverPlayer.getUUID();

            // 优先从死亡缓存恢复数据
            CompoundTag cachedNbt = DEATH_CACHE.remove(uuid);
            if (cachedNbt != null) {
                // 使用 getCapability().ifPresent() 确保写入真实 Capability 实例
                serverPlayer.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(data -> {
                    data.deserializeNBT(cachedNbt);
                    LOGGER.debug("[Kamen Rider] 玩家 {} 重生，从缓存恢复数据成功",
                            serverPlayer.getName().getString());
                });
            } else {
                LOGGER.warn("[Kamen Rider] 玩家 {} 重生，但未找到死亡缓存数据",
                        serverPlayer.getName().getString());
            }

            // 兜底：尝试从 PlayerEvent.Clone 恢复（如果克隆事件已触发过）
            RiderPlayerData manager = RiderPlayerVariablesProvider.get(serverPlayer);
        }
    }

    /**
     * 玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            LOGGER.debug("[Kamen Rider] 玩家 {} 登录，同步数据", player.getName().getString());
            RiderPlayerData manager = RiderPlayerVariablesProvider.get(serverPlayer);
        }
    }

    /**
     * 玩家切换维度事件
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            LOGGER.debug("[Kamen Rider] 玩家 {} 切换维度，同步数据", player.getName().getString());
            RiderPlayerData manager = RiderPlayerVariablesProvider.get(serverPlayer);
        }
    }

    /**
     * 玩家克隆事件（死亡重生）
     *
     * 备用方案：如果 PlayerEvent.Clone 正常触发，也会尝试克隆数据
     * 但优先使用死亡缓存（onPlayerDeath + onPlayerRespawn）
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        Player oldPlayer = event.getOriginal();

        oldPlayer.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(oldCap -> {
            newPlayer.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(newCap -> {
                CompoundTag nbt = oldCap.serializeNBT();
                newCap.deserializeNBT(nbt);
                LOGGER.debug("[Kamen Rider] 玩家 {} 重生克隆，数据恢复成功",
                        newPlayer.getName().getString());
            });
        });
    }
}