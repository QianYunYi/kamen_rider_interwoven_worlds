package com.qianyunyi.rider.client.gui.wheel;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端轮盘管理器。
 * 负责：
 * 1. 注册所有的 RadialWheelOverlay 到 Forge 的 GUI overlay 系统；
 * 2. 保证任意时刻最多只有一个轮盘处于打开状态（避免多个轮盘叠加）；
 * 3. 提供便捷的打开/关闭 API，业务代码只需与本类交互，无需关心实现细节。
 */
@OnlyIn(Dist.CLIENT)
public class WheelManager {

    public static final WheelManager INSTANCE = new WheelManager();

    private final Map<ResourceLocation, RadialWheelOverlay> wheels = new HashMap<>();
    private RadialWheelOverlay currentOpen = null;

    private WheelManager() { }

    /**
     * 注册一个轮盘。在 RegisterGuiOverlaysEvent 阶段调用。
     */
    public void register(RegisterGuiOverlaysEvent event, ResourceLocation id,
                         RadialWheelOverlay wheel) {
        wheels.put(id, wheel);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), id.getPath(), wheel);
    }

    /**
     * 打开指定的轮盘；如果此时已有其他轮盘打开，会先关闭它。
     */
    public void open(RadialWheelOverlay wheel) {
        if (wheel == null) return;
        if (currentOpen != null && currentOpen != wheel) {
            currentOpen.isOpen = false;
        }
        currentOpen = wheel;
        if (!wheel.isOpen()) wheel.open();
    }

    /**
     * 关闭当前打开的轮盘。
     */
    public void closeCurrent() {
        if (currentOpen != null) {
            if (currentOpen.isOpen()) currentOpen.close();
            currentOpen = null;
        }
    }

    public boolean isAnyOpen() {
        return currentOpen != null && currentOpen.isOpen();
    }

    public RadialWheelOverlay getCurrent() { return currentOpen; }

    /**
     * 便捷方法：直接打开"驱动器选择"轮盘。
     */
    public void openDriverSelect() {
        open(DriverSelectWheel.INSTANCE);
    }

    /**
     * 便捷方法：打开"形态切换"轮盘。
     */
    public void openFormSwitch() {
        open(FormSwitchWheel.INSTANCE);
    }

    /**
     * 便捷方法：打开"技能选择"轮盘。
     */
    public void openAbilityWheel() {
        open(AbilityWheel.INSTANCE);
    }

    /**
     * 注册所有内置的轮盘。
     */
    public void registerBuiltins(RegisterGuiOverlaysEvent event) {
        register(event,
                ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "driver_select"),
                DriverSelectWheel.INSTANCE);
        register(event,
                ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "form_switch"),
                FormSwitchWheel.INSTANCE);
        register(event,
                ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "ability_wheel"),
                AbilityWheel.INSTANCE);
    }
}