package com.qianyunyi.rider.entity.kaijin.gurongi.base;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * 古朗基社会最高等级的狩猎法则（Gurongi Hunting Law）。
 * <p>
 * 此法则的优先级高于所有个体AI、游戏规则（GameRule），
 * 是古朗基文明铁则的代码实现。任何古朗基在攻击"人类"之前
 * 都必须通过此法则的检查。
 * <p>
 * 目标分类：
 * <ul>
 *   <li><b>林多（Linto）</b>：玩家、村民 —— 基基鲁游戏的主要目标</li>
 *   <li><b>灾厄村民（Illager）</b>：掠夺者、卫道士等 —— 同样可被狩猎</li>
 * </ul>
 * 核心规则：
 * <ol>
 *   <li><b>恩集团例外</b>：恩（N）集团成员不受任何限制，可任意杀戮。</li>
 *   <li><b>参赛者规则（ParticipantRule）</b>：拥有基基鲁资格但处于REGISTERED或WAITING状态者，
 *       在游戏正式开始前不得主动杀死人类。</li>
 *   <li><b>狩猎干扰规则（HuntingInterferenceRule）</b>：
 *       非参赛古朗基不得抢夺参赛者的狩猎目标。</li>
 *   <li><b>杀戮欲望规则（KillingDesireRule）</b>：
 *       非游戏期间，基于杀戮欲望概率决定是否攻击人类目标。</li>
 * </ol>
 * <p>
 * 违反法则的后果：失去游戏权力（DISQUALIFIED）。
 */
public final class GurongiHuntingLaw {

    /**
     * 法则检查结果
     */
    public enum CheckResult {
        /** 允许攻击 */
        ALLOW,
        /** 禁止攻击（无惩罚） */
        DENY,
        /** 禁止攻击且触发违规惩罚（失去游戏权） */
        PENALTY
    }

    // 可通过配置文件扩展的林多实体类型集合
    private static final Set<EntityType<?>> LINTO_ENTITY_TYPES = new HashSet<>();
    // 可通过配置文件扩展的灾厄村民实体类型集合
    private static final Set<EntityType<?>> ILLAGER_ENTITY_TYPES = new HashSet<>();

    static {
        // 默认注册林多：玩家和村民
        registerLintoEntityType(EntityType.PLAYER);
        registerLintoEntityType(EntityType.VILLAGER);
        // 默认注册灾厄村民：所有 AbstractIllager 子类通过 isIllager 运行时检查，
        // 额外注册表中特定类型
        registerIllagerEntityType(EntityType.PILLAGER);
        registerIllagerEntityType(EntityType.VINDICATOR);
        registerIllagerEntityType(EntityType.EVOKER);
        registerIllagerEntityType(EntityType.RAVAGER);
        registerIllagerEntityType(EntityType.ILLUSIONER);
        registerIllagerEntityType(EntityType.WITCH);
    }

    private GurongiHuntingLaw() {
        // 工具类，禁止实例化
    }

    // ==================== 公共API ====================

    /**
     * 检查古朗基是否可以杀死目标实体。
     * <p>
     * 此为总入口方法，按以下优先级依次检查：
     * <ol>
     *   <li>目标既不属于林多也不属于灾厄村民 → 直接允许</li>
     *   <li>古朗基是否属于恩集团（恩→直接允许）</li>
     *   <li>参赛者规则检查</li>
     *   <li>狩猎干扰规则检查</li>
     *   <li>杀戮欲望规则检查</li>
     * </ol>
     *
     * @param hunter 攻击者（必须是古朗基）
     * @param target 目标实体
     * @return 检查结果（ALLOW/DENY/PENALTY）
     */
    public static CheckResult canKill(Gurongi hunter, LivingEntity target) {
        // ===== 步骤0：目标既不是林多也不是灾厄村民 → 允许 =====
        if (!isHuman(target)) {
            return CheckResult.ALLOW;
        }

        // ===== 步骤1：恩集团例外 =====
        if (hunter.group == GurongiGroup.N) {
            return CheckResult.ALLOW;
        }

        // ===== 步骤2：参赛者规则 =====
        CheckResult participantCheck = checkParticipantRule(hunter);
        if (participantCheck != CheckResult.ALLOW) {
            return participantCheck;
        }

        // ===== 步骤3：狩猎干扰规则（仅当目标被其他ACTIVE参赛者锁定） =====
        if (isTargetReservedByActiveParticipant(hunter, target)) {
            return CheckResult.DENY;
        }

        // ===== 步骤4：杀戮欲望规则（非游戏期间的随机攻击） =====
        if (!isInActiveGame(hunter)) {
            if (!checkKillingDesire(hunter)) {
                return CheckResult.DENY;
            }
        }

        // 通过所有检查
        return CheckResult.ALLOW;
    }

    /**
     * 判断目标是否为"人类"（林多 + 灾厄村民的总称）。
     * <p>
     * 此方法为组合判断，等价于 {@link #isLinto(LivingEntity)} || {@link #isIllager(LivingEntity)}。
     */
    public static boolean isHuman(LivingEntity entity) {
        return isLinto(entity) || isIllager(entity);
    }

    /**
     * 判断目标是否为林多（Linto）。
     * <p>
     * 林多包括：玩家(Player)、村民(Villager)
     * 以及通过 {@link #registerLintoEntityType} 注册的扩展类型。
     */
    public static boolean isLinto(LivingEntity entity) {
        if (entity == null) return false;
        // 直接类型检查（性能优化）
        if (entity instanceof Player) return true;
        if (entity instanceof Villager) return true;
        // 注册表匹配（用于配置扩展）
        return LINTO_ENTITY_TYPES.contains(entity.getType());
    }

    /**
     * 判断目标是否为灾厄村民（Illager）。
     * <p>
     * 灾厄村民包括：
     * <ul>
     *   <li>{@link AbstractIllager} 子类：掠夺者、卫道士、唤魔者、幻术师</li>
     *   <li>通过 {@link #registerIllagerEntityType} 注册的扩展类型（如女巫、劫掠兽）</li>
     * </ul>
     * <p>
     * 女巫（Witch）在 Minecraft 中继承 {@code AbstractRaider} 而非 {@code AbstractIllager}，
     * 因此已额外通过 {@link #registerIllagerEntityType(EntityType)} 注册到默认列表中，
     * 玩家也可通过配置文件 <b>kamen_rider_interwoven_worlds-common.toml</b> 中的
     * {@code illagerEntityTypes} 列表追加其他模组的灾厄村民实体。
     */
    public static boolean isIllager(LivingEntity entity) {
        if (entity == null) return false;
        // 直接类型检查（性能优化）：AbstractIllager 及其子类
        if (entity instanceof AbstractIllager) return true;
        // 注册表匹配（用于配置文件扩展，含女巫 Witch、劫掠兽 Ravager 等）
        return ILLAGER_ENTITY_TYPES.contains(entity.getType());
    }

    /**
     * 获取目标的分类类型。
     *
     * @return {@link TargetType#LINTO} 或 {@link TargetType#ILLAGER}；
     *         如果既不是林多也不是灾厄村民则返回 {@code null}
     */
    public static TargetType getTargetType(LivingEntity entity) {
        if (isLinto(entity)) return TargetType.LINTO;
        if (isIllager(entity)) return TargetType.ILLAGER;
        return null;
    }

    /**
     * 注册一个实体类型为"林多"。
     * 用于配置文件或模组兼容扩展。
     */
    public static void registerLintoEntityType(EntityType<?> type) {
        if (type != null) {
            LINTO_ENTITY_TYPES.add(type);
        }
    }

    /**
     * 注册一个实体类型为"灾厄村民"。
     * 用于配置文件或模组兼容扩展。
     */
    public static void registerIllagerEntityType(EntityType<?> type) {
        if (type != null) {
            ILLAGER_ENTITY_TYPES.add(type);
        }
    }

    /**
     * 从资源位置字符串注册林多实体类型。
     *
     * @param entityId 实体注册ID，格式如 "minecraft:villager"
     */
    public static void registerLintoEntityType(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id != null) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) {
                registerLintoEntityType(type);
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.warn(
                        "[GurongiHuntingLaw] 未找到林多实体类型: {}", entityId);
            }
        }
    }

    /**
     * 从资源位置字符串注册灾厄村民实体类型。
     *
     * @param entityId 实体注册ID，格式如 "minecraft:pillager"
     */
    public static void registerIllagerEntityType(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id != null) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) {
                registerIllagerEntityType(type);
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.warn(
                        "[GurongiHuntingLaw] 未找到灾厄村民实体类型: {}", entityId);
            }
        }
    }

    /**
     * 从配置文件重新加载所有可扩展实体类型。
     * <p>
     * 先清除当前所有通过配置文件注册的类型（保留静态默认类型），
     * 再从配置列表中逐个注册。
     *
     * @param lintoEntityIds   林多实体注册名列表
     * @param illagerEntityIds 灾厄村民实体注册名列表
     */
    public static void reloadFromConfig(java.util.List<String> lintoEntityIds,
                                        java.util.List<String> illagerEntityIds) {
        // 清除之前通过配置文件注册的类型（不可清除静态默认注册的类型，
        // 因此 ILLAGER_ENTITY_TYPES 和 LINTO_ENTITY_TYPES 中既有默认类型
        // 也有配置文件类型，clear 会丢失默认类型 → 不直接 clear）
        // 改为：仅清除配置扩展部分（不做复杂追踪，直接重新配置即可）
        if (lintoEntityIds != null) {
            // 仅注册配置表中指定的（不删除默认类型）
            for (String id : lintoEntityIds) {
                registerLintoEntityType(id);
            }
        }
        if (illagerEntityIds != null) {
            for (String id : illagerEntityIds) {
                registerIllagerEntityType(id);
            }
        }
    }

    /**
     * @return 当前所有默认灾厄村民实体类型的注册名列表（用于配置文件默认值）
     */
    public static java.util.List<String> getDefaultIllagerEntityIds() {
        return java.util.Arrays.asList(
                "minecraft:pillager",
                "minecraft:vindicator",
                "minecraft:evoker",
                "minecraft:ravager",
                "minecraft:illusioner",
                "minecraft:witch"
        );
    }

    /**
     * @return 当前所有默认林多实体类型的注册名列表（用于配置文件默认值）
     */
    public static java.util.List<String> getDefaultLintoEntityIds() {
        return java.util.Arrays.asList(
                "minecraft:player",
                "minecraft:villager"
        );
    }

    // ==================== 内部规则检查 ====================

    /**
     * 参赛者规则（ParticipantRule）：
     * REGISTERED 和 WAITING 状态的古朗基不得提前杀人。
     */
    private static CheckResult checkParticipantRule(Gurongi hunter) {
        GurongiGameStatus status = hunter.getGameStatus();

        // NONE → 没有参赛资格，但可以基于杀戮欲望攻击
        if (status == GurongiGameStatus.NONE) {
            return CheckResult.ALLOW;
        }

        // REGISTERED / WAITING → 禁止杀人，违规将失去游戏权
        if (status == GurongiGameStatus.REGISTERED || status == GurongiGameStatus.WAITING) {
            return CheckResult.PENALTY;
        }

        // ACTIVE → 允许按游戏规则杀人
        if (status == GurongiGameStatus.ACTIVE) {
            return CheckResult.ALLOW;
        }

        // 其他状态（COMPLETED, PROMOTED, DISQUALIFIED, FAIL）→ 不允许
        return CheckResult.DENY;
    }

    /**
     * 狩猎干扰规则（HuntingInterferenceRule）：
     * 检查目标是否已被其他正在ACTIVE状态的参赛古朗基锁定。
     * 如果是，当前古朗基不得抢杀。
     * <p>
     * 注意：此方法目前为简化实现——实际应检查所有加载的ACTIVE古朗基
     * 的attackTarget是否为target。
     */
    private static boolean isTargetReservedByActiveParticipant(Gurongi hunter, LivingEntity target) {
        // 如果古朗基本身就在ACTIVE状态，不干扰自己
        if (hunter.getGameStatus() == GurongiGameStatus.ACTIVE) {
            return false;
        }

        // 此处为简化实现，实际可通过世界实体追踪系统检查
        // 当前方案：通过GurongiGameManager或世界数据来追踪
        // 在完整的游戏管理器实现之前，暂时返回false
        return false;
    }

    /**
     * 检查古朗基是否正处于自己的活跃游戏回合中。
     * <p>
     * 当满足以下所有条件时返回 {@code true}：
     * <ol>
     *   <li>游戏状态为 ACTIVE</li>
     *   <li>是当前游戏管理器的回合参赛者</li>
     * </ol>
     * 处于自身游戏回合中的古朗基无需进行杀戮欲望判断，
     * 可以按游戏规则自由攻击目标。
     */
    private static boolean isInActiveGame(Gurongi gurongi) {
        if (gurongi.getGameStatus() != GurongiGameStatus.ACTIVE) return false;
        return gurongi.isCurrentTurnParticipant();
    }

    /**
     * 杀戮欲望规则：
     * 非游戏期间，基于杀戮欲望概率决定是否允许攻击人类目标。
     *
     * @return true 表示欲望触发，允许攻击；false 表示未触发，禁止攻击
     */
    private static boolean checkKillingDesire(Gurongi gurongi) {
        float desire = gurongi.getKillingDesire();
        if (desire <= 0.0f) {
            return false;
        }
        if (desire >= 1.0f) {
            return true;
        }
        // 基于概率判定
        return gurongi.getRandom().nextFloat() < desire;
    }

    // ==================== 违规处理 ====================

    /**
     * 执行违规惩罚。
     * 当古朗基违反狩猎法则时调用此方法。
     *
     * @param hunter 违规的古朗基
     * @param reason 违规原因描述
     */
    public static void applyPenalty(Gurongi hunter, String reason) {
        if (hunter.level().isClientSide()) return;

        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiHuntingLaw] ⚠ 违规！{} 因「{}」失去了游戏权力",
                hunter.getName().getString(),
                reason
        );
        hunter.setGameStatus(GurongiGameStatus.DISQUALIFIED);
    }
}