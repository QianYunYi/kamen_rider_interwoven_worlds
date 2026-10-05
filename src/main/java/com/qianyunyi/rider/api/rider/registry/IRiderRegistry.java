package com.qianyunyi.rider.api.rider.registry;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 骑士注册接口
 * 所有骑士实现此接口以支持形态切换、进阶/轮询逻辑
 */
public interface IRiderRegistry {

    // ========== 静态入口方法 ==========

    /**
     * 检查形态是否已解锁
     */
    static boolean isFormUnlocked(String form, Player player) {
        for (IRiderRegistry registry : getInstances()) {
            if (registry.checkFormUnlocked(form, player)) return true;
        }
        return false;
    }

    /**
     * 获取下一个轮询形态（潜行状态按Shift+V）
     */
    static String getNextCycleForm(String currentFormName, ServerPlayer player) {
        for (IRiderRegistry registry : getInstances()) {
            String result = registry.cycleNextForm(currentFormName, player);
            if (result != null && !result.isEmpty()) return result;
        }
        return null;
    }

    /**
     * 检查当前形态是否可以进行同级轮询
     */
    static boolean canCycle(String currentFormName) {
        for (IRiderRegistry registry : getInstances()) {
            if (registry.checkCanCycle(currentFormName)) return true;
        }
        return false;
    }

    /**
     * 获取主线形态列表（用于进阶回滚）
     */
    static List<String> getMainForms() {
        for (IRiderRegistry registry : getInstances()) {
            List<String> forms = registry.getFormList();
            if (forms != null && !forms.isEmpty()) return forms;
        }
        return List.of();
    }

    /**
     * 获取当前形态的同级形态列表（用于 Shift+V 轮盘）
     */
    static List<String> getCycleForms(String currentFormName) {
        for (IRiderRegistry registry : getInstances()) {
            List<String> result = registry.getCycleFormList(currentFormName);
            if (result != null && !result.isEmpty()) return result;
        }
        return List.of();
    }

    /**
     * 获取当前形态的进化路线列表（用于 V 键轮盘）
     * 从路线最低阶开始，向上收集所有已解锁形态
     */
    static List<String> getAdvancementLine(String currentFormName, Player player) {
        for (IRiderRegistry registry : getInstances()) {
            List<String> result = registry.getAdvancementLineList(currentFormName, player);
            if (result != null && !result.isEmpty()) return result;
        }
        return List.of();
    }

    // ========== 实例抽象方法（由实现类 @Override） ==========

    /**
     * 检查形态是否已解锁
     */
    boolean checkFormUnlocked(String form, Player player);

    /**
     * 获取当前形态的下一个轮询形态（同级切换）
     * @return 轮询形态名称，如果没有则返回空字符串
     */
    String cycleNextForm(String currentFormName, ServerPlayer player);

    /**
     * 检查当前形态是否可以进行同级轮询
     */
    boolean checkCanCycle(String currentFormName);

    /**
     * 获取该骑士的主线形态列表（按进化顺序）
     */
    List<String> getFormList();

    /**
     * 获取当前形态的同级形态列表（轮盘中显示）
     */
    List<String> getCycleFormList(String currentFormName);

    /**
     * 获取当前形态的进化路线列表（从路线最低阶到最高阶，仅已解锁）
     */
    List<String> getAdvancementLineList(String currentFormName, Player player);

    /**
     * 注册骑士配置
     */
    void register();

    /**
     * 缓存的所有 IRiderRegistry 实例列表
     */
    List<IRiderRegistry> INSTANCES = new ArrayList<>();

    /**
     * 注册一个 IRiderRegistry 实例到缓存列表
     */
    static void addInstance(IRiderRegistry registry) {
        if (registry != null && !INSTANCES.contains(registry)) {
            INSTANCES.add(registry);
        }
    }

    /**
     * 获取所有 IRiderRegistry 实例
     * 优先使用已注册的实例，其次尝试 ServiceLoader 发现
     */
    static List<IRiderRegistry> getInstances() {
        if (!INSTANCES.isEmpty()) {
            return INSTANCES;
        }
        // 兜底：尝试 ServiceLoader 发现
        List<IRiderRegistry> discovered = new ArrayList<>();
        try {
            ServiceLoader<IRiderRegistry> loader = ServiceLoader.load(IRiderRegistry.class);
            for (IRiderRegistry registry : loader) {
                discovered.add(registry);
                INSTANCES.add(registry);
            }
        } catch (Throwable ignored) {}
        return discovered.isEmpty() ? INSTANCES : discovered;
    }

    /**
     * 自动注册所有实现类
     */
    static void registerAll() {
        for (IRiderRegistry registry : getInstances()) {
            registry.register();
        }
    }
}