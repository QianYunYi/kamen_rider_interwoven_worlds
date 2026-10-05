package com.qianyunyi.rider.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 音乐处理工具类
 * 服务端：播放音乐给所有玩家
 * 客户端：播放音乐给特定玩家
 */
public class MusicHandler {
    private static final Map<String, Object> PLAYING_MUSIC = new HashMap<>();
    private static final Map<UUID, Map<String, Object>> PLAYER_MUSIC = new HashMap<>();
    private static final Map<UUID, UUID> SERVER_LOOPING_TASKS = new HashMap<>();
    private static final Map<UUID, SoundEvent> SERVER_LOOPING_SOUNDS = new HashMap<>();

    private MusicHandler() {
    }

    /* ============== 服务端方法 ============== */

    /**
     * 播放一次性音乐（服务端，通知附近所有玩家）
     */
    public static void playSound(Player player, SoundEvent sound) {
        if (sound == null || player == null) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 5.0F, 1.0F);
    }

    /**
     * 播放循环音乐（服务端，通知附近所有玩家）
     * @param player 玩家
     * @param sound 声音事件
     * @param intervalTicks 播放间隔（tick），建议设置为音效长度
     */
    public static void playLoopingSound(Player player, SoundEvent sound, int intervalTicks) {
        if (sound == null || player == null) {
            return;
        }

        UUID playerUuid = player.getUUID();

        stopLoopingSound(player);

        SERVER_LOOPING_SOUNDS.put(playerUuid, sound);
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 5.0F, 1.0F);

        UUID taskId = DelayedTask.repeat(player.level(), intervalTicks, level -> {
            Player p = level.getPlayerByUUID(playerUuid);
            if (p != null) {
                level.playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, 5.0F, 1.0F);
            }
        });

        if (taskId != null) {
            SERVER_LOOPING_TASKS.put(playerUuid, taskId);
        }
    }

    /**
     * 播放循环音乐（服务端，通知附近所有玩家，默认间隔20 tick）
     */
    public static void playLoopingSound(Player player, SoundEvent sound) {
        playLoopingSound(player, sound, 20);
    }

    /**
     * 停止循环音乐（服务端）
     */
    public static void stopLoopingSound(Player player) {
        if (player == null) {
            return;
        }

        UUID playerUuid = player.getUUID();
        UUID taskId = SERVER_LOOPING_TASKS.remove(playerUuid);
        if (taskId != null) {
            DelayedTask.cancel(taskId);
        }

        SoundEvent sound = SERVER_LOOPING_SOUNDS.remove(playerUuid);
        if (sound != null && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS));
        }
    }

    /**
     * 停止玩家的音乐（服务端）
     */
    public static void stopSound(Player player, SoundEvent sound) {
        if (sound == null || player == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.connection.send(new ClientboundStopSoundPacket(sound.getLocation(), SoundSource.PLAYERS));
    }

    /* ============== 客户端方法 ============== */

    /**
     * 播放一次性音乐（客户端，只给当前玩家）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playMusic(String musicId, SoundEvent sound) {
        playMusic(musicId, sound, 1.0F, 1.0F);
    }

    /**
     * 播放一次性音乐（客户端，只给当前玩家，自定义音量音调）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playMusic(String musicId, SoundEvent sound, float volume, float pitch) {
        if (sound == null || musicId == null) {
            return;
        }

        stopMusic(musicId);

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.MUSIC,
                volume,
                pitch,
                (RandomSource) ThreadLocalRandom.current(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
        );

        Minecraft.getInstance().getSoundManager().play(soundInstance);
        PLAYING_MUSIC.put(musicId, soundInstance);
    }

    /**
     * 播放循环音乐（客户端，只给当前玩家）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playLoopingMusic(String musicId, SoundEvent sound) {
        playLoopingMusic(musicId, sound, 1.0F, 1.0F);
    }

    /**
     * 播放循环音乐（客户端，只给当前玩家，自定义音量音调）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playLoopingMusic(String musicId, SoundEvent sound, float volume, float pitch) {
        if (sound == null || musicId == null) {
            return;
        }

        stopMusic(musicId);

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.MUSIC,
                volume,
                pitch,
                (RandomSource) ThreadLocalRandom.current(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
        );

        Minecraft.getInstance().getSoundManager().play(soundInstance);
        PLAYING_MUSIC.put(musicId, soundInstance);
    }

    /**
     * 停止指定音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void stopMusic(String musicId) {
        if (musicId == null) {
            return;
        }

        SoundInstance soundInstance = (SoundInstance) PLAYING_MUSIC.remove(musicId);
        if (soundInstance != null) {
            Minecraft.getInstance().getSoundManager().stop(soundInstance);
        }
    }

    /**
     * 停止所有音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void stopAllMusic() {
        for (Object obj : PLAYING_MUSIC.values()) {
            Minecraft.getInstance().getSoundManager().stop((SoundInstance) obj);
        }
        PLAYING_MUSIC.clear();
    }

    /**
     * 给特定玩家播放一次性音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playPlayerMusic(UUID playerUuid, String musicId, SoundEvent sound) {
        playPlayerMusic(playerUuid, musicId, sound, 1.0F, 1.0F);
    }

    /**
     * 给特定玩家播放一次性音乐（客户端，自定义音量音调）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playPlayerMusic(UUID playerUuid, String musicId, SoundEvent sound, float volume, float pitch) {
        if (playerUuid == null || musicId == null || sound == null) {
            return;
        }

        stopPlayerMusic(playerUuid, musicId);

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.MUSIC,
                volume,
                pitch,
                (RandomSource) ThreadLocalRandom.current(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
        );

        Minecraft.getInstance().getSoundManager().play(soundInstance);

        PLAYER_MUSIC.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(musicId, soundInstance);
    }

    /**
     * 给特定玩家播放循环音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playPlayerLoopingMusic(UUID playerUuid, String musicId, SoundEvent sound) {
        playPlayerLoopingMusic(playerUuid, musicId, sound, 1.0F, 1.0F);
    }

    /**
     * 给特定玩家播放循环音乐（客户端，自定义音量音调）
     */
    @OnlyIn(Dist.CLIENT)
    public static void playPlayerLoopingMusic(UUID playerUuid, String musicId, SoundEvent sound, float volume, float pitch) {
        if (playerUuid == null || musicId == null || sound == null) {
            return;
        }

        stopPlayerMusic(playerUuid, musicId);

        SimpleSoundInstance soundInstance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.MUSIC,
                volume,
                pitch,
                (RandomSource) ThreadLocalRandom.current(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
        );

        Minecraft.getInstance().getSoundManager().play(soundInstance);

        PLAYER_MUSIC.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(musicId, soundInstance);
    }

    /**
     * 停止玩家对应的音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void stopPlayerMusic(UUID playerUuid, String musicId) {
        if (playerUuid == null || musicId == null) {
            return;
        }

        Map<String, Object> playerMusicMap = PLAYER_MUSIC.get(playerUuid);
        if (playerMusicMap != null) {
            SoundInstance soundInstance = (SoundInstance) playerMusicMap.remove(musicId);
            if (soundInstance != null) {
                Minecraft.getInstance().getSoundManager().stop(soundInstance);
            }
        }
    }

    /**
     * 停止玩家的所有音乐（客户端）
     */
    @OnlyIn(Dist.CLIENT)
    public static void stopAllPlayerMusic(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }

        Map<String, Object> playerMusicMap = PLAYER_MUSIC.remove(playerUuid);
        if (playerMusicMap != null) {
            for (Object obj : playerMusicMap.values()) {
                Minecraft.getInstance().getSoundManager().stop((SoundInstance) obj);
            }
        }
    }
}