// src/main/java/com/rider/qianyunyi/system/rider/state/StandbyStateHandler.java
package com.qianyunyi.rider.internal.henshin.state;

import com.qianyunyi.rider.config.common.RiderAssertions;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.henshin.HenshinManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

public class StandbyStateHandler implements HenshinStateHandler {
    @Override
    public void handle(LivingEntity entity, boolean isCrouching, HenshinManager manager) {
        if (!(entity instanceof ServerPlayer serverPlayer)) return;

        var dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        RiderConfig riderConfig = RiderRegistryHelper.getConfig(dataManager.getActiveRiderName()).orElse(null);
        FormConfig formConfig = null;
        if (riderConfig != null) {
            formConfig = riderConfig.getForm(dataManager.getActiveFormName());
        }

        RiderAssertions.notNull(riderConfig, "骑士配置不能为空");
        RiderAssertions.notNull(formConfig, "形态配置不能为空");

        if (isCrouching) {
            formConfig.getBehavior().handleDeactivation(serverPlayer, riderConfig, formConfig);
        } else {
            formConfig.getBehavior().handleHenshin(serverPlayer, riderConfig, formConfig);
        }
    }
}