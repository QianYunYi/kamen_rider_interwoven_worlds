package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 葛·伽多鲁·巴（射击体）发射的风弹。
 * <p>
 * 视觉上与天马弩子弹（blast_pegasus）一致（渲染层复用同一 geo / texture）。
 * 天马弩子弹原本只有"玩家射手"的命中逻辑，此处做了扩展，支持任意 {@link LivingEntity} 射手
 * （例如葛·伽多鲁·巴），并以其攻击力属性计算伤害。
 */
public class GoGadoruBaWindProjectile extends AbstractArrow implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 伤害倍率（相对射手攻击力） */
    private static final float DAMAGE_MULTIPLIER = 0.5F;
    /** 击退强度 */
    private static final double KNOCKBACK = 1.6;

    public GoGadoruBaWindProjectile(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public GoGadoruBaWindProjectile(Level level, LivingEntity shooter, float damage) {
        super(EntityRegistrationHandler.GO_GADORU_BA_WIND_PROJECTILE, shooter, level);
        // 基础伤害由命中逻辑按射手攻击力计算，这里置零避免与下方手动伤害叠加
        this.setBaseDamage(0.0);
        this.setSilent(true);
        this.setCritArrow(true);
        this.setNoGravity(true);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    /**
     * 持续让子弹模型朝向其飞行方向（而非世界固定方向）。
     * 该投影物使用 GeoEntityRenderer，渲染朝向依赖 getYRot/getXRot，
     * 因此每刻按当前速度矢量重设朝向。
     */
    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        double lenSq = v.x * v.x + v.y * v.y + v.z * v.z;
        if (lenSq > 1.0E-4) {
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
            // 与射出者 faceTarget 使用同一偏航约定：atan2(z,x) - 90°
            float yaw = (float) (Math.atan2(v.z, v.x) * (180.0D / Math.PI)) - 90.0F;
            float pitch = (float) (-Math.atan2(v.y, Math.max(horizontal, 1.0E-4)) * (180.0D / Math.PI));
            this.setYRot(Mth.wrapDegrees(yaw));
            this.setXRot(Mth.wrapDegrees(pitch));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        // 不再依赖 super 自带伤害，关闭目标无敌帧后自行结算
        Entity targetEntity = result.getEntity();
        if (this.level().isClientSide || !(targetEntity instanceof LivingEntity target)) {
            return;
        }

        // 射手可能为玩家，也可能为古朗基（如葛·伽多鲁·巴）
        if (!(this.getOwner() instanceof LivingEntity owner)) {
            this.discard();
            return;
        }

        float baseAttack = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = baseAttack * DAMAGE_MULTIPLIER;

        target.invulnerableTime = 0;
        target.hurt(this.level().damageSources().mobProjectile(this, owner), damage);

        // 击退
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 0.0) {
            Vec3 knock = motion.normalize().scale(KNOCKBACK);
            target.push(knock.x, 0.5, knock.z);
        } else {
            target.push(0, 0.5, 0);
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    private <E extends GeoAnimatable> PlayState predicate(AnimationState<E> event) {
        if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}