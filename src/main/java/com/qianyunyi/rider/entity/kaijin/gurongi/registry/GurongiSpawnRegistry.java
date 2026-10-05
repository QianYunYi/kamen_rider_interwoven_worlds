package com.qianyunyi.rider.entity.kaijin.gurongi.registry;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameManager;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameStage;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class GurongiSpawnRegistry {

    // 使用Supplier延迟加载EntityType
    private static final Map<Supplier<EntityType<? extends Gurongi>>, Integer> GURONGI_SPAWN_SUPPLIERS = new LinkedHashMap<>();

    // 缓存的映射，在FMLCommonSetupEvent之后填充
    private static Map<EntityType<? extends Gurongi>, Integer> GURONGI_SPAWN_MAP = null;

    static {
        // 在静态块中只保存Supplier，不立即获取
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.ZU_GUMUN_BA::get, 1);  // EPISODE 1
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU::get, 2);  // EPISODE 2
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.ZU_MEBIO_DA::get, 3);   // EPISODE 3
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.GARUME_RE::get, 4);     // EPISODE 4
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.ZU_BADUU_BA::get, 5);   // EPISODE 5
        GURONGI_SPAWN_SUPPLIERS.put(KamenRiderInterwovenWorldsModEntities.ZU_ZAIN_DA::get, 6);    // EPISODE 6
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 在commonSetup事件中初始化映射
            initializeSpawnMap();
            registerSpawnPlacements();
        });
    }

    private static void initializeSpawnMap() {
        GURONGI_SPAWN_MAP = new LinkedHashMap<>();
        for (Map.Entry<Supplier<EntityType<? extends Gurongi>>, Integer> entry : GURONGI_SPAWN_SUPPLIERS.entrySet()) {
            EntityType<? extends Gurongi> entityType = entry.getKey().get();
            GURONGI_SPAWN_MAP.put(entityType, entry.getValue());
        }
    }

    /** 每种古朗基在同一维度仅允许存在一只 */
    private static final int DUPLICATE_CHECK_RADIUS = Integer.MAX_VALUE;

    /**
     * 跨维度 tick 级去重集合，防止同一 tick 内多区块竞态条件导致重复生成。
     * key = 维度 ResourceLocation, value = 本 tick 内已生成/正在生成的古朗基类型集合
     */
    private static final ConcurrentHashMap<ResourceLocation, Set<EntityType<? extends Gurongi>>>
            SPAWNED_THIS_TICK = new ConcurrentHashMap<>();

    public static void registerSpawnPlacements() {
        if (GURONGI_SPAWN_MAP == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.error("GURONGI_SPAWN_MAP 未初始化!");
            return;
        }

        for (Map.Entry<EntityType<? extends Gurongi>, Integer> entry : GURONGI_SPAWN_MAP.entrySet()) {
            EntityType<? extends Gurongi> entityType = entry.getKey();
            int requiredLevel = entry.getValue();

            if (entityType != null) {
                // 克隆 entityType 的引用以供 lambda 使用
                EntityType<? extends Gurongi> capturedType = entityType;
                SpawnPlacements.register(
                        entityType,
                        SpawnPlacements.Type.ON_GROUND,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (type, world, reason, pos, random) -> {
                            int currentLevel = AddonWorldData.get(world).getGurongiSpawn();

                            // ★ 检查该类型是否有晋升数据允许在当前阶段生成
                            boolean isPromotedForStage = false;
                            boolean typeExhausted = false;
                            if (world instanceof ServerLevelAccessor) {
                                ServerLevelAccessor sva = (ServerLevelAccessor) world;
                                ServerLevel serverLevel = sva.getLevel();
                                ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(capturedType);
                                if (typeKey != null && serverLevel != null) {
                                    String typePath = typeKey.getPath();
                                    GurongiGameManager manager = GurongiGameManager.get(serverLevel);
                                    GurongiGameManager.TypeGameData typeData = manager.getTypeGameData(typePath);
                                    if (typeData != null && typeData.hasCompleted && typeData.promotedToGroup != null) {
                                        GurongiGameStage currentStage = manager.getGameStage();
                                        GurongiGroup stageGroup = GurongiGameManager.getGroupForStage(currentStage);
                                        // 晋升者类型匹配当前阶段集团 → 允许生成
                                        if (typeData.promotedToGroup == stageGroup) {
                                            isPromotedForStage = true;
                                        }
                                    }
                                    // ★ 检查该类型是否已在当前阶段耗尽机会（死亡/失败）
                                    if (manager.isTypeExhaustedInStage(typePath)) {
                                        typeExhausted = true;
                                    }
                                }
                            }

                            // 等级不匹配且非晋升者 → 不允许生成
                            if (currentLevel != requiredLevel && !isPromotedForStage) return false;
                            // 类型已在当前阶段耗尽机会 → 不允许生成
                            if (typeExhausted) return false;
                            // 检查附近区块是否已有同类型古朗基
                            if (hasExistingGurongiOfType(world, pos, capturedType)) return false;
                            // 蝙蝠古朗基（Zu-Gooma-Gu）仅在夜晚生成
                            if (capturedType == KamenRiderInterwovenWorldsModEntities.ZU_GOOMA_GU.get()) {
                                return world.getSkyDarken() > 4;
                            }
                            return true;
                        }
                );
            } else {
                KamenRiderInterwovenWorldsMod.LOGGER.error("EntityType 为null，等级: {}", requiredLevel);
            }
        }
    }

    /**
     * 检查指定位置附近是否已有同类型的古朗基存在。
     * <p>
     * 使用三层防护策略：
     * <ol>
     *   <li><b>Tick 级去重</b> - 防止同一 tick 内多区块竞态条件</li>
     *   <li><b>世界实体扫描</b> - AABB 大范围搜索存活实体</li>
     *   <li><b>游戏管理器注册表</b> - 检查参赛者队列中是否已注册同类型</li>
     * </ol>
     *
     * @param world 世界访问器
     * @param pos   待生成位置
     * @param type  古朗基实体类型
     * @return true = 已存在同类型古朗基，取消生成
     */
    private static boolean hasExistingGurongiOfType(ServerLevelAccessor world,
                                                     net.minecraft.core.BlockPos pos,
                                                     EntityType<? extends Gurongi> type) {
        if (world == null || pos == null || type == null) return false;

        Level level = world.getLevel();
        if (level == null || level.isClientSide()) return false;

        ResourceLocation dimKey = level.dimension().location();
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(type);
        String typeKeyStr = typeKey != null ? typeKey.toString() : "unknown";

        // ===== 防护层1：Tick 级去重（防止同一 tick 内多区块竞态） =====
        Set<EntityType<? extends Gurongi>> tickSet = SPAWNED_THIS_TICK.get(dimKey);
        if (tickSet != null && tickSet.contains(type)) {
            KamenRiderInterwovenWorldsMod.LOGGER.debug(
                    "[GurongiSpawnRegistry] [Tick去重] 阻止 {} 在本 tick 内重复生成", typeKeyStr);
            return true;
        }

        // ===== 防护层2：世界实体列表扫描 =====
        try {
            AABB searchBox = new AABB(pos).inflate(DUPLICATE_CHECK_RADIUS);
            List<Gurongi> existing = level.getEntitiesOfClass(
                    Gurongi.class, searchBox,
                    g -> g != null && g.getType() == type && g.isAlive());
            if (!existing.isEmpty()) {
                KamenRiderInterwovenWorldsMod.LOGGER.debug(
                        "[GurongiSpawnRegistry] [实体扫描] 阻止 {} 重复生成（已有 {} 只存活）",
                        typeKeyStr, existing.size());
                return true;
            }
        } catch (Exception e) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[GurongiSpawnRegistry] 实体扫描异常: {}", e.getMessage());
        }

        // ===== 防护层3：游戏管理器注册列表检查 =====
        if (level instanceof ServerLevel serverLevel) {
            GurongiGameManager manager = GurongiGameManager.get(serverLevel);
            if (typeKey != null && manager.hasRegisteredType(typeKeyStr)) {
                KamenRiderInterwovenWorldsMod.LOGGER.debug(
                        "[GurongiSpawnRegistry] [管理器] 阻止 {} 重复生成（已在参赛者队列中）", typeKeyStr);
                return true;
            }
        }

        // ===== 通过所有检查：标记此类型已在本 tick 内允许生成 =====
        SPAWNED_THIS_TICK.computeIfAbsent(dimKey, k -> ConcurrentHashMap.newKeySet()).add(type);

        return false;
    }

    /**
     * 根据苏醒等级获取应该生成的古朗基类型
     */
    public static EntityType<? extends Gurongi> getGurongiForLevel(int level) {
        if (GURONGI_SPAWN_MAP == null) {
            initializeSpawnMap();
        }

        for (Map.Entry<EntityType<? extends Gurongi>, Integer> entry : GURONGI_SPAWN_MAP.entrySet()) {
            if (entry.getValue() == level) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取当前应该生成的古朗基
     */
    public static EntityType<? extends Gurongi> getCurrentGurongi(Level world) {
        int currentLevel = AddonWorldData.get(world).gurongiSpawn();
        return getGurongiForLevel(currentLevel);
    }

    /**
     * 获取所有已注册的古朗基实体类型集合（仅供调试/查询指令使用）。
     *
     * @return 所有注册的古朗基 EntityType 集合
     */
    public static java.util.Set<EntityType<? extends Gurongi>> getAllRegisteredTypes() {
        if (GURONGI_SPAWN_MAP == null) {
            initializeSpawnMap();
        }
        return java.util.Collections.unmodifiableSet(GURONGI_SPAWN_MAP.keySet());
    }

    /**
     * 根据古朗基的实体注册名称查找对应的 EntityType。
     * <p>
     * 例如 {@code "zu_gumun_ba"} → Zu-Gumun-Ba 的 EntityType。
     *
     * @param registryName 实体注册名（如 {@code "zu_gumun_ba"}）
     * @return 匹配的 EntityType，未找到则返回 null
     */
    @Nullable
    public static EntityType<? extends Gurongi> getTypeByRegistryName(String registryName) {
        if (GURONGI_SPAWN_MAP == null) {
            initializeSpawnMap();
        }
        if (registryName == null || registryName.isEmpty()) return null;
        for (EntityType<? extends Gurongi> type : GURONGI_SPAWN_MAP.keySet()) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key != null && key.getPath().equalsIgnoreCase(registryName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取指定古朗基实体类型所需的世界生成等级。
     *
     * @param type 古朗基 EntityType
     * @return 生成等级，如果未注册则返回 -1
     */
    public static int getLevelForType(EntityType<? extends Gurongi> type) {
        if (GURONGI_SPAWN_MAP == null) {
            initializeSpawnMap();
        }
        return GURONGI_SPAWN_MAP.getOrDefault(type, -1);
    }

    /**
     * 清除 tick 级去重集合，由 {@link com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGlobalManager}
     * 在每 tick 结束时自动调用。
     * <p>
     * 这确保了每个游戏 tick 自然生成系统都能重新尝试生成古朗基。
     */
    public static void clearSpawnedThisTick() {
        SPAWNED_THIS_TICK.clear();
    }
}