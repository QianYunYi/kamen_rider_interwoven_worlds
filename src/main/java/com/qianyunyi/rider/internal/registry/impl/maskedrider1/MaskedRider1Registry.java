package com.qianyunyi.rider.internal.registry.impl.maskedrider1;

import com.qianyunyi.rider.api.rider.IFormSwitchBehavior;
import com.qianyunyi.rider.client.gui.WindPowerHUD;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.ShowaData;
import com.qianyunyi.rider.api.rider.registry.IRiderRegistry;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.impl.maskedrider1.forms.MaskedRider1OidFormConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 假面骑士1号注册器
 */
public class MaskedRider1Registry implements IRiderRegistry {

    // 形态解锁条件映射
    private static final Map<String, Predicate<Player>> FORM_UNLOCK_CONDITIONS = new HashMap<>();
    private static volatile boolean initialized = false;

    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (MaskedRider1Registry.class) {
                if (!initialized) {
                    initializeUnlockConditions();
                    initialized = true;
                }
            }
        }
    }

    private static void initializeUnlockConditions() {
        // 基础形态：解锁旧1号且没有解锁樱岛一号和新一号
        FORM_UNLOCK_CONDITIONS.put("base", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            // 通过模块系统获取 ShowaData
            ShowaData showaData = data.getModule(ShowaData.class);
            if (showaData == null) return false;

            float windPower = showaData.getWindPower();
            float windPowerMax = showaData.getMaxWindPower();

            return (windPower < windPowerMax * 0.1f) &&
                    showaData.isMaskedRider1 &&
                    !showaData.isMaskedRider1SakurajimaVersion &&
                    !showaData.isMaskedRiderNew1;
        });

        // 樱岛一号
        FORM_UNLOCK_CONDITIONS.put("masked_rider_1_sakurajima_version", player -> {
            var data = RiderPlayerVariablesProvider.get(player);
            if (data == null) return false;

            // 通过模块系统获取 ShowaData
            ShowaData showaData = data.getModule(ShowaData.class);
            if (showaData == null) return false;

            float windPower = showaData.getWindPower();
            float windPowerMax = showaData.getMaxWindPower();
            return (windPower < windPowerMax * 0.1f) &&
                    showaData.isMaskedRider1 &&
                    showaData.isMaskedRider1SakurajimaVersion &&
                    !showaData.isMaskedRiderNew1;
        });
    }

    @Override
    public boolean checkFormUnlocked(String form, Player player) {
        ensureInitialized();
        Predicate<Player> condition = FORM_UNLOCK_CONDITIONS.get(form);
        return condition != null && condition.test(player);
    }

    @Override
    public String cycleNextForm(String currentFormName, ServerPlayer player) {
        // 假面骑士1号暂无同级轮询逻辑
        return "";
    }

    @Override
    public boolean checkCanCycle(String currentFormName) {
        return false;
    }

    @Override
    public List<String> getFormList() {
        return List.of("base", "masked_rider_1_sakurajima_version");
    }

    @Override
    public List<String> getCycleFormList(String currentFormName) {
        // 假面骑士1号暂无同级形态
        return List.of();
    }

    @Override
    public List<String> getAdvancementLineList(String currentFormName, Player player) {
        // 假面骑士1号暂无进化路线（仅基础形态→樱岛一号）
        List<String> line = new ArrayList<>();
        if (checkFormUnlocked("base", player)) {
            line.add("base");
        }
        if (checkFormUnlocked("masked_rider_1_sakurajima_version", player)) {
            line.add("masked_rider_1_sakurajima_version");
        }
        return line;
    }

    @Override
    public void register() {
        ensureInitialized();
        // 创建骑士配置（使用修改后的构造函数）
        RiderConfig config = new RiderConfig(
                "masked_rider_1",
                getDriverItem(),
                getFormSwitchBehavior()
        );

        // 注册所有形态配置
        FORM_UNLOCK_CONDITIONS.keySet().forEach(formName -> {
            Supplier<FormConfig> supplier = getFormConfigSupplier(formName);
            if (supplier != null) {
                config.addForm(formName, supplier.get());
            }
        });

        // 使用新的注册方法
        RiderRegistryHelper.register(config);
    }

    /**
     * 获取驱动器物品
     */
    private static net.minecraft.world.item.Item getDriverItem() {
        // 替换为实际的驱动器物品获取逻辑
        return null;
    }

    /**
     * 获取形态切换行为
     */
    private static IFormSwitchBehavior getFormSwitchBehavior() {
        // 替换为实际的形态切换行为
        return null;
    }

    /**
     * 根据形态名称获取对应的FormConfig供应商
     */
    private static Supplier<FormConfig> getFormConfigSupplier(String formName) {
        return switch (formName) {
            case "base" -> () -> new MaskedRider1OidFormConfig(() ->
                    FMLEnvironment.dist == Dist.CLIENT ? WindPowerHUD.getInstance() : null);
//            case "masked_rider_1_sakurajima_version" -> () -> new MaskedRider1SakurajimaVersionFormConfig(() ->
//                    FMLEnvironment.dist == Dist.CLIENT ? WindPowerHUD.getInstance() : null);
            default -> null;
        };
    }
}