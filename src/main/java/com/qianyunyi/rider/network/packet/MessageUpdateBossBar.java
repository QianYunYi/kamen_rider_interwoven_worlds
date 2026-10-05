package com.qianyunyi.rider.network.packet;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.gui.CustomBossBar;
import com.qianyunyi.rider.api.network.IMessage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateBossBar implements IMessage {
    private UUID bossBar;
    private int renderType;
    private ResourceLocation baseTexture;
    private ResourceLocation overlayTexture;
    private int baseHeight;
    private int baseTextureHeight;
    private int baseOffsetX;
    private int baseOffsetY;
    private int overlayOffsetX;
    private int overlayOffsetY;
    private int overlayWidth;
    private int overlayHeight;
    private int verticalIncrement;
    private int progressWidth;
    private int textColor;

    // 默认构造函数
    public MessageUpdateBossBar() {}

    // 完整参数的构造函数
    public MessageUpdateBossBar(UUID bossBar, int renderType,
                                ResourceLocation baseTexture, ResourceLocation overlayTexture,
                                int baseHeight, int baseTextureHeight,
                                int baseOffsetX, int baseOffsetY,
                                int overlayOffsetX, int overlayOffsetY,
                                int overlayWidth, int overlayHeight,
                                int verticalIncrement, int progressWidth,
                                int textColor) {
        this.bossBar = bossBar;
        this.renderType = renderType;
        this.baseTexture = baseTexture;
        this.overlayTexture = overlayTexture;
        this.baseHeight = baseHeight;
        this.baseTextureHeight = baseTextureHeight;
        this.baseOffsetX = baseOffsetX;
        this.baseOffsetY = baseOffsetY;
        this.overlayOffsetX = overlayOffsetX;
        this.overlayOffsetY = overlayOffsetY;
        this.overlayWidth = overlayWidth;
        this.overlayHeight = overlayHeight;
        this.verticalIncrement = verticalIncrement;
        this.progressWidth = progressWidth;
        this.textColor = textColor;
    }

    // 创建移除血条的消息
    public static MessageUpdateBossBar createRemoveMessage(UUID bossBar) {
        return new MessageUpdateBossBar(
                bossBar,
                -1,
                null, null,
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                0
        );
    }

    /**
     * 编码消息到字节缓冲区
     * @param buf 要编码的字节缓冲区
     */
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(bossBar);
        buf.writeInt(renderType);

        // 如果renderType不是-1，写入所有参数
        if (renderType != -1) {
            buf.writeResourceLocation(baseTexture);
            buf.writeResourceLocation(overlayTexture != null ? overlayTexture : ResourceLocation.fromNamespaceAndPath("", ""));
            buf.writeInt(baseHeight);
            buf.writeInt(baseTextureHeight);
            buf.writeInt(baseOffsetX);
            buf.writeInt(baseOffsetY);
            buf.writeInt(overlayOffsetX);
            buf.writeInt(overlayOffsetY);
            buf.writeInt(overlayWidth);
            buf.writeInt(overlayHeight);
            buf.writeInt(verticalIncrement);
            buf.writeInt(progressWidth);
            buf.writeInt(textColor);
        }
    }

    // 反序列化构造函数
    public MessageUpdateBossBar(FriendlyByteBuf buf) {
        this.bossBar = buf.readUUID();
        this.renderType = buf.readInt();

        if (renderType != -1) {
            this.baseTexture = buf.readResourceLocation();
            this.overlayTexture = buf.readResourceLocation();
            this.baseHeight = buf.readInt();
            this.baseTextureHeight = buf.readInt();
            this.baseOffsetX = buf.readInt();
            this.baseOffsetY = buf.readInt();
            this.overlayOffsetX = buf.readInt();
            this.overlayOffsetY = buf.readInt();
            this.overlayWidth = buf.readInt();
            this.overlayHeight = buf.readInt();
            this.verticalIncrement = buf.readInt();
            this.progressWidth = buf.readInt();
            this.textColor = buf.readInt();
        }
    }

    /**
     * 处理收到的消息
     * @param ctx 网络事件上下文
     */
    @Override
    public void handle(NetworkEvent.Context ctx) {
        // 确保在客户端线程执行
        ctx.enqueueWork(() -> {
            if (renderType == -1) {
                // 移除血条
                CustomBossBar.customBossBars.remove(bossBar);
            } else {
                // 创建自定义血条
                ChatFormatting formatting = ChatFormatting.getById(textColor);
                if (formatting == null) {
                    formatting = ChatFormatting.WHITE; // 默认颜色
                }

                CustomBossBar bar = new CustomBossBar(
                        baseTexture,
                        overlayTexture,
                        baseHeight,
                        baseTextureHeight,
                        baseOffsetX,
                        baseOffsetY,
                        overlayOffsetX,
                        overlayOffsetY,
                        overlayWidth,
                        overlayHeight,
                        verticalIncrement,
                        progressWidth,
                        formatting
                );

                // 注册血条
                CustomBossBar.customBossBars.put(bossBar, bar);

                KamenRiderInterwovenWorldsMod.LOGGER.info("注册自定义血条: {}", bossBar);
            }
        });
    }
}