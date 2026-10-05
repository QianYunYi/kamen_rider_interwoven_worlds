package com.qianyunyi.rider.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.IRiderDataModule;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.data.module.RiderDataModuleRegistry;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameManager;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameStage;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameStatus;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiHuntingLaw;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiHuntingLaw.CheckResult;
import com.qianyunyi.rider.entity.kaijin.gurongi.registry.GurongiSpawnRegistry;
import com.qianyunyi.rider.network.AddonWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "kamen_rider_interwoven_worlds", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KamenRiderInterwovenWorldsModCommand {
    private static final String DEBUG_PASSWORD = "TheUnboundPage";
    private static final Set<UUID> DEBUG_MODE_PLAYERS = new HashSet<>();

    /** 管理员玩家名称集合（不区分大小写比较） */
    private static final Set<String> ADMIN_PLAYERS = Set.of("dev", "qianyunyi");

    /** 所有作弊码列表 */
    private static final String[] CHEAT_CODES = {"SomethingForNothing", "KuugaAllForms"};

    /**
     * 古朗基类型名补全提示提供者。
     * <p>
     * 列出所有已注册的古朗基实体注册名（如 {@code zu_gumun_ba}）供指令补全。
     */
    private static final SuggestionProvider<CommandSourceStack> GURONGI_TYPE_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    getAllGurongiTypeNames(),
                    builder
            );

    /**
     * 获取所有已注册古朗基的实体注册名列表（小写，不含命名空间）。
     */
    private static List<String> getAllGurongiTypeNames() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (EntityType<? extends Gurongi> type : GurongiSpawnRegistry.getAllRegisteredTypes()) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key != null) {
                names.add(key.getPath());
            }
        }
        java.util.Collections.sort(names);
        return names;
    }

    /**
     * 根据古朗基类型名在当前维度中查找对应的古朗基实体。
     *
     * @param level        服务端世界
     * @param typeName     古朗基实体注册名（如 {@code "zu_gumun_ba"}）
     * @return 找到的 Gurongi 实体，未加载或不存在则返回 null
     */
    @Nullable
    private static Gurongi findGurongiByTypeName(ServerLevel level, String typeName) {
        EntityType<? extends Gurongi> targetType = GurongiSpawnRegistry.getTypeByRegistryName(typeName);
        if (targetType == null) return null;
        return findGurongiByType(level, targetType);
    }

    /**
     * 在当前维度中查找指定 EntityType 的存活古朗基实体。
     */
    @Nullable
    private static Gurongi findGurongiByType(ServerLevel level, EntityType<? extends Gurongi> type) {
        if (type == null) return null;
        for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
            if (e instanceof Gurongi g && g.isAlive() && g.getType() == type) {
                return g;
            }
        }
        return null;
    }

    // ========== 权限判断 ==========

    private static boolean isAdminPlayer(ServerPlayer player) {
        return ADMIN_PLAYERS.contains(player.getScoreboardName().toLowerCase(Locale.ROOT));
    }

    /** 检查指令源是否为管理员玩家 */
    private static boolean isAdminSource(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return isAdminPlayer(player);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /** 检查指令源是否为管理员或已开启调试模式的玩家 */
    private static boolean hasAdminAccess(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return isAdminPlayer(player) || DEBUG_MODE_PLAYERS.contains(player.getUUID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    // ========== 自动补全提供器 ==========

    /**
     * 作弊码补全：
     * - 管理员玩家：显示所有作弊码
     * - 开启了调试模式的玩家：显示所有作弊码
     * - 普通玩家：不显示任何建议（需要手动输入完整作弊码）
     */
    private static final SuggestionProvider<CommandSourceStack> CHEAT_SUGGESTION_PROVIDER = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (isAdminPlayer(player) || DEBUG_MODE_PLAYERS.contains(player.getUUID())) {
                return SharedSuggestionProvider.suggest(CHEAT_CODES, builder);
            }
            // 普通玩家看不到作弊码建议，必须手动输入
            return SharedSuggestionProvider.suggest(new String[]{}, builder);
        } catch (CommandSyntaxException e) {
            return SharedSuggestionProvider.suggest(new String[]{}, builder);
        }
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // ===== 根指令 /rider =====
        LiteralCommandNode<CommandSourceStack> riderNode = Commands.literal("rider")
                .requires(source -> source.hasPermission(0))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/rider cheat <code>  - 输入作弊码"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/rider data ...      - 调试数据（管理员）"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/rider debug         - 调试模式"), false);
                    return 1;
                })
                // debug 指令（普通玩家可输入密码开启）
                .then(Commands.literal("debug")
                        .executes(KamenRiderInterwovenWorldsModCommand::toggleDebugMode)
                        .then(Commands.argument("password", StringArgumentType.word())
                                .executes(KamenRiderInterwovenWorldsModCommand::toggleDebugModeWithPassword)
                        )
                )
                // cheat 指令 - 普通玩家需要手动输入完整作弊码
                .then(Commands.literal("cheat")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .suggests(CHEAT_SUGGESTION_PROVIDER)
                                .executes(KamenRiderInterwovenWorldsModCommand::executeCheatCode)
                        )
                )
                // data 指令 - 动态构建（管理员/调试模式玩家可见）
                .then(buildDataCommand().requires(KamenRiderInterwovenWorldsModCommand::hasAdminAccess))
                .build();

        dispatcher.getRoot().addChild(riderNode);
    }

    // ========== data 指令动态构建 ==========

    /**
     * 动态构建 /rider data 指令树：
     * 遍历所有已注册的数据模块，为每个模块的 {@link DataField} 自动生成 get/set 子指令。
     * 同时包含 gurongi（古朗基/基基鲁游戏）调试指令。
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildDataCommand() {
        var dataBuilder = Commands.literal("data");

        // ---- 1. 数据模块调试指令（动态生成） ----
        List<IRiderDataModule> modules = RiderDataModuleRegistry.createAllModules();
        for (IRiderDataModule module : modules) {
            String moduleName = normalizeModuleName(module.getModuleName());
            List<DataField> fields = module.getDebugFields();
            if (fields.isEmpty()) continue;

            var moduleBuilder = Commands.literal(moduleName);
            var getBuilder = Commands.literal("get");
            var setBuilder = Commands.literal("set");

            for (DataField field : fields) {
                // ---- get <field> ----
                getBuilder.then(Commands.literal(field.name())
                        .executes(ctx -> handleGetField(ctx, moduleName, field.name()))
                );

                // ---- set <field> ----
                switch (field.type()) {
                    case BOOL -> setBuilder.then(Commands.literal(field.name())
                            .then(Commands.literal("true")
                                    .executes(ctx -> handleSetField(ctx, moduleName, field.name(), 1)))
                            .then(Commands.literal("false")
                                    .executes(ctx -> handleSetField(ctx, moduleName, field.name(), 0)))
                    );
                    case INT -> {
                        int min = field.min() != null ? field.min().intValue() : Integer.MIN_VALUE;
                        int max = field.max() != null ? field.max().intValue() : Integer.MAX_VALUE;
                        setBuilder.then(Commands.literal(field.name())
                                .then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                                        .executes(ctx -> {
                                            int val = IntegerArgumentType.getInteger(ctx, "value");
                                            return handleSetField(ctx, moduleName, field.name(), val);
                                        })
                                )
                        );
                    }
                    case FLOAT -> {
                        float min = field.min() != null ? field.min().floatValue() : Float.NEGATIVE_INFINITY;
                        float max = field.max() != null ? field.max().floatValue() : Float.POSITIVE_INFINITY;
                        setBuilder.then(Commands.literal(field.name())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(min, max))
                                        .executes(ctx -> {
                                            float val = FloatArgumentType.getFloat(ctx, "value");
                                            return handleSetField(ctx, moduleName, field.name(), val);
                                        })
                                )
                        );
                    }
                }
            }

            moduleBuilder.then(getBuilder);
            moduleBuilder.then(setBuilder);
            dataBuilder.then(moduleBuilder);
        }

        // ---- 2. gurongi（古朗基/基基鲁游戏）调试指令 ----
        dataBuilder.then(Commands.literal("gurongi")
                // ===== get 子指令（合并了原 get + manager + list + all） =====
                .then(Commands.literal("get")
                        // /rider data gurongi get — 全局游戏状态概览（原 manager + get）
                        .executes(KamenRiderInterwovenWorldsModCommand::gurongiManagerStatus)
                        // /rider data gurongi get participants — 列出所有参赛者
                        .then(Commands.literal("participants")
                                .executes(KamenRiderInterwovenWorldsModCommand::gurongiManagerParticipants))
                        // /rider data gurongi get turn — 当前回合信息
                        .then(Commands.literal("turn")
                                .executes(KamenRiderInterwovenWorldsModCommand::gurongiManagerTurn))
                        // /rider data gurongi get list [radius] — 列出附近古朗基（原 list）
                        .then(Commands.literal("list")
                                .executes(ctx -> listNearbyGurongi(ctx, 64))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(8, 256))
                                        .executes(ctx -> listNearbyGurongi(ctx, IntegerArgumentType.getInteger(ctx, "radius")))))
                        // /rider data gurongi get all — 列出世界中所有古朗基（原 all）
                        .then(Commands.literal("all")
                                .executes(KamenRiderInterwovenWorldsModCommand::listAllGurongi))
                )
                // 设置基基鲁游戏阶段
                .then(Commands.literal("set")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 19))
                                .executes(KamenRiderInterwovenWorldsModCommand::setGurongiStage)
                        )
                )
                // 列出阶段对照表
                .then(Commands.literal("stages")
                        .executes(KamenRiderInterwovenWorldsModCommand::listGurongiStages)
                )
                // 重置基基鲁游戏（清除所有参赛者、重生数据，阶段归零）
                .then(Commands.literal("reset")
                        .executes(KamenRiderInterwovenWorldsModCommand::resetGurongiGame)
                )
                // info <typeName> — 查询古朗基详细信息（含目标信息，合并了原 info + target）
                .then(Commands.literal("info")
                        .then(Commands.argument("typeName", StringArgumentType.word())
                                .suggests(GURONGI_TYPE_SUGGESTIONS)
                                .executes(KamenRiderInterwovenWorldsModCommand::gurongiInfo))
                )
                // 模拟狩猎法则检查
                .then(Commands.literal("check")
                        .then(Commands.argument("hunterType", StringArgumentType.word())
                                .suggests(GURONGI_TYPE_SUGGESTIONS)
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(KamenRiderInterwovenWorldsModCommand::checkHuntingLaw)))
                )
        );

        return dataBuilder;
    }

    /**
     * 标准化模块名：去除 "DataModule" 或 "Data" 后缀，转为小写。
     * 例如 "KuugaDataModule" → "kuuga", "ShowaData" → "showa"
     */
    private static String normalizeModuleName(String rawName) {
        String lower = rawName.toLowerCase(Locale.ROOT);
        if (lower.endsWith("datamodule")) {
            return lower.substring(0, lower.length() - 10);
        }
        if (lower.endsWith("data")) {
            return lower.substring(0, lower.length() - 4);
        }
        return lower;
    }

    // ========== 动态字段 Get/Set 处理 ==========

    /**
     * 获取指定模块指定字段的调试值（通过 DataField::getter 回调）。
     */
    private static int handleGetField(CommandContext<CommandSourceStack> ctx, String moduleName, String fieldName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 动态查找字段描述符
        DataField field = findDataField(data, moduleName, fieldName);
        if (field == null) {
            ctx.getSource().sendFailure(Component.literal("§c找不到字段: " + moduleName + "." + fieldName));
            return 0;
        }

        Number val = field.getter().apply(data);
        String displayVal = field.type() == DataField.DataType.BOOL
                ? (val.intValue() != 0 ? "true" : "false")
                : val.toString();

        ctx.getSource().sendSuccess(() ->
                Component.literal("§a" + moduleName + "." + fieldName + " = §f" + displayVal), true
        );
        return 1;
    }

    /**
     * 设置指定模块指定字段的调试值（通过 DataField::setter 回调）。
     */
    private static int handleSetField(CommandContext<CommandSourceStack> ctx, String moduleName, String fieldName, Number value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        DataField field = findDataField(data, moduleName, fieldName);
        if (field == null) {
            ctx.getSource().sendFailure(Component.literal("§c找不到字段: " + moduleName + "." + fieldName));
            return 0;
        }

        field.setter().accept(data, value);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§a" + moduleName + "." + fieldName + " 已设置为 §f" + value), true
        );
        return 1;
    }

    /**
     * 在玩家数据中查找指定模块的指定调试字段。
     */
    private static DataField findDataField(RiderPlayerData data, String moduleName, String fieldName) {
        IRiderDataModule module = data.getModule(moduleName);
        if (module == null) {
            // 尝试用原始名查找（兼容大小写）
            for (IRiderDataModule m : data.getLoadedModules().values()) {
                if (normalizeModuleName(m.getModuleName()).equals(moduleName)) {
                    module = m;
                    break;
                }
            }
        }
        if (module == null) return null;

        return module.getDebugFields().stream()
                .filter(f -> f.name().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    // ========== Debug 模式 ==========

    private static int toggleDebugMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        if (isAdminPlayer(player)) {
            // 管理员玩家直接开关
            if (DEBUG_MODE_PLAYERS.contains(player.getUUID())) {
                DEBUG_MODE_PLAYERS.remove(player.getUUID());
                ctx.getSource().sendSuccess(() -> Component.literal("§c调试模式已关闭"), true);
            } else {
                DEBUG_MODE_PLAYERS.add(player.getUUID());
                ctx.getSource().sendSuccess(() -> Component.literal("§a开发者账号自动开启调试模式！"), true);
            }
            return 1;
        }

        // 普通玩家开关
        if (DEBUG_MODE_PLAYERS.contains(player.getUUID())) {
            DEBUG_MODE_PLAYERS.remove(player.getUUID());
            ctx.getSource().sendSuccess(() -> Component.literal("§c调试模式已关闭"), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("§c需要密码才能开启调试模式，请使用 /rider debug <password>"));
        }
        return 0;
    }

    /** 通过密码开启调试模式 */
    private static int toggleDebugModeWithPassword(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String password = StringArgumentType.getString(ctx, "password");

        if (!DEBUG_PASSWORD.equals(password)) {
            ctx.getSource().sendFailure(Component.literal("§c密码错误！"));
            return 0;
        }

        if (DEBUG_MODE_PLAYERS.contains(player.getUUID())) {
            ctx.getSource().sendSuccess(() -> Component.literal("§a调试模式已通过密码开启"), false);
        } else {
            DEBUG_MODE_PLAYERS.add(player.getUUID());
            ctx.getSource().sendSuccess(() -> Component.literal("§a通过密码验证，已开启调试模式！"), true);
        }
        return 1;
    }

    // ========== Cheat 作弊码 ==========

    private static int executeCheatCode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String code = StringArgumentType.getString(ctx, "code");

        if ("SomethingForNothing".equals(code)) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            KuugaDataModule kuuga = data.getKuugaData();
            if (kuuga != null) {
                kuuga.setAmadamFusion(100.0f);
                float newAmadamMax = kuuga.getAmadamEnergyMax() * 16;
                kuuga.setAmadamEnergyMax(newAmadamMax);
                kuuga.setAmadamEnergy(newAmadamMax);
                float newSuperMax = kuuga.getSuperEnergyMax() * 16;
                kuuga.setSuperEnergyMax(newSuperMax);
                kuuga.setSuperEnergy(newSuperMax);
                ctx.getSource().sendSuccess(() -> Component.literal("§a作弊码生效！融合度100%，能量×16"), true);
            }
            return 1;
        }

        if ("KuugaAllForms".equals(code)) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            KuugaDataModule kuuga = data.getKuugaData();
            if (kuuga != null) {
                kuuga.setMightyKuuga(true);
                kuuga.setDragonForm(true);
                kuuga.setPegasusForm(true);
                kuuga.setTitanForm(true);
                kuuga.setHasGoldPower(true);
                kuuga.setAmazinglyMightyForm(true);
                kuuga.setPrototypeUltimateForm(true);
                kuuga.setUltimateForm(true);
                kuuga.setRisingUltimateForm(true);
                kuuga.setSuperRisingUltimateForm(true);
                kuuga.setSomersault(true);
                kuuga.setGouramUnlocked(true);
                kuuga.setHasStoneOfEarth(true);
                kuuga.setAmadamFusion(100.0f);
                float newAmadamMax = kuuga.getAmadamEnergyMax() * 16;
                kuuga.setAmadamEnergyMax(newAmadamMax);
                kuuga.setAmadamEnergy(newAmadamMax);
                float newSuperMax = kuuga.getSuperEnergyMax() * 16;
                kuuga.setSuperEnergyMax(newSuperMax);
                kuuga.setSuperEnergy(newSuperMax);
                kuuga.setHasSummonedDaguba(true);
                ctx.getSource().sendSuccess(() -> Component.literal("§a作弊码生效！空我全形态已解锁，融合度100%，能量×16"), true);
            }
            return 1;
        }

        ctx.getSource().sendFailure(Component.literal("§c未知作弊码"));
        return 0;
    }

    // ========== 基基鲁游戏调试指令（位于 data gurongi 下） ==========

    /**
     * ============================================================
     *  等级 ↔ 游戏阶段 & 集团 统一映射表
     * ============================================================
     *
     * 等级 (int) 对应 {@link GurongiGameStage} 和 {@link GurongiGroup} 的关系：
     *
     *   0  → NONE              无
     *   1  → AUDITION (滋集团)  Zu-Gumun-Ba   (滋·古穆姆·巴)
     *   2  → AUDITION (滋集团)  Zu-Gooma-Gu   (滋·强魔·古)
     *   3  → AUDITION (滋集团)  Zu-Mebio-Da   (滋·魅比欧·达)
     *   4  → AUDITION (滋集团)  Zu-Garume-Re  (滋·伽卢魅·列)
     *   5  → AUDITION (滋集团)  Zu-Baduu-Ba   (滋·巴滋·巴)
     *   6  → AUDITION (滋集团)  Zu-Zain-Da    (滋·查因·达)
     *   7+ → SELECTION (魅集团) 及以上（预留）
     */

    /** 获取等级对应的游戏阶段 */
    private static GurongiGameStage getStageForLevel(int level) {
        if (level <= 0) return GurongiGameStage.NONE;
        if (level <= 6) return GurongiGameStage.AUDITION;
        if (level <= 10) return GurongiGameStage.SELECTION;
        if (level <= 15) return GurongiGameStage.SEMI_FINAL;
        if (level <= 18) return GurongiGameStage.WHITE_DARK_DUEL;
        return GurongiGameStage.FINAL;
    }

    /** 获取等级对应的集团 */
    private static GurongiGroup getGroupForLevel(int level) {
        return GurongiGameManager.getGroupForStage(getStageForLevel(level));
    }

    /** 同步 AddonWorldData 的整数等级和 GurongiGameManager 的游戏阶段 */
    private static void syncGurongiStage(ServerLevel level, int spawnLevel) {
        AddonWorldData worldData = AddonWorldData.get(level);
        GurongiGameManager manager = GurongiGameManager.get(level);

        worldData.setGurongiSpawn(spawnLevel);

        // 同步游戏管理器的 enum 阶段
        GurongiGameStage targetStage = getStageForLevel(spawnLevel);

        // ★ 无论阶段是否变化，都重置参赛者队列，清除已消失古朗基的残留记录
        // 用户手动设置游戏等级时，期望一个干净的初始状态
        manager.resetParticipants();

        // ★ 重置倒计时（避免与手动设置的阶段冲突）
        manager.resetCountdown();

        if (manager.getGameStage() != targetStage) {
            manager.setGameStage(targetStage);
        }

        // 同步 AddonWorldData 的 enum 阶段（保持三方一致）
        if (worldData.getGameStage() != targetStage) {
            worldData.setGameStage(targetStage);
        }

        worldData.sync(level);
    }

    /**
     * 查询当前基基鲁游戏状态（等级 + 阶段 + 生成实体）
     * /rider data gurongi get
     */
    private static int getGurongiStage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        AddonWorldData data = AddonWorldData.get(level);
        GurongiGameManager manager = GurongiGameManager.get(level);

        int spawnLevel = data.getGurongiSpawn();
        GurongiGameStage gameStage = manager.getGameStage();
        GurongiGroup group = getGroupForLevel(spawnLevel);

        EntityType<?> currentType = GurongiSpawnRegistry.getCurrentGurongi(level);
        String typeName = currentType != null
                ? currentType.getDescription().getString()
                : "§7无（最终阶段已结束）";

        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 基基鲁游戏状态 ====="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§b生成等级:  §f" + spawnLevel), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§b游戏阶段:  §f" + formatGameStage(gameStage)), false);
        if (group != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§b当前集团:  §f" + group.getChineseName()), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§b生成古朗基: §f" + typeName), false);

        return 1;
    }

    /**
     * 列出基基鲁游戏阶段对照表
     * /rider data gurongi stages
     */
    private static int listGurongiStages(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§e====== 基基鲁游戏阶段对照表 ======"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- NONE ----
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 0  §8┃ §7未开始 §8┃ 无"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- 滋集团 · AUDITION ----
        ctx.getSource().sendSuccess(() -> Component.literal("§e═══ 试镜阶段 [AUDITION] — 滋(Zu)集团 ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 1  §8┃ §fZu-Gumun-Ba    §8┃ §7滋·古穆姆·巴（蜘蛛）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 2  §8┃ §fZu-Gooma-Gu    §8┃ §7滋·强魔·古（犀牛）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 3  §8┃ §fZu-Mebio-Da    §8┃ §7滋·魅比欧·达（蜥蜴）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 4  §8┃ §fZu-Garume-Re   §8┃ §7滋·伽卢魅·列（变色龙）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 5  §8┃ §fZu-Baduu-Ba    §8┃ §7滋·巴滋·巴（蝙蝠）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 6  §8┃ §fZu-Zain-Da     §8┃ §7滋·查因·达（最后の滋集团）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- 魅集团 · SELECTION ----
        ctx.getSource().sendSuccess(() -> Component.literal("§a═══ 选拔阶段 [SELECTION] — 魅(Me)集团 ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 7-10 §8┃ §7待添加"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- 葛集团 · SEMI_FINAL ----
        ctx.getSource().sendSuccess(() -> Component.literal("§b═══ 半决赛阶段 [SEMI_FINAL] — 葛(Go)集团 ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 11-15§8┃ §7待添加"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- 拉集团 · WHITE_DARK_DUEL ----
        ctx.getSource().sendSuccess(() -> Component.literal("§5═══ 白暗决斗阶段 [WHITE_DARK_DUEL] — ラ(Ra)集团 ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 16-18§8┃ §7待添加"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        // ---- 恩集团 · FINAL ----
        ctx.getSource().sendSuccess(() -> Component.literal("§c═══ 最终阶段 [FINAL] — 恩(N)集团 ═══"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7等级 19+  §8┃ §7待添加"), false);
        ctx.getSource().sendSuccess(() -> Component.literal(""), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§e========================================"), false);
        return 1;
    }

    /**
     * 设置基基鲁游戏阶段（同步更新等级和游戏阶段）
     * /rider data gurongi set <level>
     */
    private static int setGurongiStage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        int newLevel = IntegerArgumentType.getInteger(ctx, "level");

        syncGurongiStage(level, newLevel);

        ctx.getSource().sendSuccess(() -> Component.literal("§a基基鲁游戏已同步更新:"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  生成等级: §f" + newLevel), false);

        GurongiGameStage stage = getStageForLevel(newLevel);
        GurongiGroup group = getGroupForLevel(newLevel);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  游戏阶段: §f" + formatGameStage(stage)), false);
        if (group != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7  当前集团: §f" + group.getChineseName()), false);
        }

        EntityType<?> spawnedType = GurongiSpawnRegistry.getGurongiForLevel(newLevel);
        if (spawnedType != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7  将生成: " + spawnedType.getDescription().getString()), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§7  将生成: §e（该等级无直接映射实体，由自定义生成逻辑处理）"), false);
        }

        return 1;
    }

    // ====================================================================
    //  /rider data gurongi reset  — 重置基基鲁游戏
    // ====================================================================

    /**
     * 重置基基鲁游戏：清除所有参赛者数据、重生数据、重置生成等级为 0（封印状态）。
     * <p>
     * 用法：{@code /rider data gurongi reset}
     */
    private static int resetGurongiGame(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        AddonWorldData worldData = AddonWorldData.get(level);
        GurongiGameManager manager = GurongiGameManager.get(level);

        // 重置管理器参赛者数据
        manager.resetParticipants();

        // 重置倒计时
        manager.resetCountdown();

        // 重置游戏阶段和生成等级
        manager.setGameStage(GurongiGameStage.NONE);
        worldData.setGameStage(GurongiGameStage.NONE);
        worldData.setGurongiSpawn(0);
        worldData.sync(level);

        ctx.getSource().sendSuccess(() -> Component.literal("§a基基鲁游戏已完全重置！"), true);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  - 参赛者队列已清空"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  - 重生数据已清除"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  - 生成等级已归零（石棺封印状态）"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7  - 游戏阶段已重置为: §f" + formatGameStage(GurongiGameStage.NONE)), false);

        return 1;
    }

    // ====================================================================
    //  /rider data gurongi list [radius]  — 列出附近古朗基
    // ====================================================================

    /**
     * 列出玩家附近指定半径内所有已加载的古朗基实体。
     * <p>
     * 用法：{@code /rider data gurongi list [radius]}
     *
     * @param radius 搜索半径（方块），默认 64
     */
    private static int listNearbyGurongi(CommandContext<CommandSourceStack> ctx, int radius) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = player.position();

        AABB searchBox = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                                  pos.x + radius, pos.y + radius, pos.z + radius);
        java.util.List<Gurongi> nearby = level.getEntitiesOfClass(Gurongi.class, searchBox,
                g -> g != null && g.isAlive());

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§e===== 附近古朗基 (§b" + radius + "§e 格内, 共 §f" + nearby.size() + "§e 只) ====="), false);

        if (nearby.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7附近没有发现古朗基"), false);
            return 1;
        }

        for (int i = 0; i < nearby.size(); i++) {
            Gurongi g = nearby.get(i);
            int dx = (int) (g.getX() - pos.x);
            int dz = (int) (g.getZ() - pos.z);
            int dy = (int) (g.getY() - pos.y);
            int index = i + 1;
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§f%d. §e%s §7[§f%s§7]  §b(%+d, %+d, %+d)  §7HP: §f%.0f/§f%.0f",
                    index,
                    g.getName().getString(),
                    g.getType().getDescription().getString(),
                    dx, dy, dz,
                    g.getHealth(), g.getMaxHealth()
            )), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e========================================"), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi all  — 列出世界中所有加载的古朗基
    // ====================================================================

    /**
     * 列出当前维度中所有已加载（已生成）的古朗基实体。
     * <p>
     * 用法：{@code /rider data gurongi all}
     */
    private static int listAllGurongi(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();

        java.util.List<Gurongi> all = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
            if (e instanceof Gurongi g && g.isAlive()) {
                all.add(g);
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§e===== 世界古朗基列表 (§f" + all.size() + "§e 只, 维度: §f" + level.dimension().location() + "§e) ====="), false);

        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7当前维度没有加载任何古朗基"), false);
            return 1;
        }

        for (int i = 0; i < all.size(); i++) {
            Gurongi g = all.get(i);
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§f%s §e%s §7[§f%s§7] §b(%.0f, %.0f, %.0f) §7HP: §f%.0f/%.0f  §7集团: §f%s  §7[%s]",
                    g.getName().getString(),
                    g.getType().getDescription().getString(),
                    g.getType().toShortString(),
                    g.getX(), g.getY(), g.getZ(),
                    g.getHealth(), g.getMaxHealth(),
                    g.group != null ? g.group.getChineseName() : "?",
                    getSpawnTypeDisplay(g)
            )), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e============================================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi info <target>  — 查询古朗基详细信息
    // ====================================================================

    /**
     * 查询指定古朗基的完整详细信息。
     * <p>
     * 用法：{@code /rider data gurongi info <target>}
     */
    private static int gurongiInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String typeName = StringArgumentType.getString(ctx, "typeName");
        ServerLevel level = ctx.getSource().getLevel();
        Gurongi gurongi = findGurongiByTypeName(level, typeName);

        if (gurongi == null) {
            // 检查类型名是否有效
            EntityType<? extends Gurongi> targetType = GurongiSpawnRegistry.getTypeByRegistryName(typeName);
            if (targetType == null) {
                ctx.getSource().sendFailure(Component.literal("§c未知的古朗基类型: §f" + typeName
                        + "§c。可用类型：§f" + String.join("§7, §f", getAllGurongiTypeNames())));
                return 0;
            }
            ctx.getSource().sendFailure(Component.literal("§c古朗基 §f" + typeName + "§c 在当前维度未加载（可能未生成或在其他维度）"));
            // 尝试从管理器查询存档数据
            displayManagerInfoForType(ctx, level, targetType);
            return 0;
        }

        return displayGurongiInfo(ctx, gurongi);
    }

    /**
     * 当古朗基实体未加载时，从管理器的类型级持久数据中显示存档信息。
     */
    private static void displayManagerInfoForType(CommandContext<CommandSourceStack> ctx, ServerLevel level,
                                                   EntityType<? extends Gurongi> type) {
        GurongiGameManager manager = GurongiGameManager.get(level);
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (typeKey == null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§c无法获取类型注册名"), false);
            return;
        }
        String typeName = typeKey.getPath();

        ctx.getSource().sendSuccess(() -> Component.literal("§7--- 管理器存档信息（类型级） ---"), false);

        // 1. 查询类型级持久数据
        GurongiGameManager.TypeGameData typeData = manager.getTypeGameData(typeName);
        if (typeData != null) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("§7类型: §f%s  §7总击杀: §f%d  §7已完成: %s",
                            typeName, typeData.killCount,
                            typeData.hasCompleted ? "§a是" : "§c否")
            ), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("§7晋升至: §f%s  §7有资格: %s",
                            typeData.promotedToGroup != null ? typeData.promotedToGroup.getChineseName()
                                    : "§c无（初始集团）",
                            typeData.eligible ? "§a是" : "§c否")
            ), false);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    String.format("§7最后阶段: §f%s", formatGameStage(typeData.lastStage))
            ), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§7无类型级持久数据（尚未完成过游戏）"), false);
        }

        // 2. 查询当前参赛者队列中匹配此类型的活跃条目
        ctx.getSource().sendSuccess(() -> Component.literal("§7--- 参赛者记录 ---"), false);
        List<UUID> queue = manager.getParticipantQueue();
        boolean found = false;
        for (UUID uuid : queue) {
            GurongiGameManager.ParticipantEntry entry = manager.getParticipant(uuid);
            if (entry != null && typeName.equals(entry.entityTypeId)) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        String.format("§7参赛者 UUID: §f%s  §7击杀: §f%d  §7状态: %s  §7有资格: %s",
                                uuid, entry.killCount,
                                formatGameStatus(entry.status),
                                entry.eligible ? "§a是" : "§c否")
                ), false);
                found = true;
            }
        }
        if (!found) {
            ctx.getSource().sendSuccess(() -> Component.literal("§c当前无此类型的活跃参赛者"), false);
        }
    }

    /**
     * 显示古朗基的完整详细信息。
     */
    private static int displayGurongiInfo(CommandContext<CommandSourceStack> ctx, Gurongi gurongi) {
        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 古朗基详细信息 ====="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7名称: §f" + gurongi.getName().getString()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7类型: §f" + gurongi.getType().getDescription().getString()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7UUID: §f" + gurongi.getUUID()), false);

        String groupName = gurongi.group != null ? gurongi.group.getChineseName() + " (" + gurongi.group.name() + ")" : "§c?";
        ctx.getSource().sendSuccess(() -> Component.literal("§7集团: §f" + groupName), false);

        ctx.getSource().sendSuccess(() -> Component.literal(String.format("§7位置: §f%.1f, %.1f, %.1f  §7维度: §f%s",
                gurongi.getX(), gurongi.getY(), gurongi.getZ(),
                gurongi.level().dimension().location())), false);

        ctx.getSource().sendSuccess(() -> Component.literal(String.format("§7生命: §f%.1f§7/§f%.1f",
                gurongi.getHealth(), gurongi.getMaxHealth())), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§7纹理: §f" + gurongi.getTexture()), false);

        GurongiGameStatus status = gurongi.getGameStatus();
        ctx.getSource().sendSuccess(() -> Component.literal("§7游戏状态: §f" + formatGameStatus(status)), false);

        GurongiGameStage stage = gurongi.getGameStage();
        ctx.getSource().sendSuccess(() -> Component.literal("§7游戏阶段: §f" + formatGameStage(stage)), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§7游戏击杀数: §f" + gurongi.getGurongiGameCount()), false);

        ctx.getSource().sendSuccess(() -> Component.literal(String.format("§7杀戮欲望: §f%.2f§7/§f1.00",
                gurongi.getKillingDesire())), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§7可重生: " + (gurongi.isRespawnable() ? "§a是" : "§c否")), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§7玩家放置: " + (gurongi.isPlayerPlaced() ? "§a是" : "§c否")), false);

        // 显示狼王转换标记
        boolean isWolfKingConverted = isWolfKingConverted(gurongi);
        if (isWolfKingConverted) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7狼王转换: §c是（属性减半）"), false);
        }

        // 显示生成类型
        String spawnTypeInfo = getSpawnTypeDisplay(gurongi);
        ctx.getSource().sendSuccess(() -> Component.literal("§7生成方式: " + spawnTypeInfo), false);

        if (gurongi.getGameObjective() != null) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§7游戏目标: §f%s  §7进度: §f%d§7/§f%s",
                    gurongi.getGameObjective().getDescription(),
                    gurongi.getObjectiveProgress(),
                    gurongi.getGameObjective().getTargetCount()
            )), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§7游戏目标: §c无（恩集团不受限制）"), false);
        }

        ServerLevel serverLevel = ctx.getSource().getLevel();
        GurongiGameManager manager = GurongiGameManager.get(serverLevel);
        boolean registered = manager.isRegistered(gurongi.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("§7已注册到管理器: " + (registered ? "§a是" : "§c否")), false);

        if (registered) {
            GurongiGameManager.ParticipantEntry entry = manager.getParticipant(gurongi.getUUID());
            if (entry != null) {
                ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                        "§7管理器击杀数: §f%d  §7有资格: %s",
                        entry.killCount,
                        entry.eligible ? "§a是" : "§c否"
                )), false);
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi target <target>  — 查询古朗基目标信息
    // ====================================================================

    /**
     * 查询指定古朗基的当前游戏目标与进度详情。
     * <p>
     * 用法：{@code /rider data gurongi target <target>}
     */
    private static int gurongiTargetInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String typeName = StringArgumentType.getString(ctx, "typeName");
        ServerLevel level = ctx.getSource().getLevel();
        Gurongi gurongi = findGurongiByTypeName(level, typeName);

        if (gurongi == null) {
            EntityType<? extends Gurongi> targetType = GurongiSpawnRegistry.getTypeByRegistryName(typeName);
            if (targetType == null) {
                ctx.getSource().sendFailure(Component.literal("§c未知的古朗基类型: §f" + typeName
                        + "§c。可用类型：§f" + String.join("§7, §f", getAllGurongiTypeNames())));
                return 0;
            }
            ctx.getSource().sendFailure(Component.literal("§c古朗基 §f" + typeName + "§c 在当前维度未加载，无法查询目标信息"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 古朗基目标信息 ====="), false);

        if (gurongi.getGameObjective() == null) {
            if (gurongi.group == GurongiGroup.N) {
                ctx.getSource().sendSuccess(() -> Component.literal("§c恩（N）集团无游戏目标，可以随意杀戮"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§c未分配游戏目标"), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§7目标类型: §f" + gurongi.getGameObjective().getDescription()), false);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7进度: §f%d§7/§f%d",
                gurongi.getObjectiveProgress(),
                gurongi.getGameObjective().getTargetCount()
        )), false);

        boolean completed = gurongi.isObjectiveComplete();
        ctx.getSource().sendSuccess(() -> Component.literal("§7目标完成: " + (completed ? "§a是" : "§c否")), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi manager — 游戏管理器状态
    // ====================================================================

    /**
     * 查看基基鲁游戏管理器的全局状态概览。
     * <p>
     * 用法：{@code /rider data gurongi manager}
     */
    private static int gurongiManagerStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        GurongiGameManager manager = GurongiGameManager.get(level);
        AddonWorldData worldData = AddonWorldData.get(level);

        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 基基鲁游戏概览 ====="), false);

        // 生成等级
        int spawnLevel = worldData.getGurongiSpawn();
        ctx.getSource().sendSuccess(() -> Component.literal("§b生成等级: §f" + spawnLevel), false);

        // 游戏阶段
        GurongiGameStage stage = manager.getGameStage();
        ctx.getSource().sendSuccess(() -> Component.literal("§b游戏阶段: §f" + formatGameStage(stage)), false);

        // 对应集团
        GurongiGroup group = GurongiGameManager.getGroupForStage(stage);
        if (group != null) {
            ctx.getSource().sendSuccess(() -> Component.literal("§b当前集团: §f" + group.getChineseName()), false);
        }

        // 参赛者数量
        int participantCount = manager.getParticipantQueue().size();
        ctx.getSource().sendSuccess(() -> Component.literal("§b参赛者总数: §f" + participantCount), false);

        // 阶段完成标记
        ctx.getSource().sendSuccess(() -> Component.literal("§b阶段完成: " + (manager.isStageCompleted() ? "§a是" : "§c否")), false);

        // 倒计时状态
        int countdownSecs = manager.getCountdownRemainingSeconds();
        if (countdownSecs > 0) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§b倒计时: §e" + countdownSecs + " §7秒后" + (stage == GurongiGameStage.NONE ? "游戏启动" : "阶段推进")), false);
        } else if (manager.isCountdownActive()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§b倒计时: §a倒计时即将结束"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§b倒计时: §7无"), false);
        }

        // 石棺状态
        if (stage == GurongiGameStage.NONE) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7石棺状态: " + (manager.isSarcophagusOpened() ? "§a已开启" : "§c未开启")), false);
        }

        // 当前回合
        UUID currentUuid = manager.getCurrentTurnParticipant();
        if (currentUuid != null) {
            GurongiGameManager.ParticipantEntry entry = manager.getParticipant(currentUuid);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§b当前回合: §f" + (entry != null ? entry.entityTypeId : currentUuid.toString().substring(0, 8) + "...")), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi manager participants — 列出参赛者
    // ====================================================================

    /**
     * 列出基基鲁游戏管理器中所有参赛者及其状态。
     * <p>
     * 用法：{@code /rider data gurongi manager participants}
     */
    private static int gurongiManagerParticipants(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        GurongiGameManager manager = GurongiGameManager.get(level);

        java.util.List<UUID> queue = manager.getParticipantQueue();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§e===== 参赛者列表 (§f" + queue.size() + "§e 名) ====="), false);

        if (queue.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7暂无参赛者"), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
            return 1;
        }

        for (int i = 0; i < queue.size(); i++) {
            UUID uuid = queue.get(i);
            GurongiGameManager.ParticipantEntry entry = manager.getParticipant(uuid);
            if (entry == null) continue;

            String statusColor;
            switch (entry.status) {
                case ACTIVE: statusColor = "§a"; break;
                case REGISTERED:
                case WAITING: statusColor = "§e"; break;
                case COMPLETED:
                case PROMOTED: statusColor = "§b"; break;
                case DISQUALIFIED: statusColor = "§c"; break;
                default: statusColor = "§7"; break;
            }

            String eligibleMark = entry.eligible ? "" : " §c[失格]";
            String uuidShort = entry.gurongiUuid.toString().substring(0, 8) + "...";
            String statusName = entry.status.name();
            String entityTypeId = entry.entityTypeId;
            int killCount = entry.killCount;
            int index = i + 1;
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§f%d. §7UUID: §f%s  %s%s§r  §7类型: §f%s  §7击杀: §f%d%s",
                    index, uuidShort, statusColor, statusName,
                    entityTypeId, killCount, eligibleMark
            )), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi get turn — 当前回合信息
    // ====================================================================

    /**
     * 查看当前轮到哪位参赛者进行游戏，以及阶段推进倒计时。
     * <p>
     * 用法：{@code /rider data gurongi get turn}
     */
    private static int gurongiManagerTurn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        GurongiGameManager manager = GurongiGameManager.get(level);

        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 当前回合信息 ====="), false);

        UUID currentUuid = manager.getCurrentTurnParticipant();
        if (currentUuid == null) {
            if (manager.isStageCompleted()) {
                ctx.getSource().sendSuccess(() -> Component.literal("§e所有参赛者已完成，等待阶段推进"), false);
                // 显示倒计时
                int countdownSecs = manager.getCountdownRemainingSeconds();
                if (countdownSecs > 0) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§7阶段推进倒计时: §f" + countdownSecs + " §7秒"), false);
                } else if (manager.isCountdownActive()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("§7阶段推进倒计时: §a即将结束"), false);
                }
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§c无活跃回合（无参赛者）"), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
            return 1;
        }

        GurongiGameManager.ParticipantEntry entry = manager.getParticipant(currentUuid);
        if (entry != null) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§7当前参赛者 UUID: §f%s",
                    currentUuid
            )), false);
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§7实体类型: §f%s",
                    entry.entityTypeId
            )), false);
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§7状态: §f%s",
                    entry.status.name()
            )), false);
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "§7击杀数: §f%d",
                    entry.killCount
            )), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  /rider data gurongi check <hunter> <target> — 模拟狩猎法则检查
    // ====================================================================

    /**
     * 模拟狩猎法则检查，判断古朗基是否允许攻击指定目标。
     * <p>
     * 用法：{@code /rider data gurongi check <hunter> <target>}
     */
    private static int checkHuntingLaw(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String hunterTypeName = StringArgumentType.getString(ctx, "hunterType");
        ServerLevel level = ctx.getSource().getLevel();
        net.minecraft.world.entity.Entity targetRaw = EntityArgument.getEntity(ctx, "target");

        if (!(targetRaw instanceof LivingEntity target)) {
            ctx.getSource().sendFailure(Component.literal("§c目标不是活体实体，无法被狩猎"));
            return 0;
        }

        // 先尝试找实体
        Gurongi hunter = findGurongiByTypeName(level, hunterTypeName);
        if (hunter != null) {
            return displayHuntingLawCheck(ctx, hunter, target);
        }

        // 实体未加载，检查类型名是否有效
        EntityType<? extends Gurongi> hunterType = GurongiSpawnRegistry.getTypeByRegistryName(hunterTypeName);
        if (hunterType == null) {
            ctx.getSource().sendFailure(Component.literal("§c未知的古朗基类型: §f" + hunterTypeName
                    + "§c。可用类型：§f" + String.join("§7, §f", getAllGurongiTypeNames())));
            return 0;
        }

        ctx.getSource().sendFailure(Component.literal("§c古朗基 §f" + hunterTypeName + "§c 未在当前维度加载，无法进行法则检查"));
        return 0;
    }

    /**
     * 显示狩猎法则检查结果。
     */
    private static int displayHuntingLawCheck(CommandContext<CommandSourceStack> ctx, Gurongi hunter, LivingEntity target) {
        ctx.getSource().sendSuccess(() -> Component.literal("§e===== 狩猎法则检查 ====="), false);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7攻击者: §f%s  §7[§f%s§7]  §7集团: §f%s",
                hunter.getName().getString(),
                hunter.getType().getDescription().getString(),
                hunter.group != null ? hunter.group.getChineseName() : "?"
        )), false);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7目标: §f%s  §7[§f%s§7]",
                target.getName().getString(),
                target.getType().getDescription().getString()
        )), false);

        boolean isHuman = GurongiHuntingLaw.isHuman(target);
        String targetCategory;
        if (isHuman) {
            targetCategory = GurongiHuntingLaw.isLinto(target) ? "§e林多 (Linto)" : "§c灾厄村民 (Illager)";
        } else {
            targetCategory = "§7非人类（不受狩猎法则约束）";
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§7目标分类: " + targetCategory), false);

        CheckResult result = GurongiHuntingLaw.canKill(hunter, target);

        String resultText;
        switch (result) {
            case ALLOW -> resultText = "§a§l允许攻击 ✓";
            case DENY -> resultText = "§e§l禁止攻击 ✗（无惩罚）";
            case PENALTY -> resultText = "§c§l禁止攻击 ✗（触发违规惩罚！）";
            default -> resultText = "§7未知结果";
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§7检查结果: " + resultText), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§7--- 规则细项 ---"), false);

        if (hunter.group == GurongiGroup.N) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7恩集团例外: §a生效（可任意杀戮）"), false);
        } else if (!isHuman) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7恩集团例外: §8不适用（非人类目标）"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§7恩集团例外: §8不适用"), false);
        }

        ServerLevel level = ctx.getSource().getLevel();
        GurongiGameManager manager = GurongiGameManager.get(level);
        boolean registered = manager.isRegistered(hunter.getUUID());
        if (registered) {
            GurongiGameStatus status = manager.getParticipant(hunter.getUUID()).status;
            if (status == GurongiGameStatus.REGISTERED || status == GurongiGameStatus.WAITING) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7参赛者规则: §e未激活（" + status.name() + " 状态不得提前杀人）"), false);
            } else if (status == GurongiGameStatus.ACTIVE) {
                ctx.getSource().sendSuccess(() -> Component.literal("§7参赛者规则: §a已激活（游戏中可正常狩猎）"), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal("§7参赛者规则: §8不适用（状态: " + status.name() + "）"), false);
            }
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("§7参赛者规则: §8不适用（非参赛者）"), false);
        }

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§7杀戮欲望: §f%.2f§7/§f1.00",
                hunter.getKillingDesire()
        )), false);

        ctx.getSource().sendSuccess(() -> Component.literal("§e=========================="), false);
        return 1;
    }

    // ====================================================================
    //  狼王与生成类型辅助方法
    // ====================================================================

    /**
     * 判断古朗基是否由狼王转换生成。
     */
    private static boolean isWolfKingConverted(Gurongi gurongi) {
        return gurongi.getPersistentData().contains("wolfKingId");
    }

    /**
     * 获取古朗基生成方式的显示文本。
     */
    private static String getSpawnTypeDisplay(Gurongi gurongi) {
        if (isWolfKingConverted(gurongi)) {
            return "§c狼王转换（属性减半）";
        }
        if (gurongi.isPlayerPlaced()) {
            return "§e刷怪蛋/指令生成";
        }
        return "§7自然生成";
    }

    // ====================================================================
    //  格式化辅助方法
    // ====================================================================

    private static String formatGameStatus(GurongiGameStatus status) {
        if (status == null) return "§7?";
        switch (status) {
            case NONE: return "§7无";
            case REGISTERED: return "§e已注册";
            case WAITING: return "§e等待中";
            case ACTIVE: return "§a游戏中";
            case COMPLETED: return "§b已完成";
            case PROMOTED: return "§d已晋升";
            case DISQUALIFIED: return "§c已失格";
            default: return "§7" + status.name();
        }
    }

    private static String formatGameStage(GurongiGameStage stage) {
        if (stage == null) return "§7?";
        switch (stage) {
            case NONE: return "§7未开始";
            case AUDITION: return "§e试镜（滋集团）";
            case SELECTION: return "§a选拔（魅集团）";
            case SEMI_FINAL: return "§b半决赛（葛集团）";
            case WHITE_DARK_DUEL: return "§5白暗决斗（拉集团）";
            case FINAL: return "§c最终（恩集团）";
            default: return "§7" + stage.name();
        }
    }
}