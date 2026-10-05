package com.qianyunyi.rider.internal.registry.impl.kuuga.forms;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.SummonGouram;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.SupernaturalSenses;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.WeaponType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.internal.registry.config.AttributeModifierConfig;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.MightyFormKuugaHenshinBehavior;
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
 * 天马形态配置
 */
public class KuugaPegasusFormConfig extends FormConfig {

    public KuugaPegasusFormConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "pegasus_form",
                createArmorSet(),
                createCondition(),
                null,
                null,
                createSounds(),
                FormType.AWAKENING,
                new MightyFormKuugaHenshinBehavior(),
                hudSupplier,
                null,
                createModifiers(),
                createBuffs(),
                Map.of(),
                createAbilities(),
                createWeapons(),
                null
        );
    }

    private static Item[] createArmorSet() {
        return new Item[]{
                KamenRiderInterwovenWorldsModItems.PEGASUS_FORM_KUUGA_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.PEGASUS_FORM_KUUGA_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                KamenRiderInterwovenWorldsModItems.PEGASUS_FORM_KUUGA_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        // TODO: 实现实际解锁条件
        return player -> false;
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY, KamenRiderInterwovenWorldsModSounds.STANDBY_ARCLE.get());
        sounds.put(RiderSoundType.HENSHIN, KamenRiderInterwovenWorldsModSounds.KUUGA_PEGASUS_FORM.get());
        sounds.put(RiderSoundType.QUICK_HENSHIN, KamenRiderInterwovenWorldsModSounds.KUUGA_PEGASUS_FORM.get());
        sounds.put(RiderSoundType.UNFORM,KamenRiderInterwovenWorldsModSounds.KUUGA_UNFORM.get());
        return sounds;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        return List.of(
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 3, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 30, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                //夜视
                new MobEffectInstance(MobEffects.NIGHT_VISION, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.DIG_SPEED, 20, 1, false, false, false),
                new MobEffectInstance(MobEffects.REGENERATION, 20, 0, false, false, false)
        );
    }

    private static List<IRiderSkill> createAbilities() {
        return List.of(new SupernaturalSenses(),new SummonGouram());
    }

    private static Map<WeaponType, Item> createWeapons() {
        return Map.of(WeaponType.SUBSTITUTE_CROSSBOW, KamenRiderInterwovenWorldsModItems.PEGASUS_CROSSBOW.get());
    }
}