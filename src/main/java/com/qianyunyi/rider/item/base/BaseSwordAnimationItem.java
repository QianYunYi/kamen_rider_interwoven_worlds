package com.qianyunyi.rider.item.base;

import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SetItemAnimPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.constant.DataTickets;

public abstract class BaseSwordAnimationItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public String animationprocedure;

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    public static final String TAG_ANIM = "SwordAnim";
    private static final String TAG_LAST_ANIM = "SwordLastAnim";
    private static final String TAG_DEFAULT_ANIM = "SwordDefaultAnim";
    public static final String DEFAULT_ANIM = "idle";

    public BaseSwordAnimationItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    public static String getAnim(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_ANIM);
    }

    public static void setAnim(ItemStack stack, String anim) {
        stack.getOrCreateTag().putString(TAG_ANIM, anim);
    }

    private static String getLastAnim(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_LAST_ANIM);
    }

    private static void setLastAnim(ItemStack stack, String anim) {
        stack.getOrCreateTag().putString(TAG_LAST_ANIM, anim);
    }

    /** 获取该物品栈的默认动画，未设置时使用全局默认值 */
    public static String getDefaultAnim(ItemStack stack) {
        String def = stack.getOrCreateTag().getString(TAG_DEFAULT_ANIM);
        return def.isEmpty() ? DEFAULT_ANIM : def;
    }

    /** 设置该物品栈的默认动画 */
    public static void setDefaultAnim(ItemStack stack, String anim) {
        stack.getOrCreateTag().putString(TAG_DEFAULT_ANIM, anim);
    }

    private PlayState predicate(AnimationState<? extends BaseSwordAnimationItem> event) {
        ItemStack stack = event.getData(DataTickets.ITEMSTACK);
        String anim = getAnim(stack);
        String lastAnim = getLastAnim(stack);
        String defaultAnim = getDefaultAnim(stack);

        if (!anim.equals(lastAnim)) {
            setLastAnim(stack, anim);
            if (!anim.isEmpty() && !defaultAnim.equals(anim)) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay(anim));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(defaultAnim));
            }
        } else if (!anim.isEmpty() && !defaultAnim.equals(anim)) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                setAnim(stack, defaultAnim);
                setLastAnim(stack, defaultAnim);

                Player player = (Player) event.getData(DataTickets.ENTITY);
                if (player != null) {
                    InteractionHand hand = getHandForItem(player, stack);
                    if (hand != null) {
                        NetworkHandler.sendToServer(new SetItemAnimPacket(hand, defaultAnim));
                    }
                }
            }
        } else if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(defaultAnim));
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