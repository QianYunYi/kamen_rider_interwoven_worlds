package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.network.packet.YellowLightningSyncMessage;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class YellowLightningEffectHandler {
    private static final Map<String, BiConsumer<YellowLightningSyncMessage, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    YellowLightningRenderer.setState(
                            msg.getEntityId(),
                            msg.getPhase(),
                            msg.getX(), msg.getY(), msg.getZ(),
                            msg.getDirX(), msg.getDirY(), msg.getDirZ(),
                            msg.getDuration(),
                            msg.getGameTime()
                    );
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<YellowLightningSyncMessage, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }
}