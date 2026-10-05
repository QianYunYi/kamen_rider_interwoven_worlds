package com.qianyunyi.rider.internal.registry.impl.kuuga.forms;

import com.qianyunyi.rider.internal.combat.skill.kuuga.kick.GrowingFormKuugaRiderKick;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.registry.config.AttributeModifierConfig;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.GrowingFormKuugaHenshinBehavior;
import com.qianyunyi.rider.internal.registry.impl.kuuga.KuugaRiderRegistry;
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
 * 初生形态配置
 */
public class KuugaBaseFormConfig extends FormConfig {

    public KuugaBaseFormConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "base",
                createArmorSet(),
                createCondition(),
                null,
                null,
                createSounds(),
                FormType.AWAKENING,
                new GrowingFormKuugaHenshinBehavior(),
                hudSupplier, // 使用传入的 Supplier
                null,
                createModifiers(),
                createBuffs(),
                createSkills(),
                List.of(),
                Map.of(),
                null
        );
    }

    private static Item[] createArmorSet() {
        return new Item[]{
                KamenRiderInterwovenWorldsModItems.GROWING_FORM_KUUGA_ITEM_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.GROWING_FORM_KUUGA_ITEM_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                KamenRiderInterwovenWorldsModItems.GROWING_FORM_KUUGA_ITEM_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        return player -> KuugaRiderRegistry.isFormUnlocked("base", player);
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY,KamenRiderInterwovenWorldsModSounds.STANDBY_ARCLE.get());
        //sounds.put(RiderSoundType.HENSHIN,KamenRiderInterwovenWorldsModSounds.HENSHINRM.get());
        sounds.put(RiderSoundType.UNFORM,KamenRiderInterwovenWorldsModSounds.KUUGA_UNFORM.get());
        return sounds;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        return List.of(
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 5, AttributeModifier.Operation.ADDITION, null),
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 50, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0, false, false, false),
                new MobEffectInstance(MobEffects.REGENERATION, 20, 0, false, false, false)
        );
    }

    private static Map<SkillType, IRiderSkill> createSkills() {
        return Map.of(SkillType.RIDER_KICK, new GrowingFormKuugaRiderKick());
    }

}