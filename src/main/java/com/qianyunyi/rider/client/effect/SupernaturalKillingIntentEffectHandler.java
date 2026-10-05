package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.network.packet.MessageSupernaturalKillingIntent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SupernaturalKillingIntentEffectHandler {
    private static final Map<String, BiConsumer<MessageSupernaturalKillingIntent, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) return;

                    if (msg.isEnable()) {
                        // 创造模式玩家不渲染超自然杀气渲染效果
                        if (mc.player != null && mc.player.isCreative()) return;
                        SupernaturalKillingIntentRenderer.start(msg.getDuration(), msg.getIntensity());
                    } else {
                        SupernaturalKillingIntentRenderer.stop();
                    }
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<MessageSupernaturalKillingIntent, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }
}