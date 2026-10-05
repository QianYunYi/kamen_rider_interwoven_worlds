package com.qianyunyi.rider.block.listener;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlockEntities;
import com.qianyunyi.rider.block.renderer.ShockerOperatingTableTileRenderer;
import com.qianyunyi.rider.block.renderer.KuugaSarcophagusTileRenderer;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientListener {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(KamenRiderInterwovenWorldsModBlockEntities.SHOCKER_OPERATING_TABLE.get(), context -> new ShockerOperatingTableTileRenderer());
		event.registerBlockEntityRenderer(KamenRiderInterwovenWorldsModBlockEntities.KUUGA_SARCOPHAGUS.get(), context -> new KuugaSarcophagusTileRenderer());
	}
}
