package com.qianyunyi.rider.event.listener.rider.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.SnowStormManager;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DagubaSummon2Listener implements IPlayerEventListener {
    public static final String TYPE = "daguba_summon2";

    private static final int IDLE = 0;
    private static final int WARNING = 1;
    private static final int SILENT_DOMAIN = 2;
    private static final int BOSS_SPAWNED = 3;

    private static final int DAGUBA_ARRIVAL_DURATION = 51 * 20;
    private static final int WARNING_DELAY_AFTER_TEXT = 200;

    private ServerPlayer serverPlayer;
    private boolean isRegistered = false;
    private final Random random = new Random();

    private int eventState = IDLE;
    private int eventTimer = 0;
    private int warningCounter = 0;

    // 标记：开篇剧情是否全部结束
    private boolean isOpeningFinished = false;

    private final Set<UUID> activeTaskIds = ConcurrentHashMap.newKeySet();
    private final Consumer<UUID> taskAdder = taskId -> {
        if (taskId != null) activeTaskIds.add(taskId);
    };

    private DagubaDeathListener deathListener;
    public boolean isEventInProgress = false;

    @Override
    public void initialize(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
        if(eventState == IDLE) {
            sendKuugaFinalWarning();
            eventState = WARNING;
        }
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void register() {
        if (!isRegistered && serverPlayer != null) {
            MinecraftForge.EVENT_BUS.register(this);
            isRegistered = true;
            KamenRiderInterwovenWorldsMod.LOGGER.info("注册监听器 {} 给玩家: {}", TYPE, serverPlayer.getUUID());
        }
    }

    @Override
    public void unregister() {
        if (isEventInProgress) {
            KamenRiderInterwovenWorldsMod.LOGGER.info("事件进行中，暂不注销监听器");
            return;
        }

        if (isRegistered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            isRegistered = false;
            if (serverPlayer != null) {
                KamenRiderInterwovenWorldsMod.LOGGER.info("注销监听器 {} 给玩家: {}", TYPE, serverPlayer.getUUID());
            }
        }

        if (deathListener != null) {
            deathListener.unregister();
            deathListener = null;
        }

        cancelAllTasks();
        eventState = IDLE;
        eventTimer = 0;
        warningCounter = 0;
        isOpeningFinished = false; // 重置剧情标记
    }

    private void cancelAllTasks() {
        for (UUID taskId : activeTaskIds) {
            DelayedTask.cancel(taskId);
        }
        activeTaskIds.clear();
    }

    private void addTask(UUID taskId) {
        if (taskId != null) activeTaskIds.add(taskId);
    }

    @Override
    public boolean shouldBeActive(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return data.getKuugaData().hasSummonedDaguba() && !data.getKuugaData().hasSummonedDaguba2();
    }

    @Override
    public boolean isPermanentlyDisabled(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return data.getKuugaData().hasSummonedDaguba2();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    private boolean isInSafeState() {
        if (serverPlayer == null) return false;
        return !serverPlayer.isInWater() &&
                serverPlayer.getLastHurtByMobTimestamp() + 200 < serverPlayer.level().getGameTime();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != serverPlayer) return;

        switch (eventState) {
            case IDLE: handleIdleState(); break;
            case WARNING: handleWarningState(); break;
            case SILENT_DOMAIN: handleSilentDomainState(); break;
            case BOSS_SPAWNED: handleBossSpawnedState(); break;
        }
    }

    private void handleIdleState() {
        if (isInSafeState()) {
            eventState = WARNING;
            eventTimer = DAGUBA_ARRIVAL_DURATION;
        }
    }

    private void handleWarningState() {
        eventTimer--;
        // ============== 核心修改：只有剧情结束后才播放随机警告 ==============
        if (isOpeningFinished && eventTimer % 40 == 0 && eventTimer > 0) {
            sendSnowWarning();
        }
        if (eventTimer <= 5) transformToUltimateKuuga();
        if (eventTimer <= 0) finalizeDagubaSummon();
    }

    /**
     * 自动变身/切换为究极空我
     * 未变身 → 直接变身
     * 已变身 → 切换形态
     */
    private void transformToUltimateKuuga() {
        if (serverPlayer == null) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        // 究极空我的ID
        String ULTIMATE_FORM = "ultimate_form";

        String activeRider = data.getActiveRiderName();
        if (activeRider.isEmpty() || activeRider.equals(ULTIMATE_FORM)) return;
        RiderConfig riderConfig = RiderRegistryHelper.getConfig(activeRider).orElseThrow();

        // 解除当前形态
        FormConfig currentForm = riderConfig.getForm(data.getActiveFormName());
        if (currentForm != null) {
            currentForm.getBehavior().handleUnForm(serverPlayer, riderConfig, currentForm);
        }

        // 切换为究极空我形态
        FormConfig ultimateForm = riderConfig.getForm(ULTIMATE_FORM);
        if (ultimateForm != null) {
            ultimateForm.getBehavior().handleHenshin(serverPlayer, riderConfig, ultimateForm);
        }
    }

    /**
     * 解除当前形态
     */
    private static void deactivateCurrentForm(ServerPlayer serverPlayer) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        if (dataManager.getCurrentState() == HenshinState.ACTIVE || dataManager.getCurrentState() == HenshinState.ACTIVE_AWAKENING) {
            RiderConfig cfg = RiderRegistryHelper.getConfig(dataManager.getActiveRiderName()).orElseThrow();
            FormConfig formConfig = cfg.getForm(dataManager.getActiveRiderName());
            if (formConfig != null) {
                formConfig.getBehavior().handleUnForm(serverPlayer, cfg, formConfig);
            }
        }
    }

    private static void executeCommand(ServerPlayer player, String command) {
        if (player.getServer() != null) {
            var commandSource = player.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            player.getServer().getCommands().performPrefixedCommand(commandSource, command);
        }
    }

    private void handleSilentDomainState() {
        eventTimer--;
        // ============== 静默领域阶段也只在剧情结束后播放警告 ==============
        if (isOpeningFinished && eventTimer % 40 == 0 && eventTimer > 0) {
            sendSnowWarning();
        }
        if (eventTimer <= 0) spawnDaguba();
    }

    private void handleBossSpawnedState() {
        if (deathListener != null && deathListener.isDagubaDead()) endEvent();
    }

    private void sendKuugaFinalWarning() {
        if (serverPlayer == null) return;
        isEventInProgress = true;

        TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                Component.literal("成为清澈 ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Component.literal("战士以内心之力 ").withStyle(ChatFormatting.DARK_AQUA),
                Component.literal("极限 ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal("战斗 ").withStyle(ChatFormatting.DARK_RED),
                Component.literal("将邪恶 ").withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal("埋葬 ").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD),
                Component.literal("之时").withStyle(ChatFormatting.GRAY)
        );

        int totalDelay = calculateTotalDelay(
                "成为清澈 ", "战士以内心之力 ", "极限 ", "战斗 ", "将邪恶 ", "埋葬 ", "之时"
        );

        UUID taskId = DelayedTask.run(serverPlayer.level(), totalDelay, level -> {
            TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                    Component.literal("你 ").withStyle(ChatFormatting.WHITE),
                    Component.literal("将自己 ").withStyle(ChatFormatting.YELLOW),
                    Component.literal("邪恶 ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("消除 ").withStyle(ChatFormatting.GREEN),
                    Component.literal("究极的 ").withStyle(ChatFormatting.DARK_PURPLE),
                    Component.literal("黑暗 ").withStyle(ChatFormatting.BLACK),
                    Component.literal("会消失").withStyle(ChatFormatting.LIGHT_PURPLE)
            );

            int secondDelay = calculateTotalDelay(
                    "你 ", "将自己 ", "邪恶 ", "消除 ", "究极的 ", "黑暗 ", "会消失"
            );
            addTask(DelayedTask.run(serverPlayer.level(), secondDelay + WARNING_DELAY_AFTER_TEXT, l -> {
                startSnowstormEvent();
                eventTimer = DAGUBA_ARRIVAL_DURATION;
            }));
        });
        taskAdder.accept(taskId);
    }

    private void startSnowstormEvent() {
        if (serverPlayer == null) return;
        isEventInProgress = true;

        // ==============先开启暴风雪 ==============
        SnowStormManager.INSTANCE.startGlobalStorm();
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(
                    Integer.MAX_VALUE / 2,
                    Integer.MAX_VALUE,
                    true,
                    false
            );
            serverLevel.setWeatherParameters(0, 6000, true, false);
            serverLevel.setDayTime(6000);
        }

        // ============== 暴风雪开启后 再播放日文标题 ==============
        TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                Component.literal("九郎岳の").withStyle(ChatFormatting.WHITE),
                Component.literal("封印が").withStyle(ChatFormatting.GRAY),
                Component.literal("解かれた").withStyle(ChatFormatting.DARK_RED)
        );

        int totalDelay = calculateTotalDelay("九郎岳の", "封印が", "解かれた");
        UUID taskId = DelayedTask.run(serverPlayer.level(), totalDelay, level -> {
            TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                    Component.literal("闇の").withStyle(ChatFormatting.DARK_PURPLE),
                    Component.literal("力が").withStyle(ChatFormatting.DARK_GRAY),
                    Component.literal("蘇る").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
            );
            int secondDelay = calculateTotalDelay("闇の", "力が", "蘇る");
            addTask(DelayedTask.run(serverPlayer.level(), secondDelay + WARNING_DELAY_AFTER_TEXT, l -> {
                playDagubaArrivalSound();
                // ============== 全部剧情结束，开启警告 ==============
                isOpeningFinished = true;
            }));
        });
        taskAdder.accept(taskId);
    }

    private int calculateTotalDelay(String... phrases) {
        int total = 0;
        for (String p : phrases) {
            total += p.length() * TitleUtils.DEFAULT_TYPEWRITER_DELAY;
            total += TitleUtils.DEFAULT_TYPEWRITER_DELAY * 3;
        }
        return total;
    }

    private void sendSnowWarning() {
        if (serverPlayer == null) return;
        warningCounter++;
        String msg = switch (warningCounter % 5) {
            case 0 -> "戦いの時が来た";
            case 1 -> "古朗基の王が目を覚ます";
            case 2 -> "究極の闇が迫る";
            case 3 -> "雪原に笑い声が響く";
            default -> "戦士よ、覚悟せよ";
        };
        TitleUtils.sendTitleMessage(serverPlayer, msg, ChatFormatting.RED);
    }

    private void playDagubaArrivalSound() {
        if (serverPlayer == null) return;
        SoundEvent sound = KamenRiderInterwovenWorldsModSounds.DAGUBA_ARRIVAL.get();
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void finalizeDagubaSummon() {
        TitleUtils.sendTitleMessage(serverPlayer, "ダグバ、来たる", ChatFormatting.DARK_RED);
        eventState = SILENT_DOMAIN;
        eventTimer = DAGUBA_ARRIVAL_DURATION;
        deathListener = new DagubaDeathListener(this);
        deathListener.initialize(serverPlayer);
        deathListener.register();
    }

    private void spawnDaguba() {
        if (deathListener != null) deathListener.spawnDaguba();
        eventState = BOSS_SPAWNED;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
    }

    private void endEvent() {
        SnowStormManager.INSTANCE.stopGlobalStorm();

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(6000, 0, false, false);
        }
        isEventInProgress = false;
        unregister();
    }
}