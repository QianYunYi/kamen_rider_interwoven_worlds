package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
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
 * 猎豹古朗基（滋·梅比欧·达）
 * 能力：冲刺（速度II 5秒）+ 扑击（1.5倍伤害）+ 连击（fast_boxing动画）
 */
public class ZuMebioDaEntity extends Gurongi {

    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(ZuMebioDaEntity.class, EntityDataSerializers.STRING);
    public String animationprocedure = "empty";
    private boolean swinging;

    // 冲刺冷却（tick）
    private int dashCooldown = 0;
    private static final int DASH_COOLDOWN_TICKS = 240; // 12秒 = 240 tick
    private boolean isDashing = false;

    // 扑击标记（1.5倍伤害）
    private boolean isPouncing = false;

    public ZuMebioDaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.ZU_MEBIO_DA.get(), world);
    }

    public ZuMebioDaEntity(EntityType<ZuMebioDaEntity> type, Level world) {
        super(type, world);
        xpReward = 10;
        setNoAi(false);
        this.group = GurongiGroup.ZU;
        this.setTexture("zu/zu_mebio_da");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, "undefined");
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6, false) {
            @Override
            protected void checkAndPerformAttack(@NotNull LivingEntity enemy, double dist) {
                super.checkAndPerformAttack(enemy, dist);
                // 连击动画：每次攻击触发fast_boxing
                swinging = true;
            }

            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity attackTarget) {
                return 9.0; // 玩家同等攻击距离 (3.0^2)
            }
        });
        this.goalSelector.addGoal(3, new DashGoal(this));          // 冲刺
        this.goalSelector.addGoal(4, new PounceGoal(this));        // 扑击
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========== 扑击伤害倍率 ==========

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result && isPouncing && target instanceof LivingEntity) {
            // 扑击命中：额外造成0.5倍伤害（总共1.5倍）
            float originalDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float bonusDamage = originalDamage * 0.5f;
            target.hurt(this.damageSources().mobAttack(this), bonusDamage);
            isPouncing = false;
        }
        return result;
    }

    // ========== 冲刺逻辑 ==========

    private void performDash() {
        LivingEntity target = this.getTarget();
        if (target == null) return;

        isDashing = true;
        this.dashCooldown = DASH_COOLDOWN_TICKS;

        // 给予自身速度II（5秒），不显示粒子，在HUD上显示图标
        this.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                100,    // 5秒 = 100 tick
                1,      // 速度II (等级1)
                false,  // ambient
                false,  // showParticles - 不显示粒子
                true    // showIcon - 在HUD上显示
        ));

        // 朝目标方向冲刺加速
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 toTarget = new Vec3(
                    target.getX() - this.getX(),
                    0,
                    target.getZ() - this.getZ()
            ).normalize().scale(2.5);

            this.setDeltaMovement(this.getDeltaMovement().add(toTarget.x, 0.2, toTarget.z));
            this.hurtMarked = true;
        }
    }

    // ========== 扑击逻辑 ==========

    private void performPounce(LivingEntity target) {
        isPouncing = true;

        // 跳向目标
        Vec3 toTarget = new Vec3(
                target.getX() - this.getX(),
                target.getEyeY() - this.getEyeY(),
                target.getZ() - this.getZ()
        );

        double distance = toTarget.length();
        if (distance > 0.01) {
            Vec3 pounceVec = toTarget.normalize().scale(Math.min(distance * 0.6, 3.0));
            this.setDeltaMovement(pounceVec.x, 0.5 + Math.min(distance * 0.1, 0.5), pounceVec.z);
            this.hurtMarked = true;
        }
    }

    // ========== tick 更新 ==========

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // 冲刺冷却递减
            if (dashCooldown > 0) {
                dashCooldown--;
            }
        }
    }

    // ========== 动画 ==========

    @Override
    public void swing(@NotNull InteractionHand hand) {
        super.swing(hand);
        this.swinging = true;
        this.animationprocedure = "fast_boxing";
    }

    private PlayState movementPredicate(AnimationState event) {
        if (this.animationprocedure.equals("empty")) {
            if (this.isDeadOrDying()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("death"));
            }
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.STOP;
    }

    private PlayState attackPredicate(AnimationState event) {
        if (this.swinging) {
            event.getController().forceAnimationReset();
            event.getController().setAnimation(RawAnimation.begin().thenPlay("fast_boxing"));
            this.swinging = false;
            this.animationprocedure = "empty";
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
        data.add(new AnimationController<>(this, "attack", 2, this::attackPredicate));
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
        compound.putInt("DashCooldown", dashCooldown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("DashCooldown")) {
            dashCooldown = compound.getInt("DashCooldown");
        }
    }

    public static void init() {
    }



    // ========== 冲刺AI目标 ==========

    static class DashGoal extends Goal {
        private final ZuMebioDaEntity cheetah;

        public DashGoal(ZuMebioDaEntity cheetah) {
            this.cheetah = cheetah;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cheetah.dashCooldown > 0) return false;
            LivingEntity target = cheetah.getTarget();
            if (target == null || !target.isAlive()) return false;
            double dist = cheetah.distanceToSqr(target);
            // 在5-15格范围内使用冲刺
            return dist >= 25.0 && dist <= 225.0;
        }

        @Override
        public void start() {
            cheetah.performDash();
        }

        @Override
        public boolean canContinueToUse() {
            return false; // 一次性触发
        }
    }

    // ========== 扑击AI目标 ==========

    static class PounceGoal extends Goal {
        private final ZuMebioDaEntity cheetah;
        private int cooldown = 0;

        public PounceGoal(ZuMebioDaEntity cheetah) {
            this.cheetah = cheetah;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = cheetah.getTarget();
            if (target == null || !target.isAlive()) return false;
            double dist = cheetah.distanceToSqr(target);
            // 在2-5格范围内使用扑击
            return dist >= 4.0 && dist <= 25.0;
        }

        @Override
        public void start() {
            LivingEntity target = cheetah.getTarget();
            if (target != null) {
                cheetah.performPounce(target);
                cooldown = 40; // 2秒冷却
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Gurongi.createGurongiAttributes()
                .add(Attributes.MAX_HEALTH, 260)
                .add(Attributes.ATTACK_DAMAGE, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 32);
    }
}