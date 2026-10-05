package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.effect.SonicWaveRenderer;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 蝙蝠古朗基强化体（Zu Goma Gu Enhanced Form）
 * 特殊能力：
 * - 吸血：造成的伤害10%恢复自身生命值
 * - 音波：释放音波攻击，范围虚弱I效果，冷却10秒
 * - 飞行模式：自动追击高空目标
 * - 不死逃脱：生命低于30%或受到致命伤害时，免疫死亡并高速飞离战场
 */
public class ZuGomaGuEnhancedEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuGomaGuEnhancedEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> ESCAPING = SynchedEntityData.defineId(ZuGomaGuEnhancedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SONIC_WAVE_TRIGGER = SynchedEntityData.defineId(ZuGomaGuEnhancedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ZuGomaGuEnhancedEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String ANIM_NONE = "empty";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";

    private int lastSonicWaveTrigger = 0;

    // 音波技能冷却（单位：tick，20tick = 1秒）
    private int sonicWaveCooldown = 0;
    // 逃脱模式计时器
    private int escapeTimer = 0;
    // 是否已触发逃脱
    private boolean escapeTriggered = false;
    // 一键动画自动清除计时器
    private int animClearTimer = 0;

    // ========== 普攻轮询：attack1 / attack2 ==========
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    public ZuGomaGuEnhancedEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_GOMA_GU_ENHANCED.get(), world);
    }

    public ZuGomaGuEnhancedEntity(EntityType<ZuGomaGuEnhancedEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_goma_gu_enhanced_form");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_NONE);
        this.entityData.define(ESCAPING, false);
        this.entityData.define(FLYING, false);
        this.entityData.define(SONIC_WAVE_TRIGGER, 0);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeSunGoal(this, 1.2));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity attackTarget) {
                return 9.0;
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Gurongi.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端检测音波触发信号
        if (level().isClientSide) {
            int currentTrigger = entityData.get(SONIC_WAVE_TRIGGER);
            if (currentTrigger != lastSonicWaveTrigger) {
                lastSonicWaveTrigger = currentTrigger;
                SonicWaveRenderer.triggerWaveAtEntity(this);
            }
            return;
        }

        // 音波技能冷却递减
        if (sonicWaveCooldown > 0) {
            sonicWaveCooldown--;
        }

        // 近战冷却递减
        if (meleeCooldownTicks > 0) {
            meleeCooldownTicks--;
        }

        // 一键动画自动清除计时
        if (animClearTimer > 0) {
            animClearTimer--;
            if (animClearTimer == 0) {
                entityData.set(ANIMATION, ANIM_NONE);
            }
        }

        // === 受控近战序列 ===
        if (meleeTicksRemaining > 0) {
            performMeleeTick();
            return;
        }

        // 逃脱模式处理
        if (isEscaping()) {
            handleEscapeMode();
            return;
        }

        // 检查是否触发逃脱（血量低于30%）
        if (!escapeTriggered && getHealth() / getMaxHealth() < 0.3f) {
            triggerEscape();
            return;
        }

        // 战斗飞行追击
        if (getTarget() != null && getTarget().isAlive()) {
            handleCombatFlight();
        } else if (isFlyingMode()) {
            setFlyingMode(false);
            setNoGravity(false);
        }

        // 音波技能 AI
        if (sonicWaveCooldown <= 0 && getTarget() != null && getTarget().isAlive() && distanceTo(getTarget()) <= 12) {
            performSonicWave();
            sonicWaveCooldown = 200;
        }
    }

    // ========================================================================
    //  普攻轮询（attack1/attack2） + 吸血（10%生命恢复）
    // ========================================================================

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!(target instanceof LivingEntity)
                || this.meleeCooldownTicks > 0
                || this.meleeTicksRemaining > 0) {
            return false;
        }
        this.startMeleeAttack();
        return true;
    }

    private void startMeleeAttack() {
        this.meleeTicksRemaining = MELEE_DURATION_TICKS;
        this.meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        this.lastMeleeWasAttack1 = !this.lastMeleeWasAttack1;

        String attackAnim = this.lastMeleeWasAttack1 ? ANIM_ATTACK1 : ANIM_ATTACK2;
        setAnimation(attackAnim);
        this.getNavigation().stop();

        LivingEntity target = this.getTarget();
        if (target != null) {
            this.faceTarget(target);
            this.lookAt(target, 360F, 360F);
        }
    }

    private void performMeleeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.faceTarget(target);

            if (this.meleeTicksRemaining == MELEE_HIT_TICK && this.distanceTo(target) <= 3.0D) {
                target.invulnerableTime = 0;
                float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                target.hurt(this.damageSources().mobAttack(this), damage);
                // 吸血：恢复造成伤害的10%
                heal(damage * 0.1f);
            }
        }

        this.meleeTicksRemaining--;
        if (this.meleeTicksRemaining <= 0) {
            this.meleeTicksRemaining = 0;
        }
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(this.position());
        if (toTarget.horizontalDistanceSqr() <= 0.001D) return;
        float yaw = (float) (net.minecraft.util.Mth.atan2(toTarget.z, toTarget.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    // ========================================================================
    //  不死逃脱 - 低于30%生命或致命伤害时无敌飞离战场
    // ========================================================================

    @Override
    public boolean hurt(@NotNull net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isEscaping()) {
            return false;
        }

        if (!escapeTriggered && amount >= getHealth()) {
            triggerEscape();
            return false;
        }

        boolean result = super.hurt(source, amount);

        if (result && !escapeTriggered && getHealth() / getMaxHealth() < 0.3f) {
            triggerEscape();
        }

        return result;
    }

    @Override
    public void die(@NotNull net.minecraft.world.damagesource.DamageSource source) {
        if (!escapeTriggered) {
            triggerEscape();
            return;
        }
        super.die(source);
    }

    private boolean isEscaping() {
        return entityData.get(ESCAPING);
    }

    private void setEscaping(boolean escaping) {
        entityData.set(ESCAPING, escaping);
    }

    private void triggerEscape() {
        if (escapeTriggered || level().isClientSide) return;
        escapeTriggered = true;
        setEscaping(true);
        setInvulnerable(true);
        setFlyingMode(true);
        escapeTimer = 0;
        setTarget(null);
    }

    private void handleEscapeMode() {
        escapeTimer++;

        Player nearestPlayer = level().getNearestPlayer(this, 64);

        if (nearestPlayer != null) {
            double dx = getX() - nearestPlayer.getX();
            double dz = getZ() - nearestPlayer.getZ();
            double dy = Math.min(getY() - nearestPlayer.getY() + 8, 10);

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 0) {
                double speed = 2.5;
                setDeltaMovement(
                        (dx / distance) * speed,
                        Math.max((dy / distance) * speed, 0.8),
                        (dz / distance) * speed
                );
            }

            if (distance > 40 || escapeTimer > 200) {
                triggerEscapeAdvancement();
                discard();
            }
        } else {
            setDeltaMovement(0, 1.5, 0);
            if (escapeTimer > 100) {
                triggerEscapeAdvancement();
                discard();
            }
        }
    }

    private void triggerEscapeAdvancement() {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            AddonWorldData data = AddonWorldData.get(serverLevel);
            if (data.getGurongiSpawn() == 2) {
                data.setGurongiSpawn(3);
                data.sync(serverLevel);
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] Zu-Goma-Gu-Enhanced逃脱触发阶段升级: 2 -> 3"
                );
            }
        }
    }

    // ========================================================================
    //  音波技能 - 范围虚弱效果
    // ========================================================================

    private void performSonicWave() {
        if (level().isClientSide) return;

        entityData.set(SONIC_WAVE_TRIGGER, entityData.get(SONIC_WAVE_TRIGGER) + 1);

        double range = 8.0;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range))) {
            if (target == this || !target.isAlive()) continue;
            if (distanceTo(target) > range) continue;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
        }
    }

    // ========================================================================
    //  飞行模式
    // ========================================================================

    private boolean isFlyingMode() {
        return entityData.get(FLYING);
    }

    private void setFlyingMode(boolean flying) {
        entityData.set(FLYING, flying);
        setNoGravity(flying);
    }

    private void handleCombatFlight() {
        LivingEntity target = getTarget();
        if (target == null) return;

        double dy = target.getY() - this.getY();
        double horizontalDist = this.distanceTo(target);
        double heightThreshold = 3.0;

        if (dy > heightThreshold) {
            if (!isFlyingMode()) {
                setFlyingMode(true);
            }
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            double speed = 1.2;

            double motionX = 0, motionZ = 0;
            if (dist > 0.5) {
                motionX = (dx / dist) * speed;
                motionZ = (dz / dist) * speed;
            }

            double motionY;
            if (dist < 3.0) {
                motionY = Math.min(dy * 0.5, 1.2);
            } else {
                motionY = Math.min(Math.max(dy * 0.3, 0.3), 0.8);
            }
            if (dy < -1.0) {
                motionY = -0.3;
            }

            if (horizontalDist < 6.0) {
                double factor = horizontalDist / 6.0;
                motionX *= factor;
                motionZ *= factor;
            }

            setDeltaMovement(motionX, Math.max(motionY, -0.5), motionZ);
            this.lookAt(target, 30.0F, 30.0F);

        } else if (isFlyingMode()) {
            if (horizontalDist > 8.0 && dy > -2.0) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                double speed = 1.0;
                setDeltaMovement(
                        (dx / dist) * speed,
                        -0.2,
                        (dz / dist) * speed
                );
            } else {
                setFlyingMode(false);
                setNoGravity(false);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isFlyingMode()) {
            move(MoverType.SELF, this.getDeltaMovement());
            setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean isSunBurnTick() {
        return false;
    }

    // ========================================================================
    //  动画系统 - 双控制器模式
    // ========================================================================

    public String getAnimation() {
        return entityData.get(ANIMATION);
    }

    public void setAnimation(String animation) {
        entityData.set(ANIMATION, animation);
        if (!ANIM_NONE.equals(animation)) {
            animClearTimer = 12;
        }
    }

    private PlayState moveController(AnimationState<ZuGomaGuEnhancedEntity> state) {
        if (!ANIM_NONE.equals(this.getAnimation())) {
            return PlayState.STOP;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
        }
        if (isFlyingMode() || (this.isNoGravity() && !this.onGround())) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("flight_idle"));
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    private PlayState actionController(AnimationState<ZuGomaGuEnhancedEntity> state) {
        String anim = this.getAnimation();
        if (ANIM_NONE.equals(anim)) {
            return PlayState.STOP;
        }
        AnimationController<ZuGomaGuEnhancedEntity> controller = state.getController();
        if (controller.getAnimationState() == AnimationController.State.STOPPED) {
            controller.forceAnimationReset();
            state.setAndContinue(RawAnimation.begin().thenPlay(anim));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "move_controller", 4, this::moveController));
        data.add(new AnimationController<>(this, "action_controller", 4, this::actionController));
    }

    @Override
    protected void tickDeath() {
        if (isEscaping()) return;
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    // ========================================================================
    //  数据持久化
    // ========================================================================

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("escapeTriggered", escapeTriggered);
        compound.putInt("sonicWaveCooldown", sonicWaveCooldown);
        compound.putInt("escapeTimer", escapeTimer);
        compound.putInt("MeleeCooldown", this.meleeCooldownTicks);
        compound.putInt("MeleeTicks", this.meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", this.lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("escapeTriggered")) {
            escapeTriggered = compound.getBoolean("escapeTriggered");
            if (escapeTriggered) {
                setEscaping(true);
                setInvulnerable(true);
                setNoAi(true);
                setNoGravity(true);
            }
        }
        if (compound.contains("sonicWaveCooldown")) {
            sonicWaveCooldown = compound.getInt("sonicWaveCooldown");
        }
        if (compound.contains("escapeTimer")) {
            escapeTimer = compound.getInt("escapeTimer");
        }
        if (compound.contains("MeleeCooldown")) {
            this.meleeCooldownTicks = compound.getInt("MeleeCooldown");
        }
        if (compound.contains("MeleeTicks")) {
            this.meleeTicksRemaining = compound.getInt("MeleeTicks");
        }
        if (compound.contains("LastMeleeWasAttack1")) {
            this.lastMeleeWasAttack1 = compound.getBoolean("LastMeleeWasAttack1");
        }
    }

    public static void init() {
    }

    // ========================================================================
    //  属性：生命120 攻击54 护甲20 速度0.12
    // ========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 120)
                .add(Attributes.ATTACK_DAMAGE, 54)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 20)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }
}