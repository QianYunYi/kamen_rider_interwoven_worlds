package com.qianyunyi.rider.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TitleUtils {
    // 默认时间常量
    public static final int DEFAULT_FADE_IN = 5;
    public static final int DEFAULT_STAY = 20;
    public static final int DEFAULT_FADE_OUT = 5;
    public static final int DEFAULT_TYPEWRITER_DELAY = 4;
    public static final int DEFAULT_ANCIENT_TEXT_INTERVAL = 40;

    /**
     * 发送简单标题消息
     */
    public static void sendTitleMessage(ServerPlayer player, String text, ChatFormatting color) {
        sendTitleMessage(player, text, color, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * 发送带自定义时间的标题消息
     */
    public static void sendTitleMessage(ServerPlayer player, String text, ChatFormatting color,
                                        int fadeIn, int stay, int fadeOut) {
        if (player == null) return;

        // 设置标题动画
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

        // 发送标题文本
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(text).withStyle(color)));
    }

    /**
     * 发送分阶段标题消息（打字机效果）
     */
    public static void sendPhasedTitleMessage(ServerPlayer player, Consumer<UUID> taskAdder,
                                              MutableComponent... components) {
        sendPhasedTitleMessage(player, taskAdder, DEFAULT_TYPEWRITER_DELAY,
                DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT, components);
    }

    /**
     * 发送带自定义参数的分阶段标题消息
     */
    public static void sendPhasedTitleMessage(ServerPlayer player, Consumer<UUID> taskAdder,
                                              int typewriterDelay, int fadeIn, int stay, int fadeOut,
                                              MutableComponent... components) {
        if (player == null) return;

        // 设置标题动画
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

        // 构建完整的文本
        MutableComponent fullText = Component.literal("");
        for (MutableComponent component : components) {
            fullText.append(component);
        }

        // 发送完整文本
        player.connection.send(new ClientboundSetTitleTextPacket(fullText));

        // 逐词显示打字机效果
        int delay = 0;
        for (MutableComponent component : components) {
            final String text = component.getString();
            for (int i = 0; i < text.length(); i++) {
                final int index = i;
                UUID taskId = DelayedTask.run(player.level(), delay, level -> {
                    String segment = text.substring(0, index + 1);
                    player.connection.send(new ClientboundSetTitleTextPacket(
                            Component.translatable(segment).withStyle(component.getStyle())
                    ));
                });
                taskAdder.accept(taskId);
                delay += typewriterDelay;
            }
            // 词之间增加额外间隔
            delay += typewriterDelay * 3;
        }
    }

    /**
     * 发送古老文字浮现效果（使用副标题）
     */
    public static void sendAncientText(ServerPlayer player, Consumer<UUID> taskAdder, String... lines) {
        sendAncientText(player, taskAdder, DEFAULT_ANCIENT_TEXT_INTERVAL, lines);
    }

    /**
     * 发送带自定义间隔的古老文字
     */
    public static void sendAncientText(ServerPlayer player, Consumer<UUID> taskAdder,
                                       int interval, String... lines) {
        if (player == null) return;

        for (int i = 0; i < lines.length; i++) {
            final int index = i;
            UUID taskId = DelayedTask.run(player.level(), i * interval, level -> {
                // 设置标题动画
                player.connection.send(new ClientboundSetTitlesAnimationPacket(
                        DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT));

                // 发送副标题文本
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.translatable(lines[index]).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                ));

                player.playSound(SoundEvents.AMBIENT_CAVE.get(), 0.8F, 0.5F);
            });
            taskAdder.accept(taskId);
        }
    }

    public static UUID sendPhasedSubtitleMessage(ServerPlayer player, Consumer<UUID> taskAdder,
                                                 int typewriterDelay, int fadeIn, int stay, int fadeOut,
                                                 String text) {
        if (player == null) return null;

        // 先发送一个空的主标题
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));

        // 设置标题动画
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));

        // 存储任务ID
        UUID[] taskId = {null};
        AtomicInteger charIndex = new AtomicInteger(0);

        // 创建递归任务显示字符
        Runnable showNextChar = new Runnable() {
            @Override
            public void run() {
                int index = charIndex.getAndIncrement();
                if (index < text.length()) {
                    String segment = text.substring(0, index + 1);
                    player.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.translatable(segment).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                    ));

                    // 安排下一个字符
                    taskId[0] = DelayedTask.run(player.level(), typewriterDelay, level -> this.run());
                    taskAdder.accept(taskId[0]);
                }
            }
        };

        // 开始显示第一个字符
        taskId[0] = DelayedTask.run(player.level(), 0, level -> showNextChar.run());
        taskAdder.accept(taskId[0]);

        return taskId[0];
    }
}