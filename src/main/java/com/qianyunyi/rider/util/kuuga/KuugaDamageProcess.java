package com.qianyunyi.rider.util.kuuga;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class KuugaDamageProcess {

    /**
     * 处理空我形态的伤害计算和减免
     * 返回处理后的伤害值
     */
    public static float processDamage(LivingHurtEvent event, ServerPlayer player, DamageSource source, float originalAmount) {
        if (player == null || player.level().isClientSide()) {
            return originalAmount;
        }

        // 重要：确保这是玩家自己受到伤害，而不是玩家造成的伤害
        if (!event.getEntity().getUUID().equals(player.getUUID())) {
            return originalAmount;
        }

        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        if (dataManager == null) {
            return originalAmount;
        }

        // 获取当前形态
        String formName = dataManager.getActiveFormName();
        if (formName == null) {
            return originalAmount;
        }

        // 检查伤害免疫
        if (isImmuneToDamageType(formName, source)) {
            event.setCanceled(true);
            return 0f;
        }

        // 根据形态应用伤害减免
        float reducedDamage = originalAmount * getDamageReductionMultiplier(formName);

        // 应用限伤阈值
        return applyMinDamageThreshold(formName, player, reducedDamage);
    }



    /**
     * 获取指定形态的减伤系数
     */
    public static float getDamageReductionMultiplier(String formName) {
        return switch (formName) {
            case "mighty_form", "rising_mighty_form" -> 0.9f; // 90%减伤
            case "dragon_form", "rising_dragon_form", "pegasus_form", "rising_pegasus_form" -> 1.0f; // 0%减伤
            case "titan_form", "rising_titan_form" -> 0.8f; // 80%减伤
            case "amazing_mighty_form" -> 0.85f; // 85%减伤
            case "ultimate_form", "ultimate_form_dark_eye" -> 0.70f; // 80%减伤
            case "rising_ultimate_form", "rising_ultimate_form_dark_eye" -> 0.60f; // 70%减伤
            case "super_rising_ultimate_form" -> 0.50f; // 50%减伤
            default -> 1.0f; // 无减伤
        };
    }

    private static float applyMinDamageThreshold(String formName, ServerPlayer player, float amount) {
        float maxHealth = player.getMaxHealth();
        float damageLimit;

        switch (formName) {
            case "ultimate_form":
            case "ultimate_form_dark_eye":
                damageLimit = maxHealth * 0.2f;  // 单次最多20%伤害
                break;
            case "rising_ultimate_form":
            case "rising_ultimate_form_dark_eye":
                damageLimit = maxHealth * 0.1f;  // 单次最多10%伤害
                break;
            case "super_rising_ultimate_form":
                damageLimit = maxHealth * 0.05f;  // 单次最多5%伤害
                break;
            default:
                return amount;  // 其他形态不应用限伤
        }

        // 限制单次伤害不超过阈值
        return Math.min(amount, damageLimit);
    }

    /**
     * 检查是否免疫特定类型伤害
     */
    public static boolean isImmuneToDamageType(String formName, DamageSource source) {

        switch (formName) {
            case "dragon_form":
            case "rising_dragon_form":
                if (source.is(DamageTypes.FALL) ||
                        source.is(DamageTypes.DROWN) ||
                        "fall".equals(source.getMsgId())||
                        "drown".equals(source.getMsgId())
                        ) {
                    return true;
                }
                break;
            case "ultimate_form":
            case "ultimate_form_dark_eye":
            case "rising_ultimate_form":
            case "rising_ultimate_form_dark_eye":
            case "super_rising_ultimate_form":
                if (source.is(DamageTypes.FALL) ||
                        source.is(DamageTypes.IN_WALL) ||
                        source.is(DamageTypes.DROWN) ||
                        source.is(DamageTypes.STARVE) ||
                        "fall".equals(source.getMsgId()) ||
                        "inWall".equals(source.getMsgId()) ||
                    "drown".equals(source.getMsgId()) ||
                    "starve".equals(source.getMsgId())) {
                return true;
            }
        }

        return false;
    }
}