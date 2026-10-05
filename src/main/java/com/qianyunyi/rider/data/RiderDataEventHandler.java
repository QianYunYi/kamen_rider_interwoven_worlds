package com.qianyunyi.rider.data;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.event.SetupAnimationsEvent;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.handler.RiderDataSyncHandler;
import com.qianyunyi.rider.network.packet.HUDControlMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 骑士数据事件处理器 - 1.20.1 Capability 版本
 *
 * 相比旧版简化但仍需处理：
 * - PlayerTick: 服务端差异同步
 * - PlayerLoggedIn: 首次全量同步
 * - PlayerRespawn: 重生后全量同步（Capability自动克隆数据）
 * - PlayerChangedDimension: 维度切换同步
 * - PlayerLoggedOut: 清理运行时状态，重置 isHenshining
 * - PlayerEvent.Clone: Capability 需要手动复制数据（重要！）
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiderDataEventHandler {

    // 每个玩家的同步处理器（服务端）
    private static final Map<UUID, RiderDataSyncHandler> SYNC_HANDLERS = new ConcurrentHashMap<>();

    // 客户端追踪：上一帧是否处于冲刺状态（用于检测冲刺→非冲刺的转换）
    private static final Set<UUID> wasKickDashing = ConcurrentHashMap.newKeySet();

    // ========== Tick事件 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) {
            handleClientTick(player);
        } else if (player instanceof ServerPlayer serverPlayer) {
            handleServerTick(serverPlayer);
        }
    }

    private static void handleClientTick(Player player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        data.tick(player);

        // 骑士踢冲刺时，根据玩家视角旋转身体模型
        UUID uuid = player.getUUID();
        boolean isDashing = data.isKickDashing();
        if (isDashing) {
            float pitch = player.getXRot();
            SetupAnimationsEvent.setPitchAmount(player, pitch);
            SetupAnimationsEvent.setLeanAmount(player, 0);
            wasKickDashing.add(uuid);
        } else if (wasKickDashing.remove(uuid)) {
            // 仅在冲刺→非冲刺转换时重置动画，避免干扰摩托车等其他动画
            SetupAnimationsEvent.stopLeanAnimation(player);
        }
    }

    private static void handleServerTick(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        data.tick(player);

        // 差异同步
        RiderDataSyncHandler syncHandler = SYNC_HANDLERS.computeIfAbsent(
                player.getUUID(), k -> new RiderDataSyncHandler());
        syncHandler.serverTick(player);
    }

    // ========== 登录事件 ==========
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        data.setPlayer(serverPlayer);

        // 全量同步给登录玩家
        RiderDataSyncHandler syncHandler = new RiderDataSyncHandler();
        syncHandler.onPlayerLogin(serverPlayer);
        SYNC_HANDLERS.put(serverPlayer.getUUID(), syncHandler);

        // 触发模块登录回调（添加 null 检查以防御模块加载时序问题）
       KuugaDataModule kuuga = data.getKuugaData();
        if (kuuga != null) {
            kuuga.onPlayerLogin(serverPlayer);
        } else {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[RiderDataEventHandler] KuugaDataModule 为 null，跳过登录回调");
        }
        // TODO: 其他骑士模块

        // 重置技能状态（防止上次会话异常退出导致 skillActive 卡死）
        if (data.isSkillActive()) {
            data.setSkillActive(false);
        }

        // 恢复空我形态策略状态
        RiderAttackEventDispatcher.onPlayerReLogin(serverPlayer);

        // 恢复变身状态（属性修饰符、HUD等）
        restoreHenshinState(serverPlayer, data);

        KamenRiderInterwovenWorldsMod.LOGGER.info("Player logged in: {}",
                serverPlayer.getScoreboardName());
    }

    // ========== 重生事件 ==========

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Capability 的 copyOnDeath 不会自动触发，需要手动处理 Clone 事件
        // 见下面的 onPlayerClone 方法

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        data.setPlayer(serverPlayer);

        // 重置某些状态
        if (data.isSkillActive()) {
            data.setSkillActive(false);
        }

        // 全量同步
        RiderDataSyncHandler syncHandler = new RiderDataSyncHandler();
        syncHandler.onPlayerRespawn(serverPlayer);
        SYNC_HANDLERS.put(serverPlayer.getUUID(), syncHandler);

        // 恢复变身状态（属性修饰符、HUD等）
        restoreHenshinState(serverPlayer, data);

        KamenRiderInterwovenWorldsMod.LOGGER.info("Player respawned: {}",
                serverPlayer.getScoreboardName());
    }

    // ========== 维度切换 ==========

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        RiderDataSyncHandler syncHandler = SYNC_HANDLERS.get(serverPlayer.getUUID());
        if (syncHandler != null) {
            syncHandler.onChangedDimension(serverPlayer);
        }

        KamenRiderInterwovenWorldsMod.LOGGER.debug("Player changed dimension: {}",
                serverPlayer.getScoreboardName());
    }

    // ========== 登出事件 ==========

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);

            // 重置运行中状态，防止崩溃后 isHenshining 卡死
            if (data.isHenshining()) {
                data.setHenshining(false);
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[RiderDataEventHandler] 重置玩家 {} 的 isHenshining 状态", serverPlayer.getScoreboardName());
            }
            if (data.isSkillActive()) {
                data.setSkillActive(false);
            }

            // 触发模块登出回调（添加 null 检查以防御模块加载时序问题）
            KuugaDataModule kuuga = data.getKuugaData();
            if (kuuga != null) {
                kuuga.onPlayerLogout(serverPlayer);
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.warn(
                        "[RiderDataEventHandler] KuugaDataModule 为 null，跳过登出回调");
            }
            // TODO: 其他骑士模块

            // 清理形态策略和攻击事件分发器中的缓存，防止内存泄漏
            RiderAttackEventDispatcher.onFormLeave(serverPlayer);

            // 清理同步处理器
            SYNC_HANDLERS.remove(serverPlayer.getUUID());
        }

        KamenRiderInterwovenWorldsMod.LOGGER.debug("Player logged out: {}", player.getScoreboardName());
    }

    // ========== 死亡克隆事件（Capability 必需！）==========

    /**
     * 恢复变身状态（属性修饰符、HUD等）
     * 用于玩家登录和重生时，如果之前处于变身状态，重新应用属性和HUD
     */
    private static void restoreHenshinState(ServerPlayer player, RiderPlayerData data) {
        HenshinState state = data.getCurrentState();
        if (state != HenshinState.ACTIVE && state != HenshinState.ACTIVE_AWAKENING) {
            return;
        }

        String riderName = data.getActiveRiderName();
        String formName = data.getActiveFormName();
        if (riderName == null || riderName.isEmpty() || formName == null || formName.isEmpty()) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[RiderDataEventHandler] 玩家处于变身状态但缺少骑士/形态信息: rider={}, form={}",
                    riderName, formName);
            return;
        }

        RiderConfig riderConfig = RiderRegistryHelper.getConfig(riderName).orElse(null);
        if (riderConfig == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[RiderDataEventHandler] 未找到骑士配置: {}", riderName);
            return;
        }

        FormConfig formConfig = riderConfig.getForm(formName);
        if (formConfig == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[RiderDataEventHandler] 未找到形态配置: {}/{}", riderName, formName);
            return;
        }

        // 保存血量比例（应用修饰符前，防止因修饰符变更最大生命值导致误判半血救援）
        float healthRatio = Math.min(player.getHealth() / player.getMaxHealth(), 1.0f);

        // 重新应用属性修饰符
        HenshinUtil.applyAttributeModifiers(player, formConfig);

        // 按比例恢复血量
        player.setHealth(player.getMaxHealth() * healthRatio);

        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[RiderDataEventHandler] 为玩家 {} 恢复属性修饰符: {}/{}",
                player.getScoreboardName(), riderName, formName);

        // 判断形态配置是否有HUD，有则挂载对应形态的HUD
        if (formConfig.getHudManagerSupplier() != null) {
            NetworkHandler.sendToPlayer(new HUDControlMessage(riderName, formName, true), player);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[RiderDataEventHandler] 为玩家 {} 挂载HUD: {}/{}",
                    player.getScoreboardName(), riderName, formName);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 死亡后克隆数据
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        // 从旧玩家获取 Capability 数据
        original.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(oldCap -> {
            newPlayer.getCapability(RiderPlayerVariablesProvider.RIDER_DATA).ifPresent(newCap -> {
                // 复制 NBT 数据
                CompoundTag nbt = oldCap.serializeNBT();
                newCap.deserializeNBT(nbt);

                KamenRiderInterwovenWorldsMod.LOGGER.debug(
                        "Player data cloned for: {}", newPlayer.getScoreboardName());
            });
        });
    }
}