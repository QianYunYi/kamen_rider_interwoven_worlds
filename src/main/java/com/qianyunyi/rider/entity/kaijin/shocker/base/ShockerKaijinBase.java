package com.qianyunyi.rider.entity.kaijin.shocker.base;


import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

// 怪人基类 → 所有怪人继承此类
public abstract class ShockerKaijinBase extends ShockerBase {

    public ShockerKaijinBase(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        // 怪人默认是干部级
        this.setRank(ShockerRank.Rank.KAIJIN);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD; // 怪人默认亡灵，可改
    }
}