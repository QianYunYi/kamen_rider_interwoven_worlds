package com.qianyunyi.rider.internal.henshin;


import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 骑士管理器
 *
 * @author QianYunYi
 */
public class HenshinManager {

    /**
     * 执行变身逻辑（状态机实现）
     *
     * @param entity 实体
     */
    public static void executeHenshin(LivingEntity entity) {
        // 检查是否为服务端玩家实体
        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        // 检查是否为服务器端实体
        if (serverPlayer.level().isClientSide) return;
        // 下蹲状态获取
        boolean isCrouching = serverPlayer.isCrouching();
        // 获取玩家数据管理器
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        // 获取玩家当前状态
        HenshinState currentState = dataManager.getCurrentState();
        // 获取当前变身的骑士名称
        String activeRiderName = dataManager.getActiveRiderName();
        // 获取当前形态名称
        String activeFormName = dataManager.getActiveFormName();
       // 获取当前变身的骑士配置
        RiderConfig riderConfig = activeRiderName != null && !activeRiderName.isEmpty() ?
                RiderRegistryHelper.getConfig(activeRiderName).orElse(null) : null;
      // 获取当前形态配置
        FormConfig formConfig = riderConfig != null && activeFormName != null ?
                riderConfig.getForm(activeFormName) : null;
        if (dataManager.isHenshining())return;
        switch (currentState) {
            // 未变身时
            case INACTIVE:
                //如果玩家正在变身中或者玩家正在HENSHIN_CANNOT效果不允许变身
                if (serverPlayer.getEffect(KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get())==null) {
                    handleInactiveState(serverPlayer, isCrouching, dataManager);
                }
                break;

            // 待机状态
            case STANDBY:
                    handleStandbyState(serverPlayer, isCrouching, riderConfig, formConfig);
                break;

            // 变身状态
            case ACTIVE:
                    handleActiveState(serverPlayer, isCrouching, riderConfig, formConfig, dataManager);
                break;

            // 强化形态待机状态
            case STANDBY_AWAKENING:
                    handleStandbyAwakening(serverPlayer, isCrouching, riderConfig, formConfig);
                break;

            // 强化形态变身状态
            case ACTIVE_AWAKENING:
                    handleActiveAwakening(serverPlayer, isCrouching, riderConfig, formConfig, dataManager);
                break;

            // 未知状态
            default:
                HenshinUtil.sendMessage(serverPlayer, "未知变身状态", ChatFormatting.RED);
                break;
        }
    }

    /**
     * 处理未变身状态
     */
    private static void handleInactiveState(ServerPlayer serverPlayer, boolean isCrouching, RiderPlayerData dataManager) {

        // 优先检查强化形态
        if (tryAwakeningHenshin(serverPlayer, isCrouching, dataManager)) {
            return;
        }

        // 优化：通过腰带物品快速定位骑士（O(1)），避免线性遍历所有 RiderConfig
        ItemStack legItem = serverPlayer.getItemBySlot(EquipmentSlot.LEGS);
        if (!legItem.isEmpty()) {
            String riderName = RiderRegistryHelper.getRiderNameByBelt(legItem.getItem());
            if (!riderName.isEmpty()) {
                RiderConfig fastConfig = RiderRegistryHelper.getConfig(riderName).orElse(null);
                if (fastConfig != null) {
                    FormConfig fastForm = fastConfig.getForm("base");
                    if (fastForm != null && fastForm.canTransform(serverPlayer)) {
                        processFormState(serverPlayer, isCrouching, fastConfig, fastForm);
                        return;
                    }
                }
            }
        }

        // 兜底：遍历所有骑士配置（处理非腰带变身或特殊条件）
        for (RiderConfig config : RiderRegistryHelper.getAllConfigs()) {
            FormConfig formConfig = config.getForm("base");
            if (formConfig == null) continue;

            if (formConfig.canTransform(serverPlayer)) {
                processFormState(serverPlayer, isCrouching, config, formConfig);
                return;
            }
        }
    }

    /**
     * 处理形态状态（蹲下取消变身，否则进入待机）
     */
    private static void processFormState(ServerPlayer serverPlayer, boolean isCrouching,
                                          RiderConfig config, FormConfig formConfig) {
        if (isCrouching) {
            if (HenshinUtil.isEquippedArmor(serverPlayer, formConfig.getArmorSet()) || isWearingBelt(serverPlayer, config.getRiderName())) {
                formConfig.getBehavior().handleUnForm(serverPlayer, config, formConfig);
            } else {
                formConfig.getBehavior().handleQuickHenshin(serverPlayer, config, formConfig);
            }
        } else {
            formConfig.getBehavior().enterStandbyState(serverPlayer, config, formConfig);
        }
    }

    /**
     * 尝试进行强化形态变身
     *
     * @return 如果成功处理强化形态变身，返回 true；否则返回 false
     */
    public static boolean tryAwakeningHenshin(ServerPlayer serverPlayer, boolean isCrouching, RiderPlayerData dataManager) {
        for (FormConfig formConfig : RiderRegistryHelper.getAwakeningForms()) {
            if (formConfig.canTransform(serverPlayer)) {
                String riderName = RiderRegistryHelper.getRiderNameForForm(formConfig.getFormName());
                RiderConfig config = RiderRegistryHelper.getConfig(riderName).orElse(null);
                if (config == null) continue;
                if (isCrouching) {
                    if (HenshinUtil.isEquippedArmor(serverPlayer, formConfig.getArmorSet()) || isWearingBelt(serverPlayer, formConfig.getFormName())) {
                        formConfig.getBehavior().handleUnForm(serverPlayer, config, formConfig);
                    } else {
                        formConfig.getBehavior().handleQuickHenshin(serverPlayer, config, formConfig);
                    }
                } else {
                    formConfig.getBehavior().enterStandbyState(serverPlayer, config, formConfig);
                    dataManager.setActiveFormName(formConfig.getFormName());
                    dataManager.setActiveRiderName(config.getRiderName());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 处理待机状态
     */
    private static void handleStandbyState(ServerPlayer serverPlayer, boolean isCrouching,
                                           RiderConfig riderConfig, FormConfig formConfig) {
        if (riderConfig == null || formConfig == null) {
            HenshinUtil.sendMessage(serverPlayer, "错误：未找到骑士配置", ChatFormatting.RED);
            //dataManager.setCurrentState(HenshinState.STANDBY);
            return;
        }

        if (isCrouching) {
            // 解除变身
            formConfig.getBehavior().handleUnForm(serverPlayer, riderConfig, formConfig);
        } else {
            // 完成基础变身
            formConfig.getBehavior().handleHenshin(serverPlayer, riderConfig, formConfig);
        }
    }

    /**
     * 处理变身状态
     */
    private static void handleActiveState(ServerPlayer serverPlayer, boolean isCrouching,
                                          RiderConfig riderConfig, FormConfig formConfig,
                                          RiderPlayerData dataManager) {
        if (riderConfig == null || formConfig == null) {
            HenshinUtil.sendMessage(serverPlayer, "错误：未找到骑士配置", ChatFormatting.RED);
            dataManager.setCurrentState(HenshinState.INACTIVE);
            return;
        }

        // 如果是蹲下状态，直接解除基础变身（与 handleActiveAwakening 行为一致）
        if (isCrouching) {
            //如果有不可解除变身的形态，不解除基础变身
           /* if (serverPlayer.getEffect(KamenRiderInterwovenWorldsModMobEffects.UNABLE_TO_CANCEL_HENSHIN.get())!=null) {
                return;
            }*/
            // 解除基础变身
            formConfig.getBehavior().handleUnForm(serverPlayer, riderConfig, formConfig);
        } else if (tryAwakeningHenshin(serverPlayer, isCrouching, dataManager)) {
            // 非蹲下时，尝试强化形态变身
            return;
        }
    }

    /**
     * 处理强化形态待机状态
     */
    private static void handleStandbyAwakening(ServerPlayer serverPlayer, boolean isCrouching,
                                               RiderConfig riderConfig, FormConfig formConfig) {
        if (riderConfig == null || formConfig == null) {
            HenshinUtil.sendMessage(serverPlayer, "错误：强化形态配置缺失", ChatFormatting.RED);
            return;
        }


        if (isCrouching) {
            //如果有不可解除变身的形态，不解除变身
       /*     if (serverPlayer.getEffect(KamenRiderInterwovenWorldsModMobEffects.UNABLE_TO_CANCEL_HENSHIN.get())!=null) {
                return;
            }*/
            // 取消强化变身
            formConfig.getBehavior().handleUnForm(serverPlayer, riderConfig, formConfig);
        } else {
            // 执行强化变身
            formConfig.getBehavior().handleHenshin(serverPlayer, riderConfig, formConfig);
        }
    }

    /**
     * 处理强化形态变身状态
     */
    private static void handleActiveAwakening(ServerPlayer serverPlayer, boolean isCrouching,
                                              RiderConfig riderConfig, FormConfig formConfig,
                                              RiderPlayerData dataManager) {
        if (riderConfig == null || formConfig == null) {
            HenshinUtil.sendMessage(serverPlayer, "错误：强化形态配置异常", ChatFormatting.RED);
            dataManager.setCurrentState(HenshinState.INACTIVE);
            return;
        }

        if (isCrouching) {
            //如果有不可解除变身的形态，不解除变身
          /*  if (serverPlayer.getEffect(KamenRiderInterwovenWorldsModMobEffects.UNABLE_TO_CANCEL_HENSHIN.get())!=null) {
                return;
            }*/
            // 解除强化形态
            formConfig.getBehavior().handleUnForm(serverPlayer, riderConfig, formConfig);
            // 更新玩家数据
            RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
            data.setActiveFormName(""); // 清除形态名称
        }
    }

    /**
     * 检查玩家是否穿着指定骑士的腰带
     */
    public static boolean isWearingBelt(ServerPlayer serverPlayer, String riderName) {
        RiderConfig config = RiderRegistryHelper.getConfig(riderName).orElse(null); // 使用 RiderRegistryHelper
        if (config == null) return false;

        // 获取基础形态配置
        FormConfig baseForm = config.getForm("base");
        if (baseForm == null) return false;

        ItemStack legItem = serverPlayer.getItemBySlot(EquipmentSlot.LEGS);
        return !legItem.isEmpty() && legItem.getItem() == baseForm.getBeltItem();
    }
}