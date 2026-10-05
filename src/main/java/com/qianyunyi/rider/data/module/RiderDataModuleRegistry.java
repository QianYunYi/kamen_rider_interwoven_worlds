package com.qianyunyi.rider.data.module;

import com.qianyunyi.rider.config.common.RiderAssertions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RiderDataModuleRegistry {
    private static final List<Supplier<IRiderDataModule>> MODULE_SUPPLIERS = new ArrayList<>();

    private RiderDataModuleRegistry() {}

    public static void register(Supplier<IRiderDataModule> supplier) {
        RiderAssertions.notNull(supplier, "模块提供者不能为空");
        MODULE_SUPPLIERS.add(supplier);
    }

    public static List<IRiderDataModule> createAllModules() {
        return MODULE_SUPPLIERS.stream()
                .map(Supplier::get)
                .toList();
    }

    public static int getModuleCount() {
        return MODULE_SUPPLIERS.size();
    }
}