package com.qianyunyi.rider.data;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 骑士数据 Capability 提供者 - 1.20.1
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RiderPlayerVariablesProvider implements ICapabilitySerializable<CompoundTag> {

    // ========== Capability 常量定义==========
    public static final Capability<RiderPlayerData> RIDER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation CAPABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "rider_data");

    // ========== 实例 ==========
    private final RiderPlayerData data;
    private final LazyOptional<RiderPlayerData> instance;

    // ========== 客户端检测 ==========
    public RiderPlayerVariablesProvider(Player player) {
        this.data = new RiderPlayerData();
        this.data.setPlayer(player);
        this.instance = LazyOptional.of(() -> data);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        // 为所有真实玩家（包括客户端）附着 RiderPlayerData
        // 客户端同样需要数据对象来接收 SyncRiderDataPacket 的同步内容
        if (event.getObject() instanceof Player player && !(player instanceof FakePlayer)) {
            event.addCapability(CAPABILITY_ID, new RiderPlayerVariablesProvider(player));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        // 关键修正：判断传入的 cap 是否是我们的 RIDER_DATA
        return RIDER_DATA.orEmpty(cap, instance);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    // ========== 便捷访问方法 ==========

    // 每玩家限速器：防止 Capability 未附着时刷屏
    private static final Map<UUID, Long> LAST_WARN_TIME = new ConcurrentHashMap<>();
    private static final long WARN_COOLDOWN_MS = 5000; // 5秒内只打印一次

    // 当 Capability 未附着时，缓存 fallback 实例避免每 tick new 对象
    private static final Map<UUID, RiderPlayerData> FALLBACK_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取玩家的骑士数据（安全方式）
     * 替代你原有的 PlayerDataManager.get(player)
     */
    public static RiderPlayerData get(Player player) {
        return player.getCapability(RIDER_DATA)
                .map(provider -> {
                    // 确保玩家引用是最新的（重生后可能需要更新）
                    if (provider.getPlayer() == null ||
                            !provider.getPlayer().getUUID().equals(player.getUUID())) {
                        provider.setPlayer(player);
                    }
                    return provider;
                })
                .orElseGet(() -> {
                    // 限速日志：避免刷屏
                    UUID uuid = player.getUUID();
                    long now = System.currentTimeMillis();
                    Long lastWarn = LAST_WARN_TIME.get(uuid);
                    if (lastWarn == null || now - lastWarn > WARN_COOLDOWN_MS) {
                        LAST_WARN_TIME.put(uuid, now);
                        KamenRiderInterwovenWorldsMod.LOGGER.warn(
                                "RiderPlayerData not attached to player: {} (side={})",
                                uuid,
                                player.level().isClientSide() ? "CLIENT" : "SERVER");
                    }
                    // 缓存 fallback 避免每 tick 创建新实例
                    return FALLBACK_CACHE.computeIfAbsent(uuid, k -> {
                        RiderPlayerData fb = new RiderPlayerData();
                        fb.setPlayer(player);
                        return fb;
                    });
                });
    }

    /**
     * 检查玩家是否有骑士数据
     */
    public static boolean has(Player player) {
        return player.getCapability(RIDER_DATA).isPresent();
    }
}