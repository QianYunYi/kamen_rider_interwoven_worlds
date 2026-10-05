package com.qianyunyi.rider.internal.registry.damage.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PlayerFireDamageSource extends DamageSource {
    private static final String DEATH_KEY = "death.attack.player_fire";
    private final ServerPlayer player;

    public PlayerFireDamageSource(ServerPlayer player) {
        super(
                player.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolder(DamageTypes.ON_FIRE)
                        .orElseThrow(() -> new IllegalStateException("未找到 ON_FIRE 伤害类型")),
                player,
                player
        );
        this.player = player;
    }

    public PlayerFireDamageSource(Holder<DamageType> typeHolder, ServerPlayer player) {
        super(typeHolder, player, player);
        this.player = player;
    }

    public static PlayerFireDamageSource fromSource(DamageSource original, ServerPlayer player) {
        return new PlayerFireDamageSource(original.typeHolder(), player);
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return this.player;
    }

    @Nullable
    @Override
    public Entity getDirectEntity() {
        return this.player;
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.translatable(DEATH_KEY, victim.getDisplayName(), player.getDisplayName());
    }

    public static boolean isPlayerFireDamage(DamageSource source) {
        return source instanceof PlayerFireDamageSource;
    }

    // 原版父类原生方法，可以安全Override
    @Override
    public boolean scalesWithDifficulty() {
        return true;
    }
}