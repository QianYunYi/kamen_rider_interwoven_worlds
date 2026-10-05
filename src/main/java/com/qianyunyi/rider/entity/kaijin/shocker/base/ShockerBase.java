package com.qianyunyi.rider.entity.kaijin.shocker.base;


import com.qianyunyi.rider.entity.kaijin.shocker.ai.ShockerAIGoals;
import com.qianyunyi.rider.entity.kaijin.shocker.api.IShocker;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerOrg;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRelationship;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// 顶级抽象基类：通用属性 + 友军不互殴AI
public abstract class ShockerBase extends Monster implements IShocker {
    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(ShockerBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORG_ID = SynchedEntityData.defineId(ShockerBase.class, EntityDataSerializers.INT);

    public ShockerBase(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RANK, ShockerRank.Rank.COMBATANT.getId());
        entityData.define(ORG_ID, ShockerOrg.ORG_SHOCKER);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 统一使用父类AI，无重复
        this.targetSelector.addGoal(1, new ShockerAIGoals.ShockerHurtByTargetGoal(this));
        // 如果玩家佩戴饰品 → 视为 幕后首领（上级友好）
        this.targetSelector.addGoal(2, new ShockerAIGoals.ShockerAttackGoal<>(this, Player.class));
        this.targetSelector.addGoal(3, new ShockerAIGoals.ShockerAttackGoal<>(this, Villager.class));
        this.targetSelector.addGoal(3, new ShockerAIGoals.ShockerAttackGoal<>(this, Villager.class));
    }

    // 通用友军判断
    public boolean isFriendly(LivingEntity target) {
        if (!(target instanceof ShockerBase other)) return false;
        return ShockerOrg.areFriendly(this, other);
    }

    // 关系判断
    @Override
    public ShockerRelationship getRelationshipTo(LivingEntity target) {
        // 玩家佩戴饰品 → 视为 幕后首领（上级友好）
//        if (target instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//            return ShockerRelationship.FRIENDLY;
//        }

        if (target instanceof ShockerBase sb) {
            return ShockerOrg.getRelationship(getOrgId(), sb.getOrgId());
        }

        return ShockerRelationship.HOSTILE;
    }

    // Getter & Setter
    @Override
    public ShockerRank.Rank getRank() {
        return ShockerRank.Rank.fromId(entityData.get(RANK));
    }

    public void setRank(ShockerRank.Rank rank) {
        entityData.set(RANK, rank.getId());
    }

    @Override
    public int getOrgId() {
        return entityData.get(ORG_ID);
    }

    public void setOrgId(int id) {
        entityData.set(ORG_ID, id);
    }

    // 数据存档
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rank", entityData.get(RANK));
        tag.putInt("Org", entityData.get(ORG_ID));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Rank")) entityData.set(RANK, tag.getInt("Rank"));
        if (tag.contains("Org")) entityData.set(ORG_ID, tag.getInt("Org"));
    }
}