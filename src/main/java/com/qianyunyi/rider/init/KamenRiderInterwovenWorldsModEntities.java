package com.qianyunyi.rider.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;

import com.qianyunyi.rider.entity.kaijin.shocker.ShockerScientistEntity;
import com.qianyunyi.rider.entity.kaijin.shocker.ShockerMilitaryCombatantEntity;
import com.qianyunyi.rider.entity.kaijin.shocker.ShockerMilitaryCadreEntity;
import com.qianyunyi.rider.entity.kaijin.shocker.ShockerCombatantEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.NGamioZedaEntity;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramEntity;
import com.qianyunyi.rider.entity.kaijin.shocker.ColonelSolEntity;
import com.qianyunyi.rider.entity.motoycle.BeatChaser2000Entity;
import com.qianyunyi.rider.entity.projectile.TelekineticWaveProjectile;
import com.qianyunyi.rider.entity.kaijin.gurongi.BeJiminBaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGoomaGuEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGumunBaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuMebioDaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuBaduuBaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.GoBadaaBaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.GoGadoruBaEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGomaGuEnhancedEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuGomaGuUltimateEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.GarumeReEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.ZuZainDaEntity;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class KamenRiderInterwovenWorldsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KamenRiderInterwovenWorldsMod.MODID);
	public static final RegistryObject<EntityType<ShockerCombatantEntity>> SHOCKER_COMBATANT = register("shocker_combatant",
			EntityType.Builder.<ShockerCombatantEntity>of(ShockerCombatantEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ShockerCombatantEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<NDagubaZebaEntity>> N_DAGUBA_ZEBA = register("n_daguba_zeba",
			EntityType.Builder.<NDagubaZebaEntity>of(NDagubaZebaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(NDagubaZebaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<NGamioZedaEntity>> N_GAMIO_ZEDA = register("n_gamio_zeda",
			EntityType.Builder.<NGamioZedaEntity>of(NGamioZedaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(NGamioZedaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<GouramEntity>> GOURAM = register("gouram",
			EntityType.Builder.<GouramEntity>of(GouramEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(GouramEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ShockerScientistEntity>> SHOCKER_SCIENTIST = register("shocker_scientist",
			EntityType.Builder.<ShockerScientistEntity>of(ShockerScientistEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ShockerScientistEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ShockerMilitaryCombatantEntity>> SHOCKER_MILITARY_COMBATANT = register("shocker_military_combatant",
			EntityType.Builder.<ShockerMilitaryCombatantEntity>of(ShockerMilitaryCombatantEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.setCustomClientFactory(ShockerMilitaryCombatantEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ShockerMilitaryCadreEntity>> SHOCKER_MILITARY_CADRE = register("shocker_military_cadre",
			EntityType.Builder.<ShockerMilitaryCadreEntity>of(ShockerMilitaryCadreEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)
					.setCustomClientFactory(ShockerMilitaryCadreEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ColonelSolEntity>> COLONEL_SOL = register("colonel_sol",
			EntityType.Builder.<ColonelSolEntity>of(ColonelSolEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ColonelSolEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<BeatChaser2000Entity>> BEAT_CHASER_2000 = register("beat_chaser_2000",
			EntityType.Builder.<BeatChaser2000Entity>of(BeatChaser2000Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(BeatChaser2000Entity::new)

					.sized(0.6f, 1.8f));

	public static final RegistryObject<EntityType<ZuGoomaGuEntity>> ZU_GOOMA_GU = register("zu_gooma_gu",
			EntityType.Builder.<ZuGoomaGuEntity>of(ZuGoomaGuEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuGoomaGuEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuGumunBaEntity>> ZU_GUMUN_BA = register("zu_gumun_ba",
			EntityType.Builder.<ZuGumunBaEntity>of(ZuGumunBaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuGumunBaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuMebioDaEntity>> ZU_MEBIO_DA = register("zu_mebio_da",
			EntityType.Builder.<ZuMebioDaEntity>of(ZuMebioDaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuMebioDaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuBaduuBaEntity>> ZU_BADUU_BA = register("zu_baduu_ba",
			EntityType.Builder.<ZuBaduuBaEntity>of(ZuBaduuBaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuBaduuBaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<GoBadaaBaEntity>> GO_BADAA_BA = register("go_badaa_ba",
			EntityType.Builder.<GoBadaaBaEntity>of(GoBadaaBaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(GoBadaaBaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuGomaGuEnhancedEntity>> ZU_GOMA_GU_ENHANCED = register("zu_goma_gu_enhanced",
			EntityType.Builder.<ZuGomaGuEnhancedEntity>of(ZuGomaGuEnhancedEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuGomaGuEnhancedEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuGomaGuUltimateEntity>> ZU_GOMA_GU_ULTIMATE = register("zu_goma_gu_ultimate",
			EntityType.Builder.<ZuGomaGuUltimateEntity>of(ZuGomaGuUltimateEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuGomaGuUltimateEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<GarumeReEntity>> GARUME_RE = register("garume_re",
			EntityType.Builder.<GarumeReEntity>of(GarumeReEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(GarumeReEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<ZuZainDaEntity>> ZU_ZAIN_DA = register("zu_zain_da",
			EntityType.Builder.<ZuZainDaEntity>of(ZuZainDaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(ZuZainDaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<BeJiminBaEntity>> BE_JIMIN_BA = register("be_jimin_ba",
			EntityType.Builder.<BeJiminBaEntity>of(BeJiminBaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(BeJiminBaEntity::new)

					.sized(0.6f, 1.8f));
	public static final RegistryObject<EntityType<GoGadoruBaEntity>> GO_GADORU_BA = register("go_gadoru_ba",
			EntityType.Builder.<GoGadoruBaEntity>of(GoGadoruBaEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(GoGadoruBaEntity::new)

					.sized(0.6f, 1.8f));

	public static final RegistryObject<EntityType<TelekineticWaveProjectile>> TELEKINETIC_WAVE_PROJECTILE = register("telekinetic_wave_projectile",
			EntityType.Builder.<TelekineticWaveProjectile>of(TelekineticWaveProjectile::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3)

					.sized(0.5f, 0.5f));

	private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			ShockerCombatantEntity.init();
			NDagubaZebaEntity.init();
			NGamioZedaEntity.init();
			GouramEntity.init();
			ShockerScientistEntity.init();
			ShockerMilitaryCombatantEntity.init();
			ShockerMilitaryCadreEntity.init();
			ColonelSolEntity.init();
			BeatChaser2000Entity.init();
			ZuGoomaGuEntity.init();
			ZuGumunBaEntity.init();
			ZuMebioDaEntity.init();
			ZuBaduuBaEntity.init();
			GoBadaaBaEntity.init();
			ZuGomaGuEnhancedEntity.init();
			ZuGomaGuUltimateEntity.init();
			GarumeReEntity.init();
			ZuZainDaEntity.init();
			BeJiminBaEntity.init();
			GoGadoruBaEntity.init();
		});
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SHOCKER_COMBATANT.get(), ShockerCombatantEntity.createAttributes().build());
		event.put(N_DAGUBA_ZEBA.get(), NDagubaZebaEntity.createAttributes().build());
		event.put(N_GAMIO_ZEDA.get(), NGamioZedaEntity.createAttributes().build());
		event.put(GOURAM.get(), GouramEntity.createAttributes().build());
		event.put(SHOCKER_SCIENTIST.get(), ShockerScientistEntity.createAttributes().build());
		event.put(SHOCKER_MILITARY_COMBATANT.get(), ShockerMilitaryCombatantEntity.createAttributes().build());
		event.put(SHOCKER_MILITARY_CADRE.get(), ShockerMilitaryCadreEntity.createAttributes().build());
		event.put(COLONEL_SOL.get(), ColonelSolEntity.createAttributes().build());
		event.put(BEAT_CHASER_2000.get(), BeatChaser2000Entity.createAttributes().build());
		event.put(ZU_GOOMA_GU.get(), ZuGoomaGuEntity.createAttributes().build());
		event.put(ZU_GUMUN_BA.get(), ZuGumunBaEntity.createAttributes().build());
		event.put(ZU_MEBIO_DA.get(), ZuMebioDaEntity.createAttributes().build());
		event.put(ZU_BADUU_BA.get(), ZuBaduuBaEntity.createAttributes().build());
		event.put(GO_BADAA_BA.get(), GoBadaaBaEntity.createAttributes().build());
		event.put(ZU_GOMA_GU_ENHANCED.get(), ZuGomaGuEnhancedEntity.createAttributes().build());
		event.put(ZU_GOMA_GU_ULTIMATE.get(), ZuGomaGuUltimateEntity.createAttributes().build());
		event.put(GARUME_RE.get(), GarumeReEntity.createAttributes().build());
		event.put(ZU_ZAIN_DA.get(), ZuZainDaEntity.createAttributes().build());
		event.put(BE_JIMIN_BA.get(), BeJiminBaEntity.createAttributes().build());
		event.put(GO_GADORU_BA.get(), GoGadoruBaEntity.createAttributes().build());
	}
}