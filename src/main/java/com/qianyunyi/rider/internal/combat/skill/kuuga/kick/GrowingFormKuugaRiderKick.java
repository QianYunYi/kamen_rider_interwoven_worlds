package com.qianyunyi.rider.internal.combat.skill.kuuga.kick;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class GrowingFormKuugaRiderKick extends KuugaBaseRiderKick {
    @Override
    public String getDisplayName() {
        return "初生踢";
    }

    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        // 先调用基础技能
        super.skill(serverPlayer);
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
        // 先调用基础释放
        super.releaseSkill(serverPlayer, isAutoRelease);
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        // 检查是否正在蓄力
       return super.isAvailable(serverPlayer);
    }

    @Override
    protected void triggerLanding(ServerPlayer serverPlayer, KickState s) {
        super.triggerLanding(serverPlayer, s);
    }
}