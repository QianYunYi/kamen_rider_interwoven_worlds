package com.qianyunyi.rider.internal.registry.impl.maskedrider1.forms;

import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.internal.registry.config.AttributeModifierConfig;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.henshin.behavior.impl.maskedrider1.MaskedRider1HenshinBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 旧1号形态配置 - 恢复完整功能
 */
public class MaskedRider1OidFormConfig extends FormConfig {

    public MaskedRider1OidFormConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "masked_rider_1_oid",
                createArmorSet(),
                createCondition(),
                null,  // mainHenshinItem
                null,  // offhandHenshinItem
                createSounds(),
                FormType.AWAKENING,
                new MaskedRider1HenshinBehavior(),
                hudSupplier,
                null,  // henshinStateListener// attackListener
                createModifiers(),
                createBuffs(),
                createSkills(),
                List.of(),  // abilities
                Map.of(),   // weapons
                null        // grantedItem
        );
    }

    private static Item[] createArmorSet() {
        return new Item[]{
                KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.TYPHOON_MODIFIED_PART.get(),
                KamenRiderInterwovenWorldsModItems.MASK_RIDER_1_OLD_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        return player -> false; // 默认允许变身
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY, getSound("standby"));
        sounds.put(RiderSoundType.HENSHIN, getSound("transform"));
        sounds.put(RiderSoundType.QUICK_HENSHIN, getSound("quick_transform"));
        return sounds;
    }

    private static SoundEvent getSound(String name) {
        // 替换为实际的声音获取逻辑
        // 示例：return ModSounds.getSound(name);
        return null;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        return List.of(
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 75, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 80, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.ARMOR, 30, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.KNOCKBACK_RESISTANCE, 2, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.REGENERATION, 20, 2, false, false, false),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 2, false, false, false),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.DIG_SPEED, 20, 0, false, false, false)
        );
    }

    private static Map<SkillType, IRiderSkill> createSkills() {
        // 需要替换为实际的技能实现
        return Map.of();
    }
}