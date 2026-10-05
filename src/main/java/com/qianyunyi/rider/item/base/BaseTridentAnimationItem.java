package com.qianyunyi.rider.item.base;

import com.qianyunyi.rider.entity.projectile.DragonWandEntity;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SetItemAnimPacket;
import com.qianyunyi.rider.util.kuuga.MorphingPowerUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class BaseTridentAnimationItem extends TridentItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final String TAG_ANIM = "SwordAnim";
    public static final String DEFAULT_ANIM = "idle";
    private static final String TAG_DEFAULT_ANIM = "SwordDefaultAnim";

    public BaseTridentAnimationItem(Properties properties) {
        super(properties);
    }

    public BaseTridentAnimationItem(Tier tier, Properties properties) {
        super(properties.defaultDurability(tier.getUses()));
    }

    protected abstract DragonWandEntity createThrownEntity(Level level, LivingEntity shooter, ItemStack stack);

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.getDamageValue() >= itemstack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(itemstack);
        }
        boolean isMorph = MorphingPowerUtil.isMorphWeapon(itemstack);
        if (EnchantmentHelper.getRiptide(itemstack) > 0 && !player.isInWaterOrRain() && !isMorph) {
            return InteractionResultHolder.fail(itemstack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity, int timeLeft) {
        if (livingEntity instanceof Player player) {
            int i = this.getUseDuration(stack) - timeLeft;
            if (i >= 10) {
                int riptide = EnchantmentHelper.getRiptide(stack);
                boolean isMorph = MorphingPowerUtil.isMorphWeapon(stack);
                boolean isMainHand = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND;
                boolean isOffHand = livingEntity.getUsedItemHand() == InteractionHand.OFF_HAND;

                // 变形武器：右手丢出忠诚，左手释放激流
                boolean forceThrow = isMorph && isMainHand;
                boolean forceRiptide = isMorph && isOffHand;

                if ((riptide <= 0 || forceThrow) && !forceRiptide) {
                    if (!level.isClientSide) {
                        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(livingEntity.getUsedItemHand()));

                        DragonWandEntity thrownWand = createThrownEntity(level, player, stack);

                        float power = 2.5F;
                        if (player.getAbilities().instabuild) {
                            thrownWand.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        } else {
                            thrownWand.pickup = AbstractArrow.Pickup.ALLOWED;
                        }

                        thrownWand.shootFromRotation(
                                player,
                                player.getXRot(),
                                player.getYRot(),
                                0.0F,
                                power,
                                1.0F
                        );

                        level.addFreshEntity(thrownWand);
                        level.playSound(
                                null,
                                thrownWand,
                                SoundEvents.TRIDENT_THROW,
                                SoundSource.PLAYERS,
                                1.0F,
                                1.0F
                        );

                        if (!player.getAbilities().instabuild) {
                            player.getInventory().removeItem(stack);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                } else if ((riptide > 0 && player.isInWaterOrRain()) || forceRiptide) {
                    if (!level.isClientSide) {
                        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(livingEntity.getUsedItemHand()));
                    }
                    player.awardStat(Stats.ITEM_USED.get(this));
                    int effectiveRiptide = forceRiptide ? Math.max(riptide, 1) : riptide;
                    handleRiptide(player, effectiveRiptide, level);
                }
            }
        }
    }

    private void handleRiptide(Player player, int riptide, Level level) {
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float x = -Mth.sin(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        float y = -Mth.sin(xRot * ((float) Math.PI / 180F));
        float z = Mth.cos(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        float length = Mth.sqrt(x * x + y * y + z * z);
        float power = 3.0F * ((1.0F + (float) riptide) / 4.0F);
        x *= power / length;
        y *= power / length;
        z *= power / length;
        player.push((double) x, (double) y, (double) z);
        player.startAutoSpinAttack(20);

        if (player.onGround()) {
            player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
        }

        net.minecraft.sounds.SoundEvent soundEvent;
        if (riptide >= 3) {
            soundEvent = SoundEvents.TRIDENT_RIPTIDE_3;
        } else if (riptide == 2) {
            soundEvent = SoundEvents.TRIDENT_RIPTIDE_2;
        } else {
            soundEvent = SoundEvents.TRIDENT_RIPTIDE_1;
        }

        level.playSound(
                null,
                player,
                soundEvent,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    public static String getAnim(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_ANIM)) {
            tag.putString(TAG_ANIM, DEFAULT_ANIM);
        }
        return tag.getString(TAG_ANIM);
    }

    public static void setAnim(ItemStack stack, String anim) {
        stack.getOrCreateTag().putString(TAG_ANIM, anim);
    }

    public static String getDefaultAnim(ItemStack stack) {
        String def = stack.getOrCreateTag().getString(TAG_DEFAULT_ANIM);
        return def.isEmpty() ? DEFAULT_ANIM : def;
    }

    public static void setDefaultAnim(ItemStack stack, String anim) {
        stack.getOrCreateTag().putString(TAG_DEFAULT_ANIM, anim);
    }

    private PlayState predicate(AnimationState<? extends BaseTridentAnimationItem> event) {
        ItemStack stack = event.getData(DataTickets.ITEMSTACK);
        String anim = getAnim(stack);
        String defaultAnim = getDefaultAnim(stack);

        if (anim.isEmpty() || defaultAnim.equals(anim)) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(defaultAnim));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenPlay(anim));

            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                setAnim(stack, defaultAnim);

                Player player = (Player) event.getData(DataTickets.ENTITY);
                if (player != null) {
                    InteractionHand hand = getHandForItem(player, stack);
                    if (hand != null) {
                        NetworkHandler.sendToServer(new SetItemAnimPacket(hand, defaultAnim));
                    }
                }
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private InteractionHand getHandForItem(Player player, ItemStack stack) {
        if (player.getMainHandItem() == stack) {
            return InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem() == stack) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}