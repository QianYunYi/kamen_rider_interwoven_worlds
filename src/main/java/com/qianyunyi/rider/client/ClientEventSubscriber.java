package com.qianyunyi.rider.client;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.effect.renderer.effect.FallingBlockEffectRenderer;
import com.qianyunyi.rider.client.effect.renderer.projectile.PegasusBowgunProjectileRenderer;
import com.qianyunyi.rider.client.effect.renderer.projectile.GoGadoruBaWindProjectileRenderer;
import com.qianyunyi.rider.client.effect.renderer.projectile.WebProjectileRenderer;
import com.qianyunyi.rider.client.effect.model.effect.SkillEffectEntityModel;
import com.qianyunyi.rider.client.effect.model.effect.TransformationEffectEntityModel;
import com.qianyunyi.rider.client.effect.renderer.effect.SkillEffectEntityRenderer;
import com.qianyunyi.rider.client.effect.renderer.effect.TransformationEffectEntityRenderer;
import com.qianyunyi.rider.client.renderer.entity.DragonWandEntityRenderer;
import com.qianyunyi.rider.client.renderer.entity.RisingDragonWandEntityRenderer;
import com.qianyunyi.rider.client.renderer.entity.UltimateDragonWandEntityRenderer;
import com.qianyunyi.rider.entity.layer.HorseGouramLayer;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册下落方块效果实体的渲染器
        EntityRenderers.register(
                EntityRegistrationHandler.FALLING_BLOCK_EFFECT,
                FallingBlockEffectRenderer::new
        );

        // 注册特殊效果实体的渲染器
        EntityRenderers.register(
                EntityRegistrationHandler.TRANS_FORMATION_EFFECT,
                context -> new TransformationEffectEntityRenderer(context, new TransformationEffectEntityModel())
        );
        // 注册技能效果实体的渲染器
        EntityRenderers.register(
                EntityRegistrationHandler.SKILL_EFFECT,
                context -> new SkillEffectEntityRenderer(context, new SkillEffectEntityModel())
        );

        EntityRenderers.register(
                EntityRegistrationHandler.DRAGON_WAND_ENTITY,
                DragonWandEntityRenderer::new
        );

        EntityRenderers.register(
                EntityRegistrationHandler.RISING_DRAGON_WAND_ENTITY,
                RisingDragonWandEntityRenderer::new
        );

        EntityRenderers.register(
                EntityRegistrationHandler.ULTIMATE_DRAGON_WAND_ENTITY,
                UltimateDragonWandEntityRenderer::new
        );

        EntityRenderers.register(
                EntityRegistrationHandler.PEGASUS_BOWGUN_PROJECTILE,
                PegasusBowgunProjectileRenderer::new
        );

        EntityRenderers.register(
                EntityRegistrationHandler.WEB_PROJECTILE,
                WebProjectileRenderer::new
        );

        EntityRenderers.register(
                EntityRegistrationHandler.GO_GADORU_BA_WIND_PROJECTILE,
                GoGadoruBaWindProjectileRenderer::new
        );
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Horse, HorseModel<Horse>> renderer = 
            (LivingEntityRenderer<Horse, HorseModel<Horse>>) event.getRenderer(EntityType.HORSE);
        if (renderer != null) {
            renderer.addLayer(new HorseGouramLayer(renderer));
        }
    }
}