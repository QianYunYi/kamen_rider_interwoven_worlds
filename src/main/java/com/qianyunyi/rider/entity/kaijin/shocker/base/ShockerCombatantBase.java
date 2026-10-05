package com.qianyunyi.rider.entity.kaijin.shocker.base;

import com.qianyunyi.rider.entity.kaijin.shocker.ai.ShockerCombatantAIGoals;
import com.qianyunyi.rider.entity.kaijin.shocker.ai.ShockerCombatantReinforcementAI;
import com.qianyunyi.rider.entity.kaijin.shocker.ai.ShockerCombatantSquadAI;
import com.qianyunyi.rider.entity.kaijin.shocker.data.ShockerRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// 通用战斗员基类 —— 全组织通用小队系统
public abstract class ShockerCombatantBase extends ShockerBase {
    // 同步数据
    private static final EntityDataAccessor<Boolean> CALLED_REINFORCEMENTS =
            SynchedEntityData.defineId(ShockerCombatantBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SQUAD_SIZE =
            SynchedEntityData.defineId(ShockerCombatantBase.class, EntityDataSerializers.INT);

    // 小队通用配置（可被子类重写）
    protected float getSquadLeaderChance() { return 0.15f; } // 小队长概率
    protected int getMinSquadSize() { return 3; } // 最小队员数
    protected int getMaxSquadSize() { return 5; } // 最大队员数
    protected double getFormationRange() { return 15.0D; } // 编队范围

    // 冷却时间
    private long lastReinforcementCallTime;
    private long lastCommandTime;
    private long lastInspireTime;
    private final List<UUID> squadMembers = new ArrayList<>();

    public ShockerCombatantBase(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);

        if (source.getEntity() instanceof Player player) {
//            // === 核心：先判断是否在侵略中 ===
//            boolean inRaid = isPlayerInShockerRaid(player);
//
//            if (!inRaid) {
//                // 不在侵略中：正常处理
//                if (isSquadLeader()) {
//                    // 杀死小队长 → 给予通缉令 + 召唤干部
//                    player.addEffect(new MobEffectInstance(
//                            RiderAdventureAffiliatedModMobEffects.SHOCKER_WANTED_NOTICE.get(),
//                            100000, 1, false, false
//                    ));
//                    ShockerSpawnHelper.trySpawnRandomCadreOnDeath(this, 0.05f);
//                } else {
//                    // 杀死普通战斗员 → 只召唤干部
//                    ShockerSpawnHelper.trySpawnRandomCadreOnDeath(this, 0.05f);
//                }
//            }
            // 如果在侵略中，什么都不做（不召唤干部，不给通缉令）
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CALLED_REINFORCEMENTS, false);
        entityData.define(SQUAD_SIZE, 0);
    }

    // 注册小队AI
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new ShockerCombatantAIGoals.FollowLeaderGoal(this));
        this.goalSelector.addGoal(5, new ShockerCombatantAIGoals.SquadCommandGoal(this));
        this.goalSelector.addGoal(6, new ShockerCombatantAIGoals.SquadInspireGoal(this));
        this.goalSelector.addGoal(7, new ShockerCombatantAIGoals.SquadReformGoal(this));
    }

    // 受伤呼叫增援
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide() && source.getEntity() instanceof LivingEntity attacker) {
            if (!isFriendly(attacker)) {
//                if (attacker instanceof Player player && JawOfAllOrganizationsOOOOOOHelper.hasJawOfAllOrganizationsOOOOOO(player)) {
//                    return false;
//                }
                ShockerCombatantReinforcementAI.tryCallReinforcements(this, attacker);
                notifyFriendlyUnits(attacker);
                if (isSquadCommander()) {
                    ShockerCombatantSquadAI.inspireSquad(this);
                }
            }
        }
        return result;
    }

    // 通知友军
    private void notifyFriendlyUnits(LivingEntity attacker) {
        List<ShockerCombatantBase> nearby = level().getEntitiesOfClass(
                ShockerCombatantBase.class, getBoundingBox().inflate(30),
                e -> e != this && e.isAlive() && isFriendly(e)
        );
        for (ShockerCombatantBase helper : nearby) {
            if (helper.getTarget() == null) {
                helper.setTarget(attacker);
                if (helper.isSquadLeader()) {
                    ShockerCombatantSquadAI.issueCommand(helper);
                }
            }
        }
    }

    // ====================== 通用小队核心逻辑（所有战斗员共用） ======================

    /**
     * 生成时：初始化队长 + 生成小队 + 编队附近队员
     * 子类只需要调用 super.finalizeSpawn 即可
     */
    public SpawnGroupData finalizeSpawnBase(ServerLevelAccessor accessor, MobSpawnType reason) {
        if (!level().isClientSide) {
            boolean isCaptain = rollSquadLeader();

            // 设置队长等级
            if (isCaptain) {
                setRank(ShockerRank.Rank.SQUAD_LEADER);
                setCustomName(getSquadLeaderName());
                setCustomNameVisible(true);
            } else {
                setRank(ShockerRank.Rank.COMBATANT);
            }

            // 队长：生成小队 + 编队附近同类型队员
            if (isCaptain) {
                spawnSquadMembers(accessor);
                formSquadFromNearby();
            }
        }
        return null;
    }

    // 随机队长
    private boolean rollSquadLeader() {
        return getRandom().nextFloat() < getSquadLeaderChance();
    }

    // 生成同类型小队成员（通用）
    private void spawnSquadMembers(ServerLevelAccessor accessor) {
        int count = getMinSquadSize() + getRandom().nextInt(getMaxSquadSize() - getMinSquadSize() + 1);
        for (int i = 0; i < count; i++) {
            ShockerCombatantBase member = createNewCombatantInstance();
            if (member == null) continue;

            // 随机位置
            member.setPos(getX() + (random.nextDouble() - 0.5) * 4, getY(), getZ() + (random.nextDouble() - 0.5) * 4);
            member.setOrgId(this.getOrgId());
            member.setRank(ShockerRank.Rank.COMBATANT);

            // 子类实现：设置纹理/属性/武器
            setupMemberFromLeader(member);

            this.addSquadMember(member);
            accessor.addFreshEntity(member);
        }
    }

    // 编队附近同类型无小队成员
    private void formSquadFromNearby() {
        AABB box = getBoundingBox().inflate(getFormationRange());
        List<? extends ShockerCombatantBase> list = level().getEntitiesOfClass(
                getClass(), box, e -> e != this && e.isAlive() && e.getSquadSize() == 0
        );

        int max = 3;
        for (int k = 0; k < Math.min(list.size(), max); k++) {
            ShockerCombatantBase member = list.get(k);
            setupMemberFromLeader(member);
            this.addSquadMember(member);
        }
    }

    // ====================== 子类必须重写的方法 ======================

    /**
     * 创建当前类型的战斗员实例（必须重写）
     */
    protected abstract ShockerCombatantBase createNewCombatantInstance();

    /**
     * 队长给队员统一设置纹理/属性/装备（必须重写）
     */
    protected abstract void setupMemberFromLeader(ShockerCombatantBase member);

    /**
     * 小队长显示名称（必须重写）
     */
    protected abstract net.minecraft.network.chat.Component getSquadLeaderName();

    // ====================== 小队工具方法 ======================

    // 是否是小队长
    public boolean isSquadCommander() {
        // 小队长：等级为战斗员小队长 + 小队成员数量大于0
        return getRank() == ShockerRank.Rank.SQUAD_LEADER && getSquadSize() > 0;
    }

    // 是否是小队长
    public boolean isSquadLeader() {
        return getRank() == ShockerRank.Rank.SQUAD_LEADER;
    }

    public int getSquadSize() {
        return entityData.get(SQUAD_SIZE);
    }

    public void setSquadSize(int size) {
        entityData.set(SQUAD_SIZE, Math.min(size, 6));
    }

    public boolean hasCalledReinforcements() {
        return entityData.get(CALLED_REINFORCEMENTS);
    }

    public void setCalledReinforcements(boolean flag) {
        entityData.set(CALLED_REINFORCEMENTS, flag);
    }

    public long getLastReinforcementCallTime() { return lastReinforcementCallTime; }
    public void setLastReinforcementCallTime(long time) { this.lastReinforcementCallTime = time; }
    public long getLastCommandTime() { return lastCommandTime; }
    public void setLastCommandTime(long time) { this.lastCommandTime = time; }
    public long getLastInspireTime() { return lastInspireTime; }
    public void setLastInspireTime(long time) { this.lastInspireTime = time; }
    public List<UUID> getSquadMembers() { return new ArrayList<>(squadMembers); }

    public void addSquadMember(LivingEntity entity) {
        if (entity instanceof ShockerCombatantBase && !squadMembers.contains(entity.getUUID())) {
            squadMembers.add(entity.getUUID());
            setSquadSize(squadMembers.size());
        }
    }

    // 获取小队成员
    public List<ShockerCombatantBase> getSquadCombatants() {
        List<ShockerCombatantBase> list = new ArrayList<>();
        for (UUID uuid : squadMembers) {
            if (level().getServer() != null && level().getServer() != null) {
                if (Objects.requireNonNull(level().getServer().getLevel(level().dimension())).getEntity(uuid) instanceof ShockerCombatantBase s) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    public void removeSquadMember(UUID uuid) {
        squadMembers.remove(uuid);
        setSquadSize(squadMembers.size());
    }

    // NBT 保存小队数据
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SquadSize", getSquadSize());
        nbt.putBoolean("CalledReinforcements", hasCalledReinforcements());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setSquadSize(nbt.getInt("SquadSize"));
        setCalledReinforcements(nbt.getBoolean("CalledReinforcements"));
    }
}