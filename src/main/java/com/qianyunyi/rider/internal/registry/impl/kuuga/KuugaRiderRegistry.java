package com.qianyunyi.rider.internal.registry.impl.kuuga;

import com.qianyunyi.rider.client.gui.KuugaEnergyHUD;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.registry.IRiderRegistry;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.impl.kuuga.forms.*;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.switching.KuugaSwitchBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 空我骑士注册器 - 重构版
 * 职责：维护解锁条件、形态进阶逻辑和注册
 */
public class KuugaRiderRegistry implements IRiderRegistry {

    // 主线形态进化顺序（不含黑目/红目变体，由 getAppropriateForm 处理）
    public static final List<String> MAIN_FORMS = Arrays.asList(
            "base", "mighty_form", "rising_mighty_form",
            "amazing_mighty_form", "ultimate_form",
            "rising_ultimate_form", "super_rising_ultimate_form"
    );

    // 基础派生形态列表
    private static final List<String> BASE_DERIVED_FORMS = Arrays.asList(
            "mighty_form", "dragon_form", "pegasus_form", "titan_form"
    );

    // 升华派生形态列表
    private static final List<String> RISING_DERIVED_FORMS = Arrays.asList(
            "rising_mighty_form", "rising_dragon_form",
            "rising_pegasus_form", "rising_titan_form"
    );

    // 形态轮询映射（同阶派生切换）
    public static final Map<String, List<String>> FORM_CYCLE_MAP = new HashMap<>();

    // 形态解锁条件映射（使用 Player 而非 ServerPlayer，以支持客户端检查）
    private static final Map<String, Predicate<Player>> FORM_UNLOCK_CONDITIONS = new HashMap<>();

    // HUD 提供者（客户端单例）
    private static final Supplier<IHUDManager> HUD_SUPPLIER = () ->
            FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null;

    // 形态配置映射
    private static final Map<String, Supplier<? extends FormConfig>> FORM_CONFIG_SUPPLIERS = new HashMap<>();

    // 形态颜色配置映射
    private static final Map<String, String[]> FORM_COLORS = new HashMap<>();

    // 形态组映射（同一形态的不同版本，如红目/黑目）
    private static final Map<String, String> FORM_GROUP_MAP = new HashMap<>();

    // 派生形态→进化链替换映射：{派生形态 → [基础形态, 升华形态]}
    // 用于在 V 键进阶轮盘中，将 MAIN_FORMS 中的 mighty_form/rising_mighty_form
    // 替换为当前派生形态的对应形态
    private static final Map<String, String[]> DERIVED_FORM_CHAIN = new HashMap<>();

    // 延迟初始化标志 — 首次访问时才初始化所有映射，避免静态块在类加载时阻塞
    private static volatile boolean initialized = false;

    /**
     * 确保所有静态映射已初始化（线程安全的延迟初始化）
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (KuugaRiderRegistry.class) {
                if (!initialized) {
                    initializeCycleMap();
                    initializeUnlockConditions();
                    initializeFormConfigSuppliers();
                    initializeFormColors();
                    initializeFormGroups();
                    initializeDerivedFormChain();
                    initialized = true;
                }
            }
        }
    }

    private static void initializeDerivedFormChain() {
        DERIVED_FORM_CHAIN.put("dragon_form", new String[]{"dragon_form", "rising_dragon_form"});
        DERIVED_FORM_CHAIN.put("pegasus_form", new String[]{"pegasus_form", "rising_pegasus_form"});
        DERIVED_FORM_CHAIN.put("titan_form", new String[]{"titan_form", "rising_titan_form"});
        DERIVED_FORM_CHAIN.put("rising_dragon_form", new String[]{"dragon_form", "rising_dragon_form"});
        DERIVED_FORM_CHAIN.put("rising_pegasus_form", new String[]{"pegasus_form", "rising_pegasus_form"});
        DERIVED_FORM_CHAIN.put("rising_titan_form", new String[]{"titan_form", "rising_titan_form"});
    }

    private static void initializeFormColors() {
        FORM_COLORS.put("mighty_form", new String[]{"redAmadam", "basePanel", "baseRune"});
        FORM_COLORS.put("dragon_form", new String[]{"blueAmadam", "basePanel", "baseRune"});
        FORM_COLORS.put("pegasus_form", new String[]{"greenAmadam", "basePanel", "baseRune"});
        FORM_COLORS.put("titan_form", new String[]{"purpleAmadam", "basePanel", "baseRune"});
        FORM_COLORS.put("rising_mighty_form", new String[]{"goldAmadam", "risingPanel", "baseRune"});
        FORM_COLORS.put("rising_dragon_form", new String[]{"goldAmadam", "risingPanel", "baseRune"});
        FORM_COLORS.put("rising_pegasus_form", new String[]{"goldAmadam", "risingPanel", "baseRune"});
        FORM_COLORS.put("rising_titan_form", new String[]{"goldAmadam", "risingPanel", "baseRune"});
        FORM_COLORS.put("amazing_mighty_form", new String[]{"darkAmadam", "risingPanel", "baseRune"});
        FORM_COLORS.put("ultimate_form", new String[]{"darkAmadam", "risingPanel", "risingRune"});
        FORM_COLORS.put("ultimate_form_dark_eye", new String[]{"darkAmadam", "risingPanel", "risingRune"});
        FORM_COLORS.put("rising_ultimate_form", new String[]{"darkAmadam", "risingPanel", "risingRune"});
        FORM_COLORS.put("rising_ultimate_form_dark_eye", new String[]{"darkAmadam", "risingPanel", "risingRune"});
        FORM_COLORS.put("super_rising_ultimate_form", new String[]{"goldAmadam", "risingPanel", "risingRune"});
    }

    private static void initializeCycleMap() {
        FORM_CYCLE_MAP.put("mighty_form", BASE_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("dragon_form", BASE_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("pegasus_form", BASE_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("titan_form", BASE_DERIVED_FORMS);

        FORM_CYCLE_MAP.put("rising_mighty_form", RISING_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("rising_dragon_form", RISING_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("rising_pegasus_form", RISING_DERIVED_FORMS);
        FORM_CYCLE_MAP.put("rising_titan_form", RISING_DERIVED_FORMS);
    }

    private static void initializeUnlockConditions() {
        // 基础形态：亚玛达姆灵石能量小于最大值的10%或融合度小于1%
        FORM_UNLOCK_CONDITIONS.put("base", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            float amadamEnergy = kuugaData.getAmadamEnergy();
            float amadamEnergyMax = kuugaData.getAmadamEnergyMax();
            float amadamFusion = kuugaData.getAmadamFusion();

            return (amadamEnergy < amadamEnergyMax * 0.1f) || (amadamFusion < 1.0f);
        });

        // 全能形态：亚玛达姆灵石能量大于10%且解锁了对应的形态
        FORM_UNLOCK_CONDITIONS.put("mighty_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            float amadamEnergy = kuugaData.getAmadamEnergy();
            float amadamEnergyMax = kuugaData.getAmadamEnergyMax();

            return (amadamEnergy >= amadamEnergyMax * 0.1f) && kuugaData.isMightyKuuga();
        });

        // 青龙形态：亚玛达姆灵石能量大于10%且解锁了对应的形态
        FORM_UNLOCK_CONDITIONS.put("dragon_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            float amadamEnergy = kuugaData.getAmadamEnergy();
            float amadamEnergyMax = kuugaData.getAmadamEnergyMax();

            return (amadamEnergy >= amadamEnergyMax * 0.1f) && kuugaData.isDragonForm();
        });

        // 天马形态：亚玛达姆灵石能量大于10%且解锁了对应的形态
        FORM_UNLOCK_CONDITIONS.put("pegasus_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            float amadamEnergy = kuugaData.getAmadamEnergy();
            float amadamEnergyMax = kuugaData.getAmadamEnergyMax();

            return (amadamEnergy >= amadamEnergyMax * 0.1f) && kuugaData.isPegasusForm();
        });

        // 泰坦形态：亚玛达姆灵石能量大于10%且解锁了对应的形态
        FORM_UNLOCK_CONDITIONS.put("titan_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            float amadamEnergy = kuugaData.getAmadamEnergy();
            float amadamEnergyMax = kuugaData.getAmadamEnergyMax();

            return (amadamEnergy >= amadamEnergyMax * 0.1f) && kuugaData.isTitanForm();
        });

        // 升华全能形态：解锁了金之力且解锁了对应的基础形态
        FORM_UNLOCK_CONDITIONS.put("rising_mighty_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.isHasGoldPower() && kuugaData.isMightyKuuga();
        });

        // 升华青龙形态：解锁了金之力且解锁了对应的基础形态
        FORM_UNLOCK_CONDITIONS.put("rising_dragon_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.isHasGoldPower() && kuugaData.isDragonForm();
        });

        // 升华天马形态：解锁了金之力且解锁了对应的基础形态
        FORM_UNLOCK_CONDITIONS.put("rising_pegasus_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.isHasGoldPower() && kuugaData.isPegasusForm();
        });

        // 升华泰坦形态：解锁了金之力且解锁了对应的基础形态
        FORM_UNLOCK_CONDITIONS.put("rising_titan_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.isHasGoldPower() && kuugaData.isTitanForm();
        });

        // 惊异全能形态：解锁了金之力且解锁了惊异全能
        FORM_UNLOCK_CONDITIONS.put("amazing_mighty_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.isHasGoldPower() && kuugaData.isAmazinglyMightyForm();
        });

        // 究极形态（红目）：解锁了究极形态且神圣之泉>=50
        FORM_UNLOCK_CONDITIONS.put("ultimate_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.getHolySpring() >= 50 && kuugaData.isUltimateForm();
        });

        // 究极形态（黑目）：解锁了究极形态且神圣之泉<50
        FORM_UNLOCK_CONDITIONS.put("ultimate_form_dark_eye", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.getHolySpring() < 50 && kuugaData.isUltimateForm();
        });

        // 升华究极形态（红目）：解锁了升华究极形态且神圣之泉>=50
        FORM_UNLOCK_CONDITIONS.put("rising_ultimate_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.getHolySpring() >= 50 && kuugaData.isRisingUltimateForm();
        });

        // 升华究极形态（黑目）：解锁了升华究极形态且神圣之泉<50
        FORM_UNLOCK_CONDITIONS.put("rising_ultimate_form_dark_eye", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.getHolySpring() < 50 && kuugaData.isRisingUltimateForm();
        });

        // 超升华究极形态：神圣之泉大于50，且解锁了超升华究极形态
        FORM_UNLOCK_CONDITIONS.put("super_rising_ultimate_form", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            var kuugaData = data.getKuugaData();
            if (kuugaData == null) return false;

            return kuugaData.getHolySpring() >= 50 && kuugaData.isSuperRisingUltimateForm();
        });
    }

    private static void initializeFormGroups() {
        // 将同一形态的黑目/红目版本分组
        FORM_GROUP_MAP.put("ultimate_form", "ultimate_form_dark_eye");
        FORM_GROUP_MAP.put("ultimate_form_dark_eye", "ultimate_form");
        FORM_GROUP_MAP.put("rising_ultimate_form", "rising_ultimate_form_dark_eye");
        FORM_GROUP_MAP.put("rising_ultimate_form_dark_eye", "rising_ultimate_form");
    }

    private static void initializeFormConfigSuppliers() {
        FORM_CONFIG_SUPPLIERS.put("base", () -> new KuugaBaseFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("mighty_form", () -> new KuugaMightyFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("dragon_form", () -> new KuugaDragonFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("pegasus_form", () -> new KuugaPegasusFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("titan_form", () -> new KuugaTitanFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_mighty_form", () -> new KuugaRisingMightyFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_dragon_form", () -> new KuugaRisingDragonFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_pegasus_form", () -> new KuugaRisingPegasusFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_titan_form", () -> new KuugaRisingTitanFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("amazing_mighty_form", () -> new KuugaAmazingMightyFormConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("ultimate_form", () -> new KuugaUltimateFormRedEyeConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("ultimate_form_dark_eye", () -> new KuugaUltimateFormDarkEyeConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_ultimate_form", () -> new KuugaRisingUltimateFormRedEyeConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("rising_ultimate_form_dark_eye", () -> new KuugaRisingUltimateFormDarkEyeConfig(HUD_SUPPLIER));
        FORM_CONFIG_SUPPLIERS.put("super_rising_ultimate_form", () -> new KuugaSuperRisingUltimateFormConfig(HUD_SUPPLIER));
    }


    /**
     * 检查形态是否解锁
     */
    public static boolean isFormUnlocked(String formName, Player player) {
        ensureInitialized();
        Predicate<Player> condition = FORM_UNLOCK_CONDITIONS.get(formName);
        return condition != null && condition.test(player);
    }

    /**
     * 根据神圣之泉值获取合适的形态（红目或黑目）
     */
    public static String getAppropriateForm(String baseFormName, Player player) {
        if (player == null) return baseFormName;

        var data = RiderPlayerVariablesProvider.get(player);
        if (data == null || data.getKuugaData() == null) return baseFormName;

        float holySpring = data.getKuugaData().getHolySpring();

        // 根据形态名称和神圣之泉值决定返回红目还是黑目
        return switch (baseFormName) {
            case "ultimate_form", "ultimate_form_dark_eye" ->
                    holySpring >= 50 ? "ultimate_form" : "ultimate_form_dark_eye";
            case "rising_ultimate_form", "rising_ultimate_form_dark_eye" ->
                    holySpring >= 50 ? "rising_ultimate_form" : "rising_ultimate_form_dark_eye";
            default -> baseFormName;
        };
    }

    /**
     * 获取下一个轮询形态（潜行）
     */
    public static String getNextCycleForm(String currentForm, Player player) {
        ensureInitialized();
        List<String> cycleList = FORM_CYCLE_MAP.get(currentForm);
        if (cycleList == null || cycleList.isEmpty()) {
            // 如果不是可轮询形态，检查是否有对等形态（黑目/红目切换）
            String alternateForm = FORM_GROUP_MAP.get(currentForm);
            if (alternateForm != null && isFormUnlocked(alternateForm, player)) {
                return alternateForm;
            }
            return currentForm;
        }

        int currentIndex = cycleList.indexOf(currentForm);
        if (currentIndex != -1) {
            for (int i = 1; i <= cycleList.size(); i++) {
                String nextForm = cycleList.get((currentIndex + i) % cycleList.size());
                if (isFormUnlocked(nextForm, player) || nextForm.equals(currentForm)) {
                    return nextForm;
                }
            }
        }

        for (String form : cycleList) {
            if (isFormUnlocked(form, player)) return form;
        }
        return cycleList.get(0);
    }

    /**
     * 检查形态是否可以轮询（有同级形态）
     */
    public static boolean canCycle(String currentForm) {
        ensureInitialized();
        List<String> cycleList = FORM_CYCLE_MAP.get(currentForm);
        if (cycleList != null && cycleList.size() > 1) {
            return true;
        }

        // 检查是否有对等形态（黑目/红目）
        return FORM_GROUP_MAP.containsKey(currentForm);
    }

    @Override
    public boolean checkFormUnlocked(String form, Player player) {
        return KuugaRiderRegistry.isFormUnlocked(form, player);
    }

    @Override
    public String cycleNextForm(String currentFormName, ServerPlayer player) {
        // 先从轮询映射中查找
        List<String> cycleList = FORM_CYCLE_MAP.get(currentFormName);
        if (cycleList != null && cycleList.size() > 1) {
            int currentIndex = cycleList.indexOf(currentFormName);
            if (currentIndex != -1) {
                int nextIndex = (currentIndex + 1) % cycleList.size();
                String nextForm = cycleList.get(nextIndex);
                if (KuugaRiderRegistry.isFormUnlocked(nextForm, player)) {
                    return nextForm;
                }
                // 如果下一个未解锁，继续查找
                for (int i = 1; i < cycleList.size(); i++) {
                    int idx = (currentIndex + i) % cycleList.size();
                    String candidate = cycleList.get(idx);
                    if (KuugaRiderRegistry.isFormUnlocked(candidate, player)) {
                        return candidate;
                    }
                }
            }
        }

        // 检查是否有对等形态（黑目/红目切换）
        String alternateForm = FORM_GROUP_MAP.get(currentFormName);
        if (alternateForm != null && KuugaRiderRegistry.isFormUnlocked(alternateForm, player)) {
            return alternateForm;
        }

        return "";
    }

    @Override
    public boolean checkCanCycle(String currentFormName) {
        return KuugaRiderRegistry.canCycle(currentFormName);
    }

    @Override
    public List<String> getFormList() {
        return MAIN_FORMS;
    }

    @Override
    public List<String> getCycleFormList(String currentFormName) {
        // 1. 从 FORM_CYCLE_MAP 获取当前形态所在的同级组
        List<String> cycleList = FORM_CYCLE_MAP.get(currentFormName);
        if (cycleList != null && !cycleList.isEmpty()) {
            return new ArrayList<>(cycleList);
        }

        // 2. 检查是否有对等形态（黑目/红目切换）
        String alternateForm = FORM_GROUP_MAP.get(currentFormName);
        if (alternateForm != null) {
            return List.of(currentFormName, alternateForm);
        }

        // 3. 检查是否是被对等形态指向的形态（反向查找）
        for (Map.Entry<String, String> entry : FORM_GROUP_MAP.entrySet()) {
            if (entry.getValue().equals(currentFormName)) {
                return List.of(currentFormName, entry.getKey());
            }
        }

        return List.of();
    }

    @Override
    public List<String> getAdvancementLineList(String currentFormName, Player player) {
        if (player == null) return List.of();

        // 检查当前形态是否为派生形态，获取对应的进化链替换映射
        String[] chainReplacement = DERIVED_FORM_CHAIN.get(currentFormName);

        List<String> line = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (String formName : MAIN_FORMS) {
            // 如果当前形态是派生形态，将 mighty_form / rising_mighty_form 替换为对应派生形态
            if (chainReplacement != null) {
                if ("mighty_form".equals(formName)) {
                    formName = chainReplacement[0]; // 基础派生形态（如 dragon_form）
                } else if ("rising_mighty_form".equals(formName)) {
                    formName = chainReplacement[1]; // 升华派生形态（如 rising_dragon_form）
                }
            }

            String appropriateForm = getAppropriateForm(formName, player);
            if (isFormUnlocked(appropriateForm, player) && added.add(appropriateForm)) {
                line.add(appropriateForm);
            }
        }

        return line;
    }

    /**
     * 获取形态的颜色配置
     */
    public static String[] getFormColors(String formName) {
        ensureInitialized();
        return FORM_COLORS.get(formName);
    }

    @Override
    public void register() {
        ensureInitialized();
        RiderConfig config = new RiderConfig(
                "Kuuga",
                KamenRiderInterwovenWorldsModItems.ARCLE_LEGGINGS.get(),
                new KuugaSwitchBehavior()
        );

        // 注册所有形态配置
        FORM_UNLOCK_CONDITIONS.keySet().forEach(formName -> {
            Supplier<FormConfig> supplier = getFormConfigSupplier(formName);
            if (supplier != null) {
                config.addForm(formName, supplier.get());
            }
        });

        RiderRegistryHelper.register(config);
    }

    /**
     * 根据形态名称获取对应的FormConfig供应商
     */
    private static Supplier<FormConfig> getFormConfigSupplier(String formName) {
        return switch (formName) {
            case "base" -> () -> new KuugaBaseFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "mighty_form" -> () -> new KuugaMightyFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "dragon_form" -> () -> new KuugaDragonFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "pegasus_form" -> () -> new KuugaPegasusFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "titan_form" -> () -> new KuugaTitanFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_mighty_form" -> () -> new KuugaRisingMightyFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_dragon_form" -> () -> new KuugaRisingDragonFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_pegasus_form" -> () -> new KuugaRisingPegasusFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_titan_form" -> () -> new KuugaRisingTitanFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "amazing_mighty_form" -> () -> new KuugaAmazingMightyFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "ultimate_form" -> () -> new KuugaUltimateFormRedEyeConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "ultimate_form_dark_eye" -> () -> new KuugaUltimateFormDarkEyeConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_ultimate_form" -> () -> new KuugaRisingUltimateFormRedEyeConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "rising_ultimate_form_dark_eye" -> () -> new KuugaRisingUltimateFormDarkEyeConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            case "super_rising_ultimate_form" -> () -> new KuugaSuperRisingUltimateFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? KuugaEnergyHUD.getInstance() : null);
            default -> null;
        };
    }
}