package com.qianyunyi.rider.client.effect;


import com.qianyunyi.rider.network.packet.MessageComicBlackWhiteEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ComicBlackWhiteEffectHandler {
    private static final Map<String, BiConsumer<MessageComicBlackWhiteEffect, NetworkEvent.Context>> HANDLERS = new HashMap<>();

    static {
        init();
    }

    public static void init() {
        HANDLERS.put("client", (msg, ctx) -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ctx.enqueueWork(() -> {
                    if (msg.isEnable()) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null) {
                            Entity entity = mc.level.getEntity(msg.getEntityId());
                            if (entity instanceof LivingEntity living) {
                                // 传入 invert 参数
                                ComicBlackWhiteRenderer.start(
                                        living,
                                        msg.getDuration(),
                                        msg.getIntensity(),
                                        msg.getInvert()
                                );
                            }
                        }
                    } else {
                        ComicBlackWhiteRenderer.stop();
                    }
                });
            }
            ctx.setPacketHandled(true);
        });
    }

    public static BiConsumer<MessageComicBlackWhiteEffect, NetworkEvent.Context> getHandler() {
        return HANDLERS.get("client");
    }
}