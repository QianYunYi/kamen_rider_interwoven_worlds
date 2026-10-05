package com.qianyunyi.rider.internal.registry.impl.kuuga.forms;

import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.combat.skill.kuuga.CalamityTitan;
import com.qianyunyi.rider.internal.combat.skill.kuuga.kick.SuperRisingUltimateFormKuugaRiderKick;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.*;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.SuperRisingUltimateKuugaHenshinBehavior;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.data.enums.WeaponType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.registry.config.AttributeModifierConfig;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
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
 * 超升华究极空我形态配置
 */
public class KuugaSuperRisingUltimateFormConfig extends FormConfig {

    public KuugaSuperRisingUltimateFormConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "super_rising_ultimate_form",
                createArmorSet(),
                createCondition(),
                null,
                null,
                createSounds(),
                FormType.AWAKENING,
                new SuperRisingUltimateKuugaHenshinBehavior(),
                hudSupplier,
                null,
                createModifiers(),
                createBuffs(),
                createSkills(),
                createAbilities(),
                createWeapons(),
                null
        );
    }

    private static Item[] createArmorSet() {
        return new Item[]{
                KamenRiderInterwovenWorldsModItems.SUPER_RISING_ULTIMATE_FORM_KUUGA_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.SUPER_RISING_ULTIMATE_FORM_KUUGA_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                KamenRiderInterwovenWorldsModItems.SUPER_RISING_ULTIMATE_FORM_KUUGA_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        // TODO: 实现实际解锁条件
        return player -> false;
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY, KamenRiderInterwovenWorldsModSounds.STANDBY_ARCLE.get());
        sounds.put(RiderSoundType.HENSHIN, KamenRiderInterwovenWorldsModSounds.KUUGA_RISING_ULTIMATE.get());
        sounds.put(RiderSoundType.QUICK_HENSHIN,KamenRiderInterwovenWorldsModSounds.KUUGA_RISING_ULTIMATE.get());
        sounds.put(RiderSoundType.UNFORM,KamenRiderInterwovenWorldsModSounds.KUUGA_UNFORM_RISING.get());
        return sounds;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        return List.of(
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 120, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 200, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.ARMOR, 60, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.KNOCKBACK_RESISTANCE, 2, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.ARMOR_TOUGHNESS, 2, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.SATURATION, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.REGENERATION, 20, 5, false, false, false),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 3, false, false, false),
                new MobEffectInstance(MobEffects.DIG_SPEED, 20, 1, false, false, false),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false, false),
                new MobEffectInstance(MobEffects.JUMP, 20, 3, false, false, false)
        );
    }

    private static Map<SkillType, IRiderSkill> createSkills() {
        return Map.of(SkillType.RIDER_KICK, new SuperRisingUltimateFormKuugaRiderKick(),
                SkillType.SWORD_SLASH, new CalamityTitan());
    }

    private static List<IRiderSkill> createAbilities() {
        return List.of(new SupernaturalFire(),new SupernaturalWater(),new SupernaturalThunder(),new SupernaturalWind(), new SupernaturalEarth(), new SupernaturalSenses() ,new SummonGouram());
    }

    private static Map<WeaponType, Item> createWeapons() {
        return Map.of(WeaponType.SUBSTITUTE_SWORD, KamenRiderInterwovenWorldsModItems.ULTIMATE_TITAN_SWORD.get(),
                WeaponType.SUBSTITUTE_WAND, KamenRiderInterwovenWorldsModItems.ULTIMATE_DRAGON_WAND.get(),
                WeaponType.SUBSTITUTE_CROSSBOW, KamenRiderInterwovenWorldsModItems.ULTIMATE_PEGASUS_CROSSBOW.get()
        );
    }
}