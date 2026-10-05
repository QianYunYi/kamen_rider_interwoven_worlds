package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.client.effect.SonicWaveRenderer;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameManager;
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
import net.minecraft.world.damagesource.DamageSource;
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
 * 蝙蝠古朗基（Zu Gooma Gu）
 * 特殊能力：
 * - 吸血：造成的伤害10%恢复自身生命值
 * - 音波：释放音波攻击，范围虚弱I效果，冷却10秒
 * - 不死逃脱：生命低于30%或受到致命伤害时，免疫死亡并高速飞离战场
 */
public class ZuGoomaGuEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuGoomaGuEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> ESCAPING = SynchedEntityData.defineId(ZuGoomaGuEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SONIC_WAVE_TRIGGER = SynchedEntityData.defineId(ZuGoomaGuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(ZuGoomaGuEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String ANIM_NONE = "empty";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";

    private int lastSonicWaveTrigger = 0; // 客户端记录上次触发值，用于检测变化

    // 音波技能冷却（单位：tick，20tick = 1秒）
    private int sonicWaveCooldown = 0;
    // 逃脱模式计时器
    private int escapeTimer = 0;
    // 是否已触发逃脱
    private boolean escapeTriggered = false;
    // 一键动画自动清除计时器（攻击动画播放指定 tick 后自动恢复 ANIM_NONE）
    private int animClearTimer = 0;

    // ========== 普攻轮询：attack1 / attack2 ==========
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    public ZuGoomaGuEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU.get(), world);
    }

    public ZuGoomaGuEntity(EntityType<ZuGoomaGuEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_gooma_gu");
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
                return 9.0; // 玩家同等攻击距离 (3.0^2)
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
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
            return; // 逃脱模式下不执行其他AI
        }

        // 检查是否触发逃脱（血量低于30%）
        if (!escapeTriggered && getHealth() / getMaxHealth() < 0.3f) {
            triggerEscape();
            return;
        }

        // 战斗飞行追击：有目标且未在逃脱时，自动飞向高处目标
        if (getTarget() != null && getTarget().isAlive()) {
            handleCombatFlight();
        } else if (isFlyingMode()) {
            // 没有目标时降落到地面
            setFlyingMode(false);
            setNoGravity(false);
        }

        // 音波技能 AI：有目标且冷却完毕且距离适当时释放
        if (sonicWaveCooldown <= 0 && getTarget() != null && getTarget().isAlive() && distanceTo(getTarget()) <= 12) {
            performSonicWave();
            sonicWaveCooldown = 200; // 10秒冷却（20tick × 10）
        }
    }

    // ========================================================================
    //  普攻轮询（attack1/attack2） + 吸血（10%生命恢复）
    // ========================================================================

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // 普攻：attack1/attack2 轮询（模仿犀牛）
        if (!(target instanceof LivingEntity)
                || this.meleeCooldownTicks > 0
                || this.meleeTicksRemaining > 0) {
            return false;
        }
        this.startMeleeAttack();
        return true;
    }

    // ========== 普攻轮询：attack1 / attack2 ==========

    private void startMeleeAttack() {
        this.meleeTicksRemaining = MELEE_DURATION_TICKS;
        this.meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        this.lastMeleeWasAttack1 = !this.lastMeleeWasAttack1;

        String attackAnim = this.lastMeleeWasAttack1 ? ANIM_ATTACK1 : ANIM_ATTACK2;
        setAnimation(attackAnim);
        // 动画自动清除计时器由 setAnimation 处理
        this.getNavigation().stop();

        LivingEntity target = this.getTarget();
        if (target != null) {
            this.faceTarget(target);
            this.lookAt(target, 360F, 360F);
        }
    }

    private void performMeleeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

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
        net.minecraft.world.phys.Vec3 toTarget = target.position().subtract(this.position());
        if (toTarget.horizontalDistanceSqr() <= 0.001D) return;
        float yaw = (float) (net.minecraft.util.Mth.atan2(toTarget.z, toTarget.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 逃脱模式下免疫所有伤害
        if (isEscaping()) {
            return false;
        }

        // 受到致命伤害时触发逃脱（取消死亡）
        if (!escapeTriggered && amount >= getHealth()) {
            triggerEscape();
            return false;
        }

        boolean result = super.hurt(source, amount);

        // 受伤后再次检查血量是否低于30%，触发逃脱
        if (result && !escapeTriggered && getHealth() / getMaxHealth() < 0.3f) {
            triggerEscape();
        }

        return result;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        // 未触发逃脱时强制取消死亡，转为逃脱模式
        if (!escapeTriggered) {
            triggerEscape();
            return;
        }
        super.die(source);
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

    /**
     * 战斗飞行追击逻辑：
     * 当目标明显高于蝙蝠时，启用飞行模式追向目标；
     * 当目标与蝙蝠高度差较小且水平距离近时，降回地面进行近战。
     */
    private void handleCombatFlight() {
        LivingEntity target = getTarget();
        if (target == null) return;

        double dy = target.getY() - this.getY();
        double horizontalDist = this.distanceTo(target);
        double heightThreshold = 3.0; // 高度差超过此值则起飞追击

        if (dy > heightThreshold) {
            // 目标在高处 → 起飞追击
            if (!isFlyingMode()) {
                setFlyingMode(true);
            }
            // 计算朝向目标的单位向量
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            // 基础飞行速度（战斗飞行比逃脱慢，但比走路快）
            double speed = 1.2;

            double motionX = 0, motionZ = 0;
            if (dist > 0.5) {
                motionX = (dx / dist) * speed;
                motionZ = (dz / dist) * speed;
            }

            // 如果距离目标还有一定水平距离，垂直速度 = min(高度差 * 0.3, 最大上升速度)
            // 如果已经接近目标水平位置，垂直速度加大以快速到达目标高度
            double motionY;
            if (dist < 3.0) {
                // 水平已接近，垂直追近
                motionY = Math.min(dy * 0.5, 1.2);
            } else {
                motionY = Math.min(Math.max(dy * 0.3, 0.3), 0.8);
            }
            // 如果蝙蝠比目标还高，略微下沉
            if (dy < -1.0) {
                motionY = -0.3;
            }

            // 接近目标时减速
            if (horizontalDist < 6.0) {
                double factor = horizontalDist / 6.0;
                motionX *= factor;
                motionZ *= factor;
            }

            setDeltaMovement(motionX, Math.max(motionY, -0.5), motionZ);

            // 看向目标
            this.lookAt(target, 30.0F, 30.0F);

        } else if (isFlyingMode()) {
            // 目标不在高处，但蝙蝠还在飞 → 降落到地面
            // 如果水平距离较远且地面有障碍，保持飞行接近后再降落
            if (horizontalDist > 8.0 && dy > -2.0) {
                // 水平距离较远且目标没有明显低于蝙蝠，保持飞行接近
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
                // 水平够近或目标明显低于蝙蝠 → 降落
                setFlyingMode(false);
                setNoGravity(false);
            }
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isFlyingMode()) {
            // 飞行模式下手动移动并应用摩擦，不调用 super.travel()（其含重力逻辑）
            move(MoverType.SELF, this.getDeltaMovement());
            setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean isSunBurnTick() {
        // 蝙蝠古朗基害怕阳光但不会像僵尸一样被点燃
        return false;
    }

    // ========================================================================
    //  不死逃脱 - 低于30%生命或致命伤害时无敌飞离战场
    // ========================================================================

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
        setFlyingMode(true); // 启用飞行模式 + setNoGravity
        escapeTimer = 0;

        // 清除目标
        setTarget(null);

        // ★ 只有已注册参赛者的古朗基触发逃脱才通知游戏管理器
        // 狼王转换和刷怪蛋召唤的古朗基未注册，不触发
        if (level() instanceof ServerLevel serverLevel) {
            GurongiGameManager manager = GurongiGameManager.get(serverLevel);
            if (manager.isRegistered(this.getUUID())) {
                manager.onParticipantEscaped(this.getUUID(), serverLevel);
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[ZuGoomaGu] {} 触发逃脱，放弃基基鲁游戏规则，顺位下一位参赛者",
                        this.getName().getString()
                );
            }
        }

        // ★ 标记：逃脱是游戏逻辑主动移除，告知 onRemovedFromWorld 不做清理
        this.removalHandledByGame = true;
    }

    private void handleEscapeMode() {
        escapeTimer++;

        // 找到最近的玩家并向反方向高速飞行
        Player nearestPlayer = level().getNearestPlayer(this, 64);

        if (nearestPlayer != null) {
            // 计算远离玩家的方向向量
            double dx = getX() - nearestPlayer.getX();
            double dz = getZ() - nearestPlayer.getZ();
            // 保持一定高度差向上飞行
            double dy = Math.min(getY() - nearestPlayer.getY() + 8, 10);

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 0) {
                double speed = 2.5; // 非常快的飞行速度
                setDeltaMovement(
                        (dx / distance) * speed,
                        Math.max((dy / distance) * speed, 0.8), // 确保向上
                        (dz / distance) * speed
                );
            }

            // 远离玩家足够远（>40格）或超过10秒后消失
            if (distance > 40 || escapeTimer > 200) {
                triggerEscapeAdvancement();
                discard();
            }
        } else {
            // 没有玩家时直接向上飞然后消失
            setDeltaMovement(0, 1.5, 0);
            if (escapeTimer > 100) { // 5秒后消失
                triggerEscapeAdvancement();
                discard();
            }
        }
    }

    /**
     * 逃脱成功时推进游戏阶段：EPISODE 2 → EPISODE 3
     */
    private void triggerEscapeAdvancement() {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            AddonWorldData data = AddonWorldData.get(serverLevel);
            if (data.getGurongiSpawn() == 2) {
                data.setGurongiSpawn(3);
                data.sync(serverLevel);
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] Zu-Gooma-Gu逃脱触发阶段升级: 2 -> 3"
                );
            }
        }
    }

    // ========================================================================
    //  音波技能 - 范围虚弱效果
    // ========================================================================

    private void performSonicWave() {
        if (level().isClientSide) return;

        // 通过同步数据触发客户端渲染器显示音波视觉特效
        entityData.set(SONIC_WAVE_TRIGGER, entityData.get(SONIC_WAVE_TRIGGER) + 1);

        // 对范围内所有活体实体施加虚弱I（3秒=60tick）
        double range = 8.0;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range))) {
            if (target == this || !target.isAlive()) continue;
            if (distanceTo(target) > range) continue;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
        }
    }

    // ========================================================================
    //  动画系统 - 双控制器模式
    // ========================================================================

    public String getAnimation() {
        return entityData.get(ANIMATION);
    }

    public void setAnimation(String animation) {
        entityData.set(ANIMATION, animation);
        // 设置非空动画时启动自动清除计时器
        if (!ANIM_NONE.equals(animation)) {
            animClearTimer = 12; // ~0.6 秒后自动恢复 ANIM_NONE
        }
    }

    /**
     * 移动控制器：处理循环动画（飞行/行走/待机）
     */
    private PlayState moveController(AnimationState<ZuGoomaGuEntity> state) {
        // 动作动画播放时暂停移动动画
        if (!ANIM_NONE.equals(this.getAnimation())) {
            return PlayState.STOP;
        }
        // 死亡→death
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
        }
        // 飞行模式（无重力+不在地面）→flight_idle
        if (isFlyingMode() || (this.isNoGravity() && !this.onGround())) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("flight_idle"));
        }
        // 行走→walk
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }
        // 待机→idle
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    /**
     * 动作控制器：播放一次性动作动画（攻击等），播完后自动恢复 ANIM_NONE
     */
    private PlayState actionController(AnimationState<ZuGoomaGuEntity> state) {
        String anim = this.getAnimation();
        if (ANIM_NONE.equals(anim)) {
            return PlayState.STOP;
        }
        AnimationController<ZuGoomaGuEntity> controller = state.getController();
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
        // 逃脱模式下不走正常死亡流程
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
    //  属性：生命200 攻击7 护甲4 速度0.35
    // ========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.ATTACK_DAMAGE, 21)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}