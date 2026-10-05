package com.qianyunyi.rider.internal.combat.skill.kuuga;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TitanSlash implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "泰坦剑连斩";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        TitanSwordComboHandler.executeCombo(serverPlayer);
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        return TitanSwordComboHandler.canExecute(serverPlayer);
    }
}