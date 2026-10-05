package com.qianyunyi.rider.entity.kaijin.gurongi.base;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.registry.GurongiSpawnRegistry;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 基基鲁游戏管理器（Gegeru Game Manager）。
 * <p>
 * 全局单例（世界级 SavedData），管理：
 * <ul>
 *   <li>全局游戏阶段推进（滋 → 魅 → 葛 → ...）</li>
 *   <li>古朗基参赛者的存档数据（离视线消失时、晋升时保存）</li>
 *   <li>参赛者队列与按顺序轮流进行游戏</li>
 *   <li>失去资格的参赛者顺位处理</li>
 * </ul>
 */
public class GurongiGameManager extends SavedData {

    private static final String DATA_NAME = "gurongi_game_manager";

    // ==================== 集团固定游戏顺序 ====================

    /**
     * 滋（Zu）集团固定游戏顺序（按古朗基实体注册名排列）。
     * <p>
     * 1. Zu-Gumun-Ba（蜘蛛）
     * 2. Zu-Gooma-Gu（犀牛）
     * 3. Zu-Mebio-Da（蜥蜴）
     * 4. Zu-Baduu-Ba（蝙蝠）
     * 5. Zu-Garume-Re（变色龙）
     * 6. Zu-Zain-Da（最后の滋集团）
     */
    private static final List<String> ZU_GROUP_ORDER = Collections.unmodifiableList(Arrays.asList(
            "zu_gumun_ba",     // 1. Zu-Gumun-Ba
            "zu_gooma_gu",     // 2. Zu-Gooma-Gu
            "zu_mebio_da",     // 3. Zu-Mebio-Da
            "zu_baduu_ba",     // 4. Zu-Baduu-Ba
            "garume_re",       // 5. Zu-Garume-Re
            "zu_zain_da"       // 6. Zu-Zain-Da
    ));

    /**
     * 魅（Me）集团固定游戏顺序（按古朗基实体注册名排列）。
     * <p>
     * 晋升者在对应滋集团原位置前插入，原生魅集团实体排末尾。
     */
    private static final List<String> ME_GROUP_ORDER = Collections.unmodifiableList(Arrays.asList(
            // 魅集团原生实体（晋升者会按滋集团顺序排在它们之前）
            "me_baduu_ba",
            "me_garume_re"
    ));

    /**
     * 集团 → 固定游戏顺序映射
     */
    private static final Map<GurongiGroup, List<String>> GROUP_ORDER_MAP = new HashMap<>();

    static {
        GROUP_ORDER_MAP.put(GurongiGroup.ZU, ZU_GROUP_ORDER);
        GROUP_ORDER_MAP.put(GurongiGroup.ME, ME_GROUP_ORDER);
        // 后续可扩展其他集团的固定顺序
        // GROUP_ORDER_MAP.put(GurongiGroup.GO, GO_GROUP_ORDER);
    }

    // ==================== 全局游戏阶段 ====================

    private GurongiGameStage gameStage = GurongiGameStage.NONE;

    // ==================== 参赛者追踪 ====================

    /** 所有已注册参赛者的 UUID → 游戏状态映射 */
    private final Map<UUID, ParticipantEntry> participants = new HashMap<>();

    /** 参赛者游戏顺序队列（按注册顺序排列） */
    private final List<UUID> participantQueue = new ArrayList<>();

    /** 当前轮到哪个参赛者的索引（-1 = 无活跃回合） */
    private int currentTurnIndex = -1;

    /** 标记：某阶段的游戏是否已全部完成 */
    private boolean stageCompleted = false;

    // ==================== 当前阶段类型耗尽追踪 ====================

    /**
     * 当前阶段中已完成或失败的实体类型名集合（防止同类型无限重生）。
     * <p>
     * 当一个实体类型在当前阶段死亡或失败后，其类型名被加入此集合，
     * 自然生成系统将跳过该类型的生成尝试。当阶段推进时，此集合被清空，
     * 给予所有类型在新的阶段中重新尝试的机会。
     */
    private final Set<String> typesExhaustedThisStage = new HashSet<>();

    // ==================== 重生数据存储 ====================

    /** UUID → 存档 NBT 数据（离视线消失或晋升时保存） */
    private final Map<UUID, CompoundTag> savedGurongiData = new HashMap<>();

    /** UUID → 所属集团（用于重生时判断阶段） */
    private final Map<UUID, GurongiGroup> respawnGroupMap = new HashMap<>();

    /** UUID → 游戏状态（用于重生时恢复状态） */
    private final Map<UUID, GurongiGameStatus> respawnStatusMap = new HashMap<>();

    /** UUID → 游戏阶段 */
    private final Map<UUID, GurongiGameStage> respawnStageMap = new HashMap<>();

    // ==================== 类型级持久数据（按实体类型名存储，跨实例共享） ====================

    /**
     * 实体类型名 → 类型级游戏数据。
     * <p>
     * 当一个古朗基晋升后，其游戏资格、击杀数等按类型名存档，
     * 不随旧 UUID 废弃而丢失。新生成的同类型实体可通过此数据恢复状态。
     */
    private final Map<String, TypeGameData> typeGameDataMap = new HashMap<>();

    /**
     * 类型级持久数据—按实体类型注册名（如 {@code "zu_gumun_ba"}）存储。
     * <p>
     * 用于跨实体实例共享游戏进度（击杀数、晋升状态等），
     * 确保一种古朗基的游戏资格全局统一。
     */
    public static class TypeGameData {
        /** 实体类型注册名，如 "zu_gumun_ba" */
        public final String entityTypeName;
        /** 总击杀数（跨实例累积） */
        public int killCount;
        /** 是否已晋升到此集团 */
        @Nullable
        public GurongiGroup promotedToGroup;
        /** 是否已至少完成一次游戏目标 */
        public boolean hasCompleted;
        /** 最后一次游戏阶段 */
        public GurongiGameStage lastStage;
        /** 是否仍有资格 */
        public boolean eligible;

        public TypeGameData(String entityTypeName) {
            this.entityTypeName = entityTypeName;
            this.killCount = 0;
            this.promotedToGroup = null;
            this.hasCompleted = false;
            this.lastStage = GurongiGameStage.NONE;
            this.eligible = true;
        }
    }

    // ==================== 倒计时系统（持久化） ====================

    /** 石棺打开后延迟多少 tick 才开始海选（默认 600 tick = 30 秒） */
    private static final int SARCOPHAGUS_DELAY_TICKS = 600;

    /** 阶段完成后推进下一阶段的延迟 tick（默认 1200 tick = 1 分钟） */
    private static final int STAGE_ADVANCE_DELAY_TICKS = 1200;

    /** 石棺已打开标记 */
    private boolean sarcophagusOpened = false;

    /**
     * 倒计时剩余 tick 数（-1 表示无活跃倒计时）。
     * <p>
     * 持久化存储，避免服务器重启后倒计时丢失。
     * 倒计时结束后执行的操作取决于当前游戏状态：
     * <ul>
     *   <li>若 {@code gameStage == NONE} → 启动海选（AUDITION）</li>
     *   <li>若 {@code stageCompleted == true} → 推进到下一阶段</li>
     * </ul>
     */
    private int countdownTicks = -1;

    // ==================== 内部数据类 ====================

    public static class ParticipantEntry {
        public final UUID gurongiUuid;
        /** 实体注册名，如 "zu_gumun_ba"、"garume_re"，用于固定顺序排序 */
        public final String entityTypeId;
        public GurongiGameStatus status;
        public GurongiGameStage stage;
        public int killCount;
        /** 是否有资格继续进行游戏（true = 有资格） */
        public boolean eligible = true;

        public ParticipantEntry(UUID gurongiUuid, String entityTypeId, GurongiGameStatus status, GurongiGameStage stage) {
            this.gurongiUuid = gurongiUuid;
            this.entityTypeId = entityTypeId;
            this.status = status;
            this.stage = stage;
            this.killCount = 0;
        }
    }

    // ==================== 获取管理器实例 ====================

    public static GurongiGameManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GurongiGameManager::load,
                GurongiGameManager::new,
                DATA_NAME
        );
    }

    // ==================== 构造与持久化 ====================

    public GurongiGameManager() {
        super();
    }

    public static GurongiGameManager load(CompoundTag tag) {
        GurongiGameManager manager = new GurongiGameManager();

        // 读取游戏阶段
        if (tag.contains("gameStage")) {
            try {
                manager.gameStage = GurongiGameStage.valueOf(tag.getString("gameStage"));
            } catch (IllegalArgumentException ignored) {
                manager.gameStage = GurongiGameStage.NONE;
            }
        }

        // 读取参赛者数据
        if (tag.contains("participants")) {
            CompoundTag participantsTag = tag.getCompound("participants");
            for (String key : participantsTag.getAllKeys()) {
                CompoundTag entryTag = participantsTag.getCompound(key);
                UUID uuid = UUID.fromString(key);
                GurongiGameStatus status = safeParseStatus(entryTag.getString("status"));
                GurongiGameStage stage = safeParseStage(entryTag.getString("stage"));
                String entityTypeId = entryTag.contains("entityTypeId") ? entryTag.getString("entityTypeId") : "";
                ParticipantEntry entry = new ParticipantEntry(uuid, entityTypeId, status, stage);
                entry.killCount = entryTag.getInt("killCount");
                entry.eligible = !entryTag.contains("eligible") || entryTag.getBoolean("eligible");
                manager.participants.put(uuid, entry);
            }
        }

        // 读取队列
        manager.participantQueue.clear();
        if (tag.contains("participantQueue")) {
            ListTag queueList = tag.getList("participantQueue", Tag.TAG_STRING);
            for (int i = 0; i < queueList.size(); i++) {
                try {
                    manager.participantQueue.add(UUID.fromString(queueList.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        manager.currentTurnIndex = tag.getInt("currentTurnIndex");
        manager.stageCompleted = tag.getBoolean("stageCompleted");

        // 读取存档数据
        if (tag.contains("savedGurongiData")) {
            CompoundTag dataTag = tag.getCompound("savedGurongiData");
            for (String key : dataTag.getAllKeys()) {
                try {
                    manager.savedGurongiData.put(UUID.fromString(key), dataTag.getCompound(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tag.contains("respawnGroupMap")) {
            CompoundTag groupTag = tag.getCompound("respawnGroupMap");
            for (String key : groupTag.getAllKeys()) {
                try {
                    GurongiGroup g = GurongiGroup.valueOf(groupTag.getString(key));
                    manager.respawnGroupMap.put(UUID.fromString(key), g);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tag.contains("respawnStatusMap")) {
            CompoundTag statusTag = tag.getCompound("respawnStatusMap");
            for (String key : statusTag.getAllKeys()) {
                try {
                    manager.respawnStatusMap.put(UUID.fromString(key),
                            GurongiGameStatus.valueOf(statusTag.getString(key)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tag.contains("respawnStageMap")) {
            CompoundTag stageTag = tag.getCompound("respawnStageMap");
            for (String key : stageTag.getAllKeys()) {
                try {
                    manager.respawnStageMap.put(UUID.fromString(key),
                            GurongiGameStage.valueOf(stageTag.getString(key)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // 读取石棺状态
        manager.sarcophagusOpened = tag.getBoolean("sarcophagusOpened");
        // 读取倒计时（持久化）
        manager.countdownTicks = tag.contains("countdownTicks") ? tag.getInt("countdownTicks") : -1;

        // 读取类型耗尽记录
        manager.typesExhaustedThisStage.clear();
        if (tag.contains("typesExhaustedThisStage")) {
            ListTag exhaustedList = tag.getList("typesExhaustedThisStage", Tag.TAG_STRING);
            for (int i = 0; i < exhaustedList.size(); i++) {
                manager.typesExhaustedThisStage.add(exhaustedList.getString(i));
            }
        }

        // 读取类型级持久数据
        if (tag.contains("typeGameData")) {
            CompoundTag typeDataTag = tag.getCompound("typeGameData");
            for (String key : typeDataTag.getAllKeys()) {
                CompoundTag dataTag = typeDataTag.getCompound(key);
                TypeGameData data = new TypeGameData(key);
                data.killCount = dataTag.getInt("killCount");
                data.hasCompleted = dataTag.getBoolean("hasCompleted");
                data.eligible = !dataTag.contains("eligible") || dataTag.getBoolean("eligible");
                data.lastStage = safeParseStage(dataTag.getString("lastStage"));
                if (dataTag.contains("promotedToGroup")) {
                    try {
                        data.promotedToGroup = GurongiGroup.valueOf(dataTag.getString("promotedToGroup"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                manager.typeGameDataMap.put(key, data);
            }
        }

        return manager;
    }

    private static GurongiGameStatus safeParseStatus(String name) {
        try {
            return GurongiGameStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GurongiGameStatus.NONE;
        }
    }

    private static GurongiGameStage safeParseStage(String name) {
        try {
            return GurongiGameStage.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GurongiGameStage.NONE;
        }
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putString("gameStage", gameStage.name());

        // 保存参赛者
        CompoundTag participantsTag = new CompoundTag();
        for (Map.Entry<UUID, ParticipantEntry> entry : participants.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("entityTypeId", entry.getValue().entityTypeId);
            entryTag.putString("status", entry.getValue().status.name());
            entryTag.putString("stage", entry.getValue().stage.name());
            entryTag.putInt("killCount", entry.getValue().killCount);
            entryTag.putBoolean("eligible", entry.getValue().eligible);
            participantsTag.put(entry.getKey().toString(), entryTag);
        }
        tag.put("participants", participantsTag);

        // 保存队列
        ListTag queueList = new ListTag();
        for (UUID uuid : participantQueue) {
            queueList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("participantQueue", queueList);
        tag.putInt("currentTurnIndex", currentTurnIndex);
        tag.putBoolean("stageCompleted", stageCompleted);

        // 保存存档数据
        CompoundTag dataTag = new CompoundTag();
        for (Map.Entry<UUID, CompoundTag> entry : savedGurongiData.entrySet()) {
            dataTag.put(entry.getKey().toString(), entry.getValue());
        }
        tag.put("savedGurongiData", dataTag);

        CompoundTag groupTag = new CompoundTag();
        for (Map.Entry<UUID, GurongiGroup> entry : respawnGroupMap.entrySet()) {
            groupTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("respawnGroupMap", groupTag);

        CompoundTag statusTag = new CompoundTag();
        for (Map.Entry<UUID, GurongiGameStatus> entry : respawnStatusMap.entrySet()) {
            statusTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("respawnStatusMap", statusTag);

        CompoundTag stageTag = new CompoundTag();
        for (Map.Entry<UUID, GurongiGameStage> entry : respawnStageMap.entrySet()) {
            stageTag.putString(entry.getKey().toString(), entry.getValue().name());
        }
        tag.put("respawnStageMap", stageTag);

        // 保存类型级持久数据
        CompoundTag typeDataTag = new CompoundTag();
        for (Map.Entry<String, TypeGameData> entry : typeGameDataMap.entrySet()) {
            CompoundTag typeEntryTag = new CompoundTag();
            typeEntryTag.putInt("killCount", entry.getValue().killCount);
            typeEntryTag.putBoolean("hasCompleted", entry.getValue().hasCompleted);
            typeEntryTag.putBoolean("eligible", entry.getValue().eligible);
            typeEntryTag.putString("lastStage", entry.getValue().lastStage.name());
            if (entry.getValue().promotedToGroup != null) {
                typeEntryTag.putString("promotedToGroup", entry.getValue().promotedToGroup.name());
            }
            typeDataTag.put(entry.getKey(), typeEntryTag);
        }
        tag.put("typeGameData", typeDataTag);

        tag.putBoolean("sarcophagusOpened", sarcophagusOpened);
        tag.putInt("countdownTicks", countdownTicks);

        // 保存类型耗尽记录
        ListTag exhaustedList = new ListTag();
        for (String typeName : typesExhaustedThisStage) {
            exhaustedList.add(StringTag.valueOf(typeName));
        }
        tag.put("typesExhaustedThisStage", exhaustedList);

        return tag;
    }

    // ==================== ★ 游戏阶段管理 ====================

    public GurongiGameStage getGameStage() {
        return gameStage;
    }

    public void setGameStage(GurongiGameStage stage) {
        this.gameStage = stage;
        setDirty();
    }

    /**
     * 推进全局游戏阶段到下一阶段。
     * <p>
     * 推进顺序：NONE → AUDITION(滋) → SELECTION(魅) → SEMI_FINAL(葛) → WHITE_DARK_DUEL → FINAL
     * <p>
     * 每次进入新阶段时，会自动调用 {@link #initializeGroupQueue(GurongiGroup)} 初始化
     * 对应集团的参赛者队列顺序。
     *
     * @return 推进后的阶段
     */
    public GurongiGameStage advanceGameStage() {
        switch (gameStage) {
            case NONE:
                gameStage = GurongiGameStage.AUDITION;
                break;
            case AUDITION:
                gameStage = GurongiGameStage.SELECTION;
                break;
            case SELECTION:
                gameStage = GurongiGameStage.SEMI_FINAL;
                break;
            case SEMI_FINAL:
                gameStage = GurongiGameStage.WHITE_DARK_DUEL;
                break;
            case WHITE_DARK_DUEL:
                gameStage = GurongiGameStage.FINAL;
                break;
            default:
                break;
        }
        stageCompleted = false;
        currentTurnIndex = -1;
        // ★ 阶段推进时清空类型耗尽记录，给予所有类型在新阶段重新尝试的机会
        clearExhaustedTypes();
        setDirty();

        // 初始化当前阶段对应集团的固定游戏顺序
        GurongiGroup currentGroup = getGroupForStage(gameStage);
        if (currentGroup != null) {
            initializeGroupQueue(currentGroup);
        }

        KamenRiderInterwovenWorldsMod.LOGGER.info("[GurongiGameManager] 游戏阶段推进至: {}", gameStage);
        return gameStage;
    }

    /**
     * 获取游戏阶段对应的集团。
     */
    public static GurongiGroup getGroupForStage(GurongiGameStage stage) {
        if (stage == null) return null;
        switch (stage) {
            case AUDITION: return GurongiGroup.ZU;
            case SELECTION: return GurongiGroup.ME;
            case SEMI_FINAL: return GurongiGroup.GO;
            case WHITE_DARK_DUEL: return GurongiGroup.RA;
            case FINAL: return GurongiGroup.N;
            default: return null;
        }
    }

    // ==================== ★ 倒计时驱动系统 ====================

    /**
     * 获取倒计时剩余 tick 数（用于显示）。
     *
     * @return 剩余 tick 数，无活跃倒计时返回 -1
     */
    public int getCountdownRemaining() {
        return countdownTicks;
    }

    /**
     * 获取倒计时剩余秒数（用于显示）。
     *
     * @return 剩余秒数，无活跃倒计时返回 -1
     */
    public int getCountdownRemainingSeconds() {
        return countdownTicks > 0 ? countdownTicks / 20 : -1;
    }

    /**
     * 判断是否正在倒计时中。
     */
    public boolean isCountdownActive() {
        return countdownTicks > 0;
    }

    /**
     * 每 tick 调用一次，驱动倒计时系统。
     * <p>
     * 处理两种场景：
     * <ol>
     *   <li>石棺开启后 → 倒计时结束 → 启动海选（AUDITION）</li>
     *   <li>阶段完成后 → 倒计时结束 → 推进到下一阶段</li>
     * </ol>
     */
    public void tick(ServerLevel level) {
        // ===== 倒计时归零处理 =====
        if (countdownTicks == 0) {
            countdownTicks = -1; // 重置倒计时
            setDirty();

            if (gameStage == GurongiGameStage.NONE) {
                // 封印开启后的第一次游戏启动：推进到海选
                // 此时 spawn level 已在 onSarcophagusOpened 中设置为 1
                advanceGameStage();
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[GurongiGameManager] 倒计时结束，基基鲁游戏海选阶段开始！");
            } else if (stageCompleted) {
                // 阶段完成后的阶段推进
                // 推进前已允许下一阶段古朗基生成
                advanceGameStage();
                // 推进后分配目标给所有参赛者
                assignObjectivesToAllParticipants(level);
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[GurongiGameManager] 倒计时结束，基基鲁游戏阶段推进至: {}", gameStage);
            }
        }

        // ===== 倒计时递减 =====
        if (countdownTicks > 0) {
            countdownTicks--;
            setDirty();
        }
    }

    /**
     * 玩家开启石棺时调用。
     * <p>
     * 行为：
     * <ol>
     *   <li>立即设置生成等级为 1（古朗基可以开始刷新）</li>
     *   <li>启动倒计时（默认 600 tick = 30 秒）</li>
     *   <li>倒计时结束后自动推进到海选阶段</li>
     * </ol>
     */
    public void onSarcophagusOpened(ServerLevel level, int tick) {
        if (sarcophagusOpened) return;
        this.sarcophagusOpened = true;

        // ★ 立即设置生成等级为 1，允许古朗基在倒计时期间刷新
        AddonWorldData worldData = AddonWorldData.get(level);
        worldData.setGurongiSpawn(1);
        worldData.sync(level);

        // ★ 启动倒计时
        this.countdownTicks = SARCOPHAGUS_DELAY_TICKS;
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 石棺已打开，生成等级已设为1（古朗基开始刷新），{} tick（{}秒）后正式启动基基鲁游戏海选",
                SARCOPHAGUS_DELAY_TICKS, SARCOPHAGUS_DELAY_TICKS / 20);
    }

    /**
     * 阶段完成后，启动倒计时以推进到下一阶段。
     * <p>
     * 行为：
     * <ol>
     *   <li>立即更新生成等级到下一阶段</li>
     *   <li>清除当前阶段参赛者数据</li>
     *   <li>启动 1 分钟倒计时</li>
     *   <li>倒计时结束后自动推进阶段并分配目标</li>
     * </ol>
     */
    public void startStageAdvanceCountdown(ServerLevel level) {
        if (countdownTicks > 0) return; // 已有倒计时进行中

        // ★ 计算下一阶段并立即设置生成等级，允许新阶段古朗基刷新
        GurongiGameStage nextStage = getNextStage(gameStage);
        if (nextStage == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 已是最终阶段，无需继续推进");
            return;
        }

        GurongiGroup nextGroup = getGroupForStage(nextStage);
        int nextLevel = getLevelForGroup(nextGroup);
        if (nextLevel > 0) {
            AddonWorldData worldData = AddonWorldData.get(level);
            worldData.setGurongiSpawn(nextLevel);
            worldData.sync(level);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 阶段完成，生成等级已更新至 {}（{}），古朗基开始刷新，{} tick（{}秒）后正式推进阶段",
                    nextLevel, nextStage, STAGE_ADVANCE_DELAY_TICKS, STAGE_ADVANCE_DELAY_TICKS / 20);
        }

        // ★ 启动倒计时
        this.countdownTicks = STAGE_ADVANCE_DELAY_TICKS;
        setDirty();
    }

    /**
     * 为当前阶段的所有活跃参赛者分配游戏目标。
     * <p>
     * 在阶段推进倒计时结束时调用，遍历所有存活参赛者实体并分配目标。
     */
    private void assignObjectivesToAllParticipants(ServerLevel level) {
        for (UUID uuid : participantQueue) {
            ParticipantEntry entry = participants.get(uuid);
            if (entry == null || !entry.eligible) continue;

            // 在世界中查找对应的实体
            net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
            if (entity instanceof Gurongi gurongi && gurongi.isAlive()) {
                if (!gurongi.isObjectiveInitialized()) {
                    gurongi.assignGameObjective();
                    KamenRiderInterwovenWorldsMod.LOGGER.info(
                            "[GurongiGameManager] 为目标分配目标: {} 目标: {}",
                            gurongi.getName().getString(),
                            gurongi.getGameObjective() != null ? gurongi.getGameObjective().getDescription() : "无");
                }
            }
        }
    }

    /**
     * 获取下一阶段。
     */
    private static GurongiGameStage getNextStage(GurongiGameStage current) {
        if (current == null) return GurongiGameStage.NONE;
        switch (current) {
            case NONE: return GurongiGameStage.AUDITION;
            case AUDITION: return GurongiGameStage.SELECTION;
            case SELECTION: return GurongiGameStage.SEMI_FINAL;
            case SEMI_FINAL: return GurongiGameStage.WHITE_DARK_DUEL;
            case WHITE_DARK_DUEL: return GurongiGameStage.FINAL;
            default: return null;
        }
    }

    /**
     * 根据集团获取生成等级。
     */
    private static int getLevelForGroup(@Nullable GurongiGroup group) {
        if (group == null) return -1;
        switch (group) {
            case ZU: return 1;
            case ME: return 7;
            case GO: return 11;
            case RA: return 16;
            case N:  return 19;
            default: return -1;
        }
    }

    // ==================== ★ 参赛者管理 ====================

    /**
     * 注册一个古朗基为基基鲁参赛者，并加入队列。
     * <p>
     * 如果是已晋升类型的古朗基（在 {@link #typeGameDataMap} 中有晋升记录），
     * 则插入到队列最前端，确保晋升者优先于原生实体。
     *
     * @param gurongi      古朗基实体
     * @param stage        所属游戏阶段
     * @param entityTypeId 实体注册名（如 "zu_gumun_ba"），用于固定顺序排序
     */
    public void registerParticipant(Gurongi gurongi, GurongiGameStage stage, String entityTypeId) {
        UUID uuid = gurongi.getUUID();
        if (participants.containsKey(uuid)) return;

        ParticipantEntry entry = new ParticipantEntry(uuid, entityTypeId, GurongiGameStatus.ACTIVE, stage);
        participants.put(uuid, entry);

        // ★ 判断是否为已晋升实体，若是则插入队列最前端
        TypeGameData typeData = typeGameDataMap.get(entityTypeId);
        if (typeData != null && typeData.hasCompleted && typeData.promotedToGroup != null) {
            // 该类型曾有晋升记录 → 插入队列最前端
            participantQueue.add(0, uuid);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 注册晋升参赛者: {} (类型: {}, 阶段: {}, 晋升至: {}, 击杀: {}), 插入队列首位",
                    gurongi.getName().getString(), entityTypeId, stage, typeData.promotedToGroup, typeData.killCount
            );
        } else {
            // 普通参赛者 → 追加到队列末尾
            participantQueue.add(uuid);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 注册参赛者: {} (类型: {}, 阶段: {}), 队列位置: {}",
                    gurongi.getName().getString(), entityTypeId, stage, participantQueue.size()
            );
        }

        setDirty();
    }

    /**
     * 注册一个古朗基为基基鲁参赛者（自动获取实体类型注册名）。
     */
    public void registerParticipant(Gurongi gurongi, GurongiGameStage stage) {
        String entityTypeId = getEntityRegistryName(gurongi);
        registerParticipant(gurongi, stage, entityTypeId);
    }

    /**
     * 初始化某集团的参赛者队列为固定顺序。
     * <p>
     * 当集团游戏阶段开始时调用此方法，按 {@link #GROUP_ORDER_MAP} 中定义的
     * 固定顺序重新排列参赛者队列。不在固定顺序列表中的参赛者将被排在队列末尾。
     *
     * @param group 要初始化队列的集团
     */
    public void initializeGroupQueue(GurongiGroup group) {
        List<String> fixedOrder = GROUP_ORDER_MAP.get(group);
        if (fixedOrder == null) {
            // 该集团没有定义固定顺序，保持注册顺序
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] {} 集团没有定义固定游戏顺序，保持注册顺序", group.getChineseName()
            );
            return;
        }

        // 将当前队列按固定顺序重新排列
        List<UUID> reordered = new ArrayList<>();
        List<UUID> remaining = new ArrayList<>(participantQueue);

        // 按固定顺序依次查找匹配的参赛者
        for (String typeId : fixedOrder) {
            UUID matched = null;
            for (UUID uuid : remaining) {
                ParticipantEntry entry = participants.get(uuid);
                if (entry != null && typeId.equals(entry.entityTypeId)) {
                    matched = uuid;
                    break;
                }
            }
            if (matched != null) {
                reordered.add(matched);
                remaining.remove(matched);
            }
        }

        // 剩余的（不在固定顺序列表中）追加到末尾
        reordered.addAll(remaining);

        // 更新队列
        participantQueue.clear();
        participantQueue.addAll(reordered);
        currentTurnIndex = -1; // 重置回合索引
        stageCompleted = false;
        setDirty();

        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] {} 集团游戏顺序已初始化（{} 名参赛者），固定顺序队列已就绪",
                group.getChineseName(), reordered.size()
        );
    }

    /**
     * 获取实体注册名（如 "zu_gumun_ba"、"garume_re"）。
     */
    private static String getEntityRegistryName(Gurongi gurongi) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(gurongi.getType());
        return key != null ? key.getPath() : "";
    }

    /**
     * 移除参赛者。
     */
    public void unregisterParticipant(UUID gurongiUuid) {
        participants.remove(gurongiUuid);
        participantQueue.remove(gurongiUuid);
        savedGurongiData.remove(gurongiUuid);
        respawnGroupMap.remove(gurongiUuid);
        respawnStatusMap.remove(gurongiUuid);
        respawnStageMap.remove(gurongiUuid);
        setDirty();
    }

    @Nullable
    public ParticipantEntry getParticipant(UUID gurongiUuid) {
        return participants.get(gurongiUuid);
    }

    public boolean isRegistered(UUID gurongiUuid) {
        return participants.containsKey(gurongiUuid);
    }

    /**
     * 检查某个实体类型注册名是否已在参赛者队列中。
     * 用于自然生成时的去重判断。
     *
     * @param entityTypeId 实体类型注册名（如 "kamen_rider_interwoven_worlds:zu_gumun_ba"）
     * @return 如果该类型已有存活或已注册的参赛者，返回 true
     */
    public boolean hasRegisteredType(String entityTypeId) {
        if (entityTypeId == null || entityTypeId.isEmpty()) return false;
        for (ParticipantEntry entry : participants.values()) {
            if (entityTypeId.equals(entry.entityTypeId)) {
                return true;
            }
        }
        return false;
    }

    public void updateParticipantStatus(UUID gurongiUuid, GurongiGameStatus newStatus) {
        ParticipantEntry entry = participants.get(gurongiUuid);
        if (entry != null) {
            entry.status = newStatus;
            setDirty();
        }
    }

    // ==================== ★ 顺序游戏轮转系统 ====================

    /**
     * 获取当前应该进行游戏的参赛者 UUID。
     * <p>
     * 规则：
     * <ol>
     *   <li>按参赛者注册顺序依次进行游戏</li>
     *   <li>如果当前参赛者已失去资格，自动顺位给下一个</li>
     *   <li>如果全部参赛者都完成或失去资格，标记阶段结束</li>
     * </ol>
     *
     * @return 当前回合的参赛者 UUID，null 表示无活跃回合
     */
    @Nullable
    public UUID getCurrentTurnParticipant() {
        if (participantQueue.isEmpty() || stageCompleted) return null;

        // 如果当前索引无效，从头开始查找
        if (currentTurnIndex < 0 || currentTurnIndex >= participantQueue.size()) {
            currentTurnIndex = 0;
        }

        // 从当前索引开始，找第一个有资格的参赛者
        for (int i = 0; i < participantQueue.size(); i++) {
            int idx = (currentTurnIndex + i) % participantQueue.size();
            UUID uuid = participantQueue.get(idx);
            ParticipantEntry entry = participants.get(uuid);
            if (entry != null && entry.eligible) {
                // 检查是否已完成
                if (entry.status == GurongiGameStatus.COMPLETED || entry.status == GurongiGameStatus.PROMOTED) {
                    continue;
                }
                currentTurnIndex = idx;
                return uuid;
            }
        }

        // 所有参赛者都已完成或失去资格
        stageCompleted = true;
        setDirty();
        return null;
    }

    /**
     * 将指定参赛者标记为游戏完成（击杀目标达成），并自动顺位给下一位。
     * <p>
     * 由 {@link com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi#onObjectiveCompleted()} 调用。
     *
     * @param gurongiUuid 完成游戏的古朗基 UUID
     * @param level       当前所在维度
     */
    public void onParticipantCompleted(UUID gurongiUuid, ServerLevel level) {
        ParticipantEntry entry = participants.get(gurongiUuid);
        if (entry == null) return;
        // 已完成或已失去资格的不重复处理
        if (!entry.eligible) return;
        entry.status = GurongiGameStatus.COMPLETED;
        entry.eligible = false;
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 参赛者 {} 完成游戏目标，顺位下一位", gurongiUuid
        );
        advanceToNextTurn(level);
    }

    /**
     * 将指定参赛者标记为游戏失败（被击杀），并自动顺位给下一位。
     * <p>
     * 由 {@link com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi#die(DamageSource)} 调用。
     *
     * @param gurongiUuid 被击杀的古朗基 UUID
     * @param level       当前所在维度
     */
    public void onParticipantDied(UUID gurongiUuid, ServerLevel level) {
        ParticipantEntry entry = participants.get(gurongiUuid);
        if (entry == null) return;
        // 已完成、已逃脱或已失败的不重复处理
        if (!entry.eligible) return;
        entry.status = GurongiGameStatus.FAIL;
        entry.eligible = false;
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 参赛者 {} 死亡，失去游戏资格，顺位下一位", gurongiUuid
        );
        advanceToNextTurn(level);
    }

    /**
     * 将指定参赛者标记为逃脱（Zu-Gooma-Gu 专属），并自动顺位给下一位。
     * <p>
     * 逃脱意味着放弃游戏规则（击杀目标作废）。
     * 由 {@link com.qianyunyi.rider.entity.kaijin.gurongi.ZuGoomaGuEntity#triggerEscape()} 调用。
     *
     * @param gurongiUuid 逃脱的古朗基 UUID
     * @param level       当前所在维度
     */
    public void onParticipantEscaped(UUID gurongiUuid, ServerLevel level) {
        ParticipantEntry entry = participants.get(gurongiUuid);
        if (entry == null) return;
        // 已完成或已失去资格的不重复处理
        if (!entry.eligible) return;
        entry.status = GurongiGameStatus.ESCAPED;
        entry.eligible = false;
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 参赛者 {} 逃脱，放弃游戏规则，顺位下一位", gurongiUuid
        );
        advanceToNextTurn(level);
    }

    /**
     * 内部方法：将当前回合索引移动到下一位有资格的参赛者。
     * <p>
     * 如果所有参赛者都已完成或失去资格，标记阶段结束。
     */
    private void advanceToNextTurn(ServerLevel level) {
        // 从当前索引之后开始找下一个有资格的
        int startIdx = currentTurnIndex + 1;
        for (int i = startIdx; i < participantQueue.size(); i++) {
            UUID uuid = participantQueue.get(i);
            ParticipantEntry entry = participants.get(uuid);
            if (entry != null && entry.eligible
                    && entry.status != GurongiGameStatus.COMPLETED
                    && entry.status != GurongiGameStatus.PROMOTED) {
                currentTurnIndex = i;
                setDirty();
                KamenRiderInterwovenWorldsMod.LOGGER.info(
                        "[GurongiGameManager] 顺位至下一位参赛者: {} (索引: {})",
                        uuid, i
                );
                return;
            }
        }
        // 没有更多参赛者了
        stageCompleted = true;
        currentTurnIndex = participantQueue.size(); // 标记结束
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 当前阶段所有参赛者已完成或失去资格，阶段结束，启动阶段推进倒计时"
        );

        // ★ 启动阶段推进倒计时
        startStageAdvanceCountdown(level);
    }

    /**
     * 检查当前阶段是否所有参赛者都已完成。
     */
    public boolean isStageCompleted() {
        return stageCompleted;
    }

    /**
     * 标记阶段完成。
     */
    public void setStageCompleted(boolean completed) {
        this.stageCompleted = completed;
        setDirty();
    }

    /**
     * 重置所有参赛者数据（手动设置游戏阶段时调用）。
     * <p>
     * 清除参赛者队列、重生数据、顺位索引，并重置阶段完成标记。
     * 通常在 {@code /rider data gurongi set <level>} 指令中调用，
     * 以解决自然消失的古朗基仍卡在参赛者队列中的问题。
     */
    public void resetParticipants() {
        participants.clear();
        participantQueue.clear();
        savedGurongiData.clear();
        respawnGroupMap.clear();
        respawnStatusMap.clear();
        respawnStageMap.clear();
        typeGameDataMap.clear();
        typesExhaustedThisStage.clear();
        currentTurnIndex = 0;
        stageCompleted = false;
        setDirty();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[GurongiGameManager] 参赛者队列及类型级数据已全部重置"
        );
    }

    /**
     * 获取参赛者队列中的所有 UUID。
     */
    public List<UUID> getParticipantQueue() {
        return Collections.unmodifiableList(participantQueue);
    }

    /**
     * 获取某集团对应的游戏阶段。
     */
    public static GurongiGameStage getStageForGroup(GurongiGroup group) {
        if (group == null) return GurongiGameStage.NONE;
        switch (group) {
            case ZU: return GurongiGameStage.AUDITION;
            case ME: return GurongiGameStage.SELECTION;
            case GO: return GurongiGameStage.SEMI_FINAL;
            case RA: return GurongiGameStage.WHITE_DARK_DUEL;
            case N:  return GurongiGameStage.FINAL;
            default: return GurongiGameStage.NONE;
        }
    }

    // ==================== ★ 重生数据管理 ====================

    /**
     * 保存古朗基的存档数据（离视线消失或晋升时调用）。
     */
    public void saveGurongiData(UUID uuid, CompoundTag data,
                                 @Nullable GurongiGroup group,
                                 GurongiGameStatus status,
                                 GurongiGameStage stage) {
        savedGurongiData.put(uuid, data.copy());
        if (group != null) respawnGroupMap.put(uuid, group);
        respawnStatusMap.put(uuid, status);
        respawnStageMap.put(uuid, stage);
        setDirty();
    }

    /**
     * 标记该古朗基需要重生。
     */
    public void markGurongiForRespawn(UUID uuid, @Nullable GurongiGroup group) {
        if (group != null) respawnGroupMap.put(uuid, group);
        setDirty();
    }

    /**
     * 移除该古朗基的重生标记（被玩家杀死时调用）。
     */
    public void removeGurongiRespawnData(UUID uuid) {
        savedGurongiData.remove(uuid);
        respawnGroupMap.remove(uuid);
        respawnStatusMap.remove(uuid);
        respawnStageMap.remove(uuid);
        setDirty();
    }

    /**
     * 获取古朗基的存档 NBT 数据。
     */
    @Nullable
    public CompoundTag getSavedGurongiData(UUID uuid) {
        return savedGurongiData.get(uuid);
    }

    @Nullable
    public GurongiGroup getRespawnGroup(UUID uuid) {
        return respawnGroupMap.get(uuid);
    }

    @Nullable
    public GurongiGameStatus getRespawnStatus(UUID uuid) {
        return respawnStatusMap.get(uuid);
    }

    @Nullable
    public GurongiGameStage getRespawnStage(UUID uuid) {
        return respawnStageMap.get(uuid);
    }

    /**
     * 获取所有需要重生的古朗基列表。
     */
    public Map<UUID, GurongiGroup> getAllRespawnableEntities() {
        return Collections.unmodifiableMap(respawnGroupMap);
    }

    public boolean hasRespawnData(UUID uuid) {
        return respawnGroupMap.containsKey(uuid);
    }

    // ==================== ★ 类型级持久数据管理 ====================

    /**
     * 保存/更新某种古朗基的类型级游戏数据。
     * <p>
     * 通常在古朗基完成游戏目标并晋升时调用，
     * 将击杀数、晋升集团等信息按类型名持久化。
     *
     * @param entityTypeName 实体类型注册名（如 "zu_gumun_ba"）
     * @param killCount      当前击杀数
     * @param promotedToGroup 晋升到的集团（null 表示未晋升）
     * @param lastStage      最后游戏阶段
     * @param eligible       是否仍有资格
     */
    public void saveTypeGameData(String entityTypeName, int killCount,
                                  @Nullable GurongiGroup promotedToGroup,
                                  GurongiGameStage lastStage,
                                  boolean eligible) {
        if (entityTypeName == null || entityTypeName.isEmpty()) return;
        TypeGameData data = typeGameDataMap.computeIfAbsent(entityTypeName, TypeGameData::new);
        data.killCount = Math.max(data.killCount, killCount);
        data.promotedToGroup = promotedToGroup;
        data.lastStage = lastStage;
        data.eligible = eligible;
        data.hasCompleted = true;
        setDirty();
    }

    /**
     * 获取某种古朗基的类型级游戏数据。
     *
     * @param entityTypeName 实体类型注册名
     * @return 类型级游戏数据，无记录时返回 null
     */
    @Nullable
    public TypeGameData getTypeGameData(String entityTypeName) {
        return entityTypeName != null ? typeGameDataMap.get(entityTypeName) : null;
    }

    /**
     * 获取所有已晋升到指定集团的实体类型名列表。
     * <p>
     * 用于 {@link #initializeGroupQueue(GurongiGroup)} 中确定晋升实体的优先级。
     *
     * @param group 目标集团
     * @return 晋升到此集团的实体类型名列表（按晋升顺序）
     */
    public List<String> getPromotedTypeNames(GurongiGroup group) {
        if (group == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (TypeGameData data : typeGameDataMap.values()) {
            if (data.promotedToGroup == group && data.hasCompleted) {
                result.add(data.entityTypeName);
            }
        }
        return result;
    }

    /**
     * 移除某种古朗基的类型级游戏数据。
     */
    public void removeTypeGameData(String entityTypeName) {
        if (entityTypeName != null) {
            typeGameDataMap.remove(entityTypeName);
            setDirty();
        }
    }

    /**
     * 获取所有类型级游戏数据的只读快照。
     */
    public Map<String, TypeGameData> getAllTypeGameData() {
        return Collections.unmodifiableMap(typeGameDataMap);
    }

    // ==================== ★ 类型耗尽追踪（防无限重生） ====================

    /**
     * 检查指定实体类型是否已在当前阶段耗尽机会。
     * <p>
     * 如果类型已在当前阶段死亡或失败，自然生成系统将跳过该类型的生成尝试。
     *
     * @param entityTypeName 实体类型注册名（如 "zu_gumun_ba"）
     * @return true 表示该类型已耗尽机会，不应再生成
     */
    public boolean isTypeExhaustedInStage(String entityTypeName) {
        return entityTypeName != null && typesExhaustedThisStage.contains(entityTypeName);
    }

    /**
     * 将指定实体类型标记为在当前阶段已耗尽机会。
     * <p>
     * 当晋升实体在晋升后的阶段死亡/失败时调用，
     * 防止同一类型无限重生。
     *
     * @param entityTypeName 实体类型注册名
     */
    public void markTypeExhaustedInStage(String entityTypeName) {
        if (entityTypeName != null) {
            typesExhaustedThisStage.add(entityTypeName);
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 类型 {} 已在当前阶段 {} 耗尽机会",
                    entityTypeName, gameStage
            );
            setDirty();
        }
    }

    /**
     * 清空当前阶段的类型耗尽记录。
     * <p>
     * 在阶段推进时调用，给予所有类型在新阶段重新尝试的机会。
     */
    public void clearExhaustedTypes() {
        if (!typesExhaustedThisStage.isEmpty()) {
            typesExhaustedThisStage.clear();
            KamenRiderInterwovenWorldsMod.LOGGER.info(
                    "[GurongiGameManager] 阶段 {} 的类型耗尽记录已清空", gameStage
            );
            setDirty();
        }
    }

    // ==================== 其他辅助方法 ====================

    /**
     * 递增参赛者击杀计数。
     */
    public void incrementKillCount(UUID gurongiUuid) {
        ParticipantEntry entry = participants.get(gurongiUuid);
        if (entry != null) {
            entry.killCount++;
            // ★ 同步更新类型级数据（entityTypeId 是全键名，需提取路径部分匹配 typeGameDataMap）
            String typePath = entry.entityTypeId;
            int colonIdx = typePath.indexOf(':');
            String pathOnly = colonIdx >= 0 ? typePath.substring(colonIdx + 1) : typePath;
            TypeGameData typeData = typeGameDataMap.get(pathOnly);
            if (typeData != null) {
                typeData.killCount = Math.max(typeData.killCount, entry.killCount);
            }
            setDirty();
        }
    }

    /**
     * 获取特定阶段的所有参赛者。
     */
    public List<ParticipantEntry> getParticipantsByStage(GurongiGameStage stage) {
        List<ParticipantEntry> result = new ArrayList<>();
        for (ParticipantEntry entry : participants.values()) {
            if (entry.stage == stage) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 获取石棺是否已打开。
     */
    public boolean isSarcophagusOpened() {
        return sarcophagusOpened;
    }

    /**
     * 重置倒计时（手动设置游戏阶段时调用）。
     * <p>
     * 在 {@code /rider data gurongi set <level>} 指令中调用，
     * 以停止任何正在进行的倒计时，避免与手动设置的阶段冲突。
     */
    public void resetCountdown() {
        if (countdownTicks != -1) {
            countdownTicks = -1;
            setDirty();
        }
    }
}