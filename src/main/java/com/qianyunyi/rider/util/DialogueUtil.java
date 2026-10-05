package com.qianyunyi.rider.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 对话工具类
 */
public class DialogueUtil {

    /**
     * 发送对话消息
     * @param player 目标玩家
     * @param speaker 说话者名称
     * @param message 消息内容
     */
    public static void sendDialogue(ServerPlayer player, String speaker, String message) {
        if (player == null || speaker == null || message == null) {
            return;
        }
        String formattedMessage = "§8[§4" + speaker + "§8] §c" + message;
        Component component = Component.literal(formattedMessage);
        player.displayClientMessage(component, true);
    }

    /**
     * 向所有玩家发送对话消息
     * @param level 世界
     * @param speaker 说话者名称
     * @param message 消息内容
     */
    public static void broadcastDialogue(net.minecraft.world.level.Level level, String speaker, String message) {
        if (level == null || level.isClientSide || speaker == null || message == null) {
            return;
        }

        String formattedMessage = "§8[§4" + speaker + "§8] §c" + message;
        Component component = Component.literal(formattedMessage);
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(component, true);
            }
        }
    }
}