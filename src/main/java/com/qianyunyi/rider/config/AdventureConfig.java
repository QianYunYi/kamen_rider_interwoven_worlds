package com.qianyunyi.rider.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块配置类
 * 负责管理游戏中各种功能的配置项
 * 使用Forge的配置系统，支持游戏内实时修改
 */
public class AdventureConfig {
    // 配置构建器
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    /* ================ 空我 (Kuuga) 相关配置 ================ */

    /**
     * 超自然起火每秒最大伤害
     * 值越大，每秒造成的伤害越高
     */
    public static final ForgeConfigSpec.DoubleValue SUPERNATURAL_FIRE_DAMAGE_MAX_PER_SECOND;

    /**
     * 空我黑目形态是否会导致失明效果
     * 启用后，变身为黑目形态时玩家会暂时失明
     */
    public static final ForgeConfigSpec.BooleanValue IS_EMPTY_MY_BLACK_EYE_BLINDNESS;


    /* ================ 古朗基 (Gurongi) 相关配置 ================ */

    /**
     * 林多（Linto）实体注册名列表
     * 用于 GurongiHuntingLaw 判定哪些实体属于"林多"（基基鲁游戏目标之一）
     * 可通过追加其他模组的实体注册名来扩展
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GURONGI_LINTO_ENTITY_TYPES;

    /**
     * 灾厄村民（Illager）实体注册名列表
     * 用于 GurongiHuntingLaw 判定哪些实体属于"灾厄村民"（基基鲁游戏目标之一）
     * 女巫（witch）已默认包含，可通过追加其他模组的实体来扩展
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GURONGI_ILLAGER_ENTITY_TYPES;

    /* ================ 配置初始化 ================ */

    static {

        /* ----- 空我配置 ----- */
        BUILDER.comment("Kuuga (空我) Configuration").push("kuuga");

        SUPERNATURAL_FIRE_DAMAGE_MAX_PER_SECOND = BUILDER
                .comment(
                        "超自然起火每秒最大伤害值",
                        "控制玩家每秒从超自然火焰中承受的最大伤害",
                        "50.0 = 每秒50点伤害",
                        "范围: 1.0 ~ 100.0"
                )
                .defineInRange("supernaturalFireDamageMaxPerSecond", 50.0, 1.0, 100.0);

        IS_EMPTY_MY_BLACK_EYE_BLINDNESS = BUILDER
                .comment(
                        "空我黑目形态是否导致失明",
                        "启用后，当玩家变身为空我黑目形态时会获得失明效果",
                        "true = 启用失明效果",
                        "false = 禁用失明效果"
                )
                .define("isEmptyMyBlackEyeBlindness", true);

        BUILDER.pop(); // 弹出kuuga配置组


        /* ================ 古朗基配置 ================ */
        BUILDER.push("Gurongi (古朗基) Settings");

        BUILDER.comment("古朗基狩猎法则中「林多（Linto）」实体类型的注册名列表",
                "林多包括玩家、村民等。可通过添加其他模组的实体注册名来扩展。",
                "格式: \"modid:entity_name\"").push("hunting_law");

        GURONGI_LINTO_ENTITY_TYPES = BUILDER
                .comment(
                        "林多（Linto）实体注册名列表",
                        "这些实体被视为基基鲁游戏的有效目标（林多分类）",
                        "默认: minecraft:player, minecraft:villager"
                )
                .defineList("lintoEntityTypes",
                        java.util.Arrays.asList("minecraft:player", "minecraft:villager"),
                        s -> s instanceof String);

        GURONGI_ILLAGER_ENTITY_TYPES = BUILDER
                .comment(
                        "灾厄村民（Illager）实体注册名列表",
                        "这些实体被视为基基鲁游戏的有效目标（灾厄村民分类）",
                        "包含女巫（witch），可通过追加其他模组的灾厄村民来扩展",
                        "默认: minecraft:pillager, minecraft:vindicator, minecraft:evoker,",
                        "       minecraft:ravager, minecraft:illusioner, minecraft:witch"
                )
                .defineList("illagerEntityTypes",
                        java.util.Arrays.asList(
                                "minecraft:pillager",
                                "minecraft:vindicator",
                                "minecraft:evoker",
                                "minecraft:ravager",
                                "minecraft:illusioner",
                                "minecraft:witch"
                        ),
                        s -> s instanceof String);

        BUILDER.pop(); // 弹出 hunting_law 配置组
        BUILDER.pop(); // 弹出 Gurongi Settings 配置组

        // 构建配置
        SPEC = BUILDER.build();
    }

    /* ================ 类魂BOSS战 (Soul-like Boss Battle) 配置 ================ */

    /**
     * 是否启用类魂BOSS战机制
     * 启用后，BOSS在附近玩家全部死亡时会重置状态
     */
    public static boolean SOUL_LIKE_BOSS_BATTLE_ON = true;

    /**
     * BOSS战检测距离
     * 单位为方块，用于检测BOSS附近的玩家
     */
    public static int BOSS_BATTLE_DISTANCE = 32;

    /**
     * 类魂BOSS实体列表
     * 这些实体会应用类魂机制
     */
    public static List<String> SOUL_LIKE_BOSS_BATTLE_ENTITY = List.of(
            "minecraft:wither",                  // 凋灵
            "minecraft:ender_dragon",            // 末影龙
            "minecraft:elder_guardian",          // 远古守卫者

            "rider_adventure_affiliated:n_daguba_zeba",  // 自定义BOSS: N Daguba Zeba
            "rider_adventure_affiliated:shockerrobot",   // 自定义BOSS: 修卡机器人
            "rider_adventure_affiliated:shockeroooooo",   // 自定义BOSS: 修卡首领OOOOOO

            // Cataclysm模组BOSS
            "cataclysm:ancient_remnant",
            "cataclysm:the_leviathan",
            "cataclysm:the_harbinger",
            "cataclysm:netherite_monstrosity",
            "cataclysm:ignis",
            "cataclysm:ender_guardian",
            "cataclysm:maledictus",
            "cataclysm:scylla",

            // Mowzie's Mobs模组BOSS
            "mowziesmobs:ferrous_wroughtnaut",
            "mowziesmobs:frostmaw",
            "mowziesmobs:umvuthi",
            "mowziesmobs:naga",

            // 其他模组BOSS
            "aquamirae:captain_cornelia",
            "irons_spellbooks:dead_king",
            "irons_spellbooks:fire_boss",
            "alexsmobs:void_worm",
            "alexsmobs:void_worm_part",
            "bosses_of_mass_destruction:gauntlet",
            "bosses_of_mass_destruction:lich",
            "bosses_of_mass_destruction:obsidilith",
            "bosses_of_mass_destruction:void_blossom"
    );

    /**
     * 受影响的游戏模式列表
     * 只有这些游戏模式的玩家会触发类魂机制
     */
    public static List<String> PLAYER_GAME_MODE = List.of("survival", "adventure");


}