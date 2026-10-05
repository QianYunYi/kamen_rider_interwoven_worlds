package com.qianyunyi.rider.internal.registry.damage.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class PlayerDarkDamageSource extends DamageSource {
    private static final String DEATH_KEY = "death.attack.player_dark";
    private final Player player;

    // 主构造：direct和causing都填玩家
    public PlayerDarkDamageSource(Player player) {
        super(
                player.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolder(DamageTypes.MAGIC)
                        .orElseThrow(() -> new IllegalStateException("未找到 MAGIC 伤害类型")),
                player,
                player
        );
        this.player = player;
    }

    public PlayerDarkDamageSource(Holder<DamageType> typeHolder, Player player) {
        super(typeHolder, player, player);
        this.player = player;
    }

    public static PlayerDarkDamageSource fromSource(DamageSource original, Player player) {
        return new PlayerDarkDamageSource(original.typeHolder(), player);
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

    // 识别本伤害源
    public static boolean isPlayerDarkDamage(DamageSource source) {
        return source instanceof PlayerDarkDamageSource;
    }
}