// D:/WorkSpace/MCR/kamen_rider_interwoven_worlds/src/main/java/com/rider/qianyunyi/client/gui/wheel/WheelOverlayRegister.java
package com.qianyunyi.rider.client.gui.wheel;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.qianyunyi.rider.KamenRiderInterwovenWorldsMod.MODID;
import static com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModKeyMappings.SUMMON_DRIVER;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class WheelOverlayRegister {

    private static boolean wasDown = false;

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            WheelManager.INSTANCE.registerBuiltins(event);
        }
    }

    private static boolean wasAttackDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            if (WheelManager.INSTANCE.isAnyOpen()) WheelManager.INSTANCE.closeCurrent();
            wasDown = false;
            wasAttackDown = false;
            return;
        }
        boolean down = SUMMON_DRIVER.isDown();
        if (wasDown && !down) {
            if (WheelManager.INSTANCE.isAnyOpen()) WheelManager.INSTANCE.closeCurrent();
        }
        wasDown = down;

        // 左键点击确认选择：当轮盘打开时，按下攻击键（左键）触发选中
        if (WheelManager.INSTANCE.isAnyOpen()) {
            boolean attackDown = mc.options.keyAttack.isDown();
            if (!wasAttackDown && attackDown) {
                WheelManager.INSTANCE.closeCurrent();
            }
            wasAttackDown = attackDown;
        } else {
            wasAttackDown = false;
        }
    }
}