package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.client.camera.ScreenShakeController;
import com.qianyunyi.rider.network.packet.ScreenShakeSyncMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 屏幕震动消息处理器
 * 将服务器发来的震动请求转发给 ScreenShakeController
 */
public class ScreenShakeHandler {
    private static final Map<String, BiConsumer<ScreenShakeSyncMessage, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    ScreenShakeController.getInstance().addShake(
                            msg.getIntensity(), msg.getDurationTicks());
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<ScreenShakeSyncMessage, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }
}