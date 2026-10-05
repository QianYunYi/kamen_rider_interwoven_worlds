package com.qianyunyi.rider.event;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.animation.MotorcycleLeanAnimation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SetupAnimationsEvent {

    private static final String MOD_NAMESPACE = "kamen_rider_interwoven_worlds";

    private static final ResourceLocation MAIN_LAYER_KEY = new ResourceLocation(MOD_NAMESPACE, "main_layer");
    private static final ResourceLocation LEAN_LAYER_KEY = new ResourceLocation(MOD_NAMESPACE, "lean_anim");

    private static final Map<UUID, ActiveAnimation> activeAnimations = new ConcurrentHashMap<>();

    private record ActiveAnimation(ResourceLocation animationId, long activeUntilTick) {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[SetupAnimationsEvent] onClientSetup: 注册PlayerAnimator回调");
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(SetupAnimationsEvent::registerPlayerAnimation);
    }

    private static void registerPlayerAnimation(AbstractClientPlayer player, AnimationStack stack) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[SetupAnimationsEvent] registerPlayerAnimation: player class={}", player.getClass().getSimpleName());

        ModifierLayer<IAnimation> layer = new ModifierLayer<>();
        stack.addAnimLayer(1000, layer);
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(MAIN_LAYER_KEY, layer);

        MotorcycleLeanAnimation leanAnim = new MotorcycleLeanAnimation();
        ModifierLayer<IAnimation> leanLayer = new ModifierLayer<>();
        stack.addAnimLayer(2000, leanLayer);
        leanLayer.setAnimation(leanAnim);
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(LEAN_LAYER_KEY, leanAnim);

        KamenRiderInterwovenWorldsMod.LOGGER.info("[SetupAnimationsEvent] registerPlayerAnimation: layers registered via PlayerAssociatedAnimationData");
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getMainLayer(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return null;
        IAnimation anim = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(MAIN_LAYER_KEY);
        if (anim instanceof ModifierLayer) return (ModifierLayer<IAnimation>) anim;
        return null;
    }

    private static MotorcycleLeanAnimation getLeanAnim(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return null;
        IAnimation anim = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(LEAN_LAYER_KEY);
        if (anim instanceof MotorcycleLeanAnimation lean) return lean;
        return null;
    }

    public static void setLeanAmount(Player player, float leanAmount) {
        MotorcycleLeanAnimation leanAnim = getLeanAnim(player);
        if (leanAnim != null) {
            leanAnim.setLeanAmount(leanAmount);
            leanAnim.setActive(Math.abs(leanAmount) > 0.1f || Math.abs(leanAnim.getPitchAmount()) > 0.1f);
        }
    }

    public static void setPitchAmount(Player player, float pitchAmount) {
        MotorcycleLeanAnimation leanAnim = getLeanAnim(player);
        if (leanAnim != null) {
            leanAnim.setPitchAmount(pitchAmount);
        }
    }

    public static void stopLeanAnimation(Player player) {
        MotorcycleLeanAnimation leanAnim = getLeanAnim(player);
        if (leanAnim != null) {
            leanAnim.setActive(false);
            leanAnim.setLeanAmount(0);
            leanAnim.setPitchAmount(0);
        }
    }

    public static void stopAnimation(Player player) {
        ModifierLayer<IAnimation> layer = getMainLayer(player);
        if (layer != null) {
            layer.setAnimation(null);
        }
    }

    public static void playAnimation(Player player, String animationName, boolean start) {
        ModifierLayer<IAnimation> layer = getMainLayer(player);
        if (layer == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn("[SetupAnimationsEvent] playAnimation: player {} 没有找到动画层", player.getName().getString());
            return;
        }
        if (start) {
            ResourceLocation animId = ResourceLocation.fromNamespaceAndPath(MOD_NAMESPACE, animationName);
            KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animId);
            if (animation != null) {
                layer.setAnimation(new KeyframeAnimationPlayer(animation));
                startScaleSync(player, animId);
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("[SetupAnimationsEvent] playAnimation: 找不到动画资源 {}", animId);
            }
        } else {
            layer.setAnimation(null);
        }
    }

    public static void startScaleSync(Player player, ResourceLocation animationId) {
        removeScaleSync(player);
        activeAnimations.put(player.getUUID(), new ActiveAnimation(animationId, player.tickCount + 5));
    }

    public static void removeScaleSync(Player player) {
        activeAnimations.remove(player.getUUID());
    }

    public static boolean isAnimationActive(Player player) {
        ActiveAnimation active = activeAnimations.get(player.getUUID());
        if (active == null) return false;
        if (player.tickCount > active.activeUntilTick()) {
            activeAnimations.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static Optional<ActiveAnimation> getActiveAnimation(Player player) {
        ActiveAnimation active = activeAnimations.get(player.getUUID());
        if (active == null) return Optional.empty();
        return Optional.of(active);
    }

    public static MotorcycleLeanAnimation getLeanAnimation(Player player) {
        return getLeanAnim(player);
    }
}