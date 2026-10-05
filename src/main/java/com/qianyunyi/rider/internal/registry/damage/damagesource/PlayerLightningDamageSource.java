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


/**
 * 自定义闪电伤害源，同时具有玩家伤害和闪电伤害属性
 * Minecraft 1.20+ 适配：内部自动查找原版 lightning_bolt 的 DamageType Holder
 */
public class PlayerLightningDamageSource extends DamageSource {

    private static final String DEATH_KEY = "death.attack.player_lightning";
    private final ServerPlayer player;

    /**
     * 创建玩家闪电伤害源（内部自动获取 lightning_bolt 的 DamageType Holder）
     * @param player 造成伤害的玩家
     */
    public PlayerLightningDamageSource(ServerPlayer player) {
        super(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(DamageTypes.LIGHTNING_BOLT)
                .orElseThrow(() -> new IllegalStateException("未找到 lightning_bolt 伤害类型")));
        this.player = player;
    }

    /**
     * 创建玩家闪电伤害源
     * @param damageTypeHolder 闪电伤害类型 Holder（需注册为 bypassArmor + isFire + bypassMagic）
     * @param player 造成伤害的玩家
     */
    public PlayerLightningDamageSource(Holder<DamageType> damageTypeHolder, ServerPlayer player) {
        super(damageTypeHolder);
        this.player = player;
    }

    /**
     * 便捷工厂方法：从现有伤害源提取类型创建
     * @param original 原始伤害源（用于提取 DamageType）
     * @param player 造成伤害的玩家
     */
    public static PlayerLightningDamageSource fromSource(DamageSource original, ServerPlayer player) {
        return new PlayerLightningDamageSource(original.typeHolder(), player);
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return player;
    }

    @Nullable
    @Override
    public Entity getDirectEntity() {
        return player;
    }

    @Override
    public @NotNull Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.translatable(DEATH_KEY,
                victim.getDisplayName(),
                player.getDisplayName());
    }

    /**
     * 判断是否是玩家造成的闪电伤害
     * @param source 伤害源
     * @return 如果是玩家闪电伤害返回true
     */
    public static boolean isPlayerLightningDamage(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer
                && "lightningBolt".equals(source.getMsgId());
    }
}