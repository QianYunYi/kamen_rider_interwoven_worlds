package com.qianyunyi.rider.entity.kaijin.shocker.base;


import com.qianyunyi.rider.entity.kaijin.shocker.ai.ShockerScientistAI;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 科学家基类
 */
public abstract class ShockerScientistBase extends ShockerBase {

    // 受惊状态
    private boolean panicked;
    @Nullable
    private LivingEntity panicTarget;

    public ShockerScientistBase(EntityType<? extends ShockerScientistBase> type, Level world) {
        super(type, world);
        this.panicked = false;
        this.panicTarget = null;
        this.setRank(ShockerRank.Rank.SCIENTIST);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new ShockerScientistAI.ScientistHurtGoal(this));

        // === 行为Goal（按优先级排序）===

        // 优先级1：逃跑（最高优先级，保命第一）
        this.goalSelector.addGoal(1, new ShockerScientistAI.FleeGoal(this));

        // 优先级2：辅助友军（安全时给增益）
        this.goalSelector.addGoal(2, new ShockerScientistAI.SupportGoal(this));

        // 优先级3：跟随干部（有干部时跟随）
        this.goalSelector.addGoal(3, new ShockerScientistAI.FollowCadreGoal(this));

        // 优先级4：研究（完全安全时进行研究）
        this.goalSelector.addGoal(4, new ShockerScientistAI.ResearchGoal(this));

        // 优先级5：随机游走（无所事事时）
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.4));

        // 优先级6：看向随机方向
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 优先级7：游泳
        this.goalSelector.addGoal(7, new FloatGoal(this));
    }

    // 科学家不会攻击
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    // 科学家不会追击任何目标
    @Override
    public void setTarget(LivingEntity target) {
        // 不设置任何攻击目标
    }
    public boolean isPanicked() {
        return panicked;
    }

    public void setPanicked(boolean panicked) {
        this.panicked = panicked;
    }

    @Nullable
    public LivingEntity getPanicTarget() {
        return panicTarget;
    }

    public void setPanicTarget(@Nullable LivingEntity target) {
        this.panicTarget = target;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Panicked", panicked);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Panicked")) {
            this.panicked = compound.getBoolean("Panicked");
        }
    }
}