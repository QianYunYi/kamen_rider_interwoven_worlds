package com.qianyunyi.rider.network.packet;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.event.SetupAnimationsEvent;
import com.qianyunyi.rider.api.network.IMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class AnimationMessage implements IMessage {
    private static final int MAX_ANIMATION_NAME_LENGTH = 256;

    private final String animationName;
    private final UUID playerUUID;
    private final boolean start;

    public AnimationMessage(String animationName, UUID playerUUID, boolean start) {
        if (animationName == null || animationName.isEmpty()) {
            throw new IllegalArgumentException("动画名称不能为空");
        }
        if (animationName.length() > MAX_ANIMATION_NAME_LENGTH) {
            throw new IllegalArgumentException("动画名称过长");
        }
        if (playerUUID == null) {
            throw new IllegalArgumentException("玩家UUID不能为null");
        }

        this.animationName = animationName;
        this.playerUUID = playerUUID;
        this.start = start;
    }

    public AnimationMessage(FriendlyByteBuf buffer) {
        this(
                buffer.readUtf(MAX_ANIMATION_NAME_LENGTH),
                buffer.readUUID(),
                buffer.readBoolean()
        );
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        try {
            buffer.writeUtf(animationName);
            buffer.writeUUID(playerUUID);
            buffer.writeBoolean(start);
        } catch (Exception e) {
            KamenRiderInterwovenWorldsMod.LOGGER.error("编码AnimationMessage失败: {}", e.getMessage());
            throw new RuntimeException("编码AnimationMessage失败", e);
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("收到来自非法方向的AnimationMessage");
                return;
            }

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) {
                        KamenRiderInterwovenWorldsMod.LOGGER.warn("客户端世界未加载");
                        return;
                    }

                    Player player = mc.level.getPlayerByUUID(playerUUID);
                    if (player != null) {
                        SetupAnimationsEvent.playAnimation(player, animationName, start);
                    }
                } catch (Exception e) {
                    KamenRiderInterwovenWorldsMod.LOGGER.error("处理动画消息时出错", e);
                }
            });
        });
        context.setPacketHandled(true);
    }
}