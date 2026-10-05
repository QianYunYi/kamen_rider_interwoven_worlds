package com.qianyunyi.rider.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public abstract class BaseMotorcycleEntity extends Mob {

	// ========== 物理参数（子类可重写） ==========
	protected float getAcceleration() { return 0.006f; }
	protected float getMaxAcceleration() { return 0.018f; }
	protected float getDeceleration() { return 0.92f; }
	protected float getBrakeFactor() { return 0.72f; }
	protected float getMaxForwardSpeed() { return 0.85f; }
	protected float getMaxBackwardSpeed() { return 0.20f; }
	protected float getReverseAcceleration() { return 0.003f; }
	protected float getReverseDeceleration() { return 0.88f; }
	protected float getBaseTurnSpeed() { return 7.0f; }
	protected float getTurnSmooth() { return 0.30f; }
	protected float getMaxTurnDelta() { return 3.0f; }
	protected float getDriftFactor() { return 0.90f; }
	protected float getDriftBrakeFactor() { return 0.92f; }
	protected float getDriftSpeedConvert() { return 0.30f; }
	protected float getDriftSideSpeed() { return 0.10f; }
	protected float getDriftTurnSmooth() { return 0.55f; }
	protected float getMaxLeanAngle() { return 28.0f; }
	protected float getLeanSmooth() { return 0.20f; }
	protected float getInertiaFriction() { return 0.94f; }
	protected float getInertiaBrakeFactor() { return 0.78f; }
	protected float getBrakeAngleThreshold() { return 45.0f; }
	protected int getMaxDriveTime() { return 60; }

	// ========== 状态变量 ==========
	private float currentSpeed;
	private float targetYaw;
	private float momentumYaw;
	private float leanAmount;
	private double inertiaVelX;
	private double inertiaVelZ;
	private boolean wasAcceleratingLastFrame;
	private boolean isDrifting;
	private float lastTurnInput;
	private int driveTime;

	// 缓存朝向的 sin/cos，避免重复计算
	private float cachedSin;
	private float cachedCos;

	protected BaseMotorcycleEntity(EntityType<? extends BaseMotorcycleEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setMaxUpStep(1.0F);
		setPersistenceRequired();
		this.targetYaw = getYRot();
		inertiaVelX = 0;
		inertiaVelZ = 0;
		wasAcceleratingLastFrame = false;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
	}

	// ========== 伤害免疫 ==========
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof Player
				|| source.getDirectEntity() instanceof AbstractArrow
				|| source.is(DamageTypes.FALL)
				|| source.is(DamageTypes.EXPLOSION)
				|| source.is(DamageTypes.CACTUS))
			return false;
		boolean result = super.hurt(source, amount);
		this.hurtTime = 0;
		return result;
	}

	// ========== 骑乘交互 ==========
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			player.startRiding(this);
		}
		return InteractionResult.sidedSuccess(level().isClientSide());
	}

	// ========== 碰撞伤害 ==========
	@Override
	public void push(Entity entity) {
		super.push(entity);
   		if (!level().isClientSide() && entity instanceof LivingEntity && entity != this && !this.isPassengerOfSameVehicle(entity)) {
			doCollisionDamage(entity);
		}
	}

	protected float getCollisionMultiplier() {
		return 1.0f;
	}

	protected float getKnockbackMultiplier() {
		return 1.0f;
	}

	protected void doCollisionDamage(Entity target) {
		double spd = getTotalSpeed();
		if (spd < 0.1) return;

		float mult = getCollisionMultiplier();
		float damage = (float) Math.min(16.0 * mult, spd * 7.0 * mult);
		target.hurt(damageSources().mobAttack(this), damage);

		float kbMult = getKnockbackMultiplier();
		Vec3 knockVec = position().vectorTo(target.position()).normalize().scale((0.6 + spd * 0.4) * kbMult);
		target.setDeltaMovement(knockVec.x, 0.4 * kbMult, knockVec.z);
		target.hasImpulse = true;
	}

	// ========== 墙壁碰撞检测 ==========
	@Override
	public void move(MoverType type, Vec3 pos) {
		double beforeX = getX();
		double beforeZ = getZ();
		super.move(type, pos);

		if (!level().isClientSide() && onGround()) {
			double actualMove = Math.sqrt(
					(getX() - beforeX) * (getX() - beforeX) +
					(getZ() - beforeZ) * (getZ() - beforeZ));
			double intendedMove = Math.sqrt(pos.x * pos.x + pos.z * pos.z);

			if (intendedMove > 0.05 && actualMove < intendedMove * 0.25) {
				currentSpeed = 0;
				inertiaVelX = 0;
				inertiaVelZ = 0;
				momentumYaw *= 0.2f;
			}
		}
	}

	// ========== 核心：驾驶物理 ==========
	@Override
	public void travel(Vec3 movement) {
		if (!isVehicle()) {
			idleSlowDown();
			return;
		}

		Entity rider = getPassengers().get(0);
		if (!(rider instanceof LivingEntity passenger)) {
			idleSlowDown();
			return;
		}

		float forwardInput = passenger.zza;
		float turnInput = -passenger.xxa;
		this.lastTurnInput = turnInput;

		boolean isAccelerating = forwardInput > 0.01f;
		boolean isBrakeKeyPressed = forwardInput < -0.1f;
		boolean isMovingForward = currentSpeed > 0.01f;
		boolean isMovingBackward = currentSpeed < -0.01f;
		boolean isStationary = Math.abs(currentSpeed) < 0.01f;

		boolean isBraking = isBrakeKeyPressed && isMovingForward;
		boolean isReversing = isBrakeKeyPressed && (isStationary || isMovingBackward);
		boolean isDrifting = isBrakeKeyPressed && Math.abs(turnInput) > 0.1f && (currentSpeed > 0.15f || getInertiaSpeed() > 0.15f);
		boolean isInertiaBraking = isBrakeKeyPressed && !isMovingForward && !isDrifting && getInertiaSpeed() > 0.01f;
		this.isDrifting = isDrifting;

		float speedRatio = Math.abs(currentSpeed) / getMaxForwardSpeed();

		refreshYawCache();

		// 更新驾驶状态（双端都需要，用于动画）
		if (isAccelerating) {
			applyAcceleration(speedRatio);
		} else if (isDrifting) {
			applyDrifting();
		} else if (isBraking) {
			applyBraking();
		} else if (isReversing) {
			applyReversing();
		} else {
			applyCoasting();
		}

		// 压弯视觉效果（双端都需要，不涉及 setYRot）
		updateLeanVisual(turnInput, speedRatio, isDrifting);

		// 客户端：只更新状态，不执行旋转和移动（服务端通过 lerp 同步）
		if (!this.isEffectiveAi()) {
			wasAcceleratingLastFrame = isAccelerating;
			if (isDrifting) {
				spawnDriftParticles();
			}
			return;
		}

		// ===== 以下仅服务端执行 =====

		// 转向计算（会调用 setYRot，只能服务端执行）
		applyTurning(turnInput, speedRatio, isDrifting, isBraking, isAccelerating);
		refreshYawCache();

		if (isInertiaBraking) {
			applyInertiaBraking();
		}

		double finalVelX = inertiaVelX + cachedSin * currentSpeed;
		double finalVelZ = inertiaVelZ + cachedCos * currentSpeed;

		double spd = Math.sqrt(finalVelX * finalVelX + finalVelZ * finalVelZ);
		double limit = getMaxForwardSpeed() * 1.2;
		if (spd > limit) {
			finalVelX = finalVelX / spd * limit;
			finalVelZ = finalVelZ / spd * limit;
		}

		setDeltaMovement(finalVelX, getDeltaMovement().y, finalVelZ);
		move(MoverType.SELF, getDeltaMovement());

		if (!onGround()) {
			setDeltaMovement(getDeltaMovement().add(0, -0.08, 0));
		}

		onTravelUpdate(spd);

		wasAcceleratingLastFrame = isAccelerating;
	}

	private void refreshYawCache() {
		float yawRad = (float) Math.toRadians(getYRot());
		cachedSin = -(float) Math.sin(yawRad);
		cachedCos = (float) Math.cos(yawRad);
	}

	private void idleSlowDown() {
		driveTime = Math.max(0, driveTime - 3);
		currentSpeed *= 0.7f;
		inertiaVelX *= 0.7;
		inertiaVelZ *= 0.7;
		if (Math.abs(currentSpeed) < 0.001f) currentSpeed = 0;

		refreshYawCache();

		if (this.isEffectiveAi()) {
			setDeltaMovement(
					inertiaVelX + cachedSin * currentSpeed,
					getDeltaMovement().y,
					inertiaVelZ + cachedCos * currentSpeed
			);
			move(MoverType.SELF, getDeltaMovement());
		}

		leanAmount *= 0.85f;
		wasAcceleratingLastFrame = false;
	}

	private void applyAcceleration(float speedRatio) {
		// 行驶时间加成：越久加速越快（0 → 1，3秒满）
		driveTime = Math.min(driveTime + 1, getMaxDriveTime());
		float driveRatio = (float) driveTime / getMaxDriveTime();

		// 惯性融合：车头方向的惯性转化为速度
		if (getInertiaSpeed() > 0.01f) {
			double inertiaForward = inertiaVelX * cachedSin + inertiaVelZ * cachedCos;
			double inertiaSide = inertiaVelX * cachedCos - inertiaVelZ * cachedSin;

			currentSpeed += (float) inertiaForward * (0.2f + driveRatio * 0.2f);
			inertiaVelX = cachedCos * inertiaSide * 0.5f;
			inertiaVelZ = -cachedSin * inertiaSide * 0.5f;
		}

		// 加速度随行驶时间递增
		float accel = getAcceleration() + (getMaxAcceleration() - getAcceleration()) * driveRatio;
		currentSpeed += accel;
		currentSpeed = Math.min(currentSpeed, getMaxForwardSpeed());

		// 行驶中惯性自然衰减
		inertiaVelX *= 0.9;
		inertiaVelZ *= 0.9;
	}

	private void applyBraking() {
		driveTime = Math.max(0, driveTime - 2);
		float lost = currentSpeed * (1f - getBrakeFactor());
		currentSpeed *= getBrakeFactor();
		inertiaVelX += cachedSin * lost;
		inertiaVelZ += cachedCos * lost;
		if (currentSpeed < 0.01f) {
			currentSpeed = 0;
		}
	}

	private void applyDrifting() {
		driveTime = Math.max(0, driveTime - 1);
		float convertRatio = getDriftSpeedConvert();
		float converted = currentSpeed * convertRatio;
		currentSpeed -= converted;
		inertiaVelX += cachedSin * converted;
		inertiaVelZ += cachedCos * converted;
		inertiaVelX *= 0.96f;
		inertiaVelZ *= 0.96f;
		if (currentSpeed < 0.02f) {
			currentSpeed = 0;
		}
	}

	private void applyReversing() {
		driveTime = Math.max(0, driveTime - 2);
		currentSpeed -= getReverseAcceleration();
		currentSpeed = Math.max(currentSpeed, -getMaxBackwardSpeed());
	}

	private void applyCoasting() {
		driveTime = Math.max(0, driveTime - 2);
		if (Math.abs(currentSpeed) > 0.01f) {
			convertSpeedToInertia();
			currentSpeed = 0;
		}
		inertiaVelX *= getInertiaFriction();
		inertiaVelZ *= getInertiaFriction();
	}

	private void applyTurning(float turnInput, float speedRatio, boolean isDrifting,
	                          boolean isBraking, boolean isAccelerating) {
		float totalSpeedRatio = (float) Mth.clamp(getTotalSpeed() / getMaxForwardSpeed(), 0.0, 1.0);

		float turnFactor;
		if (isDrifting) {
			turnFactor = 3.5f;
		} else if (isBraking) {
			turnFactor = 1.6f;
		} else if (totalSpeedRatio < 0.12f && isAccelerating) {
			turnFactor = 2.5f;
		} else if (totalSpeedRatio < 0.35f && isAccelerating) {
			turnFactor = 1.6f;
		} else {
			turnFactor = Mth.clamp(1.0f - totalSpeedRatio * 0.95f, 0.08f, 1.0f);
		}

		float turnSpeed = getBaseTurnSpeed() * turnFactor;
		momentumYaw = momentumYaw * getDriftFactor() + turnInput * turnSpeed;
		momentumYaw = Mth.clamp(momentumYaw, -getMaxTurnDelta(), getMaxTurnDelta());

		targetYaw += momentumYaw;
		float yawDiff = normalizeAngle(targetYaw - getYRot());
		setYRot(normalizeAngle(getYRot() + yawDiff * (isDrifting ? getDriftTurnSmooth() : getTurnSmooth())));
		yBodyRot = getYRot();
		yHeadRot = getYRot();
	}

	private void updateLeanVisual(float turnInput, float speedRatio, boolean isDrifting) {
		float totalSpeedRatio = (float) Mth.clamp(getTotalSpeed() / getMaxForwardSpeed(), 0.0, 1.0);
		float leanMultiplier = isDrifting ? 1.4f : (0.3f + totalSpeedRatio * 0.7f);
		float targetLean = turnInput * getMaxLeanAngle() * leanMultiplier;
		if (isDrifting) {
			targetLean = turnInput * getMaxLeanAngle() * 1.2f;
		}
		leanAmount += (targetLean - leanAmount) * getLeanSmooth();
	}

	private void applyInertiaBraking() {
		double inertiaSpeed = getInertiaSpeed();
		if (inertiaSpeed <= 0.01f) return;

		double inertiaDirX = inertiaVelX / inertiaSpeed;
		double inertiaDirZ = inertiaVelZ / inertiaSpeed;

		double dotForward = cachedSin * inertiaDirX + cachedCos * inertiaDirZ;
		double angleDeg = Math.toDegrees(Math.acos(Mth.clamp(dotForward, -1.0, 1.0)));

		if (angleDeg <= getBrakeAngleThreshold()) {
			double forwardComp = inertiaVelX * cachedSin + inertiaVelZ * cachedCos;
			double sideComp = inertiaVelX * cachedCos - inertiaVelZ * cachedSin;

			forwardComp *= getInertiaBrakeFactor();

			inertiaVelX = forwardComp * cachedSin + sideComp * cachedCos;
			inertiaVelZ = forwardComp * cachedCos - sideComp * cachedSin;
		}
	}

	private void convertSpeedToInertia() {
		if (Math.abs(currentSpeed) < 0.001f) return;
		float driveRatio = (float) driveTime / getMaxDriveTime();
		// 行驶越久，惯性保留越多：70% → 95%
		double inertiaFactor = 0.70 + driveRatio * 0.25;
		double inertiaAmount = currentSpeed * inertiaFactor;
		inertiaVelX += cachedSin * inertiaAmount;
		inertiaVelZ += cachedCos * inertiaAmount;
	}

	protected double getInertiaSpeed() {
		return Math.sqrt(inertiaVelX * inertiaVelX + inertiaVelZ * inertiaVelZ);
	}

	protected double getTotalSpeed() {
		double finalVelX = inertiaVelX + cachedSin * currentSpeed;
		double finalVelZ = inertiaVelZ + cachedCos * currentSpeed;
		return Math.sqrt(finalVelX * finalVelX + finalVelZ * finalVelZ);
	}

	public float getLeanAmount() {
		return leanAmount;
	}

	public float getCurrentSpeed() {
		return currentSpeed;
	}

	public boolean isDrifting() {
		return isDrifting;
	}

	public float getLastTurnInput() {
		return lastTurnInput;
	}

	public void setCurrentSpeed(float speed) {
		this.currentSpeed = speed;
	}

	/**
	 * 每帧驾驶更新后的钩子，子类可重写（如更新 GeckoLib 动画速度）
	 * @param totalSpeed 当前合成速度（方块/tick）
	 */
	protected void onTravelUpdate(double totalSpeed) {
	}

	private void spawnDriftParticles() {
		float yawRad = (float) Math.toRadians(getYRot());
		double frontX = -Math.sin(yawRad) * 1.2;
		double frontZ = Math.cos(yawRad) * 1.2;
		double backX = Math.sin(yawRad) * 1.2;
		double backZ = -Math.cos(yawRad) * 1.2;
		RandomSource random = getRandom();
		// 后轮烟雾
		for (int i = 0; i < 5; i++) {
			level().addParticle(
					ParticleTypes.POOF,
					getX() + backX + (random.nextDouble() - 0.5) * 0.6,
					getY() + 0.15,
					getZ() + backZ + (random.nextDouble() - 0.5) * 0.6,
					(random.nextDouble() - 0.5) * 0.04,
					0.02,
					(random.nextDouble() - 0.5) * 0.04
			);
		}
		for (int i = 0; i < 2; i++) {
			level().addParticle(
					ParticleTypes.CAMPFIRE_COSY_SMOKE,
					getX() + backX + (random.nextDouble() - 0.5) * 0.3,
					getY() + 0.1,
					getZ() + backZ + (random.nextDouble() - 0.5) * 0.3,
					0, 0.03, 0
			);
		}
		// 前轮烟雾
		for (int i = 0; i < 5; i++) {
			level().addParticle(
					ParticleTypes.POOF,
					getX() + frontX + (random.nextDouble() - 0.5) * 0.6,
					getY() + 0.15,
					getZ() + frontZ + (random.nextDouble() - 0.5) * 0.6,
					(random.nextDouble() - 0.5) * 0.04,
					0.02,
					(random.nextDouble() - 0.5) * 0.04
			);
		}
		for (int i = 0; i < 2; i++) {
			level().addParticle(
					ParticleTypes.CAMPFIRE_COSY_SMOKE,
					getX() + frontX + (random.nextDouble() - 0.5) * 0.3,
					getY() + 0.1,
					getZ() + frontZ + (random.nextDouble() - 0.5) * 0.3,
					0, 0.03, 0
			);
		}
	}

	private static float normalizeAngle(float angle) {
		while (angle > 180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}

	// ========== 数据持久化 ==========
	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putFloat("CurrentSpeed", this.currentSpeed);
		compound.putFloat("TargetYaw", this.targetYaw);
		compound.putDouble("InertiaVelX", this.inertiaVelX);
		compound.putDouble("InertiaVelZ", this.inertiaVelZ);
		compound.putInt("DriveTime", this.driveTime);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("CurrentSpeed"))
			this.currentSpeed = compound.getFloat("CurrentSpeed");
		if (compound.contains("TargetYaw"))
			this.targetYaw = compound.getFloat("TargetYaw");
		if (compound.contains("InertiaVelX"))
			this.inertiaVelX = compound.getDouble("InertiaVelX");
		if (compound.contains("InertiaVelZ"))
			this.inertiaVelZ = compound.getDouble("InertiaVelZ");
		if (compound.contains("DriveTime"))
			this.driveTime = compound.getInt("DriveTime");
	}
}