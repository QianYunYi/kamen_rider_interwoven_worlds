package com.qianyunyi.rider.item.base;

import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SetArmorAnimPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class BaseAnimatedArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String TAG_ANIM = "ArmorAnim";
    private static final String TAG_LAST_ANIM = "ArmorLastAnim";
    public String animationprocedure;
    public static final String DEFAULT_ANIM = "idle";

    public BaseAnimatedArmorItem(ArmorMaterial materialIn, Type type, Properties builder) {
        super(materialIn, type, builder);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "kamen_rider_interwoven_worlds:textures/armor/" + this.getMaterial().getName() + ".png";
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

    private PlayState predicate(AnimationState<?> event) {
        Entity entity = event.getData(DataTickets.ENTITY);
        if (!(entity instanceof LivingEntity living)) {
            return PlayState.STOP;
        }

        ItemStack stack = living.getItemBySlot(this.getType().getSlot());
        if (!stack.getItem().equals(this)) {
            return PlayState.STOP;
        }

        String anim = getAnim(stack);
        String lastAnim = getLastAnim(stack);

        if (!anim.equals(lastAnim)) {
            setLastAnim(stack, anim);
            if (!anim.equals(DEFAULT_ANIM) && !anim.isEmpty()) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay(anim));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop(DEFAULT_ANIM));
            }
        } else if (!anim.equals(DEFAULT_ANIM) && !anim.isEmpty()) {
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                setAnim(stack, DEFAULT_ANIM);
                setLastAnim(stack, DEFAULT_ANIM);
                event.getController().setAnimation(RawAnimation.begin().thenLoop(DEFAULT_ANIM));
                if (living instanceof Player) {
                    NetworkHandler.sendToServer(new SetArmorAnimPacket(this.getType().getSlot(), DEFAULT_ANIM));
                }
            }
        } else if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(DEFAULT_ANIM));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}