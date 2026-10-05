package com.qianyunyi.rider.internal.capability;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.capability.api.IKuugaEnergyCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 *  能力注册器
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = KamenRiderInterwovenWorldsMod.MODID)
public class CapabilityRegistry {

    // 注册 能力实例
    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        // 1.18.2 不需要额外注册
    }

    // 定义空我 能力实例
    public static final Capability<IKuugaEnergyCapability> KUUGA_ENERGY_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});


    // 获取 空我 能力实例
    public static LazyOptional<IKuugaEnergyCapability> getKuugaEnergy(Player player) {
        return player.getCapability(KUUGA_ENERGY_CAP, null);
    }


    // 是否拥有 空我 能力实例
    public static boolean hasKuugaEnergy(Player player) {
        return getKuugaEnergy(player).isPresent();
    }
}