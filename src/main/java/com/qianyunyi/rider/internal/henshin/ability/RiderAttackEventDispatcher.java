package com.qianyunyi.rider.internal.henshin.ability;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.combat.skill.kuuga.TitanSwordComboHandler;
import com.qianyunyi.rider.internal.henshin.ability.strategy.*;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.TitanSwordComboPacket;
import com.qianyunyi.rider.potion.action.SuppressEnergyExplodeAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiderAttackEventDispatcher {

    // 仅主线程访问，使用 HashMap 消除不必要的同步开销
    private static final Map<String, IRiderFormAttackStrategy> STRATEGY_MAP = new HashMap<>();
    private static final Map<UUID, String> PLAYER_FORM_MAP = new HashMap<>();

    static {
        IRiderFormAttackStrategy standard = new StandardKuugaFormAttackStrategy();
        IRiderFormAttackStrategy rising = new RisingMightyFormAttackStrategy();

        STRATEGY_MAP.put("base", new GrowingFormAttackStrategy());
        STRATEGY_MAP.put("mighty_form", standard);
        STRATEGY_MAP.put("dragon_form", standard);
        STRATEGY_MAP.put("pegasus_form", standard);
        STRATEGY_MAP.put("titan_form", standard);
        STRATEGY_MAP.put("rising_mighty_form", rising);
        STRATEGY_MAP.put("rising_dragon_form", rising);
        STRATEGY_MAP.put("rising_pegasus_form", rising);
        STRATEGY_MAP.put("rising_titan_form", rising);
        STRATEGY_MAP.put("amazing_mighty_form", rising);
        STRATEGY_MAP.put("ultimate_form", new UltimateFormAttackStrategy());
        STRATEGY_MAP.put("ultimate_form_dark_eye", new UltimateDarkFormAttackStrategy());
        STRATEGY_MAP.put("rising_ultimate_form", new RisingUltimateFormAttackStrategy());
        STRATEGY_MAP.put("rising_ultimate_form_dark_eye", new RisingUltimateDarkFormAttackStrategy());
        STRATEGY_MAP.put("super_rising_ultimate_form", new SuperRisingUltimateFormAttackStrategy());
    }

    public static void registerStrategy(String formName, IRiderFormAttackStrategy strategy) {
        STRATEGY_MAP.put(formName, strategy);
    }

    public static void onFormEnter(ServerPlayer player, String formName) {
        IRiderFormAttackStrategy strategy = STRATEGY_MAP.get(formName);
        if (strategy == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn("未找到形态策略: {}", formName);
            return;
        }
        PLAYER_FORM_MAP.put(player.getUUID(), formName);
        strategy.onFormEnter(player);
        KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}进入形态: {}", player.getName().getString(), formName);
    }

    public static void onFormLeave(ServerPlayer player) {
        String oldForm = PLAYER_FORM_MAP.remove(player.getUUID());
        if (oldForm == null) return;
        IRiderFormAttackStrategy strategy = STRATEGY_MAP.get(oldForm);
        if (strategy != null) {
            strategy.onFormLeave(player);
        }
        KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}离开形态: {}", player.getName().getString(), oldForm);
    }

    public static void onPlayerReLogin(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || !data.isActiveRider()) return;
        String formName = data.getActiveFormName();
        if (formName == null || formName.isEmpty()) return;
        onFormEnter(player, formName);
    }

    private static IRiderFormAttackStrategy getStrategy(ServerPlayer player) {
        String formName = PLAYER_FORM_MAP.get(player.getUUID());
        if (formName == null) {
            RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
            if (data == null || !data.isActiveRider()) return null;
            formName = data.getActiveFormName();
            if (formName == null) return null;
            PLAYER_FORM_MAP.put(player.getUUID(), formName);
        }
        return STRATEGY_MAP.get(formName);
    }

    private static ServerPlayer resolveRiderPlayer(Entity entity) {
        if (!(entity instanceof ServerPlayer sp)) return null;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(sp);
        return (data != null && data.isActiveRider()) ? sp : null;
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        ServerPlayer player = resolveRiderPlayer(event.getEntity());
        if (player == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(player);
        if (strategy != null) strategy.onLivingAttack(player, event);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        ServerPlayer victim = resolveRiderPlayer(event.getEntity());
        if (victim != null) {
            IRiderFormAttackStrategy strategy = getStrategy(victim);
            if (strategy != null) strategy.onLivingHurt(victim, event);
        }
        Entity srcEntity = event.getSource().getEntity();
        if (srcEntity instanceof ServerPlayer attacker && (victim == null || !victim.getUUID().equals(attacker.getUUID()))) {
            ServerPlayer resolvedAttacker = resolveRiderPlayer(attacker);
            if (resolvedAttacker != null) {
                IRiderFormAttackStrategy strategy = getStrategy(resolvedAttacker);
                if (strategy != null) strategy.onLivingHurt(resolvedAttacker, event);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();

        Entity killer = event.getSource().getEntity();
        if (killer instanceof ServerPlayer attacker) {
            ServerPlayer resolvedKiller = resolveRiderPlayer(attacker);
            if (resolvedKiller != null) {
                IRiderFormAttackStrategy strategy = getStrategy(resolvedKiller);
                if (strategy != null) strategy.onLivingDeath(resolvedKiller, event);
            }
        }
        ServerPlayer victim = resolveRiderPlayer(event.getEntity());
        if (victim != null && (killer == null || !killer.getUUID().equals(victim.getUUID()))) {
            IRiderFormAttackStrategy strategy = getStrategy(victim);
            if (strategy != null) strategy.onLivingDeath(victim, event);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerPlayer sp = resolveRiderPlayer(event.player);
        if (sp == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(sp);
        if (strategy != null) strategy.onPlayerTick(sp, event);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ServerPlayer sp = resolveRiderPlayer(event.getEntity());
        if (sp == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(sp);
        if (strategy != null) strategy.onRightClick(sp, event);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getSide().isClient()) {
            handleClientLeftClick(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getSide().isClient()) {
            handleClientLeftClick(event.getEntity());
        }
    }

    /**
     * 客户端侧：检测泰坦剑左键，校验骑士身份后发包
     */
    private static void handleClientLeftClick(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (!TitanSwordComboHandler.isHoldingTitanSword(player)) return;
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || !data.isActiveRider()) return;
        NetworkHandler.CHANNEL.sendToServer(new TitanSwordComboPacket());
    }

    /**
     * 供 TitanSwordComboPacket 调用的服务端入口，经过骑士身份校验后通过形态策略执行连击
     */
    public static void handleTitanSwordComboPacket(ServerPlayer player) {
        ServerPlayer rider = resolveRiderPlayer(player);
        if (rider == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(rider);
        if (strategy == null) return;
        // 策略的 onLeftClickEmpty / onLeftClickBlock 中统一处理泰坦剑连击
        // 此处任意选一个（不依赖具体事件对象），实际逻辑在 AbstractKuugaFormAttackStrategy.handleTitanSwordCombo
        strategy.onLeftClickEmpty(rider, null);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        ServerPlayer player = resolveRiderPlayer(event.getEntity());
        if (player == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(player);
        if (strategy != null) strategy.onLivingJump(player, event);
    }

    @SubscribeEvent
    public static void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
        ServerPlayer player = resolveRiderPlayer(event.getEntity());
        if (player == null) return;
        IRiderFormAttackStrategy strategy = getStrategy(player);
        if (strategy != null) strategy.onEntityStruckByLightning(player, event);
    }
}