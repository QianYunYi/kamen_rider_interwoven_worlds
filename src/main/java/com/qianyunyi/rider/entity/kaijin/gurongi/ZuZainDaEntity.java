package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.AddonWorldData;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import java.util.EnumSet;

/**
 * 犀牛古朗基（滋·赞因·达）
 * 能力：
 * - 高生命值和高护甲，典型的肉盾型敌人
 * - 冲锋：朝目标高速冲刺，附加击退和伤害加成，冷却8秒
 * - 猛击地面：造成范围伤害和缓慢效果，冷却12秒
 */
public class ZuZainDaEntity extends Gurongi {

    // ========== 动画 ==========

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuZainDaEntity.class, EntityDataSerializers.STRING);

    // 动画名称
    private static final String ANIMATION_IDLE = "idle";
    private static final String ANIMATION_WALK = "walk";
    private static final String ANIMATION_ATTACK1 = "attack1";
    private static final String ANIMATION_ATTACK2 = "attack2";
    private static final String ANIMATION_SPRINT = "rhinoceros_charging";
    private static final String ANIMATION_SLAM = "slam";
    private static final String ANIMATION_DEATH = "death";

    // ========== 普攻轮询 ==========

    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 20;
    private static final int MELEE_DURATION_TICKS = 20;
    private static final int MELEE_HIT_TICK = 10;

    // ========== 冲锋 ==========

    private int chargeCooldownTicks;
    private int chargeTicksRemaining;
    @Nullable
    private Vec3 chargeDirection;
    private boolean chargeHasHit;  // 本次冲锋是否已命中目标
    private static final int CHARGE_COOLDOWN_TICKS = 300;   // 15秒冷却
    private static final int CHARGE_DURATION_TICKS = 60;     // 3秒持续冲刺
    private static final double CHARGE_SPEED = 0.375D;       // 自身行走速度(0.25) × 1.5 = 0.375

    // ========== 猛击 ==========

    private int slamCooldownTicks;
    private int slamTicksRemaining;
    private static final int SLAM_COOLDOWN_TICKS = 240; // 12秒
    private static final int SLAM_DURATION_TICKS = 20;   // 猛击动画持续时长

    public ZuZainDaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_ZAIN_DA.get(), world);
    }

    public ZuZainDaEntity(EntityType<ZuZainDaEntity> type, Level world) {
        super(type, world);
        xpReward = 30;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_zain_da");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIMATION_IDLE);
    }

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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ChargeGoal(this));       // 冲锋（高优先级，4~15格）
        this.goalSelector.addGoal(3, new SlamGoal(this));          // 猛击（中优先级，近身3格）
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity attackTarget) {
                return 9.0; // 3.0^2 - 与玩家攻击距离等同
            }
        });
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========== 冲锋能力 ==========

    private void performCharge() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        this.chargeCooldownTicks = CHARGE_COOLDOWN_TICKS;
        this.chargeTicksRemaining = CHARGE_DURATION_TICKS;
        this.chargeHasHit = false;

        // 锁定冲刺方向（水平归一化向量）
        this.chargeDirection = new Vec3(
                target.getX() - this.getX(),
                0,
                target.getZ() - this.getZ()
        ).normalize();

        // 锁定面朝方向
        float yaw = (float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        // 给予自身抗性提升II，冲锋时减伤
        this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                CHARGE_DURATION_TICKS,
                1,
                false, false, true
        ));

        // 播放冲刺动画
        this.setAnimation(ANIMATION_SPRINT);

        // 初始速度脉冲
        if (this.chargeDirection != null) {
            this.setDeltaMovement(
                    this.chargeDirection.x * CHARGE_SPEED,
                    this.getDeltaMovement().y,
                    this.chargeDirection.z * CHARGE_SPEED
            );
            this.hurtMarked = true;
        }
    }

    // ========== 猛击地面能力 ==========

    private void performSlam() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        this.slamCooldownTicks = SLAM_COOLDOWN_TICKS;
        this.slamTicksRemaining = SLAM_DURATION_TICKS;

        // 播放猛击动画
        this.setAnimation(ANIMATION_SLAM);

        // 0.5秒后触发震波（方块波浪飞起 + AOE伤害 + 缓慢 + 击退）
        DelayedTask.run(this.level(), 10, () -> {
            GroundShatterEffect.createShockWaveEffect(
                    this.level(),
                    this.position(),
                    3.0,       // 半径3格（直径6）
                    20,        // flyDuration: 方块停留时间
                    10,        // hoverTime:  方块悬浮时间
                    this       // 伤害来源
            );
        });
    }

    // ========== 冲锋命中附加击退和额外伤害 ==========

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // === 冲锋命中：即时伤害 + 50%额外伤害 + 强力击退 ===
        if (this.chargeTicksRemaining > 0) {
            boolean result = super.doHurtTarget(target);
            if (result && target instanceof LivingEntity) {
                float originalDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                target.hurt(this.damageSources().mobAttack(this), originalDamage * 0.5f);

                Vec3 knockback = new Vec3(
                        target.getX() - this.getX(), 0, target.getZ() - this.getZ()
                ).normalize().scale(1.5);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.4, knockback.z));

                // 冲撞不停止，继续直线前进
                this.hurtMarked = true;
            }
            return result;
        }

        // === 普攻：attack1/attack2 轮询（技能期间禁止普攻） ===
        if (!(target instanceof LivingEntity)
                || this.chargeTicksRemaining > 0
                || this.slamTicksRemaining > 0
                || this.meleeCooldownTicks > 0
                || this.meleeTicksRemaining > 0) {
            return false;
        }
        this.startMeleeAttack();
        return true;
    }

    // ========== tick 更新 ==========

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // === 冷却递减 ===
            if (this.chargeCooldownTicks > 0) this.chargeCooldownTicks--;
            if (this.slamCooldownTicks > 0) this.slamCooldownTicks--;
            if (this.meleeCooldownTicks > 0) this.meleeCooldownTicks--;

            // === 冲锋持续（直线前进，不可调头） ===
            if (this.chargeTicksRemaining > 0) {
                this.tickCharge();
                return;
            }

            // === 猛击持续（播放动画期间锁定位置） ===
            if (this.slamTicksRemaining > 0) {
                this.slamTicksRemaining--;
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                if (this.slamTicksRemaining <= 0) {
                    this.setAnimation(ANIMATION_IDLE);
                }
                return;
            }

            // === 受控近战序列 ===
            if (this.meleeTicksRemaining > 0) {
                this.performMeleeTick();
                return;
            }
        }
    }

    private void tickCharge() {
        // 每帧沿锁定方向持续冲刺
        if (this.chargeDirection != null) {
            this.setDeltaMovement(
                    this.chargeDirection.x * CHARGE_SPEED,
                    this.getDeltaMovement().y,
                    this.chargeDirection.z * CHARGE_SPEED
            );
            // 锁定朝向，不可调头
            float yaw = (float) (Mth.atan2(this.chargeDirection.z, this.chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
        this.getNavigation().stop();

        // === 碰撞检测：检测是否命中目标 ===
        if (!this.chargeHasHit) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()
                    && this.getBoundingBox().intersects(target.getBoundingBox())) {
                this.doHurtTarget(target);
                this.chargeHasHit = true;
            }
        }

        this.chargeTicksRemaining--;
        if (this.chargeTicksRemaining <= 0) {
            this.chargeTicksRemaining = 0;
            this.chargeDirection = null;
            this.setAnimation(ANIMATION_IDLE);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ========== 普攻轮询：attack1 / attack2 ==========

    private void startMeleeAttack() {
        this.meleeTicksRemaining = MELEE_DURATION_TICKS;
        this.meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        this.lastMeleeWasAttack1 = !this.lastMeleeWasAttack1;

        this.setAnimation(this.lastMeleeWasAttack1 ? ANIMATION_ATTACK1 : ANIMATION_ATTACK2);
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
                target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }

        this.meleeTicksRemaining--;
        if (this.meleeTicksRemaining <= 0) {
            this.meleeTicksRemaining = 0;
            this.setAnimation(ANIMATION_IDLE);
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

    // ========== 动画控制器 ==========

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "action_controller", 4, this::actionAnimation));
    }

    private PlayState movementAnimation(AnimationState<ZuZainDaEntity> state) {
        // 当有动作动画在播放时，移动控制器停止
        if (!ANIMATION_IDLE.equals(this.getAnimation())) {
            return PlayState.STOP;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(ANIMATION_DEATH));
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIMATION_WALK));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(ANIMATION_IDLE));
    }

    private PlayState actionAnimation(AnimationState<ZuZainDaEntity> state) {
        String animation = this.getAnimation();
        if (ANIMATION_IDLE.equals(animation)) {
            return PlayState.STOP;
        }
        AnimationController<ZuZainDaEntity> controller = state.getController();
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

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ChargeCooldown", this.chargeCooldownTicks);
        compound.putInt("ChargeTicks", this.chargeTicksRemaining);
        if (this.chargeDirection != null) {
            compound.putDouble("ChargeDirX", this.chargeDirection.x);
            compound.putDouble("ChargeDirZ", this.chargeDirection.z);
        }
        compound.putInt("SlamCooldown", this.slamCooldownTicks);
        compound.putInt("SlamTicks", this.slamTicksRemaining);
        compound.putInt("MeleeCooldown", this.meleeCooldownTicks);
        compound.putInt("MeleeTicks", this.meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", this.lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("ChargeCooldown")) {
            this.chargeCooldownTicks = compound.getInt("ChargeCooldown");
        }
        if (compound.contains("ChargeTicks")) {
            this.chargeTicksRemaining = compound.getInt("ChargeTicks");
        }
        if (compound.contains("ChargeDirX") && compound.contains("ChargeDirZ")) {
            this.chargeDirection = new Vec3(
                    compound.getDouble("ChargeDirX"), 0, compound.getDouble("ChargeDirZ")
            );
        }
        if (compound.contains("SlamCooldown")) {
            this.slamCooldownTicks = compound.getInt("SlamCooldown");
        }
        if (compound.contains("SlamTicks")) {
            this.slamTicksRemaining = compound.getInt("SlamTicks");
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

    // ========== 冲锋AI目标 ==========

    private static class ChargeGoal extends Goal {
        private final ZuZainDaEntity entity;

        public ChargeGoal(ZuZainDaEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (entity.chargeCooldownTicks > 0) return false;
            if (entity.chargeTicksRemaining > 0) return false;
            // 猛击期间不冲锋
            if (entity.slamTicksRemaining > 0) return false;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return entity.chargeTicksRemaining > 0; // 冲锋持续期间不释放控制权
        }

        @Override
        public void start() {
            entity.performCharge();
        }
    }

    // ========== 猛击AI目标 ==========

    private static class SlamGoal extends Goal {
        private final ZuZainDaEntity entity;

        public SlamGoal(ZuZainDaEntity entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = entity.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (entity.slamCooldownTicks > 0) return false;
            // 冲锋期间不触发猛击
            if (entity.chargeTicksRemaining > 0) return false;
            // 距离在3格以内时使用猛击
            double dist = entity.distanceTo(target);
            return dist <= 3.0;
        }

        @Override
        public boolean canContinueToUse() {
            return entity.slamTicksRemaining > 0; // 猛击持续期间不释放控制权
        }

        @Override
        public void start() {
            entity.performSlam();
        }
    }

    // ========== 属性：高生命高护甲 ==========

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 320)
                .add(Attributes.ATTACK_DAMAGE, 30)
                .add(Attributes.ARMOR, 18)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }
}