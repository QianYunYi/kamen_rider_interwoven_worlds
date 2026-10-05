package com.qianyunyi.rider.entity;

import com.qianyunyi.rider.event.SetupAnimationsEvent;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.item.BeatChaser2000RightHandlebarItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.SummonGouram;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.internal.registry.damage.damagesource.PlayerLightningDamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

public class BeatChaser2000Entity extends BaseMotorcycleEntity implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(BeatChaser2000Entity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> GOURAM_COMBINED = SynchedEntityData.defineId(BeatChaser2000Entity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> RIGHT_HANDLEBAR_REMOVED = SynchedEntityData.defineId(BeatChaser2000Entity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(BeatChaser2000Entity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(BeatChaser2000Entity.class, EntityDataSerializers.STRING);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";
	private double baseMaxHealth = 20.0;
	private int gouramHideTick = -1;
	private float wheelRotation = 0f;

	public BeatChaser2000Entity(PlayMessages.SpawnEntity packet, Level world) {
		this(KamenRiderInterwovenWorldsModEntities.BEAT_CHASER_2000.get(), world);
	}

	public BeatChaser2000Entity(EntityType<BeatChaser2000Entity> type, Level world) {
		super(type, world);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(GOURAM_COMBINED, false);
		this.entityData.define(RIGHT_HANDLEBAR_REMOVED, false);
		this.entityData.define(ANIMATION, "undefined");
		this.entityData.define(TEXTURE, "beat_chaser_2000");
	}

	// ============== 骑乘时 强制同步视角 + 播放动画 ==============
	@Override
	protected void positionRider(@NotNull Entity rider, Entity.MoveFunction moveFunction) {
		if (hasPassenger(rider)) {
			double rideY = getPassengersRidingOffset();
			double front = -0.13D;

			float leanRad = (float) Math.toRadians(getLeanAmount());
			float sinLean = (float) Math.sin(leanRad);
			float cosLean = (float) Math.cos(leanRad);

			double sideOffset = rideY * sinLean;
			double newRideY = rideY * cosLean;

			double yaw = getYRot() * Math.PI / 180.0D;
			double sinYaw = Math.sin(yaw);
			double cosYaw = Math.cos(yaw);

			double x = -sinYaw * front + cosYaw * sideOffset;
			double z = cosYaw * front + sinYaw * sideOffset;

			rider.setPos(this.getX() + x, this.getY() + newRideY, this.getZ() + z);

			if (rider instanceof Player player) {
				player.setYRot(this.getYRot());
				player.yRotO = this.yRotO;
				player.yBodyRot = this.getYRot();
				player.yBodyRotO = this.yRotO;
				HenshinUtil.sendAnimationMessage(player, "beat_chaser_2000_ride");
				SetupAnimationsEvent.setLeanAmount(player, getLeanAmount());
				SetupAnimationsEvent.setPitchAmount(player, 0);
			}
		}
	}

	// ============== 当乘客下车时触发 ==============
	@Override
	public void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof Player player) {
			SetupAnimationsEvent.stopAnimation(player);
			SetupAnimationsEvent.stopLeanAnimation(player);
		}
		if (isGouramCombined()) {
			disintegrateGouram();
		}
	}

	// ============== 哥莱姆合体 ==============
	public boolean isGouramCombined() {
		return this.entityData.get(GOURAM_COMBINED);
	}

	public void setGouramCombined(boolean combined) {
		this.entityData.set(GOURAM_COMBINED, combined);
	}

	//
	public void combineGouram() {
		if (isGouramCombined()) return;
		gouramHideTick = -1;
		this.setAnimation("gouram_combine");
		setGouramCombined(true);
		baseMaxHealth = this.getMaxHealth();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseMaxHealth * 2.0);
		this.setHealth(this.getMaxHealth());
	}

	public void disintegrateGouram() {
		if (!isGouramCombined()) return;
		setGouramCombined(false);
		gouramHideTick = this.tickCount + 40;
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseMaxHealth);
		if (this.getHealth() > this.getMaxHealth()) {
			this.setHealth(this.getMaxHealth());
		}
		this.setAnimation("gouram_disintegration");
	}

	public boolean shouldHideGouram() {
		if (isGouramCombined()) return false;
		return this.tickCount >= gouramHideTick;
	}

	// ============== 速度参数 ==============
	@Override
	protected float getMaxForwardSpeed() {
		return isGouramCombined() ? 1.7f : 0.85f;
	}

	@Override
	protected float getMaxAcceleration() {
		return isGouramCombined() ? 0.036f : 0.018f;
	}

	@Override
	protected float getAcceleration() {
		return isGouramCombined() ? 0.012f : 0.006f;
	}

	// ============== 碰撞参数 ==============
	@Override
	protected float getCollisionMultiplier() {
		return isGouramCombined() ? 2.0f : 1.0f;
	}

	@Override
	protected float getKnockbackMultiplier() {
		return isGouramCombined() ? 2.0f : 1.0f;
	}

	// ============== 合体速度减伤 ==============
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (isGouramCombined() && getTotalSpeed() > 0.15) {
			float reduction = (float) Math.min(0.7f, getTotalSpeed() / getMaxForwardSpeed() * 0.7f);
			amount *= (1.0f - reduction);
		}
		return super.hurt(source, amount);
	}

	// ============== 哥莱姆合体碰撞 ==============
	@Override
	public void push(Entity entity) {
		super.push(entity);
		boolean combined = isGouramCombined();
		boolean server = !level().isClientSide();
		boolean living = entity instanceof LivingEntity;
		boolean notSelf = entity != this;
		boolean notRider = !this.getPassengers().contains(entity);
		if (server && combined && living && notSelf && notRider) {
			doGouramCollision((LivingEntity) entity);
		}
	}

	private void doGouramCollision(LivingEntity target) {
		ServerPlayer serverPlayer = null;
		for (Entity passenger : this.getPassengers()) {
			if (passenger instanceof ServerPlayer sp) {
				serverPlayer = sp;
				break;
			}
		}
		if (serverPlayer == null) return;

		RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
		if (data == null) return;

		double spd = getTotalSpeed();
		if (spd < 0.1) return;

		float chargeTime = (float) (spd * 10.0);
		float baseDamage = serverPlayer.experienceLevel * 5.0f;
		float chargeBonus = chargeTime * 10.0f;
		float totalDamage = baseDamage + chargeBonus;


		target.addEffect(new MobEffectInstance(
				KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
				20, 8, false, false));

		target.getPersistentData().putBoolean("is_explosive", true);
		target.getPersistentData().putString("explosion_player", serverPlayer.getUUID().toString());
		target.getPersistentData().putString("form", data.getActiveFormName());

		double current = target.getPersistentData().getDouble("suppress_energy");
		double newValue = Math.min(current + totalDamage * 0.5f, 2000);
		target.getPersistentData().putDouble("suppress_energy", newValue);

		target.invulnerableTime = 0;
		target.hurt(new PlayerLightningDamageSource(serverPlayer), totalDamage);

		SummonGouram.setCollisionCooldown(serverPlayer);

		// 碰撞粒子效果
		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.FLASH,
					target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
					3, 0.3, 0.3, 0.3, 0.05);
		}
	}

	// ============== 禁用原版坐姿 ==============
	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public boolean canRiderInteract() {
		return true; // 必须 true，否则交互异常
	}

	// ============== 右侧把手：拆卸 / 插回 ==============
	public boolean isRightHandlebarRemoved() {
		return this.entityData.get(RIGHT_HANDLEBAR_REMOVED);
	}

	public void setRightHandlebarRemoved(boolean removed) {
		this.entityData.set(RIGHT_HANDLEBAR_REMOVED, removed);
	}

	private InteractionResult handleHandlebarInteraction(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.CONSUME;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof BeatChaser2000RightHandlebarItem) {
			// 手持右侧把手，潜行右键：插回去
			setRightHandlebarRemoved(false);
			player.setItemInHand(hand, ItemStack.EMPTY);
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.inventoryMenu.broadcastChanges();
			}
			return InteractionResult.SUCCESS;
		} else if (stack.isEmpty() && !isRightHandlebarRemoved()) {
			// 空手潜行右键：拆下右侧把手
			setRightHandlebarRemoved(true);
			player.addItem(new ItemStack(KamenRiderInterwovenWorldsModItems.BEAT_CHASER_2000_RIGHT_HANDLEBAR.get()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	// ============== 骑乘交互（潜行=卸装/插回把手，否则正常骑乘） ==============
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			return handleHandlebarInteraction(player, hand);
		}
		// 把手被拆下时无法骑乘
		if (isRightHandlebarRemoved()) {
			return InteractionResult.FAIL;
		}
		if (!level().isClientSide()) {
			player.startRiding(this);
		}
		return InteractionResult.sidedSuccess(level().isClientSide());
	}

	// 骑乘高度
	@Override
	public double getPassengersRidingOffset() {
		return 0.05D;
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putBoolean("GouramCombined", this.isGouramCombined());
		compound.putBoolean("RightHandlebarRemoved", this.isRightHandlebarRemoved());
		compound.putDouble("BaseMaxHealth", this.baseMaxHealth);
		compound.putInt("GouramHideTick", this.gouramHideTick);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("GouramCombined"))
			this.setGouramCombined(compound.getBoolean("GouramCombined"));
		if (compound.contains("RightHandlebarRemoved"))
			this.setRightHandlebarRemoved(compound.getBoolean("RightHandlebarRemoved"));
		if (compound.contains("BaseMaxHealth"))
			this.baseMaxHealth = compound.getDouble("BaseMaxHealth");
		if (compound.contains("GouramHideTick"))
			this.gouramHideTick = compound.getInt("GouramHideTick");
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
		double speed = getTotalSpeed();
		wheelRotation += (float) (speed * 0.6f);
	}

	public float getWheelRotation() {
		return wheelRotation;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return super.getDimensions(pose).scale(1.0f);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.5)
				.add(Attributes.MAX_HEALTH, 50.0)
				.add(Attributes.ARMOR, 50.0)
				.add(Attributes.ATTACK_DAMAGE, 20)
				.add(Attributes.FOLLOW_RANGE, 16);
	}

	// ========== GeckoLib 动画 ==========
	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			// 移动不再使用 walk（该动画不存在），摩托的姿态仅由 idle + 车轮旋转 + 压弯旋转控制
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}
		return PlayState.STOP;
	}

	String prevAnim = "empty";

	private PlayState procedurePredicate(AnimationState event) {
		if (!animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED
				|| (!this.animationprocedure.equals(prevAnim) && !this.animationprocedure.equals("empty"))) {
			if (!this.animationprocedure.equals(prevAnim))
				event.getController().forceAnimationReset();
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		} else if (animationprocedure.equals("empty")) {
			prevAnim = "empty";
			return PlayState.STOP;
		}
		prevAnim = this.animationprocedure;
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 20) {
			this.remove(BeatChaser2000Entity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	public String getSyncedAnimation() {
		return this.entityData.get(ANIMATION);
	}

	public void setAnimation(String animation) {
		this.entityData.set(ANIMATION, animation);
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
}