package com.qianyunyi.rider.event;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.capability.provider.KuugaEnergyProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber
public class CapabilityAttachEvent {
    private static final ResourceLocation KUUGA_ENERGY_CAP =  ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "kuuga_energy_cap");

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(KUUGA_ENERGY_CAP, new KuugaEnergyProvider(player));
        }
    }
}