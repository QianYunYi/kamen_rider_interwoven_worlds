package com.qianyunyi.rider.entity.kaijin.shocker;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerCombatantBase;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShockerCombatantEntity extends ShockerCombatantBase {

	// 同步纹理/类别
	private static final EntityDataAccessor<Integer> DATA_TEXTURE_INDEX =
			SynchedEntityData.defineId(ShockerCombatantEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_CATEGORY_INDEX =
			SynchedEntityData.defineId(ShockerCombatantEntity.class, EntityDataSerializers.INT);

	// 同步FuseTime
	private static final EntityDataAccessor<Integer> DATA_FUSE_TIME =
			SynchedEntityData.defineId(ShockerCombatantEntity.class, EntityDataSerializers.INT);

	// 纹理
	private static final List<ResourceLocation> SHOCKER_COMBATANT = new ArrayList<>();
	private static final List<ResourceLocation> SHOCKER_COMBATANT_CAPTAIN = new ArrayList<>();
	private static final List<ResourceLocation> SHOCKER_BONE_COMBATANT = new ArrayList<>();
	private static final List<ResourceLocation> SHOCKER_BONE_COMBATANT_CAPTAIN = new ArrayList<>();
	private static final List<ResourceLocation> BERET_SHOCKER = new ArrayList<>();
	private static final List<ResourceLocation> BERET_SHOCKER_CAPTAIN = new ArrayList<>();
	private static final List<ResourceLocation> FEMALE_SHOCKER = new ArrayList<>();
	private static final List<ResourceLocation> FEMALE_SHOCKER_CAPTAIN = new ArrayList<>();

	static {
		SHOCKER_COMBATANT.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/shocker_combatant_3.png"));
		SHOCKER_COMBATANT_CAPTAIN.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/shocker_combatant_2.png"));

		SHOCKER_BONE_COMBATANT.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/shocker_combatant.png"));
		SHOCKER_BONE_COMBATANT_CAPTAIN.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/shocker_combatant_captain.png"));

		BERET_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/beret_shocker_combatant.png"));
		BERET_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/beret_shocker_combatant_2.png"));
		BERET_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/beret_shocker_combatant_3.png"));
		BERET_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/beret_shocker_combatant_4.png"));
		BERET_SHOCKER_CAPTAIN.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/beret_shocker_combatant_captain.png"));

		FEMALE_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/female_shocker_combatant.png"));
		FEMALE_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/female_shocker_combatant_3.png"));
		FEMALE_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/female_shocker_combatant_4.png"));
		FEMALE_SHOCKER.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/female_shocker_combatant_5.png"));
		FEMALE_SHOCKER_CAPTAIN.add(ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/female_shocker_combatant_captain.png"));
	}

	private static final int CATEGORY_SHOCKER = 0;
	private static final int CATEGORY_BERET = 1;
	private static final int CATEGORY_FEMALE = 2;
	private static final int CATEGORY_BONE_SHOCKER = 3;

	// 自爆常量
	private static final int MAX_FUSE_TIME = 30;
	private static final float EXPLOSION_RADIUS = 3.0f;
	private static final float EXPLOSION_DAMAGE = 12.0f;
	private static final double TRIGGER_HEALTH_PERCENT = 0.1;

	private int lastFuseTime = 0;

	// ====================== 【必加：MC 网络构造方法】======================
	public ShockerCombatantEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(KamenRiderInterwovenWorldsModEntities.SHOCKER_COMBATANT.get(), world);
	}

	// ====================== 正常构造方法 ======================
	public ShockerCombatantEntity(EntityType<ShockerCombatantEntity> type, Level world) {
		super(type, world);
		xpReward = 10;
		setNoAi(false);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_TEXTURE_INDEX, 0);
		this.entityData.define(DATA_CATEGORY_INDEX, 0);
		this.entityData.define(DATA_FUSE_TIME, -1);
	}

	// ====================== 重写基类抽象方法 ======================
	@Override
	protected ShockerCombatantBase createNewCombatantInstance() {
		return new ShockerCombatantEntity(KamenRiderInterwovenWorldsModEntities.SHOCKER_COMBATANT.get(), level());
	}

	@Override
	protected void setupMemberFromLeader(ShockerCombatantBase member) {
		if (!(member instanceof ShockerCombatantEntity m)) return;

		m.entityData.set(DATA_CATEGORY_INDEX, this.entityData.get(DATA_CATEGORY_INDEX));
		m.assignTextureByCategory(m.entityData.get(DATA_CATEGORY_INDEX), false);
		m.applyStatsByCategory(m.entityData.get(DATA_CATEGORY_INDEX), false);
		m.assignWeapon();
	}

	@Override
	protected Component getSquadLeaderName() {
		return Component.nullToEmpty("§6【修卡小队长】");
	}

	// ====================== 生成逻辑 ======================
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor,
										net.minecraft.world.DifficultyInstance difficulty,
										MobSpawnType reason,
										@Nullable SpawnGroupData spawnData,
										@Nullable CompoundTag dataTag) {
		if (!this.level().isClientSide) {
			this.setOrgId(ShockerOrg.ORG_SHOCKER);
			int category = this.getRandom().nextInt(4);
			this.entityData.set(DATA_CATEGORY_INDEX, category);
			assignTextureByCategory(category, isSquadLeader());
			applyStatsByCategory(category, isSquadLeader());
			assignWeapon();
		}
		finalizeSpawnBase(accessor, reason);
		return super.finalizeSpawn(accessor, difficulty, reason, spawnData, dataTag);
	}

	// ====================== 工具方法 ======================
	private void assignTextureByCategory(int category, boolean isCaptain) {
		if (category == CATEGORY_SHOCKER)
			this.entityData.set(DATA_TEXTURE_INDEX, isCaptain ? 0 : this.getRandom().nextInt(SHOCKER_COMBATANT.size()));
		else if (category == CATEGORY_BONE_SHOCKER)
			this.entityData.set(DATA_TEXTURE_INDEX, isCaptain ? 0 : this.getRandom().nextInt(SHOCKER_BONE_COMBATANT.size()));
		else if (category == CATEGORY_BERET)
			this.entityData.set(DATA_TEXTURE_INDEX, isCaptain ? 0 : this.getRandom().nextInt(BERET_SHOCKER.size()));
		else if (category == CATEGORY_FEMALE)
			this.entityData.set(DATA_TEXTURE_INDEX, isCaptain ? 0 : this.getRandom().nextInt(FEMALE_SHOCKER.size()));
	}

	private void applyStatsByCategory(int category, boolean isCaptain) {
		double baseHp = 10;
		double baseAttack = 2;
		double baseArmor = 2;
		double baseSpeed = 0.3;

		if (category == CATEGORY_BERET || category == CATEGORY_FEMALE) {
			if (isCaptain) {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * 3 * 2);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * 3 * 2);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * 3 * 2);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.15);
			} else {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * 3);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * 3);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * 3);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.1);
			}
		} else if (category == CATEGORY_SHOCKER) {
			if (isCaptain) {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * 4.5 * 2);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * 4.5 * 2);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * 4.5 * 2);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.2);
			} else {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * 4.5);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * 4.5);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * 4.5);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.15);
			}
		} else if (category == CATEGORY_BONE_SHOCKER) {
			double multiplier = 10 + this.getRandom().nextDouble() * 5;
			if (isCaptain) {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * multiplier * 2);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * multiplier * 2);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * multiplier * 1.2);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.25);
			} else {
				getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHp * multiplier);
				getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * multiplier);
				getAttribute(Attributes.ARMOR).setBaseValue(baseArmor * multiplier);
				getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * 1.2);
			}
		}
		this.setHealth(this.getMaxHealth());
	}

	private void assignWeapon() {
		List<Item> weapons = Arrays.asList(null, null, null,
				KamenRiderInterwovenWorldsModItems.SHOCKER_SABER.get(),
				KamenRiderInterwovenWorldsModItems.SHOCKER_COMBAT_STAFF.get());
		Item weapon = weapons.get(this.getRandom().nextInt(weapons.size()));
		this.setItemSlot(EquipmentSlot.MAINHAND, weapon != null ? new ItemStack(weapon) : ItemStack.EMPTY);
	}

	public ResourceLocation getCurrentTexture() {
		int cat = entityData.get(DATA_CATEGORY_INDEX);
		int idx = entityData.get(DATA_TEXTURE_INDEX);

		if (isSquadLeader()) {
			if (cat == CATEGORY_SHOCKER) return SHOCKER_COMBATANT_CAPTAIN.get(0);
			if (cat == CATEGORY_BONE_SHOCKER) return SHOCKER_BONE_COMBATANT_CAPTAIN.get(0);
			if (cat == CATEGORY_BERET) return BERET_SHOCKER_CAPTAIN.get(0);
			return FEMALE_SHOCKER_CAPTAIN.get(0);
		}

		if (cat == CATEGORY_SHOCKER) {
			idx = Math.max(0, Math.min(idx, SHOCKER_COMBATANT.size() - 1));
			return SHOCKER_COMBATANT.get(idx);
		}
		if (cat == CATEGORY_BONE_SHOCKER) {
			idx = Math.max(0, Math.min(idx, SHOCKER_BONE_COMBATANT.size() - 1));
			return SHOCKER_BONE_COMBATANT.get(idx);
		}
		if (cat == CATEGORY_BERET) {
			idx = Math.max(0, Math.min(idx, BERET_SHOCKER.size() - 1));
			return BERET_SHOCKER.get(idx);
		}
		idx = Math.max(0, Math.min(idx, FEMALE_SHOCKER.size() - 1));
		return FEMALE_SHOCKER.get(idx);
	}

	// ====================== 死亡 ======================
	@Override
	public void die(@NotNull DamageSource source) {
		super.die(source);
	}

	// ====================== AI/音效/属性 ======================
	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override public MobType getMobType() { return MobType.UNDEFINED; }

	public void playShockerCry() {
		if (!level().isClientSide) {
			SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
					ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "shocker_cry"));
			if (sound != null) playSound(sound, 1.0F, 1.0F);
		}
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
				ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "shocker_cry"));
		return sound != null ? sound : SoundEvents.GENERIC_HURT;
	}

	@Override
	public SoundEvent getDeathSound() {
		SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
				ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "shocker_cry"));
		return sound != null ? sound : SoundEvents.GENERIC_DEATH;
	}

	public static void init() {
		SpawnPlacements.register(KamenRiderInterwovenWorldsModEntities.SHOCKER_COMBATANT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
	}

	public static boolean checkSpawnRules(EntityType<? extends Mob> entityType, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
		return level.getDifficulty() != Difficulty.PEACEFUL
				&& Monster.isDarkEnoughToSpawn(level, pos, random)
				&& Mob.checkMobSpawnRules(entityType, level, reason, pos, random);
	}


	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.MAX_HEALTH, 60)
				.add(Attributes.ARMOR, 6)
				.add(Attributes.ATTACK_DAMAGE, 3)
				.add(Attributes.FOLLOW_RANGE, 24);
	}

	// ====================== NBT ======================
	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
		super.addAdditionalSaveData(nbt);
		nbt.putInt("Tex", entityData.get(DATA_TEXTURE_INDEX));
		nbt.putInt("Cat", entityData.get(DATA_CATEGORY_INDEX));
		nbt.putInt("Fuse", entityData.get(DATA_FUSE_TIME));
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
		super.readAdditionalSaveData(nbt);
		entityData.set(DATA_TEXTURE_INDEX, nbt.getInt("Tex"));
		entityData.set(DATA_CATEGORY_INDEX, nbt.getInt("Cat"));
		entityData.set(DATA_FUSE_TIME, nbt.getInt("Fuse"));
	}

	// ====================== 自爆逻辑 ======================
	private boolean shouldStartExploding() {
		if (this.level().isClientSide) return false;
		if (isExploding()) return false;
		double healthPercent = this.getHealth() / this.getMaxHealth();
		if (healthPercent > TRIGGER_HEALTH_PERCENT) return false;
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive() && target instanceof Player && this.distanceToSqr(target) < 100;
	}

	private void startExploding() {
		this.entityData.set(DATA_FUSE_TIME, 0);
		playShockerFuseSound();
	}

	public boolean isExploding() {
		return this.entityData.get(DATA_FUSE_TIME) >= 0;
	}

	public int getFuseTime() {
		return this.entityData.get(DATA_FUSE_TIME);
	}

	public float getSwelling(float partialTicks) {
		int fuse = this.lastFuseTime + (int)((this.getFuseTime() - this.lastFuseTime) * partialTicks);
		return Math.min(1.0f, (float)fuse / MAX_FUSE_TIME);
	}

	public void playShockerFuseSound() {
		if (!level().isClientSide) {
			playSound(SoundEvents.CREEPER_PRIMED, 1.5F, 0.8F + random.nextFloat() * 0.4F);
		}
	}

	private void explode() {
		if (this.level().isClientSide) return;
		this.entityData.set(DATA_FUSE_TIME, -1);
		this.level().explode(this, getX(), getY(), getZ(), EXPLOSION_RADIUS, Level.ExplosionInteraction.MOB);
		this.discard();
	}

	private void cancelExplosion() {
		this.entityData.set(DATA_FUSE_TIME, -1);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide) {
			this.lastFuseTime = getFuseTime();
			return;
		}
		if (isExploding()) {
			LivingEntity target = getTarget();
			if (target == null || !target.isAlive() || distanceToSqr(target) > 144) {
				cancelExplosion();
				return;
			}
			int currentFuse = getFuseTime() + 1;
			entityData.set(DATA_FUSE_TIME, currentFuse);
			if (currentFuse % 5 == 0) playShockerFuseSound();
			getLookControl().setLookAt(target, 30, 30);
			if (getNavigation().isDone() || tickCount % 10 == 0) {
				getNavigation().moveTo(target, 1.5);
			}
			if (currentFuse >= MAX_FUSE_TIME) explode();
		} else {
			if (shouldStartExploding()) startExploding();
		}
	}

	// ====================== 受伤判断 ======================
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (isExploding() && !level().isClientSide) {
			if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.EXPLOSION)) {
				explode();
				return true;
			}
		}
		return super.hurt(source, amount);
	}
}