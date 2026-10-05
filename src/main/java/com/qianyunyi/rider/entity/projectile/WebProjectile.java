package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * 蜘蛛古朗基（Zu-Gumun-Ba）的吐丝弹射物
 * 命中目标后给予缓慢效果35秒，不显示粒子，在HUD上显示图标
 */
public class WebProjectile extends AbstractArrow {

    // 缓慢效果持续时间：35秒 = 700 tick
    private static final int SLOWNESS_DURATION_TICKS = 700;
    // 缓慢效果等级（0 = 缓慢I）
    private static final int SLOWNESS_AMPLIFIER = 1;

    public WebProjectile(Level level, LivingEntity shooter) {
        super(EntityRegistrationHandler.WEB_PROJECTILE, shooter, level);
        this.setSilent(true);
        this.setCritArrow(false);
        this.setBaseDamage(0); // 吐丝不造成伤害
    }

    public WebProjectile(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        if (this.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // 给予目标缓慢效果
        // 参数：效果类型，持续时间（tick），等级，是否显示粒子，是否显示HUD图标
        livingTarget.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                SLOWNESS_DURATION_TICKS,
                SLOWNESS_AMPLIFIER,
                false,  // ambient - 不削减效果可见度
                false,  // showParticles - 不显示粒子
                true    // showIcon - 在HUD上显示
        ));

        // 轻微的击退效果（模拟被蛛网粘住）
        Vec3 knockback = this.getDeltaMovement().normalize().scale(0.5);
        target.push(knockback.x, 0.2, knockback.z);

        // 在目标脚下生成蜘蛛网
        placeCobwebAt(target.blockPosition());

        // 移除弹射物
        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            // 在命中位置生成蜘蛛网
            placeCobwebAt(result.getBlockPos());
            this.discard();
        }
    }

    /**
     * 在指定位置及周围放置蜘蛛网（3x3区域）
     */
    private void placeCobwebAt(BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos placePos = center.offset(dx, 0, dz);
                BlockState state = this.level().getBlockState(placePos);
                if (state.canBeReplaced()) {
                    this.level().setBlock(placePos, Blocks.COBWEB.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return ItemStack.EMPTY; // 不可拾取
    }

    @Override
    public void tick() {
        super.tick();
        // 如果飞行超过100 tick（5秒），自动移除
        if (this.tickCount > 100) {
            this.discard();
        }
    }
}