package com.qianyunyi.rider.entity.vehicle.gouram;

import com.qianyunyi.rider.event.SetupAnimationsEvent;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;

public class GouramEntity extends PathfinderMob implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(GouramEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GouramEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(GouramEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(GouramEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> RESCUE_ACTIVE = SynchedEntityData.defineId(GouramEntity.class, EntityDataSerializers.BOOLEAN);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";
	private Optional<UUID> ownerUUID = Optional.empty();
	private int diveAttackCooldown = 0;
	private LivingEntity diveTarget = null;

	private final GouramRideBehavior rideBehavior = new GouramRideBehavior(this);
	private final GouramRescueBehavior rescueBehavior = new GouramRescueBehavior(this);
	private final GouramPatrolBehavior patrolBehavior = new GouramPatrolBehavior(this);

	public GouramEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), world);
	}

	public GouramEntity(EntityType<GouramEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setMaxUpStep(1.2f);
		this.moveControl = new FlyingMoveControl(this, 10, false);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(ANIMATION, "undefined");
		this.entityData.define(TEXTURE, "gouram");
		this.entityData.define(STATE, GouramState.IDLE.getPriority());
		this.entityData.define(RESCUE_ACTIVE, false);
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	@Nullable
	public UUID getOwnerUUID() {
		return this.ownerUUID.orElse(null);
	}

	public void setOwnerUUID(@Nullable UUID uuid) {
		this.ownerUUID = Optional.ofNullable(uuid);
	}

	@Nullable
	public Player getOwner() {
		UUID uuid = getOwnerUUID();
		return uuid != null ? this.level().getPlayerByUUID(uuid) : null;
	}

	public GouramState getState() {
		return GouramState.fromPriority(this.entityData.get(STATE));
	}

	public void setState(GouramState state) {
		this.entityData.set(STATE, state.getPriority());
	}

	public boolean requestState(GouramState target) {
		GouramState current = getState();
		if (current == target) {
			return true;
		}
		if (current.canBeInterruptedBy(target)) {
			setState(target);
			return true;
		}
		return false;
	}

	@Override
	public void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof Player player) {
			SetupAnimationsEvent.stopAnimation(player);
			SetupAnimationsEvent.stopLeanAnimation(player);
		}
		if (getPassengers().isEmpty()) {
			setState(GouramState.IDLE);
			rescueBehavior.onPassengerRemoved();
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// GRAB_HAND状态骑手位于 entity.y - 1.0，与哥莱姆碰撞箱可能重叠。
		// 将玩家放到哥莱姆前方 2.5 格、下方 2.0 格的安全位置，避免骑手下马时卡在实体内部
		// 导致相机面向自己。
		double yawRad = Math.toRadians(this.getYRot());
		double offsetX = -Math.sin(yawRad) * 2.5;
		double offsetZ = Math.cos(yawRad) * 2.5;
		return new Vec3(
			this.getX() + offsetX,
			this.getY() - 2.0,
			this.getZ() + offsetZ
		);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected PathNavigation createNavigation(Level world) {
		return new FlyingPathNavigation(this, world);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
			}
		});
		this.goalSelector.addGoal(2, new Goal() {
			@Override
			public boolean canUse() {
				if (isVehicle() || isPassenger()) return false;
				Player owner = getOwner();
				return owner != null && owner.isAlive() && distanceToSqr(owner) > GouramConfig.followStartDistance * GouramConfig.followStartDistance;
			}

			@Override
			public boolean canContinueToUse() {
				if (isVehicle() || isPassenger()) return false;
				Player owner = getOwner();
				return owner != null && owner.isAlive() && distanceToSqr(owner) > GouramConfig.followStopDistance * GouramConfig.followStopDistance;
			}

			@Override
			public void start() {
				requestState(GouramState.FOLLOW_OWNER);
			}

			@Override
			public void stop() {
				if (getState() == GouramState.FOLLOW_OWNER) {
					setState(GouramState.IDLE);
				}
			}

			@Override
			public void tick() {
				Player owner = getOwner();
				if (owner != null) {
					getNavigation().moveTo(owner, GouramConfig.followSpeed);
				}
			}
		});
		this.goalSelector.addGoal(3, new Goal() {
			@Override
			public boolean canUse() {
				if (diveAttackCooldown > 0) return false;
				if (isVehicle() || isPassenger()) return false;
				if (getState() != GouramState.IDLE) return false;
				Player owner = getOwner();
				if (owner == null) return false;
				// 优先响应玩家攻击的目标，其次响应攻击玩家的目标（战斗支援）
				LivingEntity target = owner.getLastHurtMob();
				if (target == null || !target.isAlive() || target.distanceToSqr(owner) > 400) {
					target = owner.getLastHurtByMob();
					if (target == null || !target.isAlive() || target.distanceToSqr(owner) > 400) {
						return false;
					}
				}
				return random.nextFloat() < GouramConfig.diveAttackChance;
			}

			@Override
			public boolean canContinueToUse() {
				return diveTarget != null && diveTarget.isAlive() && getState() == GouramState.DIVE_ATTACK;
			}

			@Override
			public void start() {
				Player owner = getOwner();
				if (owner != null) {
					// 优先锁定玩家攻击的目标，若无效则锁定攻击玩家的目标
					diveTarget = owner.getLastHurtMob();
					if (diveTarget == null || !diveTarget.isAlive()) {
						diveTarget = owner.getLastHurtByMob();
					}
					if (diveTarget != null && diveTarget.isAlive()) {
						requestState(GouramState.DIVE_ATTACK);
						getNavigation().moveTo(diveTarget, GouramConfig.diveAttackSpeed);
					}
				}
			}

			@Override
			public void stop() {
				diveTarget = null;
				diveAttackCooldown = GouramConfig.diveAttackCooldownTicks;
				if (getState() == GouramState.DIVE_ATTACK) {
					setState(GouramState.IDLE);
				}
			}

			@Override
			public void tick() {
				if (diveTarget != null && diveTarget.isAlive()) {
					getNavigation().moveTo(diveTarget, GouramConfig.diveAttackSpeed);
					if (distanceToSqr(diveTarget) < 4) {
						diveTarget.hurt(damageSources().mobAttack(GouramEntity.this), GouramConfig.diveAttackDamage);
						stop();
					}
				} else {
					stop();
				}
			}
		});
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEAD;
	}

	@Override
	public SoundEvent getAmbientSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("kamen_rider_interwoven_worlds:gouram_fly"));
	}

	@Override
	public void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("kamen_rider_interwoven_worlds:gouram_fly")), 0.15f, 1);
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public boolean causeFallDamage(float l, float d, DamageSource source) {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// 救援期间哥莱姆为霸体，避免怪物击杀/击伤导致救援中途失败
		if (getState() == GouramState.EMERGENCY_RESCUE || rescueBehavior.isActive()) {
			return false;
		}
		if (source.getEntity() instanceof Player player) {
			UUID ownerId = getOwnerUUID();
			if (ownerId != null && player.getUUID().equals(ownerId)) {
				return false;
			}
		}
		return super.hurt(source, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putInt("State", getState().getPriority());
		compound.putInt("DiveAttackCooldown", diveAttackCooldown);
		if (this.ownerUUID.isPresent()) {
			compound.putUUID("Owner", this.ownerUUID.get());
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("State"))
			setState(GouramState.fromPriority(compound.getInt("State")));
		if (compound.contains("DiveAttackCooldown"))
			diveAttackCooldown = compound.getInt("DiveAttackCooldown");
		if (compound.hasUUID("Owner")) {
			this.ownerUUID = Optional.of(compound.getUUID("Owner"));
		}
	}

	@Override
	public InteractionResult mobInteract(Player sourceentity, InteractionHand hand) {
		ItemStack itemstack = sourceentity.getItemInHand(hand);
		InteractionResult retval = InteractionResult.sidedSuccess(this.level().isClientSide());
		super.mobInteract(sourceentity, hand);
		RiderPlayerData data = RiderPlayerVariablesProvider.get(sourceentity);
		if (data != null && "Kuuga".equals(data.getActiveRiderName())) {
			if (sourceentity.getMainHandItem().isEmpty()) {
				if (sourceentity.isPassenger() && sourceentity.getVehicle() == this) {
					return retval;
				}
				UUID interactorUUID = sourceentity.getUUID();
				if (this.getOwnerUUID() != null && !this.getOwnerUUID().equals(interactorUUID)) {
					return retval;
				}
				this.setOwnerUUID(interactorUUID);
				boolean isSneaking = sourceentity.isShiftKeyDown();
				if (isSneaking) {
					if (requestState(GouramState.RIDE_BACK)) {
						rideBehavior.initRideBack();
						sourceentity.startRiding(this);
					}
				} else {
					if (requestState(GouramState.GRAB_HAND)) {
						sourceentity.startRiding(this);
					}
				}
			} else if (sourceentity.isPassenger() && sourceentity.getVehicle() == this && getState() == GouramState.RIDE_BACK) {
				rideBehavior.toggleHoverMode();
			}
		}
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	@Override
	public void travel(Vec3 dir) {
		if (this.isPassenger()) {
			if (getState() != GouramState.MERGE_VEHICLE) {
				setState(GouramState.MERGE_VEHICLE);
			}
			Entity vehicle = this.getVehicle();
			if (vehicle != null) {
				double rad = Math.toRadians(vehicle.getYRot());
				double forwardOffset = 0.25;
				this.setPos(vehicle.getX() + Math.sin(-rad) * forwardOffset, this.getY(), vehicle.getZ() + Math.cos(rad) * forwardOffset);
				this.setYRot(vehicle.getYRot());
				this.setXRot(vehicle.getXRot() * 0.5F);
				this.setRot(this.getYRot(), this.getXRot());
				this.yBodyRot = vehicle.getYRot();
				this.yHeadRot = vehicle.getYRot();
			}
			return;
		}
		Entity entity = this.getPassengers().isEmpty() ? null : (Entity) this.getPassengers().get(0);
		if (this.isVehicle()) {
			if (getState() == GouramState.RIDE_BACK && entity instanceof LivingEntity passenger) {
				rideBehavior.travelRideBack(passenger);
				float pitchTarget = Mth.clamp(-entity.getXRot(), -60f, 60f);
				float pitchDiff = pitchTarget - this.getXRot();
				this.setXRot(Mth.clamp(this.getXRot() + pitchDiff * 0.6f, -60f, 60f));
				this.setRot(this.getYRot(), this.getXRot());
				this.yBodyRot = this.getYRot();
				this.yHeadRot = this.getYRot();
				return;
			}

			this.setYRot(entity.getYRot());
			this.setXRot(entity.getXRot());
			this.setRot(this.getYRot(), this.getXRot());
			this.yBodyRot = entity.getYRot();
			this.yHeadRot = entity.getYRot();

			if (entity instanceof LivingEntity passenger) {
				rideBehavior.travelGrabHand(passenger);
			}
			double d1 = this.getX() - this.xo;
			double d0 = this.getZ() - this.zo;
			float f1 = (float) Math.sqrt(d1 * d1 + d0 * d0) * 4;
			if (f1 > 1.0F)
				f1 = 1.0F;
			this.walkAnimation.setSpeed(this.walkAnimation.speed() + (f1 - this.walkAnimation.speed()) * 0.4F);
			this.walkAnimation.position(this.walkAnimation.position() + this.walkAnimation.speed());
			this.calculateEntityAnimation(true);
			return;
		}
		if (this.animationprocedure.equals("empty") || this.animationprocedure.equals("flight")) {
			this.animationprocedure = "empty";
		}
		if (rescueBehavior.shouldSkipDefaultTravel()) {
			this.move(MoverType.SELF, this.getDeltaMovement());
			return;
		}
		// 巡逻行为（自主防卫冲撞/环绕飞行）控制时，跳过 super.travel() 的摩擦力衰减，
		// 直接移动，避免每 tick 速度被衰减后又重新设回导致的卡顿
		if (getState() == GouramState.IDLE && !this.getNavigation().isInProgress()) {
			this.move(MoverType.SELF, this.getDeltaMovement());
			return;
		}
		int groundY = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition()).getY();
		if (this.getY() < groundY + 3.0) {
			super.travel(new Vec3(dir.x, 0.5, dir.z));
		} else {
			super.travel(dir);
		}
	}

	@Override
	public double getPassengersRidingOffset() {
		if (getState() == GouramState.RIDE_BACK) {
			return GouramConfig.rideBackHeightOffset;
		}
		return 0.5D;
	}

	@Override
	public double getMyRidingOffset() {
		return -1.0D;
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
		double yawRad = Math.toRadians(this.getYRot());
		if (getState() == GouramState.RIDE_BACK) {
			double pitchRad = Math.toRadians(this.getXRot()) * GouramConfig.rideBackPitchScale;
			double pivotY = GouramConfig.rideBackPivotY;
			double localUp = GouramConfig.rideBackHeightOffset;
			double localForward = GouramConfig.rideBackForwardOffset;
			double cosPitch = Math.cos(pitchRad);
			double sinPitch = Math.sin(pitchRad);
			double relUp = localUp - pivotY;
			double yOffset = pivotY + relUp * cosPitch + localForward * sinPitch;
			double zForward = localForward * cosPitch - relUp * sinPitch;
			passenger.setPos(
				this.getX() + (-Math.sin(yawRad)) * zForward,
				this.getY() + yOffset,
				this.getZ() + Math.cos(yawRad) * zForward
			);
		} else {
			super.positionRider(passenger, callback);
			double forwardOffset = GouramConfig.grabForwardOffset;
			double downwardOffset = GouramConfig.grabDownwardOffset;
			passenger.setPos(
				passenger.getX() + Math.sin(-yawRad) * forwardOffset,
				passenger.getY() - downwardOffset,
				passenger.getZ() + Math.cos(yawRad) * forwardOffset
			);
		}
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public boolean canRiderInteract() {
		return true;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
	}

	@Override
	public void setNoGravity(boolean ignored) {
		super.setNoGravity(true);
	}

	@Override
	public void aiStep() {
		this.updateSwingTime();
		this.setNoGravity(true);

		if (diveAttackCooldown > 0) {
			diveAttackCooldown--;
		}
		patrolBehavior.tickCooldowns();

		if (rescueBehavior.isActive()) {
			rescueBehavior.tick();
			return;
		}

		if (this.isVehicle()) {
			if (getState() != GouramState.GRAB_HAND && getState() != GouramState.RIDE_BACK && getState() != GouramState.EMERGENCY_RESCUE) {
				setState(GouramState.GRAB_HAND);
			}
			this.travel(new Vec3(0, 0, 0));
			if (!this.getPassengers().isEmpty()) {
				Entity rider = this.getPassengers().get(0);
				float pitchTarget = -rider.getXRot();
				float pitchDiff = pitchTarget - this.getXRot();
				this.setXRot(this.getXRot() + pitchDiff * 0.6f);
				this.setRot(this.getYRot(), this.getXRot());
			}
			for (Entity passenger : this.getPassengers()) {
				if (passenger instanceof Player player) {
					if (getState() == GouramState.RIDE_BACK) {
						HenshinUtil.sendAnimationMessage(player, GouramConfig.ANIM_PLAYER_RIDE_BACK);
						SetupAnimationsEvent.setLeanAmount(player, rideBehavior.getLeanAmount());
						SetupAnimationsEvent.setPitchAmount(player, -this.getXRot());
					} else {
						player.setYBodyRot(this.getYRot());
						HenshinUtil.sendAnimationMessage(player, GouramConfig.ANIM_PLAYER_GRAB_RIDING);
					}
				}
			}
			return;
		}

		if (this.isPassenger()) {
			super.aiStep();
			return;
		}

		super.aiStep();

		if (getState() == GouramState.IDLE && !this.getNavigation().isInProgress()) {
			patrolBehavior.doIdlePatrol();
		}

		if (getState() == GouramState.IDLE && !patrolBehavior.isGroundSkimCooldownActive() && this.getNavigation().isInProgress()) {
			patrolBehavior.checkGroundSkim();
		}
	}

	public static void init() {
		SpawnPlacements.register(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.6);
		builder = builder.add(Attributes.MAX_HEALTH, 10);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		builder = builder.add(Attributes.FLYING_SPEED, 0.6);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if (getState() == GouramState.DIVE_ATTACK) {
				return event.setAndContinue(RawAnimation.begin().thenLoop(GouramConfig.ANIM_GOURAM_DIVE_ATTACK));
			}
			if (getState() == GouramState.RIDE_BACK) {
				return event.setAndContinue(RawAnimation.begin().thenLoop(GouramConfig.ANIM_GOURAM_RIDE_BACK));
			}
			if (this.isVehicle()) {
				return event.setAndContinue(RawAnimation.begin().thenLoop(GouramConfig.ANIM_GOURAM_FLIGHT_RIDE));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop(GouramConfig.ANIM_GOURAM_FLIGHT));
		}
		return PlayState.STOP;
	}

	String prevAnim = "empty";

	private PlayState procedurePredicate(AnimationState event) {
		if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		}
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 20) {
			this.remove(GouramEntity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		// 哥莱姆死亡时，记录死亡时间到玩家数据，用于召唤技能1分钟冷却
		if (reason == Entity.RemovalReason.KILLED) {
			UUID ownerId = getOwnerUUID();
			if (ownerId != null) {
				Player owner = this.level().getPlayerByUUID(ownerId);
				if (owner instanceof ServerPlayer serverOwner) {
					RiderPlayerData data = RiderPlayerVariablesProvider.get(serverOwner);
					if (data != null) {
						KuugaDataModule kuugaData = data.getKuugaData();
						if (kuugaData != null) {
							kuugaData.setGouramDeathTick(serverOwner.level().getGameTime());
							kuugaData.setGouramSummoned(false);
						}
					}
				}
			}
		}
		super.remove(reason);
	}

	public String getSyncedAnimation() {
		return this.entityData.get(ANIMATION);
	}

	public boolean isHoverMode() {
		return rideBehavior.isHoverMode();
	}

	public float getLeanAmount() {
		return rideBehavior.getLeanAmount();
	}

	public boolean isRescueActive() {
		return this.entityData.get(RESCUE_ACTIVE);
	}

	public boolean isRescueMounted() {
		return rescueBehavior.isRescueMounted();
	}

	public void setRescueRush(boolean rush) {
		rescueBehavior.setRescueRush(rush);
	}

	public void setRescueClear(boolean clear) {
		rescueBehavior.setRescueClear(clear);
	}

	public void setRescueActiveData(boolean active) {
		this.entityData.set(RESCUE_ACTIVE, active);
	}

	public GouramRideBehavior getRideBehavior() {
		return rideBehavior;
	}

	public GouramRescueBehavior getRescueBehavior() {
		return rescueBehavior;
	}

	public GouramPatrolBehavior getPatrolBehavior() {
		return patrolBehavior;
	}

	public void doSuperTravel(Vec3 dir) {
		super.travel(dir);
	}

	public void setAnimation(String animation) {
		this.entityData.set(ANIMATION, animation);
		this.animationprocedure = animation;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
		data.add(new AnimationController<>(this, "procedure", 0, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	private static float normalizeAngle(float angle) {
		while (angle > 180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}
}