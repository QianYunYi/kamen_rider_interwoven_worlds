package com.qianyunyi.rider.internal.henshin.state;

import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;

import com.qianyunyi.rider.internal.henshin.HenshinManager;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

public abstract class InactiveStateHandler implements HenshinStateHandler {
    @Override
    public void handle(LivingEntity entity, boolean isCrouching, HenshinManager manager) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;

        var dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        if (manager.tryAwakeningHenshin(serverPlayer, isCrouching, dataManager)) {
            return;
        }

        for (var entry : RiderRegistryHelper.getAllConfigs()) {
            RiderConfig config = entry.getValue();
            FormConfig formConfig = config.getForm("base");

            if (formConfig == null || !formConfig.canTransform(serverPlayer)) {
                continue;
            }

            if (isCrouching) {
                if (HenshinUtil.isEquippedArmor(serverPlayer, formConfig.getArmorSet())
                        || manager.isWearingBelt(serverPlayer, config.getRiderName())) {
                    formConfig.getBehavior().handleUnForm(serverPlayer, config, formConfig);
                } else {
                    formConfig.getBehavior().handleQuickHenshin(serverPlayer, config, formConfig);
                }
            } else {
                formConfig.getBehavior().enterStandbyState(serverPlayer, config, formConfig);
                dataManager.setActiveFormName(formConfig.getFormName());
                dataManager.setActiveRiderName(config.getRiderName());
            }
            return;
        }
    }
}