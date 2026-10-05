package com.qianyunyi.rider.internal.registry.impl.kuuga.forms;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.internal.combat.skill.kuuga.kick.AmazingMightyFormKuugaRiderKick;
import com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural.SummonGouram;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
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
 * 惊异全能形态配置
 */
public class KuugaAmazingMightyFormConfig extends FormConfig {

    public KuugaAmazingMightyFormConfig(Supplier<IHUDManager> hudSupplier) {
        super(
                "amazing_mighty_form",
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
                createSkills(),
                createAbilities(),
                Map.of(),
                null
        );
    }

    private static Item[] createArmorSet() {
        return new Item[]{
                KamenRiderInterwovenWorldsModItems.AMAZING_MIGHTY_FORM_KUUGA_HELMET.get(),
                KamenRiderInterwovenWorldsModItems.AMAZING_MIGHTY_FORM_KUUGA_CHESTPLATE.get(),
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                KamenRiderInterwovenWorldsModItems.AMAZING_MIGHTY_FORM_KUUGA_BOOTS.get()
        };
    }

    private static Predicate<ServerPlayer> createCondition() {
        // TODO: 实现实际解锁条件
        return player -> false;
    }

    private static Map<RiderSoundType, SoundEvent> createSounds() {
        Map<RiderSoundType, SoundEvent> sounds = new HashMap<>();
        sounds.put(RiderSoundType.STANDBY, KamenRiderInterwovenWorldsModSounds.STANDBY_ARCLE.get());
        sounds.put(RiderSoundType.HENSHIN, KamenRiderInterwovenWorldsModSounds.KUUGA_AMAZING_MIGHTY.get());
        sounds.put(RiderSoundType.QUICK_HENSHIN, KamenRiderInterwovenWorldsModSounds.KUUGA_AMAZING_MIGHTY.get());
        return sounds;
    }

    private static List<AttributeModifierConfig> createModifiers() {
        // 注意：惊异全能的数值需要修正
        return List.of(
                // 攻击伤害+75
                new AttributeModifierConfig(Attributes.ATTACK_DAMAGE, 75, AttributeModifier.Operation.ADDITION, null),
                // 最大生命值+80
                new AttributeModifierConfig(Attributes.MAX_HEALTH, 80, AttributeModifier.Operation.ADDITION, null),
                // 护甲+30
                new AttributeModifierConfig(Attributes.ARMOR, 30, AttributeModifier.Operation.ADDITION, null),
                // 击退抗性+2
                new AttributeModifierConfig(Attributes.KNOCKBACK_RESISTANCE, 2, AttributeModifier.Operation.ADDITION, null)
        );
    }

    private static List<MobEffectInstance> createBuffs() {
        return List.of(
                // 隐身+20秒
                new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false),
                // 伤害提升+20秒
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20, 0, false, false, false),
                // 回复+20秒
                new MobEffectInstance(MobEffects.REGENERATION, 20, 2, false, false, false),
                // 伤害抗性+20秒
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 2, false, false, false),
                // 移动速度+20秒
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, false, false, false),
                // 挖掘速度+20秒
                new MobEffectInstance(MobEffects.DIG_SPEED, 20, 0, false, false, false)
        );
    }

    private static List<IRiderSkill> createAbilities() {
        return List.of(new SummonGouram());
    }

    private static Map<SkillType, IRiderSkill> createSkills() {
        return Map.of(SkillType.RIDER_KICK, new AmazingMightyFormKuugaRiderKick());
    }
}