package com.qianyunyi.rider.mixin.weather;

import com.qianyunyi.rider.util.SnowStormManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeSnowMixin {

    @Unique
    private void forceSnowIfStormActive(CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (SnowStormManager.INSTANCE.getGlobalStorm()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }

    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void forceTempIfStormActive(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (SnowStormManager.INSTANCE.getGlobalStorm()) {
            cir.setReturnValue(0.0F);
        }
    }
}