package com.qianyunyi.rider.entity.kaijin.shocker.api;


import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRelationship;
import net.minecraft.world.entity.LivingEntity;

public interface IShocker {
    ShockerRank.Rank getRank();
    int getOrgId();
    ShockerRelationship getRelationshipTo(LivingEntity target);

    // ========== 新增 ==========
//    default boolean isPlayerSuperiorLeader(Player player) {
//        return CapabilityRegistry.hasJawOfAllOrganizationsOOOOOO(player);
//    }

    default boolean isBelowLeader() {
        return getRank().ordinal() < ShockerRank.Rank.SHADOW_LEADER.ordinal();
    }
}