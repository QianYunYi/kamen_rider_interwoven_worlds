package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.entity.motoycle.base.BaseMotorcycleEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 葛·巴达·巴（Go Badaa Ba）
 * <p>
 * 行走、攻击等动画与 {@link ZuBaduuBaEntity} 一致：
 * - 近战轮询 attack1 / attack2
 * <p>
 * 特殊能力（召马）：
 * - 默认隐藏骨骼组 horse
 * - 血量低于90时，取消隐藏horse组，立即播放 mount_a_horse 动画（时长2秒），
 *   初始速度延迟0.75秒后获得，向前移动约6格
 * - 上马后待机播放 horse_riding_idle，行走播放 horse_riding
 * <p>
 * 无跳跃逻辑与跳跃动画（区别于 ZuBaduuBaEntity）。
 */
public class GoBadaaBaEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.STRING);

    // ===== 骨骼组horse可见性 =====
    private static final EntityDataAccessor<Boolean> HORSE_VISIBLE = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.BOOLEAN);
    // ===== 是否已上马 =====
    private static final EntityDataAccessor<Boolean> MOUNTED = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.BOOLEAN);
    // ===== mount_a_horse 剩余播放时长（2秒=40 tick） =====
    private static final EntityDataAccessor<Integer> MOUNT_TIMER = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.INT);
    // ===== 骑马冲撞中（用于骑乘动画切换） =====
    private static final EntityDataAccessor<Boolean> RAMMING = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.BOOLEAN);
    // ===== 急刹剩余时长（2秒=40 tick，0表示未急刹） =====
    private static final EntityDataAccessor<Integer> EMERGENCY_BRAKE = SynchedEntityData.defineId(GoBadaaBaEntity.class, EntityDataSerializers.INT);

    // ========== 动画名称常量 ==========
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_WALK = "walk";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";
    private static final String ANIM_DEATH = "death";

    // ===== 召马专属动画 =====
    private static final String ANIM_MOUNT_A_HORSE = "mount_a_horse";
    private static final String ANIM_HORSE_RIDING_IDLE = "horse_riding_idle";
    private static final String ANIM_HORSE_RIDING = "horse_riding";
    private static final String ANIM_EMERGENCY_BRAKE = "emergency_brake";

    // ===== 召马参数 =====
    private static final float MOUNT_THRESHOLD = 90.0F;          // 血量低于90触发
    private static final int MOUNT_DURATION_TICKS = 40;          // 2秒
    private static final int MOUNT_CHARGE_DELAY_TICKS = 15;      // 加速度在0.75秒(15 tick)延迟后开始
    private static final int MOUNT_CHARGE_DURATION_TICKS = 25;   // 加速度持续1.25秒(25 tick)
    private static final double CHARGE_ACCELERATION = 0.14;      // 每tick的水平向前加速度

    // ===== 召马冲刺计时 =====
    private int mountChargeDelay = -1;          // 0.75秒延迟计时
    private int chargeTicksRemaining = -1;      // 1.25秒加速度剩余计时

    // ===== 骑马惯性（参考 BaseMotorcycleEntity 的惯性模型，不继承） =====
    private float horseForwardSpeed;                    // 朝向前向速度
    private double horseInertiaVelX;                    // 侧向惯性速度X
    private double horseInertiaVelZ;                    // 侧向惯性速度Z
    private float horseYawSin;                          // 朝向缓存 sin
    private float horseYawCos;                          // 朝向缓存 cos
    private boolean mountedPhysicsInitialized = false;  // 首次接管物理时继承当前速度
    private int ramHitCooldown = 0;                     // 冲撞命中冷却

    // 有目标冲撞（快） & 无目标行走（慢）
    private static final float MOUNTED_CHARGE_SPEED = 0.60F;        // 冲撞目标速度（最大速度）
    private static final float MOUNTED_WALK_SPEED = 0.22F;          // 无目标行走速度(慢)
    private static final float MOUNTED_CHARGE_ACCEL = 0.03F;        // 冲撞加速度（放缓，让直线行驶更渐进到上限）
    private static final float MOUNTED_WALK_ACCEL = 0.03F;          // 行走加速
    private static final float MOUNTED_DECEL = 0.90F;               // 速度回落(朝目标/行走速度)衰减
    private static final float MOUNTED_INERTIA_FRICTION = 0.94F;    // 侧向惯性摩擦
    private static final double MOUNTED_BASE_TURN = 16.0;           // 低速最大转向角/帧
    private static final double MOUNTED_MIN_TURN = 1.5;             // 高速最小转向角/帧
    private static final double MOUNTED_TURN_REDUCTION = 20.0;      // 速度越大转向越小的转换比（提高阻尼，高速转向更慢）
    private static final int RAM_HIT_COOLDOWN = 12;                 // 冲撞命中冷却(tick)
    private static final double RAM_HIT_RANGE = 2.2;                // 冲撞命中范围(格)

    // ===== 急刹（仅高速需要大转向时触发，低速可直接转弯） =====
    private static final int EMERGENCY_BRAKE_DURATION = 40;        // 急刹2秒
    private static final int EMERGENCY_BRAKE_ANIM_DELAY = 20;      // 延迟1秒(20 tick)后开始播放动画，2秒结束恢复转向
    private static final double U_TURN_TURN_DEG = 80.0;            // 高速下朝向差超过该值就刹停重瞄（低于绕圈时的~90°，避免绕目标打转）
    private static final float U_TURN_SPEED_THRESHOLD = MOUNTED_CHARGE_SPEED * 0.8F; // 速度+惯性(合成速度)接近满速(>0.48)才急刹，中低速不触发，避免过灵敏

    // ===== 普攻轮询 =====
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    public GoBadaaBaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.GO_BADAA_BA.get(), world);
    }

    public GoBadaaBaEntity(EntityType<GoBadaaBaEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.GO;
        this.setTexture("go/go_badaa_ba");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_IDLE);
        this.entityData.define(HORSE_VISIBLE, false);   // 默认隐藏horse骨骼组
        this.entityData.define(MOUNTED, false);
        this.entityData.define(MOUNT_TIMER, 0);
        this.entityData.define(RAMMING, false);
        this.entityData.define(EMERGENCY_BRAKE, 0);
    }

    // ========== 动画驱动：采用 ZuBaduuBa 的 entityData 同步模式 ==========

    public String getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    public void setAnimation(String anim) {
        this.entityData.set(ANIMATION, anim);
    }

    // ========== 骨骼组horse可见性 ==========

    public void setHorseVisible(boolean visible) {
        this.entityData.set(HORSE_VISIBLE, visible);
    }

    public boolean isHorseVisible() {
        return this.entityData.get(HORSE_VISIBLE);
    }

    // ========== 上马状态 ==========

    public boolean isMounted() {
        return this.entityData.get(MOUNTED);
    }

    public void setMounted(boolean mounted) {
        this.entityData.set(MOUNTED, mounted);
    }

    public int getMountTimer() {
        return this.entityData.get(MOUNT_TIMER);
    }

    public void setMountTimer(int ticks) {
        this.entityData.set(MOUNT_TIMER, Math.max(0, ticks));
    }

    // ========== 骑马冲撞状态 ==========

    public boolean isRamming() {
        return this.entityData.get(RAMMING);
    }

    public void setRamming(boolean ramming) {
        this.entityData.set(RAMMING, ramming);
    }

    // ========== 急刹状态 ==========

    public int getEmergencyBrake() {
        return this.entityData.get(EMERGENCY_BRAKE);
    }

    public void setEmergencyBrake(int ticks) {
        this.entityData.set(EMERGENCY_BRAKE, Math.max(0, ticks));
    }

    // ========== 重置骑马速度与惯性（击杀时调用） ==========

    private void resetMountedMomentum() {
        horseForwardSpeed = 0;
        horseInertiaVelX = 0;
        horseInertiaVelZ = 0;
        mountChargeDelay = -1;
        chargeTicksRemaining = -1;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            public boolean canUse() {
                // 骑马时不使用普攻，改为冲撞对手
                return !isMounted() && super.canUse();
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
                return 9.0;
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                // 骑马冲撞期间禁止随机游荡
                return !isMounted() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========================================================================
    //  tick 更新（服务端权威驱动）
    // ========================================================================

    @Override
    public void tick() {
        super.tick();

        // ===== 召马触发：血量低于90时（仅服务端，触发一次） =====
        if (!level().isClientSide && !isMounted() && !isDeadOrDying() && this.getHealth() < MOUNT_THRESHOLD) {
            startMount();
        }

        // ===== mount_a_horse 播放计时（服务端） =====
        if (!level().isClientSide && isMounted() && getMountTimer() > 0) {
            int t = getMountTimer() - 1;
            setMountTimer(t);
            if (t == 0) {
                // 播放结束，复位为待机动画，由移动层播放 horse_riding_idle
                setAnimation(ANIM_IDLE);
            }
        }

        // ===== 急刹计时（服务端）：2秒，期间不能转向，急刹触发即播放动画，2秒结束恢复 =====
        if (!level().isClientSide && getEmergencyBrake() > 0) {
            int eb = getEmergencyBrake() - 1;
            setEmergencyBrake(eb);
            if (eb == 0) {
                // 2秒结束：复位动画，解除急刹。此时速度已为低速，由移动层正常转弯面向目标
                setAnimation(ANIM_IDLE);
                setRamming(false);
            }
        }

        // ===== 召马冲刺：延迟0.75秒后开始，向前加速度持续1.25秒（服务端） =====
        if (!level().isClientSide) {
            if (mountChargeDelay > 0) {
                mountChargeDelay--;
                if (mountChargeDelay == 0) {
                    // 延迟结束，进入加速度阶段
                    chargeTicksRemaining = MOUNT_CHARGE_DURATION_TICKS;
                }
            } else if (chargeTicksRemaining > 0) {
                // 加速度阶段：持续施加向前加速度
                applyChargeAcceleration();
                chargeTicksRemaining--;
            }
        }

        // ===== 冷却递减 =====
        if (meleeCooldownTicks > 0) meleeCooldownTicks--;
        if (ramHitCooldown > 0) ramHitCooldown--;

        // ===== 受控近战序列 =====
        if (meleeTicksRemaining > 0) {
            performMeleeTick();
            return;
        }

        // ===== 骑马冲撞（服务端）：骑马不使用普攻，改为直线冲撞对手 =====
        if (!level().isClientSide) {
            handleRamAttack();
        } else {
            // 客户端仅跟随 synced 的 ramming 状态，无需计算
            if (!isMounted()) {
                setRamming(false);
            }
        }

        // ===== 地面站立：恢复默认动画，movementAnimation 根据移动与上马状态播放 =====
        if (onGround() && !isMounted()) {
            setAnimation(ANIM_IDLE);
        }
    }

    /**
     * 骑马移动：有目标时冲撞(速度快、带惯性、转向随速度受限)；无目标时减速回落到慢速行走。
     * 物理模型参考 {@link BaseMotorcycleEntity}（仅参考，不继承）。
     */
    private void handleRamAttack() {
        if (!isMounted() || getMountTimer() > 0 || isDeadOrDying()) {
            setRamming(false);
            return;
        }

        // 先处理接触伤害：只要范围内有生物就撞飞。放在最前面，
        // 避免触发急停(掉头)提前 return 而漏掉「已撞到但未造成伤害」的目标。
        applyRamContactDamage();

        // 急刹期间：2秒不能转向、不能冲撞，只做减速刹车
        if (getEmergencyBrake() > 0) {
            handleEmergencyBrake();
            setRamming(false);
            return;
        }

        getNavigation().stop();

        LivingEntity target = getTarget();
        boolean hasTarget = target != null && target.isAlive();

        if (hasTarget) {
            // 有目标：冲撞，转向受当前速度限制。若需要约180°掉头，先急刹停下，再转向
            if (needUTurn(target)) {
                triggerEmergencyBrake();
                return;
            }
            steerTowardCharging(target);
            setRamming(true);
            updateMountedPhysics(true);
        } else {
            // 无目标：减速回落至慢速行走（带惯性，滑行减速）
            setRamming(false);
            setAnimation(ANIM_IDLE);
            updateMountedPhysics(false);
        }
    }

    /**
     * 冲撞接触伤害：以自身为中心扫描范围内全部生物实体（只要不是自己，一律撞飞并造成伤害）。
     * 放在 handleRamAttack 最前面调用，保证即使随后触发急停(掉头)导致提前 return，
     * 已碰撞到的目标也已受到伤害。
     */
    private void applyRamContactDamage() {
        if (ramHitCooldown <= 0) {
            ramHitCooldown = RAM_HIT_COOLDOWN;
            List<LivingEntity> targets = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(RAM_HIT_RANGE),
                    e -> e != this && e.isAlive());
            for (LivingEntity t : targets) {
                t.invulnerableTime = 0;
                t.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                Vec3 kb = t.position().subtract(this.position()).normalize().scale(0.8);
                t.setDeltaMovement(t.getDeltaMovement().x + kb.x, 0.3, t.getDeltaMovement().z + kb.z);
                t.hurtMarked = true;
            }
        }
    }

    /**
     * 急刹刹车物理（2秒）：期间不能转向，0~1秒把前向速度线性减速到0（惯性已清零），
     * 1~2秒保持静止。立即播放的 emergency_brake 动画全程(2秒)持续至结束，恢复转向与移动。
     */
    private void handleEmergencyBrake() {
        int remaining = getEmergencyBrake();
        if (remaining > EMERGENCY_BRAKE_ANIM_DELAY) {
            // 0~1秒（剩40~20 tick）：线性减速到0
            float factor = (float) (remaining - EMERGENCY_BRAKE_ANIM_DELAY) / EMERGENCY_BRAKE_ANIM_DELAY;
            horseForwardSpeed = horseForwardSpeed * factor;
        } else {
            // 1~2秒：已停止，保持静止（动画播放阶段）
            horseForwardSpeed = 0;
        }

        refreshMountedYawCache();
        double finalVelX = horseInertiaVelX + horseYawSin * horseForwardSpeed;
        double finalVelZ = horseInertiaVelZ + horseYawCos * horseForwardSpeed;
        setDeltaMovement(finalVelX, this.getDeltaMovement().y, finalVelZ);
        this.hasImpulse = true;
    }

    /**
     * 触发急刹（掉头时调用）：开启2秒急刹并立即播放急刹动画（动画持续2秒）。
     * 前向速度不瞬间归零——保留当前速度，由 handleEmergencyBrake 在0~1秒内线性减速到0；
     * 仅清掉侧向惯性、停止冲刺计时。
     */
    private void triggerEmergencyBrake() {
        if (getEmergencyBrake() > 0) return;
        setEmergencyBrake(EMERGENCY_BRAKE_DURATION);
        setAnimation(ANIM_EMERGENCY_BRAKE);
        mountedPhysicsInitialized = true;
        horseInertiaVelX = 0;
        horseInertiaVelZ = 0;
        mountChargeDelay = -1;
        chargeTicksRemaining = -1;
        setRamming(false);
    }

    /**
     * 判断是否需要急刹：
     * - 仅当当前速度较高（高速冲撞）且需要约180°掉头时才触发急刹停下；
     * - 低速时允许直接正常转弯（此时返回false，交给 steerTowardCharging 渐进转向）。
     */
    private boolean needUTurn(LivingEntity target) {
        // 低速时不触发急刹，直接正常转弯
        if (getMountedResolvedSpeed() <= U_TURN_SPEED_THRESHOLD) return false;

        Vec3 toTarget = target.position().subtract(this.position()).multiply(1, 0, 1);
        if (toTarget.lengthSqr() < 1e-4) return false;
        float targetYaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F);
        float diff = Mth.wrapDegrees(targetYaw - this.getYRot());
        return Math.abs(diff) > U_TURN_TURN_DEG;
    }

    /**
     * 面朝目标转向，转向角受当前速度限制：速度越快转向越小(直线感越强)
     */
    private void steerTowardCharging(LivingEntity target) {
        refreshMountedYawCache();
        Vec3 toTarget = target.position().subtract(this.position()).multiply(1, 0, 1);
        if (toTarget.lengthSqr() < 1e-4) return;
        toTarget = toTarget.normalize();

        double curSpeed = getMountedResolvedSpeed();
        double maxTurn = Math.max(MOUNTED_MIN_TURN, MOUNTED_BASE_TURN - curSpeed * MOUNTED_TURN_REDUCTION);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F);
        float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
        yawDiff = (float) Math.max(-maxTurn, Math.min(maxTurn, yawDiff));
        this.setYRot(this.getYRot() + yawDiff);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        refreshMountedYawCache();
    }

    /**
     * 冲撞/行走物理（仅服务端）：
     * 总速度 = 朝向×前向速度 + 侧向惯性；冲撞更快速，无目标回落到慢速行走并摩擦减速。
     */
    private void updateMountedPhysics(boolean charging) {
        // 首次接管骑马物理时，继承当前速度(来自召马脚本冲刺)，保证衔接流畅
        if (!mountedPhysicsInitialized) {
            mountedPhysicsInitialized = true;
            inheritMountedVelocity();
        }
        refreshMountedYawCache();

        // 侧向惯性：每个 tick 都做摩擦衰减（指数衰减，有目标冲撞/无目标行走均生效），避免横向漂移永不消失
        horseInertiaVelX *= MOUNTED_INERTIA_FRICTION;
        horseInertiaVelZ *= MOUNTED_INERTIA_FRICTION;

        if (charging) {
            horseForwardSpeed += MOUNTED_CHARGE_ACCEL;
            horseForwardSpeed = Math.min(horseForwardSpeed, MOUNTED_CHARGE_SPEED);
        } else {
            // 无目标：回落到慢速行走；前向速度高于行走速度时惯性减速
            if (horseForwardSpeed > MOUNTED_WALK_SPEED) {
                horseForwardSpeed = Math.max(MOUNTED_WALK_SPEED, horseForwardSpeed * MOUNTED_DECEL);
            } else if (horseForwardSpeed < MOUNTED_WALK_SPEED) {
                horseForwardSpeed = Math.min(MOUNTED_WALK_SPEED, horseForwardSpeed + MOUNTED_WALK_ACCEL);
            }
        }

        // 合成总速度：前向 + 侧向惯性
        double finalVelX = horseInertiaVelX + horseYawSin * horseForwardSpeed;
        double finalVelZ = horseInertiaVelZ + horseYawCos * horseForwardSpeed;

        double spd = Math.sqrt(finalVelX * finalVelX + finalVelZ * finalVelZ);
        double limit = MOUNTED_CHARGE_SPEED * 1.3;
        if (spd > limit) {
            finalVelX = finalVelX / spd * limit;
            finalVelZ = finalVelZ / spd * limit;
        }

        setDeltaMovement(finalVelX, this.getDeltaMovement().y, finalVelZ);
        this.hasImpulse = true;
        // 爬坡：向前移动被前方方块挡住但头顶有空位时，跳起跃上斜坡/台阶
        tryClimbStep(finalVelX, finalVelZ);
    }

    /**
     * 骑马爬坡/上台阶：纯水平速度面对斜坡或一级台阶会被碰撞卡住。
     * 若前方(身体高度)是实心方块、再往上一格可通行，则施加一个向上的跳跃速度，
     * 使实体跃上斜坡或台阶继续前进；正常平地上前方为空气，不触发。
     */
    private void tryClimbStep(double vx, double vz) {
        if (!this.onGround()) return;
        double hyp = Math.sqrt(vx * vx + vz * vz);
        if (hyp < 1e-4) return;
        double nx = vx / hyp;
        double nz = vz / hyp;
        Vec3 pos = this.position();
        BlockPos body = BlockPos.containing(pos.x + nx * 0.55D, pos.y + 0.9D, pos.z + nz * 0.55D);
        if (this.level().getBlockState(body).isSolid()
                && !this.level().getBlockState(body.above()).isSolid()) {
            Vec3 mv = this.getDeltaMovement();
            this.setDeltaMovement(mv.x, 0.42D, mv.z);
            this.hasImpulse = true;
        }
    }

    /**
     * 把当前 deltaMovement 分解到朝向(前向)与侧向(惯性)，作为骑马物理的初始状态
     */
    private void inheritMountedVelocity() {
        refreshMountedYawCache();
        Vec3 v = this.getDeltaMovement();
        double fx = horseYawSin;   // 朝向X
        double fz = horseYawCos;   // 朝向Z
        horseForwardSpeed = (float) (v.x * fx + v.z * fz);
        double sx = -fz;           // 侧向X
        double sz = fx;            // 侧向Z
        double lat = v.x * sx + v.z * sz;
        horseInertiaVelX = sx * lat;
        horseInertiaVelZ = sz * lat;
    }

    private void refreshMountedYawCache() {
        float yawRad = (float) Math.toRadians(this.getYRot());
        horseYawSin = -(float) Math.sin(yawRad);
        horseYawCos = (float) Math.cos(yawRad);
    }

    private double getMountedResolvedSpeed() {
        return Math.sqrt(
                (horseInertiaVelX + horseYawSin * horseForwardSpeed) * (horseInertiaVelX + horseYawSin * horseForwardSpeed)
                        + (horseInertiaVelZ + horseYawCos * horseForwardSpeed) * (horseInertiaVelZ + horseYawCos * horseForwardSpeed));
    }

    /**
     * 触发召马：取消隐藏horse组，播放 mount_a_horse，
     * 加速度在0.75秒延迟后开始，并持续1.25秒
     */
    private void startMount() {
        setMounted(true);
        setHorseVisible(true);
        setMountTimer(MOUNT_DURATION_TICKS);
        setAnimation(ANIM_MOUNT_A_HORSE);

        // 记录延迟计时，延迟结束后进入持续加速阶段（见 tick）
        mountChargeDelay = MOUNT_CHARGE_DELAY_TICKS;
        chargeTicksRemaining = -1;
    }

    /**
     * 加快水平速度：沿面朝方向（水平）施加一次向前加速度
     */
    private void applyChargeAcceleration() {
        Vec3 look = getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        if (h.lengthSqr() > 1e-6) {
            h = h.normalize();
        }
        Vec3 vel = this.getDeltaMovement();
        setDeltaMovement(vel.x + h.x * CHARGE_ACCELERATION, vel.y, vel.z + h.z * CHARGE_ACCELERATION);
        hasImpulse = true;
    }

    // ========================================================================
    //  动画控制器（参考 ZuBaduuBa 的 entityData 驱动模式 + NGamioZeda 上马状态）
    // ========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "action_controller", 2, this::actionAnimation));
    }

    /**
     * 移动层控制器：处理行走/空闲/空中滞留/骑马循环动画
     */
    private PlayState movementAnimation(AnimationState<GoBadaaBaEntity> state) {
        String anim = this.getAnimation();
        // 单次动作动画播放时移动层停止（含 mount_a_horse）
        if (!ANIM_IDLE.equals(anim) && !ANIM_WALK.equals(anim)
                && !ANIM_HORSE_RIDING_IDLE.equals(anim) && !ANIM_HORSE_RIDING.equals(anim)) {
            return PlayState.STOP;
        }

        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(ANIM_DEATH));
        }

        // ===== 上马状态与骑乘动画 =====
        if (isMounted()) {
            // mount_a_horse 播放期间（2秒）只播上马动画，移动层停止
            if (getMountTimer() > 0) {
                return PlayState.STOP;
            }
            if (isRamming() || state.isMoving() || this.walkAnimation.speed() > 0.01F
                    || this.getDeltaMovement().horizontalDistance() > 0.01) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_HORSE_RIDING));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_HORSE_RIDING_IDLE));
        }

        if (state.isMoving() || this.walkAnimation.speed() > 0.01F) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_WALK));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(ANIM_IDLE));
    }

    /**
     * 动作层控制器：处理单次触发动画（攻击1/攻击2/上马）
     */
    private PlayState actionAnimation(AnimationState<GoBadaaBaEntity> state) {
        String anim = this.getAnimation();
        AnimationController<GoBadaaBaEntity> controller = state.getController();

        if (ANIM_ATTACK1.equals(anim) || ANIM_ATTACK2.equals(anim)
                || ANIM_MOUNT_A_HORSE.equals(anim) || ANIM_EMERGENCY_BRAKE.equals(anim)) {
            // 急刹动画 emergency_brake 在动画文件中设置为 hold_on_last_frame，
            // 播放1秒动作后定格在最后一帧，整个急刹(2秒)期间控制器保持播放状态而不会回到STOPPED，
            // 因此不会因STOPPED被重复重启（避免"播放两次/只播1秒"）。
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                controller.forceAnimationReset();
                controller.setAnimationSpeed(1d);
                state.setAndContinue(RawAnimation.begin().thenPlay(anim));
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    // ========================================================================
    //  普攻轮询 attack1 / attack2（与 ZuBaduuBa 一致）
    // ========================================================================

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // 骑马时不使用普攻 attack1/attack2，改由冲撞逻辑伤害
        if (isMounted()) return false;
        if (meleeCooldownTicks <= 0 && meleeTicksRemaining <= 0) {
            startMeleeAttack();
            return true;
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
    //  死亡 & 数据持久化
    // ========================================================================

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HorseVisible", isHorseVisible());
        compound.putBoolean("Mounted", isMounted());
        compound.putInt("MountTimer", getMountTimer());
        compound.putInt("MountChargeDelay", mountChargeDelay);
        compound.putInt("ChargeTicksRemaining", chargeTicksRemaining);
        compound.putInt("MeleeCooldown", meleeCooldownTicks);
        compound.putInt("MeleeTicks", meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", lastMeleeWasAttack1);
        compound.putInt("EmergencyBrake", getEmergencyBrake());
        compound.putFloat("HorseForwardSpeed", horseForwardSpeed);
        compound.putDouble("HorseInertiaVelX", horseInertiaVelX);
        compound.putDouble("HorseInertiaVelZ", horseInertiaVelZ);
        compound.putBoolean("MountedPhysicsInitialized", mountedPhysicsInitialized);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("HorseVisible")) {
            setHorseVisible(compound.getBoolean("HorseVisible"));
        }
        if (compound.contains("Mounted")) {
            setMounted(compound.getBoolean("Mounted"));
        }
        if (compound.contains("MountTimer")) {
            setMountTimer(compound.getInt("MountTimer"));
        }
        if (compound.contains("MountChargeDelay")) {
            mountChargeDelay = compound.getInt("MountChargeDelay");
        }
        if (compound.contains("ChargeTicksRemaining")) {
            chargeTicksRemaining = compound.getInt("ChargeTicksRemaining");
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
        if (compound.contains("EmergencyBrake")) {
            setEmergencyBrake(compound.getInt("EmergencyBrake"));
        }
        if (compound.contains("HorseForwardSpeed")) {
            horseForwardSpeed = compound.getFloat("HorseForwardSpeed");
        }
        if (compound.contains("HorseInertiaVelX")) {
            horseInertiaVelX = compound.getDouble("HorseInertiaVelX");
        }
        if (compound.contains("HorseInertiaVelZ")) {
            horseInertiaVelZ = compound.getDouble("HorseInertiaVelZ");
        }
        if (compound.contains("MountedPhysicsInitialized")) {
            mountedPhysicsInitialized = compound.getBoolean("MountedPhysicsInitialized");
        }
    }

    public static void init() {
    }

    // ========================================================================
    //  属性（与 ZuBaduuBa 一致，最大血量需高于召马阈值90）
    // ========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 220)
                .add(Attributes.ATTACK_DAMAGE, 24)
                .add(Attributes.ARMOR, 5)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}