package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.client.camera.CameraController;
import com.qianyunyi.rider.network.packet.CameraPerspectiveSyncMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 相机视角切换消息处理器
 * 1 = 越肩视角（保存当前视角 → 平滑过渡到越肩）
 * 2 = 救援追踪哥莱姆视角（镜头持续给哥莱姆特写）
 * 0 = 恢复正常（平滑过渡回原视角）
 */
public class CameraPerspectiveHandler {
    private static final Map<String, BiConsumer<CameraPerspectiveSyncMessage, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    CameraController.CameraMode mode;
                    int p = msg.getPerspective();
                    if (p == 2) {
                        mode = CameraController.CameraMode.TRACKING_GOURAM;
                    } else if (p == 1) {
                        mode = CameraController.CameraMode.OVER_SHOULDER;
                    } else {
                        mode = CameraController.CameraMode.NORMAL;
                    }
                    CameraController.getInstance().requestMode(mode);
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<CameraPerspectiveSyncMessage, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }
}