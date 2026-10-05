package com.qianyunyi.rider.internal.registry;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.registry.IRiderRegistry;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.impl.kuuga.KuugaRiderRegistry;
import com.qianyunyi.rider.internal.registry.impl.maskedrider1.MaskedRider1Registry;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * 骑士配置注册与查询工具类
 * 提供便捷的注册和查询方法
 */
public class RiderRegistryHelper {

    /**
     * 注册骑士配置
     */
    public static void register(RiderConfig config) {
        if (config != null && config.getRiderName() != null) {
            RiderRegistries.registerRider(config);
            KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 注册骑士: {}", config.getRiderName());
        }
    }

    /**
     * 批量注册骑士配置
     */
    public static void registerAll(RiderConfig... configs) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 开始注册所有骑士配置...");
        long startTime = System.currentTimeMillis();

        for (RiderConfig config : configs) {
            register(config);
        }

        long endTime = System.currentTimeMillis();
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[Kamen Rider] 注册完成，共注册 {} 个骑士，耗时 {} ms",
                RiderRegistries.getRiderConfigCount(),
                endTime - startTime
        );
    }

    /**
     * 获取骑士配置
     */
    public static Optional<RiderConfig> getConfig(String riderName) {
        return Optional.ofNullable(RiderRegistries.getRiderConfig(riderName));
    }

    /**
     * 获取玩家当前形态配置
     */
    public static Optional<FormConfig> getCurrentFormConfig(Player player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return Optional.empty();

        return getConfig(data.getActiveRiderName())
                .flatMap(config -> Optional.ofNullable(config.getForm(data.getActiveFormName())));
    }

    /**
     * 获取骑士中某个形态的配置
     */
    public static Optional<FormConfig> getFormConfig(String riderName, String formName) {
        return getConfig(riderName)
                .flatMap(config -> Optional.ofNullable(config.getForm(formName)));
    }

    /**
     * 检测形态是否属于当前骑士
     */
    public static boolean isForm(String riderName, String formName) {
        return getConfig(riderName)
                .map(config -> config.isForm(formName))
                .orElse(false);
    }

    /**
     * 获取所有骑士配置
     */
    public static java.util.Collection<RiderConfig> getAllConfigs() {
        return RiderRegistries.getAllRiderConfigs();
    }

    private static volatile List<FormConfig> cachedAwakeningForms;
    private static volatile Map<String, String> formToRiderName;
    private static volatile Map<net.minecraft.world.item.Item, String> beltToRiderName;

    /**
     * 获取所有觉醒形态配置（缓存版本，用于避免 O(n×m) 线性查找）
     */
    public static List<FormConfig> getAwakeningForms() {
        ensureAwakeningCache();
        return cachedAwakeningForms;
    }

    /**
     * 根据形态名获取对应的骑士名
     */
    public static String getRiderNameForForm(String formName) {
        ensureAwakeningCache();
        return formToRiderName.getOrDefault(formName, "");
    }

    /**
     * 根据腰带物品获取对应的骑士名（O(1) 查找，避免线性遍历所有 RiderConfig）
     */
    public static String getRiderNameByBelt(net.minecraft.world.item.Item beltItem) {
        ensureAwakeningCache();
        return beltToRiderName.getOrDefault(beltItem, "");
    }

    private static void ensureAwakeningCache() {
        if (cachedAwakeningForms == null) {
            synchronized (RiderRegistryHelper.class) {
                if (cachedAwakeningForms == null) {
                    List<FormConfig> list = new ArrayList<>();
                    Map<String, String> nameMap = new HashMap<>();
                    Map<net.minecraft.world.item.Item, String> beltMap = new HashMap<>();
                    for (RiderConfig config : RiderRegistries.getAllRiderConfigs()) {
                        for (FormConfig fc : config.getForms().values()) {
                            if (fc.getFormType() == FormType.AWAKENING) {
                                list.add(fc);
                                nameMap.put(fc.getFormName(), config.getRiderName());
                            }
                            // 构建腰带→骑士名映射（基础形态的腰带物品）
                            if (fc.getFormType() == FormType.BASE) {
                                net.minecraft.world.item.Item belt = fc.getBeltItem();
                                if (belt != null) {
                                    beltMap.put(belt, config.getRiderName());
                                }
                            }
                        }
                    }
                    cachedAwakeningForms = list;
                    formToRiderName = nameMap;
                    beltToRiderName = beltMap;
                }
            }
        }
    }

    /**
     * 清除觉醒形态缓存（注册新骑士后调用）
     */
    public static void invalidateAwakeningFormsCache() {
        cachedAwakeningForms = null;
        formToRiderName = null;
        beltToRiderName = null;
    }

    /**
     * 获取骑士数量
     */
    public static int getConfigCount() {
        return RiderRegistries.getRiderConfigCount();
    }

    /**
     * 检查骑士是否已注册
     */
    public static boolean hasConfig(String riderName) {
        return RiderRegistries.hasRiderConfig(riderName);
    }

    // ============ 自动发现与注册 ============

    /**
     * 显式兜底的骑士注册表列表（当 ServiceLoader 无法发现时仍能注册）
     * 添加新骑士时请在此列表中补充一行即可（同时建议同步添加到 META-INF/services）
     */
    private static final List<Supplier<IRiderRegistry>> FALLBACK_REGISTRIES = List.of(
            KuugaRiderRegistry::new,
            MaskedRider1Registry::new
    );

    /**
     * 自动发现并注册所有骑士配置（推荐调用入口）
     * 策略：优先 ServiceLoader 自动发现 → 若未发现任何实现则使用显式兜底列表
     * 应在 mod 构造函数（user code block mod init）中调用一次
     */
    public static void autoRegisterAll() {
        KamenRiderInterwovenWorldsMod.LOGGER.info("[KamenRider] ========== 开始自动发现骑士配置 ==========");
        long startTime = System.currentTimeMillis();

        int registered = 0;
        List<IRiderRegistry> found = autoDiscoverRegistries();
        if (found.isEmpty()) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[KamenRider] ServiceLoader 未发现任何 IRiderRegistry 实现，回退到显式列表");
            for (Supplier<IRiderRegistry> supplier : FALLBACK_REGISTRIES) {
                try {
                    IRiderRegistry registry = supplier.get();
                    // 注册到 IRiderRegistry 缓存，使静态方法可访问
                    IRiderRegistry.addInstance(registry);
                    registry.register();
                    registered++;
                    KamenRiderInterwovenWorldsMod.LOGGER.info(
                            "[KamenRider] 显式注册: {}", registry.getClass().getSimpleName());
                } catch (Exception e) {
                    KamenRiderInterwovenWorldsMod.LOGGER.error("[KamenRider] 注册失败: {}", e.getMessage(), e);
                }
            }
        } else {
            for (IRiderRegistry registry : found) {
                try {
                    // 注册到 IRiderRegistry 缓存，使静态方法可访问
                    IRiderRegistry.addInstance(registry);
                    registry.register();
                    registered++;
                    KamenRiderInterwovenWorldsMod.LOGGER.info(
                            "[KamenRider] ServiceLoader 注册: {}", registry.getClass().getSimpleName());
                } catch (Exception e) {
                    KamenRiderInterwovenWorldsMod.LOGGER.error("[KamenRider] 注册失败: {}", e.getMessage(), e);
                }
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        KamenRiderInterwovenWorldsMod.LOGGER.info(
                "[KamenRider] ========== 注册完成，处理 {} 个 Registry，耗时 {} ms，已注册 RiderConfig: {} ==========",
                registered, cost, RiderRegistries.getRiderConfigCount());
    }

    /**
     * 使用 ServiceLoader 自动发现所有 IRiderRegistry 实现类
     * 需要 META-INF/services/com.rider.qianyunyi.api.rider.registry.IRiderRegistry 中列出实现类全名
     * 返回空列表代表未发现任何实现（此时会走显式兜底路径）
     */
    private static List<IRiderRegistry> autoDiscoverRegistries() {
        try {
            ServiceLoader<IRiderRegistry> loader = ServiceLoader.load(IRiderRegistry.class);
            List<IRiderRegistry> list = new ArrayList<>();
            for (IRiderRegistry registry : loader) {
                list.add(registry);
            }
            return list;
        } catch (Throwable t) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn(
                    "[KamenRider] ServiceLoader 扫描 IRiderRegistry 失败: {}", t.getMessage());
            return List.of();
        }
    }
}