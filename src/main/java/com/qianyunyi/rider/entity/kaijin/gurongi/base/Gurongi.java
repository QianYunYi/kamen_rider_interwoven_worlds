package com.qianyunyi.rider.entity.kaijin.gurongi.base;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameManager.ParticipantEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

/**
 * 古朗基基类。
 * <p>
 * 所有古朗基族怪物都应该继承此类。
 * 包含以下核心系统：
 * <ul>
 *   <li>集团系统（GurongiGroup）- 7个集团等级</li>
 *   <li>纹理系统 - 基于集团的纹理路径自动构造</li>
 *   <li>基基鲁游戏系统（Gegeru）- 游戏目标、阶段、状态</li>
 *   <li>杀戮欲望系统（KillingDesire）- 非游戏期间的攻击概率</li>
 *   <li>狩猎法则系统（GurongiHuntingLaw）- 社会规则的强制执行</li>
 *   <li>集团升阶系统 - 完成基基鲁游戏后升阶，属性 ×1.2，纹理切换</li>
 * </ul>
 */
public abstract class Gurongi extends Monster implements GeoEntity {

    // ==================== 动画工厂 ====================

    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    // ==================== 基础属性 ====================

    /** 所属集团 */
    public GurongiGroup group = null;
    /** 最后攻击者的UUID */
    protected UUID lastDamager;
    /** 非虚空伤害减免（50%），达古巴等特殊个体设为 false 禁用 */
    protected boolean hasDamageReduction = true;

    /** 是否为狼王转换的古朗基（不存档也不读档） */
    protected boolean isWolfKingConverted = false;

    /**
     * 标记移除是否已通过游戏逻辑处理（死亡/完成/逃脱）。
     * <p>
     * 用于 {@link #onRemovedFromWorld()} 中区分：
     * <ul>
     *   <li>{@code true} - 游戏逻辑主动移除（死亡、目标完成晋升、逃脱消失），不清理管理器</li>
     *   <li>{@code false} - 自然消失（玩家远离导致 despawn 或区块卸载），需从管理器移除以允许重新生成</li>
     * </ul>
     */
    protected boolean removalHandledByGame = false;

    /** 是否可重新生成（被玩家杀死后设为 false） */
    private static final EntityDataAccessor<Boolean> RESPAWNABLE =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.BOOLEAN);

    // ==================== 纹理系统 ====================

    private static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.STRING);

    // ==================== 基鲁鲁游戏计数 ====================

    private static final EntityDataAccessor<Integer> GURONGI_GAME_COUNT =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.INT);

    // ==================== 基基鲁游戏目标系统 ====================

    protected GurongiGameObjective gameObjective = null;
    protected int objectiveProgress = 0;
    protected boolean objectiveInitialized = false;

    // ==================== ★ 杀戮欲望系统 ====================

    /**
     * 杀戮欲望值（0.0 ~ 1.0）。
     * <p>
     * 在非游戏期间，当目标是人类时，以 killingDesire 的百分比概率
     * 选择人类目标进行攻击（违反社会规则的行为）。
     */
    private static final EntityDataAccessor<Float> KILLING_DESIRE =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.FLOAT);

    // ==================== ★ 基基鲁游戏状态 ====================

    /**
     * 当前基基鲁游戏状态。
     */
    private static final EntityDataAccessor<String> GAME_STATUS =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.STRING);

    /**
     * 当前基基鲁游戏阶段。
     */
    private static final EntityDataAccessor<String> GAME_STAGE =
            SynchedEntityData.defineId(Gurongi.class, EntityDataSerializers.STRING);

    // ==================== 构造与初始化 ====================

    public Gurongi(EntityType<? extends Monster> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE, "");
        this.entityData.define(GURONGI_GAME_COUNT, 0);
        this.entityData.define(KILLING_DESIRE, 0.0f);
        this.entityData.define(GAME_STATUS, GurongiGameStatus.NONE.name());
        this.entityData.define(GAME_STAGE, GurongiGameStage.NONE.name());
        this.entityData.define(RESPAWNABLE, true);
    }

    public static void init() {
    }

    // ==================== GeoEntity 实现 ====================

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    // ==================== 纹理 ====================

    /**
     * 获取纹理标识符（不含路径和扩展名）。
     * <p>
     * 格式示例：{@code "zu/zu_baduu_ba"} 或 {@code "me/me_baduu_ba"}。
     * 模型层使用 {@code "textures/entities/gurongi/" + getTexture() + ".png"} 构造完整纹理路径。
     */
    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    /**
     * 设置纹理标识符。
     *
     * @param texture 纹理标识符，如 {@code "zu/zu_baduu_ba"}
     */
    public void setTexture(String texture) {
        this.entityData.set(TEXTURE, texture);
    }

    // ==================== 重生控制 ====================

    /**
     * @return 是否可重新生成（玩家杀死后不可重生）
     */
    public boolean isRespawnable() {
        return this.entityData.get(RESPAWNABLE);
    }

    /**
     * 设置是否可重新生成。被玩家杀死后设为 false。
     */
    public void setRespawnable(boolean respawnable) {
        this.entityData.set(RESPAWNABLE, respawnable);
    }

    // ==================== 杀戮欲望 ====================

    public float getKillingDesire() {
        return this.entityData.get(KILLING_DESIRE);
    }

    public void setKillingDesire(float desire) {
        this.entityData.set(KILLING_DESIRE, Math.max(0.0f, Math.min(1.0f, desire)));
    }

    /**
     * 初始化杀戮欲望值。
     * <p>
     * 滋集团：50%概率为0%，50%概率为1%（0.01）。
     * 其他集团默认0（可由子类覆写）。
     */
    protected void initKillingDesire() {
        if (group == GurongiGroup.ZU) {
            this.setKillingDesire(this.random.nextBoolean() ? 0.0f : 0.01f);
        } else {
            this.setKillingDesire(0.0f);
        }
    }

    // ==================== 基基鲁游戏状态 ====================

    public GurongiGameStatus getGameStatus() {
        String name = this.entityData.get(GAME_STATUS);
        try {
            return GurongiGameStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GurongiGameStatus.NONE;
        }
    }

    public void setGameStatus(GurongiGameStatus status) {
        this.entityData.set(GAME_STATUS, status.name());
    }

    public GurongiGameStage getGameStage() {
        String name = this.entityData.get(GAME_STAGE);
        try {
            return GurongiGameStage.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GurongiGameStage.NONE;
        }
    }

    public void setGameStage(GurongiGameStage stage) {
        this.entityData.set(GAME_STAGE, stage.name());
    }

    public boolean isInActiveGame() {
        return getGameStatus() == GurongiGameStatus.ACTIVE;
    }

    public boolean isDisqualified() {
        return getGameStatus() == GurongiGameStatus.DISQUALIFIED;
    }

    /**
     * 检查此古朗基是否处于"自己的游戏回合"中。
     * <p>
     * 当此古朗基注册在游戏管理器中，且管理器当前回合指向此古朗基时，
     * 返回 {@code true}。在自身游戏回合中，古朗基可以按游戏规则攻击目标，
     * 无需进行杀戮欲望判断。
     *
     * @return 是否是当前活跃的回合参赛者
     */
    public boolean isCurrentTurnParticipant() {
        if (this.level().isClientSide()) return false;
        if (!(this.level() instanceof ServerLevel serverLevel)) return false;
        GurongiGameManager manager = GurongiGameManager.get(serverLevel);
        UUID currentUuid = manager.getCurrentTurnParticipant();
        return this.getUUID().equals(currentUuid);
    }

    // ==================== 目标类型工具 ====================

    /**
     * 判断目标是否为"人类"（林多或灾厄村民）。
     *
     * @see GurongiHuntingLaw#isLinto(LivingEntity)
     * @see GurongiHuntingLaw#isIllager(LivingEntity)
     */
    public static boolean isHumanTarget(LivingEntity entity) {
        return GurongiHuntingLaw.isHuman(entity);
    }

    // ==================== 基基鲁游戏目标 ====================

    public GurongiGameObjective getGameObjective() {
        return gameObjective;
    }

    public void setGameObjective(GurongiGameObjective objective) {
        this.gameObjective = objective;
        this.objectiveProgress = 0;
        this.objectiveInitialized = true;
    }

    /**
     * @return 目标是否已初始化（无论成功与否）
     */
    public boolean isObjectiveInitialized() {
        return objectiveInitialized;
    }

    public int getObjectiveProgress() {
        return objectiveProgress;
    }

    public boolean isObjectiveComplete() {
        return gameObjective != null && objectiveProgress >= gameObjective.getTargetCount();
    }

    /**
     * 分配游戏目标（根据当前集团）。
     * <p>
     * 供 {@link GurongiGameManager#assignObjectivesToAllParticipants} 在阶段推进时调用。
     */
    public void assignGameObjective() {
        if (objectiveInitialized) return;
        this.gameObjective = GurongiGameObjective.forGroup(this.group);
        this.objectiveProgress = 0;
        this.objectiveInitialized = this.gameObjective != null;
    }

    /**
     * 检查是否有有效的游戏目标（非空且集团不是恩集团）。
     * 恩集团（N 集团，即 0 号）没有游戏目标，不参与基基鲁游戏。
     */
    public boolean hasValidGameObjective() {
        return gameObjective != null && group != GurongiGroup.N;
    }

    /**
     * 当古朗基通过目标条件击杀目标时调用。
     * 此方法会先通过 {@link GurongiHuntingLaw} 检查合法性。
     *
     * @param target 被击杀的目标实体
     * @return true 如果目标已完成
     */
    public boolean onObjectiveKill(LivingEntity target) {
        if (gameObjective == null) return false;

        if (!this.level().isClientSide()) {
            GurongiHuntingLaw.CheckResult lawCheck = GurongiHuntingLaw.canKill(this, target);
            if (lawCheck == GurongiHuntingLaw.CheckResult.PENALTY) {
                GurongiHuntingLaw.applyPenalty(this, "违规杀死了目标");
                return false;
            }
            if (lawCheck == GurongiHuntingLaw.CheckResult.DENY) {
                KamenRiderInterwovenWorldsMod.LOGGER.debug(
                        "[Gurongi] {} 试图杀死 {} 但被狩猎法则禁止",
                        this.getName().getString(), target.getName().getString()
                );
                return false;
            }
        }

        this.objectiveProgress++;
        this.incrementGurongiGameCount();

        if (!this.level().isClientSide()) {
            // 同步到世界管理器
            if (this.level() instanceof ServerLevel serverLevel) {
                GurongiGameManager manager = GurongiGameManager.get(serverLevel);
                manager.incrementKillCount(this.getUUID());
            }
        }

        if (isObjectiveComplete()) {
            onObjectiveCompleted();
            return true;
        }
        return false;
    }

    /**
     * 目标完成时的回调。
     * <p>
     * 行为：
     * <ol>
     *   <li>游戏状态推进为 COMPLETED</li>
     *   <li>自动触发集团升阶</li>
     *   <li>保存属性到管理器</li>
     *   <li>从世界清除（晋升后将在新阶段重新生成）</li>
     * </ol>
     */
    protected void onObjectiveCompleted() {
        if (!this.level().isClientSide()) {
            this.setGameStatus(GurongiGameStatus.COMPLETED);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[Gurongi] {} 完成了基基鲁游戏目标: {} (击杀数: {})",
                    this.getName().getString(),
                    gameObjective != null ? gameObjective.getDescription() : "?",
                    objectiveProgress
            );

            // ★ 只有已注册参赛者的古朗基完成目标才触发顺位逻辑
            // 狼王转换和刷怪蛋召唤的古朗基未注册，不触发
            if (this.level() instanceof ServerLevel serverLevel) {
                GurongiGameManager manager = GurongiGameManager.get(serverLevel);
                if (manager.isRegistered(this.getUUID())) {
                    manager.onParticipantCompleted(this.getUUID(), serverLevel);
                }
            }

            // 检查是否已达到最高集团（N 或 NU）
            GurongiGroup nextGroup = getNextGroup();
            if (nextGroup == null) {
                // 最高集团，保留在世界中
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] {} 已是最集团，保留在世界中", this.getName().getString()
                );
                return;
            }

            // 1. 先晋升（更新属性、纹理等）
            promoteToNextGroup();

            // 2. 保存属性到管理器
            if (this.level() instanceof ServerLevel serverLevel) {
                GurongiGameManager manager = GurongiGameManager.get(serverLevel);

                CompoundTag data = new CompoundTag();
                saveToManagerTag(data);
                manager.saveGurongiData(this.getUUID(), data, this.group, GurongiGameStatus.PROMOTED, getGameStage());

                // 标记该古朗基需要在新阶段重新生成
                manager.markGurongiForRespawn(this.getUUID(), this.group);

                // ★ 保存类型级持久数据（跨实例共享）
                ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
                String typeName = typeKey != null ? typeKey.getPath() : null;
                if (typeName != null) {
                    manager.saveTypeGameData(typeName, objectiveProgress,
                            this.group, getGameStage(), false);
                    KamenRiderInterwovenWorldsMod.LOGGER.info(
                            "[Gurongi] 类型级数据已存档: {} (击杀: {}, 晋升至: {})",
                            typeName, objectiveProgress, this.group
                    );
                }

                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] {} 晋升为 {}，属性已存档，即将从世界清除并重新生成",
                        this.getName().getString(), this.group.name()
                );
            }

            // ★ 标记：目标完成晋升是游戏逻辑主动移除，告知 onRemovedFromWorld 不做清理
            this.removalHandledByGame = true;

            // 3. 从世界清除
            this.discard();
        }
    }

    /**
     * 判断给定的实体实例是否匹配当前游戏目标的击杀条件。
     * <p>
     * 所有"人类"（玩家、村民、灾厄村民）均匹配。
     */
    public boolean isEntityValidObjectiveTarget(LivingEntity victim) {
        if (gameObjective == null) return false;
        return gameObjective.matchesTarget(victim);
    }

    // ==================== 基鲁鲁游戏计数 ====================

    public int getGurongiGameCount() {
        return this.entityData.get(GURONGI_GAME_COUNT);
    }

    public void setGurongiGameCount(int count) {
        this.entityData.set(GURONGI_GAME_COUNT, count);
    }

    public void incrementGurongiGameCount() {
        int current = getGurongiGameCount();
        this.entityData.set(GURONGI_GAME_COUNT, current + 1);
    }

    // ==================== ★ 集团升阶系统 ====================

    /**
     * 获取此古朗基集团对应的下一晋升集团。
     * <p>
     * 晋升顺序：BE → ZU → ME → GO → RA → N
     */
    @Nullable
    public GurongiGroup getNextGroup() {
        if (group == null) return null;
        switch (group) {
            case BE:  return GurongiGroup.ZU;
            case ZU:  return GurongiGroup.ME;
            case ME:  return GurongiGroup.GO;
            case GO:  return GurongiGroup.RA;
            case RA:  return GurongiGroup.N;
            default:  return null;  // NU 和 N 无法晋升
        }
    }

    /**
     * 晋升到下一集团。
     * <p>
     * 效果：
     * <ol>
     *   <li>集团变更为下一等级</li>
     *   <li>纹理切换为对应集团纹理（如 zu_baduu_ba → me_baduu_ba）</li>
     *   <li>所有基础属性 ×1.2</li>
     *   <li>满血恢复</li>
     *   <li>游戏状态设为 PROMOTED</li>
     *   <li>同步到世界管理器</li>
     * </ol>
     */
    public void promoteToNextGroup() {
        if (this.level().isClientSide()) return;
        if (group == null) return;

        GurongiGroup nextGroup = getNextGroup();
        if (nextGroup == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[Gurongi] {} 已是最高集团，无法继续晋升", this.getName().getString()
            );
            return;
        }

        // 1. 更新纹理
        updateTextureForGroup(nextGroup);

        // 2. 更新集团
        this.group = nextGroup;

        // 3. 属性 ×1.2
        applyAttributeMultiplier(1.2);

        // 4. 满血恢复
        this.setHealth(this.getMaxHealth());

        // 5. 状态推进
        this.setGameStatus(GurongiGameStatus.PROMOTED);

        // 6. 同步到世界管理器
        if (this.level() instanceof ServerLevel serverLevel) {
            GurongiGameManager manager = GurongiGameManager.get(serverLevel);
            manager.updateParticipantStatus(this.getUUID(), GurongiGameStatus.PROMOTED);
        }

        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[Gurongi] {} 晋升为 {} 集团！属性 ×1.2", this.getName().getString(), nextGroup.name()
        );
    }

    /**
     * 根据新集团更新纹理路径。
     * <p>
     * 默认实现从当前纹理名称中提取短名称并重新构造。
     * 例如：{@code "zu/zu_baduu_ba"} → {@code "me/me_baduu_ba"}
     *
     * @param newGroup 目标集团
     */
    protected void updateTextureForGroup(GurongiGroup newGroup) {
        String currentTexture = getTexture();
        if (currentTexture == null || currentTexture.isEmpty()) return;

        // 提取短名称：去掉集团前缀（如 "zu/zu_"），得到 "baduu_ba"
        // 纹理格式为 "{group_lower}/{group_lower}_{shortName}"
        int slashIndex = currentTexture.indexOf('/');
        if (slashIndex < 0) return;
        String shortName = currentTexture.substring(slashIndex + 1);          // "zu_baduu_ba"
        int underscoreIndex = shortName.indexOf('_');
        if (underscoreIndex < 0) return;
        shortName = shortName.substring(underscoreIndex + 1);                  // "baduu_ba"

        String groupPrefix = newGroup.name().toLowerCase();
        String newTexture = groupPrefix + "/" + groupPrefix + "_" + shortName;
        setTexture(newTexture);
    }

    /**
     * 对古朗基的所有基础属性应用倍率。
     * <p>
     * 影响的属性：MAX_HEALTH, ATTACK_DAMAGE, MOVEMENT_SPEED, ARMOR, FOLLOW_RANGE
     *
     * @param multiplier 倍率（如 1.2 表示 +20%）
     */
    protected void applyAttributeMultiplier(double multiplier) {
        applyAttributeMultiplier(Attributes.MAX_HEALTH, multiplier);
        applyAttributeMultiplier(Attributes.ATTACK_DAMAGE, multiplier);
        applyAttributeMultiplier(Attributes.MOVEMENT_SPEED, multiplier);
        applyAttributeMultiplier(Attributes.ARMOR, multiplier);
        applyAttributeMultiplier(Attributes.FOLLOW_RANGE, multiplier);
    }

    /**
     * 对单个属性应用倍率。
     */
    protected void applyAttributeMultiplier(net.minecraft.world.entity.ai.attributes.Attribute attribute, double multiplier) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            double newValue = instance.getBaseValue() * multiplier;
            instance.setBaseValue(newValue);
        }
    }

    // ==================== ★ 玩家放置标记 ====================

    /**
     * 是否为玩家主动生成（创造模式或刷怪蛋放置）。
     * <p>
     * 玩家放置的古朗基会调用 {@link #setPersistenceRequired()} 防止
     * Minecraft 原版区块卸载时的自然消失。
     */
    protected boolean playerPlaced = false;

    /**
     * 标记该古朗基为玩家主动放置（创造模式/刷怪蛋/指令生成）。
     * 被标记后不会因区块卸载而自然消失。
     */
    public void markAsPlayerPlaced() {
        this.playerPlaced = true;
        setPersistenceRequired();
    }

    /**
     * @return 是否为玩家主动放置的实体
     */
    public boolean isPlayerPlaced() {
        return playerPlaced;
    }

    @Override
    public void tick() {
        super.tick();
        // 移除自定义离视线消失逻辑：使用 Minecraft 原版行为，
        // 未标记 persistenceRequired 的古朗基会在区块卸载时自然消失（与僵尸等原版怪物一致）。
    }

    /**
     * 将古朗基的持久化数据写入 NBT（供管理器存档用）。
     */
    protected void saveToManagerTag(CompoundTag tag) {
        tag.putString("Texture", getTexture());
        tag.putInt("gurongiGameCount", getGurongiGameCount());
        tag.putString("group", group != null ? group.name() : "");
        if (gameObjective != null) {
            tag.putString("gameObjective", gameObjective.name());
            tag.putInt("objectiveProgress", objectiveProgress);
            tag.putBoolean("objectiveInitialized", objectiveInitialized);
        }
        tag.putFloat("killingDesire", getKillingDesire());
        // 保存当前生命值和属性
        tag.putDouble("health", this.getHealth());
        tag.putDouble("maxHealth", this.getMaxHealth());
        tag.putDouble("attackDamage", this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        tag.putDouble("movementSpeed", this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        tag.putDouble("armor", this.getAttributeValue(Attributes.ARMOR));
    }

    // ==================== ★ 死亡处理（玩家击杀 → 不可重生） ====================

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                GurongiGameManager manager = GurongiGameManager.get(serverLevel);

                // 检查是否为玩家击杀
                if (source.getEntity() instanceof Player) {
                    this.setRespawnable(false);
                    manager.removeGurongiRespawnData(this.getUUID());
                    KamenRiderInterwovenWorldsMod.LOGGER.info(
                            "[Gurongi] {} 被玩家杀死，不再重生", this.getName().getString()
                    );

                    // ★ 如果是晋升实体在晋升后阶段死亡，标记其类型为当前阶段已耗尽机会
                    ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
                    if (entityKey != null) {
                        String typePath = entityKey.getPath();
                        GurongiGameManager.TypeGameData typeData = manager.getTypeGameData(typePath);
                        if (typeData != null && typeData.hasCompleted && typeData.promotedToGroup != null) {
                            // 该类型有晋升记录 → 是晋升实体，标记为当前阶段已耗尽
                            manager.markTypeExhaustedInStage(typePath);
                            KamenRiderInterwovenWorldsMod.LOGGER.info(
                                    "[Gurongi] 晋升实体 {} (类型: {}) 在 {} 阶段死亡，类型已标记为已耗尽",
                                    this.getName().getString(), typePath, manager.getGameStage()
                            );
                        }
                    }
                }

                // ★ 只有已注册参赛者的古朗基死亡才触发顺位逻辑
                // 狼王转换和刷怪蛋召唤的古朗基未注册，不触发
                if (manager.isRegistered(this.getUUID())) {
                    manager.onParticipantDied(this.getUUID(), serverLevel);
                }
            }
            // ★ 标记：死亡是游戏逻辑主动移除，告知 onRemovedFromWorld 不做清理
            this.removalHandledByGame = true;
        }
        super.die(source);
    }

    // ==================== ★ 自然消失检测：清理管理器，允许重新生成 ====================

    /**
     * 拦截所有移除操作，区分游戏逻辑移除 vs 自然消失。
     * <p>
     * <strong>为什么不用 {@code onRemovedFromWorld()}</strong>：该方法是客户端钩子，
     * 服务端实体自然消失（{@link Mob#checkDespawn()} → {@code remove(DISCARDED)}）
     * 不会触发它。
     * </p>
     * <p>
     * 移除原因判断：
     * <ul>
     *   <li>{@link Entity.RemovalReason#DISCARDED} + {@code !removalHandledByGame} → 自然消失</li>
     *   <li>{@link Entity.RemovalReason#DISCARDED} + {@code removalHandledByGame} → 游戏逻辑（晋升/逃脱）</li>
     *   <li>{@link Entity.RemovalReason#KILLED} → 死亡（已在 {@link #die} 中处理）</li>
     *   <li>{@link Entity.RemovalReason#UNLOADED_TO_CHUNK} → 区块卸载</li>
     * </ul>
     * </p>
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        // ★ 检测自然消失：DISCARDED（自然 despawn）或 UNLOADED_TO_CHUNK（区块卸载）
        // 且未通过游戏逻辑标记
        if (!this.level().isClientSide()
                && !removalHandledByGame
                && (reason == Entity.RemovalReason.DISCARDED
                        || reason == Entity.RemovalReason.UNLOADED_TO_CHUNK)
                && this.level() instanceof ServerLevel serverLevel) {
            GurongiGameManager manager = GurongiGameManager.get(serverLevel);
            if (manager.isRegistered(this.getUUID())) {
                manager.removeGurongiRespawnData(this.getUUID());
                manager.unregisterParticipant(this.getUUID());
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] {} 因自然消失（区块卸载/远离）从参赛者队列中移除，允许重新生成同类型古朗基",
                        this.getName().getString()
                );
            }
        }
        super.remove(reason);
    }

    // ==================== 数据持久化 ====================

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Texture", getTexture());
        compound.putInt("gurongiGameCount", getGurongiGameCount());

        if (gameObjective != null) {
            compound.putString("gameObjective", gameObjective.name());
            compound.putInt("objectiveProgress", objectiveProgress);
            compound.putBoolean("objectiveInitialized", objectiveInitialized);
        }

        compound.putFloat("killingDesire", getKillingDesire());
        compound.putString("gameStatus", getGameStatus().name());
        compound.putString("gameStage", getGameStage().name());
        compound.putBoolean("playerPlaced", playerPlaced);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        // 读取纹理
        if (compound.contains("Texture")) {
            setTexture(compound.getString("Texture"));
        }

        if (compound.contains("gurongiGameCount")) {
            setGurongiGameCount(compound.getInt("gurongiGameCount"));
        }

        if (compound.contains("gameObjective")) {
            this.gameObjective = GurongiGameObjective.fromString(compound.getString("gameObjective"));
            this.objectiveProgress = compound.getInt("objectiveProgress");
            this.objectiveInitialized = compound.getBoolean("objectiveInitialized");
        }

        if (compound.contains("killingDesire")) {
            setKillingDesire(compound.getFloat("killingDesire"));
        }

        if (compound.contains("gameStatus")) {
            try {
                setGameStatus(GurongiGameStatus.valueOf(compound.getString("gameStatus")));
            } catch (IllegalArgumentException ignored) {
                setGameStatus(GurongiGameStatus.NONE);
            }
        }
        if (compound.contains("gameStage")) {
            try {
                setGameStage(GurongiGameStage.valueOf(compound.getString("gameStage")));
            } catch (IllegalArgumentException ignored) {
                setGameStage(GurongiGameStage.NONE);
            }
        }

        if (compound.contains("playerPlaced")) {
            playerPlaced = compound.getBoolean("playerPlaced");
            if (playerPlaced) {
                setPersistenceRequired();
            }
        }

        // 读取狼王转换标记
        if (compound.contains("wolfKingId")) {
            this.isWolfKingConverted = true;
        }
    }

    // ==================== 首次生成：分配游戏目标与杀戮欲望 ====================

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        if (!level.isClientSide()) {
            // ★ 自然生成不在 finalizeSpawn 中分配目标，延迟到阶段推进时分配
            // 只有玩家放置（刷怪蛋/指令）的古朗基立即初始化目标
            if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
                if (!objectiveInitialized) {
                    this.gameObjective = GurongiGameObjective.forGroup(this.group);
                    this.objectiveProgress = 0;
                    this.objectiveInitialized = this.gameObjective != null;
                }
            }

            if (dataTag == null || !dataTag.contains("killingDesire")) {
                initKillingDesire();
            }
        }

        // 刷怪蛋/指令生成的古朗基标记为玩家放置，不自动消失
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) {
            markAsPlayerPlaced();
        }

        // ===== 自然生成的古朗基注册到基基鲁游戏参赛者队列 =====
        // 只有自然生成的（NATURAL/REINFORCEMENT）且非狼王转换的才注册为参赛者
        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel
                && group != GurongiGroup.N  // 恩集团不注册参赛者
                && !isWolfKingConverted
                && reason != MobSpawnType.SPAWN_EGG
                && reason != MobSpawnType.COMMAND) {
            GurongiGameManager manager = GurongiGameManager.get(serverLevel);

            // ★ 检查是否有类型级晋升数据（首次生成时为 null，晋升后重新生成时非 null）
            ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(this.getType());
            String typeId = entityKey != null ? entityKey.toString() : "unknown";
            String typeName = entityKey != null ? entityKey.getPath() : null;
            GurongiGameManager.TypeGameData typeData = typeName != null ? manager.getTypeGameData(typeName) : null;

            if (typeData != null && typeData.hasCompleted && typeData.promotedToGroup != null) {
                // ★ 晋升后重新生成：恢复集团、属性、击杀进度
                GurongiGroup promotedGroup = typeData.promotedToGroup;
                this.group = promotedGroup;
                updateTextureForGroup(promotedGroup);
                applyAttributeMultiplier(1.2);
                this.setHealth(this.getMaxHealth());
                this.objectiveProgress = Math.min(typeData.killCount, this.gameObjective != null ? this.gameObjective.getTargetCount() : typeData.killCount);

                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[Gurongi] {} 以晋升实体重新生成（集团: {}, 击杀继承: {}）",
                        this.getName().getString(), promotedGroup.name(), this.objectiveProgress
                );
            }

            if (!manager.isRegistered(this.uuid)) {
                GurongiGameStage stage = GurongiGameManager.getStageForGroup(this.group);
                if (stage != null && stage != GurongiGameStage.NONE) {
                    manager.registerParticipant(this, stage, typeId);

                    // ★ 同步更新实体自身的游戏状态和阶段，确保指令查询显示一致
                    this.setGameStatus(GurongiGameStatus.ACTIVE);
                    this.setGameStage(stage);
                }
            }
        }

        return data;
    }

    // ==================== 伤害减免 ====================

    private static final ResourceKey<DamageType> VOID_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "out_of_world")
    );

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (hasDamageReduction && !source.is(VOID_DAMAGE)) {
            amount *= 0.5f;
        }
        return super.hurt(source, amount);
    }

    // ==================== 属性构建 ====================

    /**
     * 创建古朗基的基础属性。
     * <p>
     * 默认值：
     * <ul>
     *   <li>最大生命值：100</li>
     *   <li>攻击力：10</li>
     *   <li>移动速度：0.23</li>
     *   <li>追踪范围：32</li>
     * </ul>
     */
    public static AttributeSupplier.Builder createGurongiAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
}