package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.entity.projectile.WebProjectile;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * 蜘蛛古朗基（Zu-Gumun-Ba）
 * 能力：
 * - 爬墙：接触墙壁时可攀爬，播放 climb_the_wall 动画
 * - 吐丝：发射蛛丝弹射物，命中后给予缓慢III（35秒，无粒子，HUD显示）
 * - 结网：冷却15秒，在脚下放置3x3蜘蛛网，在蜘蛛网中自由行动
 */
public class ZuGumunBaEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuGumunBaEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(ZuGumunBaEntity.class, EntityDataSerializers.BYTE);

    // 动画名称
    private static final String ANIMATION_IDLE = "idle";
    private static final String ANIMATION_WALK = "walk";
    private static final String ANIMATION_ATTACK1 = "attack1";
    private static final String ANIMATION_ATTACK2 = "attack2";
    private static final String ANIMATION_CLIMB = "climb_the_wall";
    private static final String ANIMATION_DEATH = "death";

    // ========== 普攻轮询 ==========

    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 20;
    private static final int MELEE_DURATION_TICKS = 20;
    private static final int MELEE_HIT_TICK = 10;

    // 吐丝冷却
    private int webShootCooldown = 0;
    private static final int WEB_SHOOT_COOLDOWN_TICKS = 200; // 10秒

    // 结网冷却
    protected int webNetCooldown = 0;
    private static final int WEB_NET_COOLDOWN_TICKS = 300; // 15秒 = 300 tick

    public ZuGumunBaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_GUMUN_BA.get(), world);
    }

    public ZuGumunBaEntity(EntityType<ZuGumunBaEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_gumun_ba");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIMATION_IDLE);
        this.entityData.define(DATA_FLAGS_ID, (byte) 0);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public String getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    public void setAnimation(String anim) {
        this.entityData.set(ANIMATION, anim);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SpiderAttackGoal(this));           // 近战攻击（含爬墙追击）
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));    // 扑向目标（触发撞墙→攀爬）
        this.goalSelector.addGoal(4, new WebShootGoal(this));              // 吐司
        this.goalSelector.addGoal(5, new WebNetGoal(this));                // 结网
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========== 墙壁攀爬导航器（原版蜘蛛核心机制） ==========
    // WallClimberNavigation 在无路径时也会保存目标位置，
    // 通过 MoveControl 直接向目标移动，撞墙触发 horizontalCollision → 攀爬

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    // ========== 爬墙能力 ==========

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (climbing) {
            b = (byte) (b | 1);
        } else {
            b = (byte) (b & -2);
        }
        this.entityData.set(DATA_FLAGS_ID, b);
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (state.is(Blocks.COBWEB)) {
            return; // 蜘蛛网中自由行动（无视减速）
        }
        super.makeStuckInBlock(state, motionMultiplier);
    }

    // ========== 吐丝能力 ==========

    private void shootWebAtTarget(LivingEntity target) {
        if (this.level().isClientSide) return;

        // 创建蛛丝弹射物
        WebProjectile webProjectile = new WebProjectile(this.level(), this);
        webProjectile.setPos(
                this.getX(),
                this.getEyeY() - 0.3,
                this.getZ()
        );

        // 计算朝向目标的向量
        Vec3 toTarget = new Vec3(
                target.getX() - this.getX(),
                target.getEyeY() - this.getEyeY(),
                target.getZ() - this.getZ()
        ).normalize().scale(1.5);

        webProjectile.setDeltaMovement(toTarget);

        this.level().addFreshEntity(webProjectile);
        this.webShootCooldown = WEB_SHOOT_COOLDOWN_TICKS;
    }

    // ========== 结网能力 ==========

    private void placeWebNet() {
        if (this.level().isClientSide) return;

        BlockPos centerPos = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos placePos = centerPos.offset(dx, 0, dz);
                // 只在空气或可替换方块上放置蜘蛛网
                if (this.level().getBlockState(placePos).canBeReplaced()) {
                    this.level().setBlock(placePos, Blocks.COBWEB.defaultBlockState(), 3);
                }
                // 在头顶也放一层，增加困敌效果
                BlockPos abovePos = placePos.above();
                if (this.level().getBlockState(abovePos).canBeReplaced()) {
                    this.level().setBlock(abovePos, Blocks.COBWEB.defaultBlockState(), 3);
                }
            }
        }
    }

    // ========== 普攻轮询：attack1 / attack2 ==========

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        // 技能/冷却期间禁止普攻
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

        this.setAnimation(this.lastMeleeWasAttack1 ? ANIMATION_ATTACK1 : ANIMATION_ATTACK2);
        this.getNavigation().stop();

        LivingEntity target = this.getTarget();
        if (target != null) {
            this.faceTarget(target);
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

    // ========== tick 更新 ==========

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 原版蜘蛛方式：直接根据水平碰撞设置攀爬状态
            // WallClimberNavigation 确保稳定撞墙，不会闪烁
            this.setClimbing(this.horizontalCollision);
        }

        // 攀爬时面向目标（朝向墙壁）
        if (this.isClimbing()) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                float yaw = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
            }
        }

        // 冷却递减
        if (webShootCooldown > 0) webShootCooldown--;
        if (webNetCooldown > 0) webNetCooldown--;

        if (!this.level().isClientSide) {
            // 近战冷却递减
            if (this.meleeCooldownTicks > 0) this.meleeCooldownTicks--;

            // 受控近战序列
            if (this.meleeTicksRemaining > 0) {
                this.performMeleeTick();
            }
        }
    }

    // ========== 移动 / 爬墙物理 ==========

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isClimbing()) {
            this.fallDistance = 0.0F;
            // WallClimberNavigation 负责向目标移动（撞墙）
            // MoveControl 负责转向 + 设置 zza（前进速度）
            // 但我们还需要向上速度来爬墙：当目标在上方时主动添加向上推力
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && !this.level().isClientSide) {
                double dy = target.getEyeY() - this.getEyeY();
                if (dy > 0.5) {
                    Vec3 motion = this.getDeltaMovement();
                    // 稳定向上爬升，速度根据高度差动态调整
                    double climbSpeed = Math.min(0.06 + dy * 0.003, 0.18);
                    this.setDeltaMovement(motion.x, Math.max(motion.y, climbSpeed), motion.z);
                }
            }
        }
        super.travel(travelVector);
    }

    // ========== 动画 ==========

    private PlayState movementAnimation(AnimationState<ZuGumunBaEntity> state) {
        // 当有动作动画在播放时，移动控制器停止
        if (!ANIMATION_IDLE.equals(this.getAnimation())) {
            return PlayState.STOP;
        }
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay(ANIMATION_DEATH));
        }
        if (this.isClimbing()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIMATION_CLIMB));
        }
        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(ANIMATION_WALK));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(ANIMATION_IDLE));
    }

    private PlayState actionAnimation(AnimationState<ZuGumunBaEntity> state) {
        String animation = this.getAnimation();
        if (ANIMATION_IDLE.equals(animation)) {
            return PlayState.STOP;
        }
        AnimationController<ZuGumunBaEntity> controller = state.getController();
        if (controller.getAnimationState() == AnimationController.State.STOPPED) {
            controller.forceAnimationReset();
            state.setAndContinue(RawAnimation.begin().thenPlay(animation));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "action_controller", 4, this::actionAnimation));
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    // ========== 数据持久化 ==========

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("WebNetCooldown", webNetCooldown);
        compound.putInt("MeleeCooldown", this.meleeCooldownTicks);
        compound.putInt("MeleeTicks", this.meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", this.lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("WebShootCooldown")) {
            webShootCooldown = compound.getInt("WebShootCooldown");
        }
        if (compound.contains("WebNetCooldown")) {
            webNetCooldown = compound.getInt("WebNetCooldown");
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



    // ========== 自定义近战攻击目标（含爬墙追击） ==========

    /**
     * 原版蜘蛛的自定义近战目标：
     * - 在寻路失败时仍直接向目标位置移动（撞墙触发攀爬）
     * - 靠墙攀爬时目标在上方则主动向上移动
     */
    static class SpiderAttackGoal extends MeleeAttackGoal {
        private final ZuGumunBaEntity spider;

        public SpiderAttackGoal(ZuGumunBaEntity spider) {
            super(spider, 1.0, true);
            this.spider = spider;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !spider.isVehicle();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && !spider.isVehicle();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy, double dist) {
            double reachSqr = this.getAttackReachSqr(enemy);
            if (dist <= reachSqr && this.getTicksUntilNextAttack() <= 0) {
                this.resetAttackCooldown();
                this.mob.doHurtTarget(enemy);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = spider.getTarget();
            if (target == null) return;

            // WallClimberNavigation 自动处理：
            // - 有路径 → 正常寻路
            // - 无路径 → 保存目标位置，通过 MoveControl 直线靠近 → 撞墙触发爬墙
            spider.getNavigation().moveTo(target, 1.0);

            // 攀爬/非攀爬都调用 super.tick() 检查攻击距离
            super.tick();
        }

        @Override
        protected double getAttackReachSqr(@NotNull LivingEntity attackTarget) {
            return 9.0; // 玩家同等攻击距离 (3.0^2)
        }
    }

    // ========== 吐丝AI目标 ==========

    static class WebShootGoal extends Goal {
        private final ZuGumunBaEntity spider;

        public WebShootGoal(ZuGumunBaEntity spider) {
            this.spider = spider;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (spider.webShootCooldown > 0) return false;
            LivingEntity target = spider.getTarget();
            if (target == null || !target.isAlive()) return false;
            double dist = spider.distanceToSqr(target);
            // 2-8格范围内使用吐丝
            return dist >= 4.0 && dist <= 64.0;
        }

        @Override
        public void start() {
            LivingEntity target = spider.getTarget();
            if (target != null) {
                spider.shootWebAtTarget(target);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    // ========== 结网AI目标 ==========

    static class WebNetGoal extends Goal {
        private final ZuGumunBaEntity spider;

        public WebNetGoal(ZuGumunBaEntity spider) {
            this.spider = spider;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (spider.webNetCooldown > 0) return false;
            LivingEntity target = spider.getTarget();
            // 必须在有目标且靠近目标（3格内）时才结网
            return target != null && target.isAlive() && spider.distanceToSqr(target) <= 9.0;
        }

        @Override
        public void start() {
            spider.placeWebNet();
            spider.webNetCooldown = WEB_NET_COOLDOWN_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 240)
                .add(Attributes.ATTACK_DAMAGE, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}