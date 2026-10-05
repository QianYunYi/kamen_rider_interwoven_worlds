package com.qianyunyi.rider.entity.kaijin.gurongi.gamio;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.gui.CustomBossBar;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills.AbstractGamioSkill;
import com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills.GamioSkillManager;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.MessageSupernaturalKillingIntent;
import com.qianyunyi.rider.network.packet.MessageUpdateBossBar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class NGamioZedaEntity extends Gurongi {

	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.STRING);
	/**
	 * 动画版本计数器：每次 setAnimation 调用都会递增（即使动画名相同），
	 * 确保客户端能检测到每次动画设置事件，解决同名动画重复播放失败的问题。
	 */
	public static final EntityDataAccessor<Integer> ANIM_VERSION = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.INT);

	// 骨骼组可见性
	public static final EntityDataAccessor<Boolean> TAIL_VISIBLE = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> GREEN_LIGHTNING_VISIBLE = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Boolean> IS_FLOATING = SynchedEntityData.defineId(NGamioZedaEntity.class, EntityDataSerializers.BOOLEAN);

	public enum BossState {
		IDLE,
		COMBAT,
		SKILL_CASTING,
		STUNNED
	}

	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public static final String ANIM_NONE = "empty";
	public String animationprocedure = ANIM_NONE;

	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.GREEN, ServerBossEvent.BossBarOverlay.NOTCHED_10);

	// 定义血条配置
	private static final CustomBossBar BOSS_BAR = new CustomBossBar(
			ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/boss_bar/gamio_bar_base.png"),
			ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/boss_bar/gamio_bar_overlay.png"),
			5, 16,
			1, 7,
			-6, -8,
			256, 32,
			25, 182,
			ChatFormatting.RED
	);

	// 技能管理器
	private final GamioSkillManager skillManager = new GamioSkillManager(this);
	private BossState bossState = BossState.IDLE;

	// 伤害计数 - 受到10次伤害从空中掉落
	private int damageCount = 0;
	private static final int FALL_DAMAGE_THRESHOLD = 10;
	private int breakDuration = 0;

	// 念力浮空冷却 (15秒 = 300 ticks)
	private int telekineticCooldown = 0;
	private static final int TELEKINETIC_COOLDOWN_TICKS = 300;

	// 浮空状态
	private boolean isFloating = false;
	private int floatingAttackIndex = 0; // 0=attack1, 1=attack2 轮询

	// 超自然杀气光环
	private int supernaturalKillingIntentTicks = 0;

	// === 击杀回血被动 ===
	private LivingEntity trackedKillTarget = null;
	private boolean killHealed = true;

	// 动画自动复位 tick
	private int autoResetTick = 0;

	// 骨骼组隐藏/显示计时器
	private int tailHideTimer = -1;
	private int greenLightningHideTimer = -1;
	private static final int TAIL_HIDE_DELAY = 26; // 1.3秒
	private static final int GREEN_LIGHTNING_HIDE_DELAY = 35; // 1.75秒

	// 狼王标记管理
	private final Set<UUID> wolfPackMembers = new java.util.HashSet<>();

	// 浮空降落延迟（无目标时延迟5秒降落）
	private int floatingLandDelay = 0;
	private static final int FLOATING_LAND_DELAY_TICKS = 100; // 5秒

	// 同类吸收尝试冷却（防止空转重试）
	private int absorptionCheckCooldown = 0;
	private static final int ABSORPTION_CHECK_INTERVAL = 20; // 每1秒检查一次

	public NGamioZedaEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(KamenRiderInterwovenWorldsModEntities.N_GAMIO_ZEDA.get(), world);
	}

	public NGamioZedaEntity(EntityType<NGamioZedaEntity> type, Level world) {
		super(type, world);
		xpReward = 500;
		setNoAi(false);
		setPersistenceRequired();
		registerBossBar();
		this.hasDamageReduction = false; // 恩·伽米奥·杰达不受50%减免
		this.group = GurongiGroup.N;
		this.setTexture("n/n_gamio_zeda");
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(ANIMATION, ANIM_NONE);
		this.entityData.define(ANIM_VERSION, 0);
		this.entityData.define(TAIL_VISIBLE, false);  // 默认隐藏tail
		this.entityData.define(GREEN_LIGHTNING_VISIBLE, false); // 默认隐藏green_lightning
		this.entityData.define(IS_FLOATING, false);
	}

	/**
	 * 同步动画数据到客户端
	 * 客户端通过 ANIM_VERSION 版本号检测每次动画设置事件，
	 * 确保动画正确播放。
	 */
	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (ANIMATION.equals(key)) {
			this.animationprocedure = this.entityData.get(ANIMATION);
		}
		if (IS_FLOATING.equals(key)) {
			this.isFloating = this.entityData.get(IS_FLOATING);
		}
	}

	@Override
	public void tick() {
		super.tick();
		// 动画自动复位
		if (!level().isClientSide && autoResetTick > 0 && tickCount >= autoResetTick) {
			if (!ANIM_NONE.equals(this.entityData.get(ANIMATION))) {
				this.entityData.set(ANIMATION, ANIM_NONE);
			}
			autoResetTick = 0;
		}
	}

	@Override
	public void customServerAiStep() {
		super.customServerAiStep();
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());

		// 念力浮空冷却递减
		if (telekineticCooldown > 0) telekineticCooldown--;

		// 骨骼组隐藏计时
		if (tailHideTimer > 0) {
			tailHideTimer--;
			if (tailHideTimer == 0) {
				setTailVisible(false);
			}
		}
		if (greenLightningHideTimer > 0) {
			greenLightningHideTimer--;
			if (greenLightningHideTimer == 0) {
				setGreenLightningVisible(false);
			}
		}

		// 每5秒回复5%的生命值（仅在非击破状态）
		if (!isStunned() && this.tickCount % 100 == 0) {
			float healAmount = this.getMaxHealth() * 0.05f;
			this.heal(healAmount);
		}

		// 被动：击杀实体时回复5%最大生命值
		if (!killHealed) {
			if (trackedKillTarget == null || !trackedKillTarget.isAlive()) {
				// === 如果击杀的是村民或灾厄村民，转换为古朗基 ===
				if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
					if (trackedKillTarget instanceof Villager villager) {
						KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王] Killed villager, converting to gurongi...");
						convertKilledCivilian(villager, serverLevel);
					} else if (trackedKillTarget instanceof AbstractIllager illager) {
						KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王] Killed illager, converting to gurongi...");
						convertKilledCivilian(illager, serverLevel);
					}
				}

				float healAmount = this.getMaxHealth() * 0.05f;
				this.heal(healAmount);
				killHealed = true;
				trackedKillTarget = null;
			}
		}

		// 自动浮空/降落
		if (isFloating) {
			// 无目标时延迟5秒降落
			if (getTarget() == null) {
				// 无目标时锁定高度（Y轴锁零），只保留水平阻尼运动
				Vec3 motion = this.getDeltaMovement();
				this.setDeltaMovement(motion.x * 0.95, 0, motion.z * 0.95);

				if (floatingLandDelay <= 0) {
					floatingLandDelay = FLOATING_LAND_DELAY_TICKS;
				} else {
					floatingLandDelay--;
					if (floatingLandDelay <= 0) {
						setFloating(false);
						floatingLandDelay = 0;
					}
				}
			} else {
				// 有目标时重置延迟
				floatingLandDelay = 0;
			}
		} else if (!isStunned() && bossState != BossState.SKILL_CASTING
				&& telekineticCooldown <= 0 && getTarget() != null) {
			setFloating(true);
			floatingLandDelay = 0;
		}

		// 每20 tick引导狼群成员跟随狼王
		if (!level().isClientSide && tickCount % 20 == 0 && !wolfPackMembers.isEmpty()) {
			guideWolfPackMembers();
		}

		// 自动触发同类吸收（血量低于30%时，按间隔检查）
		if (!level().isClientSide && bossState != BossState.SKILL_CASTING && bossState != BossState.STUNNED) {
			if (absorptionCheckCooldown > 0) {
				absorptionCheckCooldown--;
			} else {
				absorptionCheckCooldown = ABSORPTION_CHECK_INTERVAL;
				float healthRatio = this.getHealth() / this.getMaxHealth();
				if (healthRatio <= 0.30f && !wolfPackMembers.isEmpty()) {
					for (AbstractGamioSkill skill : skillManager.getSkills()) {
						if (skill instanceof com.qianyunyi.rider.entity.kaijin.gurongi.gamio.skills.WolfKingAbsorptionSkill) {
							if (skill.canExecute(null)) {
								skillManager.executeSkill(skill);
							}
							break;
						}
					}
				}
			}
		}

		// 更新浮空状态同步数据
		this.entityData.set(IS_FLOATING, isFloating);
	}

	/**
	 * 注册Boss血条
	 */
	private void registerBossBar() {
		if (!level().isClientSide) {
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
			CustomBossBar.customBossBars.remove(bossEventUuid);
			NetworkHandler.sendToAll(MessageUpdateBossBar.createRemoveMessage(bossEventUuid));
		}
	}

	/**
	 * 发送血条更新消息
	 */
	private void sendBossBarUpdate(CustomBossBar bar, UUID bossEventUuid) {
		NetworkHandler.sendToAll(new MessageUpdateBossBar(
				bossEventUuid,
				0,
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

	// ========== 骨骼组可见性控制 ==========

	public void setTailVisible(boolean visible) {
		this.entityData.set(TAIL_VISIBLE, visible);
	}

	public boolean isTailVisible() {
		return this.entityData.get(TAIL_VISIBLE);
	}

	public void setGreenLightningVisible(boolean visible) {
		this.entityData.set(GREEN_LIGHTNING_VISIBLE, visible);
	}

	public boolean isGreenLightningVisible() {
		return this.entityData.get(GREEN_LIGHTNING_VISIBLE);
	}

	/**
	 * 显示tail骨骼组并设置延迟隐藏
	 */
	public void showTailTemporary() {
		setTailVisible(true);
		tailHideTimer = TAIL_HIDE_DELAY;
	}

	/**
	 * 显示green_lightning骨骼组并设置延迟隐藏
	 */
	public void showGreenLightningTemporary() {
		setGreenLightningVisible(true);
		greenLightningHideTimer = GREEN_LIGHTNING_HIDE_DELAY;
	}

	// ========== 浮空状态控制 ==========

	/**
	 * 设置浮空状态
	 * 首次浮空时自动提升高度到离地约10格
	 */
	public void setFloating(boolean floating) {
		this.isFloating = floating;
		this.entityData.set(IS_FLOATING, floating);
		if (floating) {
			this.setNoGravity(true);
			// 不再强制提升高度，保持当前位置自然浮空
		} else {
			this.setNoGravity(false);
		}
	}

	public boolean isFloating() {
		return isFloating;
	}

	/**
	 * 受到伤害时处理浮空逻辑
	 */
	public void onDamageDuringFlight() {
		if (!isFloating) return;
		damageCount++;
		if (damageCount >= FALL_DAMAGE_THRESHOLD) {
			// 从空中掉落
			setFloating(false);
			telekineticCooldown = TELEKINETIC_COOLDOWN_TICKS;
			damageCount = 0;
			setAnimation("teleport", 20); // 掉落动画（1S后自动复位）
		}
	}

	public int getTelekineticCooldown() {
		return telekineticCooldown;
	}

	public boolean canUseTelekineticFlight() {
		return telekineticCooldown <= 0 && !isFloating;
	}

	/**
	 * 获取下一个浮空攻击索引并轮询
	 */
	public int getNextFloatingAttackIndex() {
		int index = floatingAttackIndex;
		floatingAttackIndex = (floatingAttackIndex + 1) % 2;
		return index; // 0=attack1, 1=attack2
	}

	@Override
	public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();

		// 攻击目标（优先级1）
		this.goalSelector.addGoal(1, new NGamioZedaAttackGoal(this));
		// 被攻击时反击（优先级2）
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));

		// 主动攻击目标（优先级3）- 不攻击古朗基
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 32, true, true,
				target -> target != this && !(target instanceof Gurongi)
		));

		// 随机游荡行为（优先级4）
		this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
		// 环顾四周行为（优先级5）
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

	public GamioSkillManager getSkillManager() {
		return skillManager;
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	// ========== 狼王标记管理 ==========

	/**
	 * 获取狼群成员UUID集合
	 */
	public Set<UUID> getWolfPackMembers() {
		return wolfPackMembers;
	}

	/**
	 * 添加狼群成员
	 */
	public void addWolfPackMember(UUID memberId) {
		wolfPackMembers.add(memberId);
	}

	/**
	 * 移除狼群成员
	 */
	public void removeWolfPackMember(UUID memberId) {
		wolfPackMembers.remove(memberId);
	}

	/**
	 * 获取可转换的古朗基类型（拉集团以下 = NU, GO, ME, ZU, BE）
	 * 目前仅实现了ZU集团，后续可扩展
	 */
	public static List<EntityType<? extends Gurongi>> getAvailableGurongiTypes() {
		List<EntityType<? extends Gurongi>> types = new ArrayList<>();

		// 滋（Zu）集团 - 已实现的古朗基
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.ZU_GUMUN_BA.get());
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU.get());
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.ZU_MEBIO_DA.get());
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.ZU_BADUU_BA.get());
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.ZU_ZAIN_DA.get());
		addIfInstance(types, KamenRiderInterwovenWorldsModEntities.GARUME_RE.get());

		return types;
	}

	@SuppressWarnings("unchecked")
	private static void addIfInstance(List<EntityType<? extends Gurongi>> list, EntityType<?> type) {
		if (type != null) {
			list.add((EntityType<? extends Gurongi>) type);
		}
	}

	/**
	 * 将被狼王击杀的村民或灾厄村民转换为古朗基
	 */
	public void convertKilledCivilian(LivingEntity target, ServerLevel serverLevel) {
		List<EntityType<? extends Gurongi>> availableTypes = getAvailableGurongiTypes();
		if (availableTypes.isEmpty()) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[狼王] No available gurongi types to convert killed civilian!");
			return;
		}

		// 随机选择一种古朗基类型
		Random random = new Random();
		EntityType<? extends Gurongi> gurongiType = availableTypes.get(random.nextInt(availableTypes.size()));

		// 生成古朗基实体
		Gurongi gurongi = gurongiType.create(serverLevel);
		if (gurongi == null) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[狼王] Failed to create gurongi from type {}", gurongiType.getDescriptionId());
			return;
		}

		// 设置位置到目标的位置
		gurongi.setPos(target.getX(), target.getY(), target.getZ());

		// 属性调整为原版的50%
		double originalMaxHealth = gurongi.getAttributeBaseValue(Attributes.MAX_HEALTH);
		double originalAttackDamage = gurongi.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		double originalArmor = gurongi.getAttributeBaseValue(Attributes.ARMOR);
		double originalSpeed = gurongi.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

		gurongi.getAttribute(Attributes.MAX_HEALTH).setBaseValue(originalMaxHealth * 0.5);
		gurongi.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(originalAttackDamage * 0.5);
		gurongi.getAttribute(Attributes.ARMOR).setBaseValue(originalArmor * 0.5);
		gurongi.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(originalSpeed * 0.5);

		// 设置生命值为最大值（满血生成）
		gurongi.setHealth((float) (originalMaxHealth * 0.5));

		// 设置狼王标记（持久化数据）
		gurongi.getPersistentData().putString("wolfKingId", this.getStringUUID());

		// 添加到狼王的管理列表
		this.addWolfPackMember(gurongi.getUUID());

		// 生成到世界
		serverLevel.addFreshEntity(gurongi);

		// 播放转换音效
		serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
				net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.5f, 0.3f);

		// 粒子效果
		serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
				target.getX(), target.getY() + 1.0, target.getZ(),
				15, 0.5, 0.5, 0.5, 0.05);

		KamenRiderInterwovenWorldsMod.LOGGER.debug("[狼王] Successfully converted killed civilian to {}", gurongiType.getDescriptionId());

		// 移除目标（已死，直接丢弃）
		target.remove(Entity.RemovalReason.DISCARDED);
	}

	/**
	 * 清空狼群
	 */
	public void clearWolfPackMembers() {
		wolfPackMembers.clear();
	}

	/**
	 * 引导狼群成员跟随狼王
	 * 每5 tick调用一次，驱赶有狼王标记的古朗基跟随并攻击狼王的目标
	 */
	private void guideWolfPackMembers() {
		if (level().isClientSide()) return;
		if (!(level() instanceof ServerLevel serverLevel)) return;

		LivingEntity wolfTarget = this.getTarget();

		Iterator<UUID> iterator = wolfPackMembers.iterator();
		while (iterator.hasNext()) {
			UUID memberId = iterator.next();
			Entity member = serverLevel.getEntity(memberId);

			if (member == null || !member.isAlive()) {
				iterator.remove();
				continue;
			}

			if (member instanceof Mob mob) {
				// 设置为跟随狼王
				mob.getNavigation().moveTo(this, 1.2);

				// 如果狼王有目标，狼群也攻击该目标
				if (wolfTarget != null && wolfTarget.isAlive()) {
					if (mob.getTarget() != wolfTarget) {
						mob.setTarget(wolfTarget);
					}
				} else {
					mob.setTarget(null);
				}
			}
		}
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
	 * 移动动画：始终播放 walk/idle/telekinetic_flight
	 * 不与 action_controller 互斥，GeckoLib 会自动混合两个控制器的动画，
	 * 确保移动动画（腿部）和动作动画（上半身）同时正确显示。
	 */
	private PlayState movementAnimation(AnimationState<NGamioZedaEntity> state) {
		if (this.isDeadOrDying()) {
			return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
		}
		if (this.entityData.get(IS_FLOATING)) {
			return state.setAndContinue(RawAnimation.begin().thenLoop("telekinetic_flight"));
		}
		if (state.isMoving()) {
			return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
		}
		return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
	}

	/**
	 * 动作动画：客户端跟踪模式
	 * 使用 ANIM_VERSION 检测每次 setAnimation 调用（即使动画名相同），
	 * 确保动画播放事件不遗漏。
	 * 动作动画与移动动画（movementAnimation）独立播放，
	 * GeckoLib 自动混合两者，无需互斥。
	 */
	// 客户端记录的最近动画版本号
	private int lastAnimVersion = -1;

	private PlayState actionAnimation(AnimationState<NGamioZedaEntity> state) {
		String animation = this.entityData.get(ANIMATION);
		int version = this.entityData.get(ANIM_VERSION);

		if (animation.equals(ANIM_NONE)) {
			lastAnimVersion = -1;
			return PlayState.STOP;
		}

		// 版本号变化（每次 setAnimation 递增）时开始/重启动画
		if (version != lastAnimVersion) {
			lastAnimVersion = version;
			state.getController().forceAnimationReset();
			return state.setAndContinue(RawAnimation.begin().thenPlay(animation));
		}

		// 正在播放当前动画
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 20) {
			this.remove(NGamioZedaEntity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	public String getSyncedAnimation() {
		return this.entityData.get(ANIMATION);
	}

	/**
	 * 设置动画，默认不复位
	 */
	public void setAnimation(String animation) {
		setAnimation(animation, 0);
	}

	/**
	 * 设置动画，带自动复位
	 * @param animation 动画名
	 * @param resetDelay 多少 tick 后自动复位为 ANIM_NONE（<=0 表示不复位）
	 */
	public void setAnimation(String animation, int resetDelay) {
		if (!animation.equals(this.entityData.get(ANIMATION))) {
			this.entityData.set(ANIMATION, animation);
		}
		// 每次调用都递增版本号，即使动画名相同
		// 确保客户端能检测到每次 setAnimation 调用，正确重启动画
		this.entityData.set(ANIM_VERSION, this.entityData.get(ANIM_VERSION) + 1);
		if (resetDelay > 0) {
			autoResetTick = tickCount + resetDelay;
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
		data.add(new AnimationController<>(this, "action_controller", 4, this::actionAnimation));
	}

	// ========== Getters/Setters ==========

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

	public int getDamageCount() {
		return damageCount;
	}

	public void setDamageCount(int count) {
		this.damageCount = count;
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
	 */
	@Override
	public boolean hurt(@NotNull DamageSource source, float amount) {
		return GamioDamageHandler.handleDamage(this, source, amount);
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
		compound.putInt("TelekineticCooldown", telekineticCooldown);
		compound.putBoolean("IsFloating", isFloating);
		compound.putInt("FloatingAttackIndex", floatingAttackIndex);
		compound.putInt("SupernaturalKillingIntentTicks", supernaturalKillingIntentTicks);
		compound.putInt("FloatingLandDelay", floatingLandDelay);
		// 保存狼群成员UUID
		CompoundTag wolfPackTag = new CompoundTag();
		int idx = 0;
		for (UUID uuid : wolfPackMembers) {
			wolfPackTag.putUUID("member_" + idx, uuid);
			idx++;
		}
		wolfPackTag.putInt("count", idx);
		compound.put("WolfPackMembers", wolfPackTag);
		skillManager.save(compound);
	}

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
		if (compound.contains("TelekineticCooldown")) {
			telekineticCooldown = compound.getInt("TelekineticCooldown");
		}
		if (compound.contains("IsFloating")) {
			boolean wasFloating = compound.getBoolean("IsFloating");
			setFloating(wasFloating);
		}
		if (compound.contains("FloatingAttackIndex")) {
			floatingAttackIndex = compound.getInt("FloatingAttackIndex");
		}
		if (compound.contains("SupernaturalKillingIntentTicks")) {
			supernaturalKillingIntentTicks = compound.getInt("SupernaturalKillingIntentTicks");
		}
		if (compound.contains("FloatingLandDelay")) {
			floatingLandDelay = compound.getInt("FloatingLandDelay");
		}
		// 读取狼群成员UUID
		wolfPackMembers.clear();
		if (compound.contains("WolfPackMembers")) {
			CompoundTag wolfPackTag = compound.getCompound("WolfPackMembers");
			int count = wolfPackTag.getInt("count");
			for (int i = 0; i < count; i++) {
				if (wolfPackTag.contains("member_" + i)) {
					wolfPackMembers.add(wolfPackTag.getUUID("member_" + i));
				}
			}
		}
		skillManager.load(compound);
	}
}