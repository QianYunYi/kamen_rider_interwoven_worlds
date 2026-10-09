package com.qianyunyi.rider.util;

import com.qianyunyi.rider.entity.vehicle.gouram.GouramEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

public final class RiderCompanionUtil {

    private RiderCompanionUtil() {}

    /**
     * 判断 target 是否属于 owner 的同伴/座驾/宠物，应当被技能/伤害逻辑跳过。
     */
    public static boolean isPlayerCompanion(Entity target, UUID ownerUUID) {
        if (target == null || ownerUUID == null) return false;
        if (target.getUUID().equals(ownerUUID)) return true;

        if (target instanceof GouramEntity gouram) {
            UUID gouramOwner = gouram.getOwnerUUID();
            if (gouramOwner != null && gouramOwner.equals(ownerUUID)) return true;
        }

        if (target instanceof TamableAnimal tamable) {
            UUID tamableOwner = tamable.getOwnerUUID();
            if (tamableOwner != null && tamableOwner.equals(ownerUUID)) return true;
        }

        return false;
    }
}