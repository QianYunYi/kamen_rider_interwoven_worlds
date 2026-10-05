package com.qianyunyi.rider.entity.kaijin.gurongi;

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
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * 贝·吉敏·巴（Be-Jimin-Ba）
 * <p>
 * 贝集团的基础古朗基，类似于战斗员的存在，没有技能。
 * <ul>
 *   <li>近战攻击轮询 attack1 / attack2</li>
 *   <li>无特殊技能，仅近战</li>
 *   <li>开启封印后可在野外自然刷新</li>
 *   <li>绑定贝集团生成开关，允许重复生成（像僵尸一样，一次生成一只）</li>
 * </ul>
 */
public class BeJiminBaEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(BeJiminBaEntity.class, EntityDataSerializers.STRING);

    // ========== 动画名称 ==========
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_WALK = "walk";
    private static final String ANIM_ATTACK1 = "attack1";
    private static final String ANIM_ATTACK2 = "attack2";
    private static final String ANIM_DEATH = "death";

    // ========== 普攻轮询 ==========
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 8;

    public BeJiminBaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.BE_JIMIN_BA.get(), world);
    }

    public BeJiminBaEntity(EntityType<BeJiminBaEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.BE;
        this.setTexture("be/be_jimin_ba");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_IDLE);
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
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
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
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========== 普攻轮询：attack1 / attack2 ==========

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

    // ========== tick 更新 ==========

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.meleeCooldownTicks > 0) this.meleeCooldownTicks--;

            if (this.meleeTicksRemaining > 0) {
                this.performMeleeTick();
            } else if (this.onGround() && ANIM_ATTACK1.equals(this.getAnimation())) {
                this.setAnimation(ANIM_IDLE);
            }
        }
    }

    // ========== 动画 ==========

    private PlayState movementAnimation(AnimationState<BeJiminBaEntity> state) {
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

    private PlayState actionAnimation(AnimationState<BeJiminBaEntity> state) {
        String animation = this.getAnimation();
        if (ANIM_IDLE.equals(animation)) {
            return PlayState.STOP;
        }
        AnimationController<BeJiminBaEntity> controller = state.getController();
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
        compound.putInt("MeleeCooldown", this.meleeCooldownTicks);
        compound.putInt("MeleeTicks", this.meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", this.lastMeleeWasAttack1);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
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

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.23);
    }

    /**
     * 自然生成注册。
     * <p>
     * 绑定贝集团生成开关：只有封印已开启（石棺已被打开、游戏已启动）时，
     * 贝·吉敏·巴才会像僵尸一样在野外自然刷新，且允许重复生成（一次生成一只）。
     */
    public static void init() {
        SpawnPlacements.register(KamenRiderInterwovenWorldsModEntities.BE_JIMIN_BA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, world, reason, pos, random) -> {
                    if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
                    if (!isBeiGroupSwitchOn(world)) return false;
                    return Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random);
                });
    }

    /**
     * 检查贝集团生成开关是否已开启。
     * <p>
     * 贝集团是最低级的集团，封印一旦开启（石棺被打破、基基鲁游戏启动），
     * 贝集团的古朗基即可在野外刷新。
     *
     * @return true = 封印已开启，允许贝集团生成
     */
    private static boolean isBeiGroupSwitchOn(ServerLevelAccessor world) {
        if (world == null || world.getLevel() == null) return false;
        Level level = world.getLevel();
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            return AddonWorldData.get(sl).getGurongiSpawn() > 0;
        }
        return false;
    }
}