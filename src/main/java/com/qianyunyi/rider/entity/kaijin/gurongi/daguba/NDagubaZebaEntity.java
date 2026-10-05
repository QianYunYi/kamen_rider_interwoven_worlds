package com.qianyunyi.rider.entity.kaijin.gurongi.daguba;

import com.qianyunyi.rider.client.gui.CustomBossBar;
import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.skills.DagubaSkillManager;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.MessageSupernaturalKillingIntent;
import com.qianyunyi.rider.network.packet.MessageUpdateBossBar;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.UUID;

public class NDagubaZebaEntity extends Gurongi {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(NDagubaZebaEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(NDagubaZebaEntity.class, EntityDataSerializers.STRING);

	public enum BossState {
		IDLE,
		COMBAT,
		SKILL_CASTING,
		STUNNED
	}

	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";

	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.PURPLE, ServerBossEvent.BossBarOverlay.NOTCHED_10);

	// 定义血条配置
	private static final CustomBossBar BOSS_BAR = new CustomBossBar(
			ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/boss_bar/daguba_bar_base.png"),
			ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/boss_bar/daguba_bar_overlay.png"),
			5, 16,
			1, 7,
			-6, -8,
			256, 32,
			25, 182,
			ChatFormatting.DARK_PURPLE
	);

	// 伤害
	private final DagubaSkillManager skillManager = new DagubaSkillManager(this);
	private BossState bossState = BossState.IDLE;
	private int damageCount = 0;
	private int breakDuration = 0;
	private int damageTeleportCooldown = 0;

	// 超自然杀气光环
	private int supernaturalKillingIntentTicks = 0;

	// === 击杀回血被动 ===
	/** 最近攻击的目标，用于检测击杀 */
	private LivingEntity trackedKillTarget = null;
	/** 是否已经为当前目标的死亡回血 */
	private boolean killHealed = true;

	public NDagubaZebaEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(KamenRiderInterwovenWorldsModEntities.N_DAGUBA_ZEBA.get(), world);
	}

	public NDagubaZebaEntity(EntityType<NDagubaZebaEntity> type, Level world) {
		super(type, world);
		xpReward = 500;
		setNoAi(false);
		setPersistenceRequired();
		registerBossBar();
		this.hasDamageReduction = false; // 达古巴不受50%减免
		this.group = GurongiGroup.N;
		this.setTexture("n/n_daguba_zeba");
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(ANIMATION, "undefined");
	}

	// 在tick中同步血量和最大血量到客户端
	@Override
	public void tick() {
		super.tick();
	}

	/**
	 * 自定义服务器AI步骤，用于更新Boss信息进度条
	 */
	@Override
	public void customServerAiStep() {
		super.customServerAiStep();
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());

		// 每5秒回复5%的生命值（仅在非击破状态）
		if (!isStunned() && this.tickCount % 100 == 0) {
			float healAmount = this.getMaxHealth() * 0.05f;
			this.heal(healAmount);
		}

		// 被动：击杀实体时回复5%最大生命值
		if (!killHealed) {
			if (trackedKillTarget == null || !trackedKillTarget.isAlive()) {
				float healAmount = this.getMaxHealth() * 0.05f;
				this.heal(healAmount);
				killHealed = true;
				trackedKillTarget = null;
			}
		}
	}

	/**
	 * 注册Boss血条
	 */
	private void registerBossBar() {
		if (!level().isClientSide) {
			// 使用BossEvent的UUID作为键
			UUID bossEventUuid = this.bossInfo.getId();
			CustomBossBar.customBossBars.put(bossEventUuid, BOSS_BAR);
			sendBossBarUpdate(BOSS_BAR, bossEventUuid);
		}
	}

	/**
	 * 注销Boss血条
	 */
	private void unregisterBossBar(UUID bossEventUuid) {
		if (!level().isClientSide) {
			// 在服务端注销
			CustomBossBar.customBossBars.remove(bossEventUuid);

			// 发送移除消息给客户端
			NetworkHandler.sendToAll(MessageUpdateBossBar.createRemoveMessage(bossEventUuid));
		}
	}

	/**
	 * 发送血条更新消息
	 */
	private void sendBossBarUpdate(CustomBossBar bar, UUID bossEventUuid) {
		NetworkHandler.sendToAll(new MessageUpdateBossBar(
				bossEventUuid, // 使用BossEvent的UUID
				0, // renderType 0 表示自定义血条
				bar.getBaseTexture(),
				bar.getOverlayTexture(),
				bar.getBaseHeight(),
				bar.getBaseTextureHeight(),
				bar.getBaseOffsetX(),
				bar.getBaseOffsetY(),
				bar.getOverlayOffsetX(),
				bar.getOverlayOffsetY(),
				bar.getOverlayWidth(),
				bar.getOverlayHeight(),
				bar.getVerticalIncrement(),
				bar.getProgressWidth(),
				bar.getTextColor().getId()
		));
	}

	@Override
	public void startSeenByPlayer(@NotNull ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(@NotNull ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	@Override
	public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();

		// 攻击目标（优先级1）
		this.goalSelector.addGoal(1, new NDagubaZebaAttackGoal(this));
		// 被攻击时反击（优先级2）
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));

		// 主动攻击目标（优先级3）- 攻击所有活体实体（包括古朗基）
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 32, true, true,
				// 谓词：不攻击自己
				target -> target != this
		));

		// 随机 游荡 行为（优先级4）
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
		// 环顾四周 行为（优先级5）
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		// 浮空行为（优先级6）
		this.goalSelector.addGoal(6, new FloatGoal(this));
	}

	@Override
	public @NotNull MobType getMobType() {
		return MobType.ILLAGER;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.death"));
	}

	public boolean superHurt(DamageSource source, float amount) {
		return super.hurt(source, amount);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
		skillManager.tick();
		if (damageTeleportCooldown > 0) damageTeleportCooldown--;

		if (bossState == BossState.STUNNED && breakDuration > 0) {
			breakDuration--;
			if (breakDuration <= 0) {
				bossState = BossState.COMBAT;
			}
		}

		if (bossState == BossState.IDLE && getTarget() != null) {
			bossState = BossState.COMBAT;
		}

		tickSupernaturalKillingIntent();
	}

	private void tickSupernaturalKillingIntent() {
		if (supernaturalKillingIntentTicks <= 0) return;
		supernaturalKillingIntentTicks--;

		if (!level().isClientSide && tickCount % 20 == 0) {
			applySuppressionToNearby();
		}

		if (supernaturalKillingIntentTicks <= 0 && !level().isClientSide) {
			NetworkHandler.sendToAll(new MessageSupernaturalKillingIntent(false, 0, 0));
		}
	}

	private void applySuppressionToNearby() {
		double range = 16.0;
		for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
				getBoundingBox().inflate(range))) {
			if (target == this || !target.isAlive()) continue;
			double dist = distanceTo(target);
			if (dist > range) continue;

			if (target instanceof Player player) {
				float healthRatio = player.getHealth() / player.getMaxHealth();
				if (healthRatio < 0.15f) {
					player.getFoodData().addExhaustion(4.0f);
				}
			}
		}
	}

	public void activateSupernaturalKillingIntent(int duration) {
		this.supernaturalKillingIntentTicks = duration;
	}

	public boolean isSupernaturalKillingIntentActive() {
		return supernaturalKillingIntentTicks > 0;
	}

	public DagubaSkillManager getSkillManager() {
		return skillManager;
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	public static void init() {

	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.5);
		builder = builder.add(Attributes.MAX_HEALTH, 500);
		builder = builder.add(Attributes.ARMOR, 200);
		builder = builder.add(Attributes.ARMOR_TOUGHNESS, 20);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 180);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 10);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 2);
		return builder;
	}

	/**
	 * 移动动画预测
	 * @param event 动画事件
	 * @return 播放状态
	 */
	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
			}
			if (this.isDeadOrDying()) {
				return event.setAndContinue(RawAnimation.begin().thenPlay("death"));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}
		return PlayState.STOP;
	}

	/**
	 * 过程动画预测
	 * @param event 动画事件
	 * @return 播放状态
	 */
	private PlayState procedurePredicate(AnimationState event) {
		Entity entity = this;
		Level world = entity.level();
		boolean loop = false;
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		if (!loop && this.lastloop) {
			this.lastloop = false;
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			event.getController().forceAnimationReset();
			return PlayState.STOP;
		}
		if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			if (!loop) {
				event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
				if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
					this.animationprocedure = "empty";
					event.getController().forceAnimationReset();
				}
			} else {
				event.getController().setAnimation(RawAnimation.begin().thenLoop(this.animationprocedure));
				this.lastloop = true;
			}
		}
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 20) {
			this.remove(NDagubaZebaEntity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	public String getSyncedAnimation() {
		return this.entityData.get(ANIMATION);
	}

	public void setAnimation(String animation) {
		this.entityData.set(ANIMATION, animation);
		this.animationprocedure = animation;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<>(this, "movement", 1, this::movementPredicate));
		data.add(new AnimationController<>(this, "procedure", 1, this::procedurePredicate));
	}

	public int getDamageTeleportCooldown() {
		return damageTeleportCooldown;
	}

	public void setDamageTeleportCooldown(int cooldown) {
		this.damageTeleportCooldown = cooldown;
	}

	public void incrementDamageCount() {
		this.damageCount++;
	}

	public int getDamageCount() {
		return damageCount;
	}

	public void resetDamageCount() {
		this.damageCount = 0;
	}

	public BossState getBossState() {
		return bossState;
	}

	public void setBossState(BossState state) {
		this.bossState = state;
	}

	public boolean isStunned() {
		return bossState == BossState.STUNNED;
	}

	public boolean isCastingSkill() {
		return bossState == BossState.SKILL_CASTING;
	}

	public void setBreakDuration(int duration) {
		this.breakDuration = duration;
	}

	/**
	 * 记录最近攻击的目标，用于检测击杀回血
	 */
	@Override
	public boolean doHurtTarget(Entity target) {
		boolean result = super.doHurtTarget(target);
		if (result && target instanceof LivingEntity livingTarget) {
			trackedKillTarget = livingTarget;
			killHealed = false;
		}
		return result;
	}

	/**
	 * 处理实体受伤事件
	 * @param source 伤害来源
	 * @param amount 伤害量
	 * @return 是否成功受伤
	 */
	@Override
	public boolean hurt(@NotNull DamageSource source, float amount) {
		return DagubaDamageHandler.handleDamage(this, source, amount);
	}

	public void setUsingSkill(boolean usingSkill) {
		if (usingSkill) {
			bossState = BossState.SKILL_CASTING;
		} else if (bossState == BossState.SKILL_CASTING) {
			bossState = BossState.COMBAT;
		}
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("DamageCount", damageCount);
		compound.putString("BossState", bossState.name());
		compound.putInt("BreakDuration", breakDuration);
		compound.putInt("DamageTeleportCooldown", damageTeleportCooldown);
		compound.putInt("SupernaturalKillingIntentTicks", supernaturalKillingIntentTicks);
		skillManager.save(compound);
	}

	/**
	 * 读取实体额外保存数据
	 * @param compound 保存数据CompoundTag
	 */
	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DamageCount")) {
			damageCount = compound.getInt("DamageCount");
		}
		if (compound.contains("BossState")) {
			try {
				bossState = BossState.valueOf(compound.getString("BossState"));
			} catch (IllegalArgumentException e) {
				bossState = BossState.IDLE;
			}
		}
		if (compound.contains("BreakDuration")) {
			breakDuration = compound.getInt("BreakDuration");
		}
		if (compound.contains("DamageTeleportCooldown")) {
			damageTeleportCooldown = compound.getInt("DamageTeleportCooldown");
		}
		if (compound.contains("SupernaturalKillingIntentTicks")) {
			supernaturalKillingIntentTicks = compound.getInt("SupernaturalKillingIntentTicks");
		}
		skillManager.load(compound);
	}

}