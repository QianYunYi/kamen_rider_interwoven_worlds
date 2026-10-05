package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class PegasusBowgunProjectile extends AbstractArrow implements GeoEntity {

    // 镇压标记持续时间（秒）
    private static final int SUPPRESS_DURATION = 1;
    // 标记为爆炸实体的标记
    public static final String IS_EXPLOSIVE_KEY = "is_explosive";
    public static final String EXPLOSION_PLAYER_KEY = "explosion_player";
    public static final String RISING_PEGASUS_FORM = "rising_pegasus_form";
    public static final String PEGASUS_FORM = "pegasus_form";

    // 存储发射者的UUID
    @Nullable
    private UUID shooterUUID;

    // 动画缓存
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PegasusBowgunProjectile(Level level, LivingEntity shooter, float damage) {
        super(EntityRegistrationHandler.PEGASUS_BOWGUN_PROJECTILE, shooter, level);
        this.setBaseDamage(damage);
        this.setSilent(true);
        this.setCritArrow(true);

        // 保存发射者的UUID
        this.shooterUUID = shooter.getUUID();
    }

    public PegasusBowgunProjectile(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    /**
     * 持续让子弹模型朝向其飞行方向（而非世界固定方向）。
     * 该投影物使用 GeoEntityRenderer，渲染朝向依赖 getYRot/getXRot，
     * 因此每刻按当前速度矢量重设朝向（与葛·伽多鲁·巴的风弹同一套约定）。
     */
    @Override
    public void tick() {
        super.tick();
        Vec3 v = this.getDeltaMovement();
        double lenSq = v.x * v.x + v.y * v.y + v.z * v.z;
        if (lenSq > 1.0E-4) {
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
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
        super.onHitEntity(result);
        Entity target = result.getEntity();

        if (this.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // 给予目标镇压标记
        livingTarget.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.SUPPRESS_ENERGY.get(),
                SUPPRESS_DURATION * 20,  // 转换为刻
                0,
                false,
                true
        ));

        // 标记为爆炸性实体
        markAsExplosive(livingTarget);

        //通过uuid获取玩家
        Player shooter = null;
        if (shooterUUID != null) {
            shooter = this.level().getPlayerByUUID(shooterUUID);
        }
        if (shooter == null) {
            return;
        }

        RiderPlayerData data = RiderPlayerVariablesProvider.get(shooter);
        String activeFormName = data != null ? data.getActiveFormName() : null;
        if (activeFormName == null) return;

        //造成玩家攻击力*5的伤害
        float damage = (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE) * 5.0f;

        if (activeFormName.equals(RISING_PEGASUS_FORM) || activeFormName.equals(PEGASUS_FORM)) {
            damage = damage * 5;
        }
        KamenRiderInterwovenWorldsMod.LOGGER.info("{}当前伤害: {}", activeFormName, damage);

        livingTarget.hurt(shooter.level().damageSources().playerAttack(shooter), damage);

        // 击退效果
        if (this.getDeltaMovement().lengthSqr() > 0.0) {
            Vec3 knockback = this.getDeltaMovement().normalize().scale(2.0);
            target.push(knockback.x, 0.5, knockback.z);
        } else {
            target.push(0, 0.5, 0);
        }

        // 移除弹射物
        this.discard();
    }

    /**
     * 标记目标为爆炸性实体
     * @param target 目标实体
     */
    private void markAsExplosive(LivingEntity target) {
        target.getPersistentData().putBoolean(IS_EXPLOSIVE_KEY, true);

        // 如果发射者是玩家，标记其UUID
        if (shooterUUID != null) {
            target.getPersistentData().putString(EXPLOSION_PLAYER_KEY, shooterUUID.toString());

            // 可选：添加视觉或音效提示
            if (target instanceof Player) {
                // 对玩家目标给予视觉提示
                target.playSound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.0f);
            }
        }
    }

    private <E extends GeoAnimatable> PlayState predicate(AnimationState<E> event) {
        if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return ItemStack.EMPTY; // 不可拾取
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        // 击中方块时移除弹射物
        if (!this.level().isClientSide) {
            this.discard();
        }
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