package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
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
 * 变色龙古朗基（葛·伽鲁梅·丽）
 * 能力：隐身 + 超自然看破（开启超自然感官的玩家才能看见）
 */
public class GarumeReEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GarumeReEntity.class, EntityDataSerializers.STRING);

    // ========== 动画名称常量 ==========
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_WALK = "walk";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";
    private static final String ANIM_DEATH = "death";

    // ========== 普攻轮询（参考犀牛古朗基设计模式：attack1 / attack2） ==========
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    // ========== 变色龙隐身能力 ==========
    private static final int CYCLE_TOTAL_TICKS = 300;       // 总周期 (15秒 = 300 ticks)
    private static final int INVISIBLE_TICKS = 200;         // 隐身持续时间 (10秒 = 200 ticks)

    private int chameleonTimer = 0;          // 周期计时器（每15秒循环）
    private boolean chameleonInvisible = false; // 当前是否隐身状态
    private boolean needsInvisibilityUpdate = false; // 是否需要更新隐身状态

    public GarumeReEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.GARUME_RE.get(), world);
    }

    public GarumeReEntity(EntityType<GarumeReEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_garume_re");
        // 初始先可见5秒，再进入隐身循环
        this.chameleonTimer = CYCLE_TOTAL_TICKS - INVISIBLE_TICKS;
    }

    // ========== 动画同步（entityData 驱动，参考犀牛古朗基） ==========

    public String getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    public void setAnimation(String anim) {
        this.entityData.set(ANIMATION, anim);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_IDLE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            tickChameleonInvisibility();

            // === 近战冷却递减 ===
            if (this.meleeCooldownTicks > 0) this.meleeCooldownTicks--;

            // === 受控近战序列（参考犀牛古朗基） ===
            if (this.meleeTicksRemaining > 0) {
                this.performMeleeTick();
                return;
            }
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false) {
            @Override
            public void tick() {
                LivingEntity target = this.mob.getTarget();
                if (target != null && target.isAlive() && !isBehindTarget(target)) {
                    // 不在目标背后 → 绕到背后位置
                    double yawRad = Math.toRadians(target.getYRot());
                    double behindX = target.getX() + Math.sin(yawRad) * 3.0;
                    double behindZ = target.getZ() - Math.cos(yawRad) * 3.0;
                    this.mob.getNavigation().moveTo(behindX, target.getY(), behindZ, 1.3);
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                } else {
                    // 在目标背后 → 默认行为（接近并攻击）
                    super.tick();
                }
            }

            @Override
            protected void checkAndPerformAttack(@NotNull LivingEntity enemy, double dist) {
                if (meleeTicksRemaining > 0) return;
                double reachSqr = getAttackReachSqr(enemy);
                if (dist <= reachSqr && this.getTicksUntilNextAttack() <= 0) {
                    this.resetAttackCooldown();
                    this.mob.doHurtTarget(enemy);
                }
            }

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

    // ========== 变色龙隐身逻辑 ==========

    private void tickChameleonInvisibility() {
        if (this.isDeadOrDying()) {
            if (this.isInvisible()) {
                this.setInvisible(false);
                this.chameleonInvisible = false;
            }
            return;
        }

        // 有目标时持续隐身（战斗状态不退出隐身）
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            if (!chameleonInvisible) {
                chameleonInvisible = true;
                needsInvisibilityUpdate = true;
            }
            if (needsInvisibilityUpdate) {
                this.setInvisible(true);
                needsInvisibilityUpdate = false;
            }
            return;
        }

        // 无目标：每15秒循环，前10秒隐身，后5秒可见
        chameleonTimer++;
        if (chameleonTimer >= CYCLE_TOTAL_TICKS) {
            chameleonTimer = 0;
        }

        boolean shouldBeInvisible = chameleonTimer < INVISIBLE_TICKS;
        if (shouldBeInvisible != chameleonInvisible) {
            chameleonInvisible = shouldBeInvisible;
            needsInvisibilityUpdate = true;
        }

        if (needsInvisibilityUpdate) {
            this.setInvisible(chameleonInvisible);
            needsInvisibilityUpdate = false;
        }
    }

    // ========== 普攻轮询：attack1 / attack2（参考犀牛古朗基设计模式） ==========

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

        this.setAnimation(this.lastMeleeWasAttack1 ? ANIM_ATTACK1 : ANIM_ATTACK2);
        this.getNavigation().stop();

        LivingEntity target = this.getTarget();
        if (target != null) {
            faceTarget(target);
            this.lookAt(target, 360F, 360F);
        }
    }

    private void performMeleeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            faceTarget(target);

            if (this.meleeTicksRemaining == MELEE_HIT_TICK && this.distanceTo(target) <= 3.0D) {
                target.invulnerableTime = 0;
                float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                target.hurt(this.damageSources().mobAttack(this), damage);

                // 背刺：从背后攻击造成额外50%伤害（总计1.5倍）
                if (isBehindTarget(target)) {
                    float extraDamage = damage * 0.5f;
                    target.hurt(this.damageSources().mobAttack(this), extraDamage);
                }
            }
        }

        this.meleeTicksRemaining--;
        if (this.meleeTicksRemaining <= 0) {
            this.meleeTicksRemaining = 0;
            this.setAnimation(ANIM_IDLE);
        }
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(this.position());
        if (toTarget.horizontalDistanceSqr() <= 0.001D) return;
        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /**
     * 判断攻击者是否在目标背后
     * 通过比较目标的视线方向与目标→攻击者方向，若方向相反（点积 < 0）则为背后
     */
    private boolean isBehindTarget(LivingEntity target) {
        Vec3 toAttacker = new Vec3(
                this.getX() - target.getX(),
                0,
                this.getZ() - target.getZ()
        ).normalize();

        Vec3 targetLook = Vec3.directionFromRotation(0, target.getYRot());

        return toAttacker.dot(targetLook) < 0;
    }

    /**
     * 控制玩家能否看到此实体:
     * - 正常玩家：隐身时不可见
     * - 开启超自然感官的玩家：始终可见
     */
    @Override
    public boolean isInvisibleTo(Player player) {
        if (!this.isInvisible()) {
            return false;
        }
        try {
            if (RiderPlayerVariablesProvider.get(player) != null
                    && RiderPlayerVariablesProvider.get(player).getKuugaData() != null
                    && RiderPlayerVariablesProvider.get(player).getKuugaData().isSupernaturalSensesActive()) {
                return false; // 超自然感官可以看到隐身
            }
        } catch (Exception e) {
            // 安全保护
        }
        return super.isInvisibleTo(player);
    }

    // ========== 动画（参考犀牛古朗基的 entityData 驱动模式） ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "action_controller", 2, this::actionAnimation));
    }

    /**
     * 移动层控制器：处理行走/空闲/死亡循环动画
     */
    private PlayState movementAnimation(AnimationState<GarumeReEntity> state) {
        // 当有动作动画在播放时，移动控制器停止
        if (!ANIM_IDLE.equals(this.getAnimation())) {
            return PlayState.STOP;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(ANIM_DEATH));
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_WALK));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_IDLE));
    }

    /**
     * 动作层控制器：播放 attack1 / attack2 等单次动画
     */
    private PlayState actionAnimation(AnimationState<GarumeReEntity> state) {
        String animation = this.getAnimation();
        if (ANIM_IDLE.equals(animation)) {
            return PlayState.STOP;
        }
        AnimationController<GarumeReEntity> controller = state.getController();
        if (controller.getAnimationState() == AnimationController.State.STOPPED) {
            controller.forceAnimationReset();
            state.setAndContinue(RawAnimation.begin().thenPlay(animation));
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    // ========== NBT 存档/读档 ==========

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ChameleonTimer", chameleonTimer);
        compound.putBoolean("ChameleonInvisible", chameleonInvisible);
        compound.putInt("MeleeCooldown", this.meleeCooldownTicks);
        compound.putInt("MeleeTicks", this.meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", this.lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ChameleonTimer")) {
            chameleonTimer = compound.getInt("ChameleonTimer");
        }
        if (compound.contains("ChameleonInvisible")) {
            chameleonInvisible = compound.getBoolean("ChameleonInvisible");
            needsInvisibilityUpdate = true;
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

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.ATTACK_DAMAGE, 50)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 30)
                .add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32);
    }
}