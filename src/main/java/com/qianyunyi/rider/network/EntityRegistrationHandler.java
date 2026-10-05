package com.qianyunyi.rider.network;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.effect.FallingBlockEffectEntity;
import com.qianyunyi.rider.entity.effect.SkillEffectEntity;
import com.qianyunyi.rider.entity.effect.TransformationEffectEntity;
import com.qianyunyi.rider.entity.projectile.DragonWandEntity;
import com.qianyunyi.rider.entity.projectile.GoGadoruBaWindProjectile;
import com.qianyunyi.rider.entity.projectile.PegasusBowgunProjectile;
import com.qianyunyi.rider.entity.projectile.RisingDragonWandEntity;
import com.qianyunyi.rider.entity.projectile.UltimateDragonWandEntity;
import com.qianyunyi.rider.entity.projectile.WebProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistrationHandler {

    public static EntityType<FallingBlockEffectEntity> FALLING_BLOCK_EFFECT;
    public static EntityType<TransformationEffectEntity> TRANS_FORMATION_EFFECT;
    public static EntityType<SkillEffectEntity> SKILL_EFFECT;
    public static EntityType<PegasusBowgunProjectile> PEGASUS_BOWGUN_PROJECTILE;
    public static EntityType<DragonWandEntity> DRAGON_WAND_ENTITY;
    public static EntityType<RisingDragonWandEntity> RISING_DRAGON_WAND_ENTITY;
    public static EntityType<UltimateDragonWandEntity> ULTIMATE_DRAGON_WAND_ENTITY;
    public static EntityType<WebProjectile> WEB_PROJECTILE;
    public static EntityType<GoGadoruBaWindProjectile> GO_GADORU_BA_WIND_PROJECTILE;

    @SubscribeEvent
    public static void onRegisterEntities(RegisterEvent event) {
        event.register(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), helper -> {

            FALLING_BLOCK_EFFECT = EntityType.Builder
                    .<FallingBlockEffectEntity>of(FallingBlockEffectEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("falling_block_effect");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "falling_block_effect"),
                    FALLING_BLOCK_EFFECT
            );

            TRANS_FORMATION_EFFECT = EntityType.Builder
                    .<TransformationEffectEntity>of(TransformationEffectEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("trans_formation_effect");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "trans_formation_effect"),
                    TRANS_FORMATION_EFFECT
            );

            SKILL_EFFECT = EntityType.Builder
                    .<SkillEffectEntity>of(SkillEffectEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("skill_effect");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "skill_effect"),
                    SKILL_EFFECT
            );

            PEGASUS_BOWGUN_PROJECTILE = EntityType.Builder
                    .<PegasusBowgunProjectile>of(PegasusBowgunProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("pegasus_bowgun_projectile");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "pegasus_bowgun_projectile"),
                    PEGASUS_BOWGUN_PROJECTILE
            );

            DRAGON_WAND_ENTITY = EntityType.Builder
                    .<DragonWandEntity>of(DragonWandEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("dragon_wand_entity");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "dragon_wand_entity"),
                    DRAGON_WAND_ENTITY
            );

            RISING_DRAGON_WAND_ENTITY = EntityType.Builder
                    .<RisingDragonWandEntity>of(RisingDragonWandEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("rising_dragon_wand_entity");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "rising_dragon_wand_entity"),
                    RISING_DRAGON_WAND_ENTITY
            );

            ULTIMATE_DRAGON_WAND_ENTITY = EntityType.Builder
                    .<UltimateDragonWandEntity>of(UltimateDragonWandEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("ultimate_dragon_wand_entity");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "ultimate_dragon_wand_entity"),
                    ULTIMATE_DRAGON_WAND_ENTITY
            );

            WEB_PROJECTILE = EntityType.Builder
                    .<WebProjectile>of(WebProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(4)
                    .build("web_projectile");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "web_projectile"),
                    WEB_PROJECTILE
            );

            GO_GADORU_BA_WIND_PROJECTILE = EntityType.Builder
                    .<GoGadoruBaWindProjectile>of(GoGadoruBaWindProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(20)
                    .build("go_gadoru_ba_wind_projectile");
            helper.register(
                    ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "go_gadoru_ba_wind_projectile"),
                    GO_GADORU_BA_WIND_PROJECTILE
            );
        });
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        // event.put(FALLING_BLOCK_EFFECT, FallingBlockEffectEntity.createAttributes().build());
    }
}