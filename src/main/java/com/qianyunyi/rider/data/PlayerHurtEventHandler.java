package com.qianyunyi.rider.data;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.SyncHurtFractionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerHurtEventHandler {

    private static final Set<String> SUPER_ARMOR_FORMS = Set.of(
            "titan_form",
            "rising_titan_form",
            "ultimate_form",
            "ultimate_form_dark_eye",
            "rising_ultimate_form",
            "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    /**
     * 伤害应用前：为霸体形态记录受伤前生命值。
     * LivingHurtEvent 在 actuallyHurt() 扣血前触发。
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 只处理玩家受伤事件
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 获取玩家数据管理器
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 检查玩家是否处于变身中
        if (data != null && data.isHenshining()) {
            // 取消伤害事件
            event.setCanceled(true);
            return;
        }

        // 霸体形态：捕获伤害应用前的生命值，供 LivingDamageEvent 计算实际扣除量
        if (data != null && SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
            data.setHurtHealthBefore(player.getHealth());
        }
    }

    /**
     * 伤害应用后：用“受伤前血量 - 受伤后血量”计算实际扣减的生命值比例。
     * LivingDamageEvent 在 actuallyHurt() 扣血后触发，此时 getHealth() 已是受伤后的值。
     */
    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;
        if (!SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) return;

        float before = data.getHurtHealthBefore();
        float after = player.getHealth();
        // 实际扣减免去的生命值（红心），吸收护盾不再干扰
        float loss = before >= 0f ? Math.max(0f, before - after) : 0f;
        float maxHealth = player.getMaxHealth();
        float fraction = maxHealth > 0 ? loss / maxHealth : 0f;

        data.setHurtHealthBefore(-1f); // 复位快照，避免泄漏到下一次
        data.setLastHurtFraction(fraction);

        // 受伤瞬间即时推送，避免依赖周期脏同步造成一次受伤的延迟
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(
                    new SyncHurtFractionPacket(serverPlayer.getUUID(), fraction), serverPlayer);
        }

        // 只有实际扣减的生命值 ≤5%（成功格挡）才播放泰坦音效；破防（>5%）时不播放，保留原版受击音效
        if (fraction <= 0.05f) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    KamenRiderInterwovenWorldsModSounds.TITAN_ABILITY.get(),
                    player.getSoundSource(), 1.0f, 1.0f);
        }
    }

    /**
     * 弹射物命中霸体玩家且未破防（实际扣血 ≤ 最大生命 5%）时，将弹射物弹开而非插在身上/造成命中。
     * 破防（>5%）时放行，弹射物正常命中插在身上。
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof Player player)) return;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;
        if (data.isHenshining()) {
            deflectProjectile(player, event);
            event.setCanceled(true);
            return;
        }
        if (SUPER_ARMOR_FORMS.contains(data.getActiveFormName())) {
            // 未破防弹开，破防（>5%）不予处理放行
            if (data.getLastHurtFraction() <= 0.05f) {
                deflectProjectile(player, event);
                event.setCanceled(true);
            }
        }
    }

    /**
     * 沿玩家面向方向弹开弹射物，保持原有弹速。
     * 命中已被 setCanceled(true) 取消。
     */
    private static void deflectProjectile(Player player, ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        Vec3 look = player.getLookAngle();
        Vec3 motion = projectile.getDeltaMovement();
        projectile.setDeltaMovement(look.scale(motion.length()));
        projectile.setYRot(player.getYRot());
        projectile.setXRot(player.getXRot());
    }
}