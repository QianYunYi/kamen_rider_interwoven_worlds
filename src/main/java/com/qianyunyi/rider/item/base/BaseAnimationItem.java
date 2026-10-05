package com.qianyunyi.rider.item.base;

import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SetItemAnimPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.constant.DataTickets;

public abstract class BaseAnimationItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final String TAG_ANIM = "ItemAnim";
    private static final String TAG_LAST_ANIM = "ItemLastAnim";
    public String animationprocedure;
    public static final String DEFAULT_ANIM = "idle";

    public BaseAnimationItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private PlayState predicate(AnimationState<? extends BaseAnimationItem> event) {
        ItemStack stack = event.getData(DataTickets.ITEMSTACK);
        String anim = getAnim(stack);
        String lastAnim = getLastAnim(stack);

        if (!anim.equals(lastAnim)) {
            setLastAnim(stack, anim);
            if (!anim.isEmpty() && !DEFAULT_ANIM.equals(anim)) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay(anim));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(DEFAULT_ANIM));
            }
        } else if (!anim.isEmpty() && !DEFAULT_ANIM.equals(anim)) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                setAnim(stack, DEFAULT_ANIM);
                setLastAnim(stack, DEFAULT_ANIM);

                Player player = (Player) event.getData(DataTickets.ENTITY);
                if (player != null) {
                    InteractionHand hand = getHandForItem(player, stack);
                    if (hand != null) {
                        NetworkHandler.sendToServer(new SetItemAnimPacket(hand, DEFAULT_ANIM));
                    }
                }
            }
        } else if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(DEFAULT_ANIM));
        }

        return PlayState.CONTINUE;
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