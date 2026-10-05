package com.qianyunyi.rider.internal.capability.provider;

import com.qianyunyi.rider.internal.capability.CapabilityRegistry;
import com.qianyunyi.rider.internal.capability.api.IKuugaEnergyCapability;
import com.qianyunyi.rider.internal.capability.impl.KuugaEnergyCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class KuugaEnergyProvider implements ICapabilityProvider {

    private final KuugaEnergyCapability cap;
    private final LazyOptional<IKuugaEnergyCapability> lazy;

    public KuugaEnergyProvider(Player player) {
        this.cap = new KuugaEnergyCapability(player);
        this.lazy = LazyOptional.of(() -> cap);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityRegistry.KUUGA_ENERGY_CAP) {
            return lazy.cast();
        }
        return LazyOptional.empty();
    }
}