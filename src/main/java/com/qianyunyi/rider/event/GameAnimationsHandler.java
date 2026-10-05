package com.qianyunyi.rider.event;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.item.base.BaseAnimationItem;
import com.qianyunyi.rider.item.base.BaseCrossbowAnimationItem;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.item.base.BaseTridentAnimationItem;
import com.qianyunyi.rider.item.PetrifyArcleItemItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import software.bernie.geckolib.animatable.GeoItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FORGE事件总线上的游戏运行时动画处理器。
 * 处理 PlayerTickEvent 和 LivingTickEvent，驱动 GeckoLib 物品/盔甲动画。
 *
 * 注意：此类与 SetupAnimationsEvent（MOD总线）分离，因为 PlayerTickEvent/LivingTickEvent
 * 是 Forge 事件（非 IModBusEvent），不能注册在 Mod 总线上。
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GameAnimationsHandler {

    private static final String NBT_GECKO_ANIM = "geckoAnim";
    private static final String UNDEFINED_ANIM = "undefined";
    private static final String EMPTY_ANIM = "empty";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // ==================== Per-Tick Processing ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player == null) return;

        if (event.phase == TickEvent.Phase.END) {
            processAnimatedArmors(event.player);
        }
        if (event.phase == TickEvent.Phase.START) {
            processAnimatedItems(event.player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() != null) {
            processAnimatedEntity(event.getEntity());
        }
    }

    // ==================== Armor Animation Processing ====================

    private static void processAnimatedArmors(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (!(item instanceof GeoItem)) continue;

            String animation = stack.getOrCreateTag().getString(NBT_GECKO_ANIM);
            if (animation.isEmpty()) continue;

            stack.getOrCreateTag().putString(NBT_GECKO_ANIM, "");
            if (!player.level().isClientSide()) continue;

            if (item instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(stack, animation);
            }
        }
    }

    // ==================== Item Animation Processing ====================

    private static void processAnimatedItems(Player player) {
        processHandItem(player, player.getMainHandItem());
        processHandItem(player, player.getOffhandItem());
    }

    private static void processHandItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        if (!(item instanceof GeoItem)) return;

        String animation = stack.getOrCreateTag().getString(NBT_GECKO_ANIM);
        if (animation.isEmpty()) return;

        stack.getOrCreateTag().putString(NBT_GECKO_ANIM, "");
        if (!player.level().isClientSide()) return;

        if (item instanceof BaseAnimationItem) {
            BaseAnimationItem.setAnim(stack, animation);
        } else if (item instanceof BaseSwordAnimationItem) {
            BaseSwordAnimationItem.setAnim(stack, animation);
        } else if (item instanceof BaseCrossbowAnimationItem) {
            BaseCrossbowAnimationItem.setAnim(stack, animation);
        } else if (item instanceof BaseTridentAnimationItem) {
            BaseTridentAnimationItem.setAnim(stack, animation);
        } else if (item instanceof PetrifyArcleItemItem petrify) {
            petrify.animationprocedure = animation;
        }
    }

    // ==================== Entity Animation Processing ====================

    private static final Map<Class<?>, java.lang.reflect.Method> syncedAnimGetters = new ConcurrentHashMap<>();
    private static final Map<Class<?>, java.lang.reflect.Method> syncedAnimSetters = new ConcurrentHashMap<>();
    private static final Map<Class<?>, java.lang.reflect.Field> animProcedureFields = new ConcurrentHashMap<>();

    private static void processAnimatedEntity(LivingEntity entity) {
        String animation = getSyncedAnimationCached(entity);
        if (animation == null || UNDEFINED_ANIM.equals(animation)) return;

        setSyncedAnimationCached(entity, UNDEFINED_ANIM);
        setAnimationProcedureCached(entity, animation);
    }

    private static String getSyncedAnimationCached(LivingEntity entity) {
        java.lang.reflect.Method method = syncedAnimGetters.computeIfAbsent(entity.getClass(), cls -> {
            try {
                return cls.getMethod("getSyncedAnimation");
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
        if (method == null) return null;
        try {
            return (String) method.invoke(entity);
        } catch (Exception e) {
            return UNDEFINED_ANIM;
        }
    }

    private static void setSyncedAnimationCached(LivingEntity entity, String animation) {
        java.lang.reflect.Method method = syncedAnimSetters.computeIfAbsent(entity.getClass(), cls -> {
            try {
                return cls.getMethod("setAnimation", String.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
        if (method == null) return;
        try {
            method.invoke(entity, animation);
        } catch (Exception ignored) {
        }
    }

    private static void setAnimationProcedureCached(LivingEntity entity, String animation) {
        java.lang.reflect.Field field = animProcedureFields.computeIfAbsent(entity.getClass(), cls -> {
            try {
                return cls.getField("animationprocedure");
            } catch (NoSuchFieldException e) {
                return null;
            }
        });
        if (field == null) return;
        try {
            field.set(entity, animation);
        } catch (Exception ignored) {
        }
    }
}