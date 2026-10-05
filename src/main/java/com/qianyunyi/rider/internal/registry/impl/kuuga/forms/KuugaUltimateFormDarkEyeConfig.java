package com.qianyunyi.rider.internal.registry.impl.kuuga.forms;

import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.combat.skill.kuuga.CalamityTitan;
import com.qianyunyi.rider.internal.combat.skill.kuuga.DarkSurgePalm;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.SummonGouram;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.UltimateFormKuugaHenshinBehavior;
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
 * 究极空我暗目形态配置
 */
public class KuugaUltimateFormDarkEyeConfig extends FormConfig {

    public KuugaUltimateFormDarkEyeConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "ultimate_form_dark_eye",
                createArmorSet(),
                createCondition(),
                null,
                null,
                createSounds(),
                FormType.AWAKENING,
                new UltimateFormKuugaHenshinBehavior(),
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
                KamenRiderInterwovenWorldsModItems.ULTIMATE_FORM_KUUGA_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.ULTIMATE_FORM_KUUGA_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                KamenRiderInterwovenWorldsModItems.ULTIMATE_FORM_KUUGA_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        // TODO: 实现实际解锁条件
        return player -> false;
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY, KamenRiderInterwovenWorldsModSounds.STANDBY_ARCLE.get());
        sounds.put(RiderSoundType.HENSHIN, KamenRiderInterwovenWorldsModSounds.CHANGE_ULTIMATE.get());
        sounds.put(RiderSoundType.QUICK_HENSHIN,KamenRiderInterwovenWorldsModSounds.CHANGE_ULTIMATE.get());
        sounds.put(RiderSoundType.UNFORM,KamenRiderInterwovenWorldsModSounds.KUUGA_UNFORM.get());
        return sounds;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        return List.of(
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 80, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 100, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.ARMOR, 40, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.KNOCKBACK_RESISTANCE, 2, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                // 隐身
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                // 饱和
                new MobEffectInstance(MobEffects.SATURATION, 20, 0, false, false, false),
                // 力量
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0, false, false, false),
                // 再生
                new MobEffectInstance(MobEffects.REGENERATION, 20, 3, false, false, false),
                // 抗性提升
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 3, false, false, false),
                // 挖掘提升
                new MobEffectInstance(MobEffects.DIG_SPEED, 20, 2, false, false, false),
                // 速度
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, false, false, false),
                // 跳跃提升
                new MobEffectInstance(MobEffects.JUMP, 20, 2, false, false, false)
        );
    }

    private static Map<SkillType, IRiderSkill> createSkills() {
        return Map.of(SkillType.RIDER_PUNCH, new DarkSurgePalm(),
                SkillType.SWORD_SLASH, new CalamityTitan());
    }

    private static List<IRiderSkill> createAbilities() {
        return List.of(new SummonGouram());
    }
    private static Map<WeaponType, Item> createWeapons() {
        return Map.of(WeaponType.SUBSTITUTE_SWORD, KamenRiderInterwovenWorldsModItems.ULTIMATE_TITAN_SWORD.get(),
                WeaponType.SUBSTITUTE_WAND, KamenRiderInterwovenWorldsModItems.ULTIMATE_DRAGON_WAND.get(),
                WeaponType.SUBSTITUTE_CROSSBOW, KamenRiderInterwovenWorldsModItems.ULTIMATE_PEGASUS_CROSSBOW.get()
        );
    }
}