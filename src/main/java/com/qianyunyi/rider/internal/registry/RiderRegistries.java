package com.qianyunyi.rider.internal.registry;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 骑士系统注册表管理类
 * 基于 Forge 1.20.1 设计，使用内存 Map 方案
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RiderRegistries {

    // 注册表名称常量
    public static final ResourceLocation RIDER_CONFIG_REGISTRY_NAME =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "rider_configs");
    public static final ResourceLocation FORM_CONFIG_REGISTRY_NAME =
            ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "form_configs");

    // 内存注册表（线程安全）
    private static final Map<String, RiderConfig> RIDER_CONFIG_MAP = new ConcurrentHashMap<>();
    private static final Map<String, FormConfig> FORM_CONFIG_MAP = new ConcurrentHashMap<>();

    // DeferredRegister 用于和 Forge 事件系统集成
    public static final DeferredRegister<RiderConfig> RIDER_CONFIG_DEFERRED =
            DeferredRegister.create(RIDER_CONFIG_REGISTRY_NAME, KamenRiderInterwovenWorldsMod.MODID);

    public static final DeferredRegister<FormConfig> FORM_CONFIG_DEFERRED =
            DeferredRegister.create(FORM_CONFIG_REGISTRY_NAME, KamenRiderInterwovenWorldsMod.MODID);

    /**
     * 注册骑士配置
     */
    public static synchronized void registerRider(RiderConfig config) {
        if (config != null && config.getRiderName() != null) {
            String key = config.getRiderName().toLowerCase();
            if (RIDER_CONFIG_MAP.containsKey(key)) {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("[Kamen Rider] 重复注册骑士: {}", key);
                return;
            }
            RIDER_CONFIG_MAP.put(key, config);
            KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 注册骑士配置: {}", key);
        }
    }

    /**
     * 注册形态配置
     */
    public static synchronized void registerForm(FormConfig config) {
        if (config != null && config.getFormName() != null) {
            String key = config.getFormName().toLowerCase();
            if (FORM_CONFIG_MAP.containsKey(key)) {
                KamenRiderInterwovenWorldsMod.LOGGER.warn("[Kamen Rider] 重复注册形态: {}", key);
                return;
            }
            FORM_CONFIG_MAP.put(key, config);
            KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 注册形态配置: {}", key);
        }
    }

    /**
     * 获取骑士配置
     */
    public static RiderConfig getRiderConfig(String riderName) {
        return RIDER_CONFIG_MAP.get(riderName.toLowerCase());
    }

    /**
     * 获取形态配置
     */
    public static FormConfig getFormConfig(String formName) {
        return FORM_CONFIG_MAP.get(formName.toLowerCase());
    }

    /**
     * 获取所有骑士配置
     */
    public static Collection<RiderConfig> getAllRiderConfigs() {
        return Collections.unmodifiableCollection(RIDER_CONFIG_MAP.values());
    }

    /**
     * 获取所有形态配置
     */
    public static Collection<FormConfig> getAllFormConfigs() {
        return Collections.unmodifiableCollection(FORM_CONFIG_MAP.values());
    }

    /**
     * 获取骑士配置数量
     */
    public static int getRiderConfigCount() {
        return RIDER_CONFIG_MAP.size();
    }

    /**
     * 获取形态配置数量
     */
    public static int getFormConfigCount() {
        return FORM_CONFIG_MAP.size();
    }

    /**
     * 检查骑士是否已注册
     */
    public static boolean hasRiderConfig(String riderName) {
        return RIDER_CONFIG_MAP.containsKey(riderName.toLowerCase());
    }

    /**
     * 检查形态是否已注册
     */
    public static boolean hasFormConfig(String formName) {
        return FORM_CONFIG_MAP.containsKey(formName.toLowerCase());
    }

    // ==============================================
    // 修复点：@SubscribeEvent 方法只能有 1 个参数（事件本身）
    // IEventBus 通过 FMLJavaModLoadingContext.get().getModEventBus() 获取
    // 参考同项目 EntityRegistrationHandler 的做法
    // ==============================================
    @SubscribeEvent
    public static void onConstructMod(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 初始化骑士配置注册表");

            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            RIDER_CONFIG_DEFERRED.register(modBus);
            FORM_CONFIG_DEFERRED.register(modBus);
        });
    }

}