package com.qianyunyi.rider.event.listener.rider.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.TimeUtil;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DagubaSummonListener implements IPlayerEventListener {
    public static final String TYPE = "daguba_summon";

    // 事件状态
    private static final int IDLE = 0;
    private static final int WARNING = 1;
    private static final int SILENT_DOMAIN = 2;
    private static final int BOSS_SPAWNED = 3;

    // 时间常量
    private static final int DAGUBA_ARRIVAL_DURATION = 51 * 20; // 51秒
    private static final int WARNING_DELAY_AFTER_TEXT = 200; // 10秒

    private ServerPlayer serverPlayer;
    private boolean isRegistered = false;
    private final Random random = new Random();

    // 事件状态机
    private int eventState = IDLE;
    private int eventTimer = 0;
    private int warningCounter = 0;

    // 存储所有活动任务ID
    private final Set<UUID> activeTaskIds = ConcurrentHashMap.newKeySet();

    // 任务添加器
    private final Consumer<UUID> taskAdder = taskId -> {
        if (taskId != null) {
            activeTaskIds.add(taskId);
        }
    };

    // 死亡事件监听器实例
    private DagubaDeathListener deathEventListener;

    // 事件进行中标记
    public boolean isEventInProgress = false;

    @Override
    public void initialize(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
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
            KamenRiderInterwovenWorldsMod.LOGGER.info("注册监听器{}给玩家: {}", TYPE, serverPlayer.getUUID());
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
                KamenRiderInterwovenWorldsMod.LOGGER.info("注销监听器{}给玩家: {}", TYPE, serverPlayer.getUUID());
            }
        }

        if (deathEventListener != null) {
            deathEventListener.unregister();
            deathEventListener = null;
        }

        cancelAllTasks();

        eventState = IDLE;
        eventTimer = 0;
        warningCounter = 0;
    }

    private void cancelAllTasks() {
        for (UUID taskId : activeTaskIds) {
            DelayedTask.cancel(taskId);
        }
        activeTaskIds.clear();
    }

    private void addTask(UUID taskId) {
        if (taskId != null) {
            activeTaskIds.add(taskId);
        }
    }

    @Override
    public boolean shouldBeActive(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return !data.getKuugaData().hasSummonedDaguba();
    }

    @Override
    public boolean isPermanentlyDisabled(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return data.getKuugaData().hasSummonedDaguba();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    private boolean isInSafeState() {
        return serverPlayer != null && !serverPlayer.isDeadOrDying() && serverPlayer.isAlive();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 安全校验
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer)
                || event.player != serverPlayer
                || !isInSafeState()) {
            return;
        }

        // 全局声音关闭
        if (eventState == SILENT_DOMAIN) {
            closeOtherSounds();
        }

        // 状态机处理
        switch (eventState) {
            case IDLE -> handleIdleState();
            case WARNING -> handleWarningState(); // 修复：新增 WARNING 逻辑
            case SILENT_DOMAIN -> handleSilentDomainState();
        }
    }

    // 【修复】IDLE 只触发一次启动
    private void handleIdleState() {
        sendKuugaWarning();
        eventState = WARNING; // 进入等待状态
        eventTimer = WARNING_DELAY_AFTER_TEXT; // 10秒等待
    }

    // 【修复】新增：警告等待状态，倒计时结束进入静默领域
    private void handleWarningState() {
        if (eventTimer > 0) {
            eventTimer--;
        }

        // 倒计时结束 → 启动静默领域
        if (eventTimer <= 0) {
            eventState = SILENT_DOMAIN;
            eventTimer = DAGUBA_ARRIVAL_DURATION;
            warningCounter = 0;
        }
    }

    // 【修复】静默领域：每帧都执行计时器、失明、警告
    private void handleSilentDomainState() {
        if (eventTimer <= 0) {
            finalizeDagubaSummon();
            return;
        }

        eventTimer--;

        // 持续环境效果
        applySilentDomainEffects();

        // 25% 概率失明（每tick都判断）
        if (random.nextFloat() < 0.25F) {
            applyBlindnessEffect();
        }

        // 每2秒发一次随机警告
        if (eventTimer % 40 == 0) {
            sendRandomWarning();
        }
    }

    // 失明效果
    private void applyBlindnessEffect() {
        serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                60, // 3秒，短持续时间 + 频繁触发 = 持续失明
                0,
                false,
                false,
                false
        ));
    }

    // 最终召唤达古巴
    private void finalizeDagubaSummon() {
        TitleUtils.sendTitleMessage(serverPlayer, "来了", ChatFormatting.DARK_RED);

        if (deathEventListener != null) {
            deathEventListener.spawnDaguba();
        }

        eventState = BOSS_SPAWNED;
        isEventInProgress = false;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        data.getKuugaData().setHasSummonedDaguba(true);
    }

    // 播放登场音效 + 三段笑声
    private void playDagubaArrivalSound() {
        if (serverPlayer == null) return;

        SoundEvent arrival = KamenRiderInterwovenWorldsModSounds.DAGUBA_ARRIVAL.get();
        SoundEvent laugh = KamenRiderInterwovenWorldsModSounds.DAGUBA_LAUGHTER.get();

        serverPlayer.level().playSound(null,
                serverPlayer.blockPosition(),
                arrival,
                SoundSource.PLAYERS,
                1.0F, 1.0F);

        scheduleLaughterSound(4 * 20, 0.8F, 0.9F);
        scheduleLaughterSound(9 * 20, 0.9F, 0.8F);
        scheduleLaughterSound(14 * 20, 1.0F, 0.7F);
    }

    private void scheduleLaughterSound(int delayTicks, float volume, float pitch) {
        addTask(DelayedTask.run(serverPlayer.level(), delayTicks, level -> {
            float realPitch = pitch + random.nextFloat() * 0.2F;
            serverPlayer.level().playSound(null,
                    serverPlayer.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.DAGUBA_LAUGHTER.get(),
                    SoundSource.PLAYERS,
                    volume, realPitch);
        }));
    }

    // 启动事件：古老文字 + 音效 + 天气
    private void sendKuugaWarning() {
        if (!isInSafeState()) return;

        isEventInProgress = true;
        closeOtherSounds();
        playDagubaArrivalSound();

        // 雷暴天气
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(0, 6000, true, true);
        }

        // 注册死亡监听器
        deathEventListener = new DagubaDeathListener(this);
        deathEventListener.initialize(serverPlayer);
        deathEventListener.register();

        // 【关键】发送古文字字幕
        TitleUtils.sendAncientText(serverPlayer, taskAdder,
                "古の刻印が解かれし時",
                "闇の王は目を覚ます",
                "終焉の刻は近づいた...");

        // 凋零生成音效
        serverPlayer.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 0.5F);
    }

    // 关闭所有无关声音
    private void closeOtherSounds() {
        if (serverPlayer == null) return;

        // 使用原生数据包方式停止声音，避免命令解析开销
        net.minecraft.network.protocol.game.ClientboundStopSoundPacket musicPacket = 
            new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                null, net.minecraft.sounds.SoundSource.MUSIC);
        net.minecraft.network.protocol.game.ClientboundStopSoundPacket ambientPacket = 
            new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                null, net.minecraft.sounds.SoundSource.AMBIENT);
        net.minecraft.network.protocol.game.ClientboundStopSoundPacket blocksPacket = 
            new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                null, net.minecraft.sounds.SoundSource.BLOCKS);
        net.minecraft.network.protocol.game.ClientboundStopSoundPacket playersPacket = 
            new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                null, net.minecraft.sounds.SoundSource.PLAYERS);

        serverPlayer.connection.send(musicPacket);
        serverPlayer.connection.send(ambientPacket);
        serverPlayer.connection.send(blocksPacket);
        serverPlayer.connection.send(playersPacket);
    }

    // 随机警告字幕 + 音效
    private void sendRandomWarning() {
        warningCounter++;
        String msg;
        float pitch = 0.8F + random.nextFloat() * 0.4F;

        switch (warningCounter % 4) {
            case 1 -> {
                msg = "你仿佛听到了...";
                serverPlayer.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, pitch);
            }
            case 2 -> {
                msg = "...!!!";
                serverPlayer.playSound(SoundEvents.PILLAGER_CELEBRATE, 0.8F, pitch);
            }
            case 3 -> {
                msg = "......";
                serverPlayer.playSound(SoundEvents.VINDICATOR_CELEBRATE, 0.8F, pitch);
            }
            default -> {
                msg = "...?";
                serverPlayer.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.8F, pitch);
            }
        }

        TitleUtils.sendTitleMessage(serverPlayer, msg, ChatFormatting.DARK_PURPLE);
    }

    // 平滑过渡到当天夜晚
    private void applySilentDomainEffects() {
        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            TimeUtil.setToNightSmooth(serverLevel);
        }
    }
}