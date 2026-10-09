package com.qianyunyi.rider.internal.combat.skill.kuuga.supernatural;

import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.entity.motoycle.BeatChaser2000Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * BeatChaser2000 爆炸冲锋必杀技
 * <p>
 * 骑乘 BeatChaser2000（普通/哥莱姆合体）时按下 X 键激活，
 * 激活后下次碰撞对目标施加爆炸标记，碰撞后技能结束并进入 30 秒冷却。
 */
public class BeatChaserExplosiveChargeSkill implements IRiderSkill {

    @Override
    public String getDisplayName() {
        return "爆炸冲锋";
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/gui/skill/explosive_charge.png");
    }

    @Override
    public void skill(ServerPlayer serverPlayer) {
        if (serverPlayer.getVehicle() instanceof BeatChaser2000Entity beatChaser) {
            beatChaser.activateExplosiveCharge(serverPlayer);
        }
    }

    @Override
    public void releaseSkill(ServerPlayer serverPlayer, boolean isAutoRelease) {
    }

    @Override
    public boolean isAvailable(ServerPlayer serverPlayer) {
        if (!(serverPlayer.getVehicle() instanceof BeatChaser2000Entity beatChaser)) {
            return false;
        }
        return beatChaser.canActivateExplosiveCharge(serverPlayer);
    }
}