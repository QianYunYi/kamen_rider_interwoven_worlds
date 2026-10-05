package com.qianyunyi.rider.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import com.qianyunyi.rider.client.renderer.ShockerScientistRenderer;
import com.qianyunyi.rider.client.renderer.ShockerMilitaryCombatantRenderer;
import com.qianyunyi.rider.client.renderer.ShockerMilitaryCadreRenderer;
import com.qianyunyi.rider.client.renderer.ShockerCombatantRenderer;
import com.qianyunyi.rider.client.renderer.NDagubaZebaRenderer;
import com.qianyunyi.rider.client.renderer.NGamioZedaRenderer;
import com.qianyunyi.rider.client.renderer.GouramRenderer;
import com.qianyunyi.rider.client.renderer.ColonelSolRenderer;
import com.qianyunyi.rider.client.renderer.BeatChaser2000Renderer;

import com.qianyunyi.rider.client.renderer.ZuGoomaGuRenderer;
import com.qianyunyi.rider.client.renderer.BeJiminBaRenderer;
import com.qianyunyi.rider.client.renderer.ZuGumunBaRenderer;
import com.qianyunyi.rider.client.renderer.ZuMebioDaRenderer;
import com.qianyunyi.rider.client.renderer.ZuBaduuBaRenderer;
import com.qianyunyi.rider.client.renderer.GoBadaaBaRenderer;
import com.qianyunyi.rider.client.renderer.GoGadoruBaRenderer;
import com.qianyunyi.rider.client.renderer.ZuGomaGuEnhancedRenderer;
import com.qianyunyi.rider.client.renderer.ZuGomaGuUltimateRenderer;
import com.qianyunyi.rider.client.renderer.GarumeReRenderer;
import com.qianyunyi.rider.client.renderer.ZuZainDaRenderer;
import com.qianyunyi.rider.client.renderer.entity.TelekineticWaveProjectileRenderer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KamenRiderInterwovenWorldsModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.SHOCKER_COMBATANT.get(), ShockerCombatantRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.N_DAGUBA_ZEBA.get(), NDagubaZebaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.N_GAMIO_ZEDA.get(), NGamioZedaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), GouramRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.SHOCKER_SCIENTIST.get(), ShockerScientistRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.SHOCKER_MILITARY_COMBATANT.get(), ShockerMilitaryCombatantRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.SHOCKER_MILITARY_CADRE.get(), ShockerMilitaryCadreRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.COLONEL_SOL.get(), ColonelSolRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.BEAT_CHASER_2000.get(), BeatChaser2000Renderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU.get(), ZuGoomaGuRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_GUMUN_BA.get(), ZuGumunBaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.BE_JIMIN_BA.get(), BeJiminBaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_MEBIO_DA.get(), ZuMebioDaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_BADUU_BA.get(), ZuBaduuBaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.GO_BADAA_BA.get(), GoBadaaBaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.GO_GADORU_BA.get(), GoGadoruBaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_GOMA_GU_ENHANCED.get(), ZuGomaGuEnhancedRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_GOMA_GU_ULTIMATE.get(), ZuGomaGuUltimateRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.GARUME_RE.get(), GarumeReRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.ZU_ZAIN_DA.get(), ZuZainDaRenderer::new);
		event.registerEntityRenderer(KamenRiderInterwovenWorldsModEntities.TELEKINETIC_WAVE_PROJECTILE.get(), TelekineticWaveProjectileRenderer::new);
	}
}