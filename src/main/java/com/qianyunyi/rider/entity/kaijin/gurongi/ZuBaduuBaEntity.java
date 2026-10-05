package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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

import java.util.EnumSet;

/**
 * 蝗虫古朗基（Zu Baduu Ba）
 * 特殊能力：
 * - 跳跃10格高度，跳跃播放jump动画，落地播放landing动画
 * - 空中杀法：将目标抓取绑定在自身前方1格，跟随移动；
 *   跳跃时播放jump_capture动画，滞空持续播放jump_capture_idle；
 *   延迟1秒后取消抓取，目标自然坠落；
 *   释放后冷却5秒（100 tick）才能再次抓取
 * - 永久免疫坠落伤害
 */
public class ZuBaduuBaEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuBaduuBaEntity.class, EntityDataSerializers.STRING);
    // ========== 动画名称常量 ==========
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_WALK = "walk";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";
    private static final String ANIM_JUMP = "jump";
    private static final String ANIM_JUMP_IDLE = "jump_idle";
    private static final String ANIM_JUMP_CAPTURE = "jump_capture";
    private static final String ANIM_JUMP_CAPTURE_IDLE = "jump_capture_idle";
    private static final String ANIM_LANDING = "landing";
    private static final String ANIM_DEATH = "death";

    // ===== 跳跃 / 落地状态追踪 =====
    private boolean wasOnGround = true;
    private boolean jumping = false;
    private boolean landing = false;

    // ===== 空中抓取系统 =====
    private boolean isCapturing = false;
    private int captureTimer = -1;
    private int capturedEntityId = -1;

    // ===== 独立冷却系统（修复空中杀法对玩家不生效的bug） =====
    private int jumpCooldown = 0;       // 跳跃冷却，仅控制跳跃频率
    private int captureCooldown = 0;    // 抓取冷却，仅控制抓取频率

    // ===== 普攻轮询（参考犀牛古朗基设计模式：attack1 / attack2） =====
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    public ZuBaduuBaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_BADUU_BA.get(), world);
    }

    public ZuBaduuBaEntity(EntityType<ZuBaduuBaEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_baduu_ba");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_IDLE);
    }

    // ========== 动画驱动：采用犀牛古朗基的 entityData 同步模式 ==========

    public String getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    public void setAnimation(String anim) {
        this.entityData.set(ANIMATION, anim);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new JumpAttackGoal());
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected void checkAndPerformAttack(@NotNull LivingEntity enemy, double dist) {
                // 抓取或近战动画进行中时跳过
                if (isCapturing || meleeTicksRemaining > 0) return;
                double reachSqr = getAttackReachSqr(enemy);
                if (dist <= reachSqr && this.getTicksUntilNextAttack() <= 0) {
                    this.resetAttackCooldown();
                    this.mob.doHurtTarget(enemy);
                }
            }

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
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========================================================================
    //  tick 更新（服务端权威驱动，修复原版 jumping 标记只会在客户端重置的bug）
    // ========================================================================

    @Override
    public void tick() {
        super.tick();

        // ===== 冷却递减 =====
        if (jumpCooldown > 0) jumpCooldown--;
        if (captureCooldown > 0) captureCooldown--;
        if (meleeCooldownTicks > 0) meleeCooldownTicks--;

        // ===== 跳跃 / 落地检测 =====
        if (wasOnGround && !onGround()) {
            jumping = true;
            setAnimation(isCapturing ? ANIM_JUMP_CAPTURE : ANIM_JUMP);
        }
        if (!wasOnGround && onGround()) {
            landing = true;
            setAnimation(ANIM_LANDING);
        }
        wasOnGround = onGround();

        // ===== 受控近战序列（参考犀牛古朗基） =====
        if (meleeTicksRemaining > 0) {
            performMeleeTick();
            return;
        }

        // ===== 空中持续动画状态（此处在 jumping/landing 标记重置前判断） =====
        if (!onGround()) {
            // 跳跃/落地标记为 true 时说明本 tick 刚触发跳跃/落地动画，
            // 下个 tick 标记重置后进入此分支，切换为对应的 idle 循环动画
            if (!jumping && !landing) {
                if (isCapturing && !ANIM_JUMP_CAPTURE_IDLE.equals(getAnimation())) {
                    setAnimation(ANIM_JUMP_CAPTURE_IDLE);
                } else if (!isCapturing && !ANIM_JUMP_IDLE.equals(getAnimation())) {
                    setAnimation(ANIM_JUMP_IDLE);
                }
            }
        } else {
            // 地面站立：恢复默认动画，movementAnimation 根据移动状态播放 walk / idle
            if (!jumping && !landing) {
                setAnimation(ANIM_IDLE);
            }
        }

        // ===== 空中抓取处理（仅服务端） =====
        if (!level().isClientSide && isCapturing) {
            handleCapture();
        }

        // ===== 每 tick 结束重置单次触发标记（确保服务端 jumping/landing 只持续一 tick） =====
        jumping = false;
        landing = false;
    }

    // ========================================================================
    //  动画控制器（参考犀牛古朗基的 entityData 驱动模式）
    // ========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "action_controller", 2, this::actionAnimation));
    }

    /**
     * 移动层控制器：处理行走/空闲/空中滞留循环动画
     */
    private PlayState movementAnimation(AnimationState<ZuBaduuBaEntity> state) {
        String anim = this.getAnimation();
        // 单次动作动画（跳跃/攻击/落地）播放时，移动层停止
        if (!ANIM_IDLE.equals(anim) && !ANIM_WALK.equals(anim)
                && !ANIM_JUMP_IDLE.equals(anim) && !ANIM_JUMP_CAPTURE_IDLE.equals(anim)) {
            return PlayState.STOP;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(ANIM_DEATH));
        }
        if (ANIM_JUMP_IDLE.equals(anim)) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_JUMP_IDLE));
        }
        if (ANIM_JUMP_CAPTURE_IDLE.equals(anim)) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_JUMP_CAPTURE_IDLE));
        }
        if (state.isMoving() || this.walkAnimation.speed() > 0.01F) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_WALK));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_IDLE));
    }

    /**
     * 动作层控制器：处理单次触发动画（跳跃/抓取跳跃/落地/攻击1/攻击2）
     */
    private PlayState actionAnimation(AnimationState<ZuBaduuBaEntity> state) {
        String anim = this.getAnimation();
        AnimationController<ZuBaduuBaEntity> controller = state.getController();

        if (ANIM_JUMP.equals(anim) || ANIM_JUMP_CAPTURE.equals(anim)
                || ANIM_LANDING.equals(anim) || ANIM_ATTACK1.equals(anim) || ANIM_ATTACK2.equals(anim)) {
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                controller.forceAnimationReset();
                state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    // ========================================================================
    //  普攻轮询 attack1 / attack2（参考犀牛古朗基设计模式）
    // ========================================================================

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // 先尝试空中抓取（对玩家也生效，修复原版不对玩家使用的bug）
        if (target instanceof LivingEntity living) {
            if (tryCaptureTarget(living)) {
                return true;
            }
            // 抓取未触发且近战冷却结束 → 执行普攻轮询
            if (meleeCooldownTicks <= 0 && meleeTicksRemaining <= 0) {
                startMeleeAttack();
                return true;
            }
        }
        return false;
    }

    private void startMeleeAttack() {
        meleeTicksRemaining = MELEE_DURATION_TICKS;
        meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        lastMeleeWasAttack1 = !lastMeleeWasAttack1;

        setAnimation(lastMeleeWasAttack1 ? ANIM_ATTACK1 : ANIM_ATTACK2);

        LivingEntity target = getTarget();
        if (target != null) {
            faceTarget(target);
        }
    }

    private void performMeleeTick() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            faceTarget(target);

            // 在攻击判定帧造成伤害
            if (meleeTicksRemaining == MELEE_HIT_TICK && distanceTo(target) <= 3.0D) {
                target.invulnerableTime = 0;
                target.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }

        meleeTicksRemaining--;
        if (meleeTicksRemaining <= 0) {
            meleeTicksRemaining = 0;
            setAnimation(ANIM_IDLE);
        }
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(this.position());
        if (toTarget.horizontalDistanceSqr() <= 0.001D) return;
        float yaw = (float) (Math.atan2(toTarget.z, toTarget.x) * (180D / Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    // ========================================================================
    //  空中杀法 - 抓取目标绑定在自身前方1格
    // ========================================================================

    /**
     * 尝试对目标发动空中抓取
     * 条件：未在抓取中、抓取冷却完毕
     * 抓取与跳跃使用独立冷却，修复原版跳跃后无法抓取的bug
     */
    private boolean tryCaptureTarget(LivingEntity target) {
        if (isCapturing || captureCooldown > 0) return false;
        startCapture(target);
        return true;
    }

    private void startCapture(LivingEntity target) {
        isCapturing = true;
        captureTimer = 20;
        capturedEntityId = target.getId();

        // 自身起跳，将目标带离地面
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist > 0.01) {
            double vh = Math.min(hDist * 0.15, 0.6);
            setDeltaMovement(dx / hDist * vh, 1.4, dz / hDist * vh);
        } else {
            setDeltaMovement(0, 1.4, 0);
        }
        hasImpulse = true;
    }

    private void handleCapture() {
        if (captureTimer > 0) {
            captureTimer--;

            Entity captured = level().getEntity(capturedEntityId);
            if (captured == null || !captured.isAlive()) {
                releaseCapture();
                return;
            }

            // 将目标位置设定在自身前方1格（与自身眼睛平齐）
            Vec3 front = getForward().scale(1.0);
            captured.setPos(
                    getX() + front.x,
                    getY() + 0.5,
                    getZ() + front.z
            );
            captured.setYRot(yRotO);
            captured.setXRot(xRotO);

            if (captured instanceof LivingEntity living) {
                living.setDeltaMovement(0, 0, 0);
            }
        }

        if (captureTimer <= 0 && isCapturing) {
            releaseCapture();
        }
    }

    private void releaseCapture() {
        Entity captured = level().getEntity(capturedEntityId);
        if (captured != null) {
            captured.setDeltaMovement(captured.getDeltaMovement().x, -0.5, captured.getDeltaMovement().z);
        }
        isCapturing = false;
        captureTimer = -1;
        capturedEntityId = -1;
        // 使用独立冷却，不影响跳跃
        captureCooldown = 100;
    }

    // ========================================================================
    //  永久免疫坠落伤害
    // ========================================================================

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    // ========================================================================
    //  跳跃AI目标 - 动态抛物线，距离越远跳得越高越远
    // ========================================================================

    private class JumpAttackGoal extends Goal {

        public JumpAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (jumpCooldown > 0) return false;
            if (isCapturing) return false;
            if (!onGround()) return false;
            if (meleeTicksRemaining > 0) return false;

            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            return horizontalDist >= 2.0 && horizontalDist <= 24.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) return;

            getLookControl().setLookAt(target, 360.0f, 360.0f);
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double heightDiff = target.getY() - getY();

            double normalizedDist = Math.min(Math.max((horizontalDist - 3.0) / 21.0, 0.0), 1.0);
            double vyFromDist = 0.6 + normalizedDist * 1.2;
            double minVyForHeight = heightDiff > 0
                    ? Math.sqrt(0.16 * heightDiff) + 0.3
                    : 0.4;
            double vy = Math.max(vyFromDist, minVyForHeight);
            vy = Math.min(vy, 2.2);

            double airTime = vy * 25.0;
            double dragFactor = 1.0 - Math.pow(0.91, airTime);
            double targetLanding = horizontalDist * 0.9;
            double vh = targetLanding * 0.09 / Math.max(dragFactor, 0.01);
            vh = Math.max(vh, 0.2);
            vh = Math.min(vh, 1.5);

            if (horizontalDist < 0.01) {
                setDeltaMovement(0, vy, 0);
            } else {
                setDeltaMovement(dx / horizontalDist * vh, vy, dz / horizontalDist * vh);
            }
            hasImpulse = true;

            // 跳跃冷却仅控制跳跃频率，不影响空中抓取
            jumpCooldown = 100;
        }
    }

    // ========================================================================
    //  死亡 & 数据持久化
    // ========================================================================

    @Override
    protected void tickDeath() {
        if (isCapturing) {
            releaseCapture();
        }
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("isCapturing", isCapturing);
        compound.putInt("captureTimer", captureTimer);
        compound.putInt("capturedEntityId", capturedEntityId);
        compound.putInt("jumpCooldown", jumpCooldown);
        compound.putInt("captureCooldown", captureCooldown);
        compound.putInt("MeleeCooldown", meleeCooldownTicks);
        compound.putInt("MeleeTicks", meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("isCapturing")) {
            isCapturing = compound.getBoolean("isCapturing");
        }
        if (compound.contains("captureTimer")) {
            captureTimer = compound.getInt("captureTimer");
        }
        if (compound.contains("capturedEntityId")) {
            capturedEntityId = compound.getInt("capturedEntityId");
        }
        if (compound.contains("jumpCooldown")) {
            jumpCooldown = compound.getInt("jumpCooldown");
        }
        if (compound.contains("captureCooldown")) {
            captureCooldown = compound.getInt("captureCooldown");
        }
        if (compound.contains("MeleeCooldown")) {
            meleeCooldownTicks = compound.getInt("MeleeCooldown");
        }
        if (compound.contains("MeleeTicks")) {
            meleeTicksRemaining = compound.getInt("MeleeTicks");
        }
        if (compound.contains("LastMeleeWasAttack1")) {
            lastMeleeWasAttack1 = compound.getBoolean("LastMeleeWasAttack1");
        }
    }

    public static void init() {
    }

    // ========================================================================
    //  属性
    // ========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 220)
                .add(Attributes.ATTACK_DAMAGE, 24)
                .add(Attributes.ARMOR, 5)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}