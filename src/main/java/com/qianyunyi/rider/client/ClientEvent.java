package com.qianyunyi.rider.client;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.effect.ComicBlackWhiteRenderer;
import com.qianyunyi.rider.client.effect.SonicWaveRenderer;
import com.qianyunyi.rider.client.effect.SupernaturalKillingIntentRenderer;
import com.qianyunyi.rider.client.effect.SupernaturalScanRenderer;
import com.qianyunyi.rider.client.effect.SupernaturalSensesTrackHandler;
import com.qianyunyi.rider.client.effect.WaterRippleRenderer;
import com.qianyunyi.rider.client.effect.YellowLightningRenderer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvent {

    public ClientEvent() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SupernaturalSensesTrackHandler.clientTick();
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            if (ComicBlackWhiteRenderer.isActive()) {
                ComicBlackWhiteRenderer.render(event.getPartialTick());
            }
            if (SupernaturalKillingIntentRenderer.isActive()) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null && !mc.player.isCreative()) {
                    SupernaturalKillingIntentRenderer.render(event.getPartialTick());
                }
            }
            WaterRippleRenderer.render(event);
            SonicWaveRenderer.render(event);
            SupernaturalScanRenderer.render(event);
            YellowLightningRenderer.render(event);
            SupernaturalSensesTrackHandler.renderOverlay(event);
        }
    }

}