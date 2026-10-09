package com.qianyunyi.rider.internal.henshin.armor;

import com.qianyunyi.rider.api.rider.listener.IPlayerEventListener;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 变身护甲守卫
 *
 * <p>负责处理变身状态下的护甲完整性检查和丢弃保护</p>
 */
public class HenshinArmorGuard implements IPlayerEventListener {
    private static final long BUFF_COOLDOWN = 2000;
    // 2秒冷却时间

    private ServerPlayer serverPlayer;
    private boolean isRegistered = false;
    private long lastProcessedTime = 0;
    private final Map<MobEffect, Long> effectCooldowns = new HashMap<>();

    // 缓存数据
    private FormConfig cachedFormConfig;
    private RiderConfig cachedRiderConfig;
    private Set<Item> protectedItemsCache;
    private List<MobEffectInstance> buffsCache;

    // 记录当前形态的标识符，用于检测形态变化
    private String currentRiderName;
    private String currentFormName;

    // 添加安全期计数器
    private int safeTicks = 0;

    /**
     * 初始化监听器
     *
     * @param serverPlayer 玩家
     */
    @Override
    public void initialize(ServerPlayer serverPlayer) {
        this.serverPlayer = serverPlayer;
        updateCache();
    }

    /**
     * 更新缓存数据
     */
    private void updateCache() {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        // 检查形态是否变化
        String newRiderName = data.getActiveRiderName();
        String newFormName = data.getActiveFormName();

        if (Objects.equals(currentRiderName, newRiderName) &&
                Objects.equals(currentFormName, newFormName)) {
            // 形态未变化，不需要更新缓存
            return;
        }

        // 形态变化，更新缓存
        currentRiderName = newRiderName;
        currentFormName = newFormName;

        cachedRiderConfig = RiderRegistryHelper.getConfig(currentRiderName).orElse(null);
        if (cachedRiderConfig == null) return;
        cachedFormConfig = cachedRiderConfig.getForm(currentFormName);

        if (cachedFormConfig != null) {
            // 缓存受保护物品
            protectedItemsCache = Stream.concat(
                    Arrays.stream(cachedFormConfig.getArmorSet()),
                    cachedFormConfig.getWeapons().values().stream()
            ).collect(Collectors.toSet());

            // 缓存药水效果
            buffsCache = cachedFormConfig.getEffects();

            // 形态变化时设置安全期
            safeTicks = 10;
            // 10 ticks的安全期
        } else {
            protectedItemsCache = null;
            buffsCache = null;
        }
    }

    /**
     * 检查是否已注册
     *
     * @return 是否已注册
     */
    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * 注册监听器
     */
    @Override
    public void register() {
        if (!isRegistered && serverPlayer != null) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
            isRegistered = true;
        }
    }

    /**
     * 注销监听器
     */
    @Override
    public void unregister() {
        if (isRegistered) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
            isRegistered = false;

            // 清理缓存
            cachedFormConfig = null;
            cachedRiderConfig = null;
            protectedItemsCache = null;
            buffsCache = null;
            currentRiderName = null;
            currentFormName = null;
            safeTicks = 0;
        }
    }

    /**
     * 检查是否需要激活监听
     *
     * @param serverPlayer 玩家
     * @return 是否需要激活监听
     */
    @Override
    public boolean shouldBeActive(ServerPlayer serverPlayer) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        return data != null &&
                !data.isHenshining() &&
                (data.getCurrentState() == HenshinState.ACTIVE ||
                        data.getCurrentState() == HenshinState.ACTIVE_AWAKENING);
    }

    /**
     * 获取监听器类型
     *
     * @return 监听器类型
     */
    @Override
    public String getType() {
        return "henshin_armor_guard";
    }

    /**
     * 服务端 Tick：装备完整性检查
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player != serverPlayer || event.phase != TickEvent.Phase.END) {
            return;
        }

        // 检查玩家数据是否存在
        RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
        if (data == null) return;

        // 检查上次处理时间间隔
        long now = System.currentTimeMillis();
        if (now - lastProcessedTime < 250) return;
        lastProcessedTime = now;

        // 更新缓存
        updateCache();

        // 只在变身状态下应用buff和检查装备（待机状态不应给予变身buff）
        if (!data.isHenshining() &&
                (data.getCurrentState() == HenshinState.ACTIVE ||
                        data.getCurrentState() == HenshinState.ACTIVE_AWAKENING)) {

            // 应用形态的buff
            applyFormBuffsWithCooldown(serverPlayer);
            // 检查缓存是否有效
            if (cachedFormConfig == null || cachedRiderConfig == null) return;

            // 减少安全期计数器
            if (safeTicks > 0) {
                safeTicks--;
                return;
                // 在安全期内不检查装备
            }

            // 检查所有装备完整性
            checkAllEquipmentIntegrity(serverPlayer, cachedRiderConfig, cachedFormConfig);
        }
    }

    /**
     * 检查所有装备完整性
     * 包括盔甲四件套和所有武器
     */
    private void checkAllEquipmentIntegrity(ServerPlayer serverPlayer, RiderConfig riderCfg,
                                            FormConfig formCfg) {
        // 获取配置的装备数组
        Item[] armorSet = formCfg.getArmorSet();
        if (armorSet == null || armorSet.length < 4) return;

        // 检查盔甲四件套
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,    // 头盔
                EquipmentSlot.CHEST,   // 胸甲
                EquipmentSlot.LEGS,    // 护腿
                EquipmentSlot.FEET     // 靴子
        };

        for (int i = 0; i < 4; i++) {
            ItemStack currentItem = serverPlayer.getItemBySlot(armorSlots[i]);
            Item expectedItem = armorSet[i];

            // 如果配置的装备不为null，但玩家穿戴的不是这个装备，就解除变身
            if (expectedItem != null && !currentItem.is(expectedItem)) {
                triggerDeactivation(serverPlayer, riderCfg, formCfg);
                return;
            }
        }
    }

    /**
     * 触发解除变身
     */
    private void triggerDeactivation(ServerPlayer serverPlayer, RiderConfig riderCfg,
                                     FormConfig formCfg) {
        DelayedTask.run(serverPlayer.level(), 1, () -> {
                RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);
                formCfg.getBehavior().handleUnForm(serverPlayer, riderCfg, formCfg);
                // 强制重置为INACTIVE，避免残留状态导致下次解除时报"未找到骑士配置"
                data.setCurrentState(HenshinState.INACTIVE);
                data.setActiveRiderName("");
                data.setActiveFormName("");
                GlobalListenerManager.updateListenerState(serverPlayer);
        }
        );
    }

    /**
     * 带冷却时间的应用形态buff
     *
     * @param player 玩家
     */
    private void applyFormBuffsWithCooldown(ServerPlayer player) {
        if (buffsCache == null || buffsCache.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (MobEffectInstance e : buffsCache) {
            MobEffect effectKey = e.getEffect();
            // 检查冷却时间
            Long lastApplied = effectCooldowns.get(effectKey);
            if (lastApplied != null && now - lastApplied < BUFF_COOLDOWN) {
                continue;
            }

            // 检查效果是否已存在
            MobEffectInstance existingEffect = player.getEffect(effectKey);
            if (existingEffect != null && existingEffect.getDuration() > 200) {
                continue; // 效果还在持续，不需要重新应用
            }

            // 应用效果
            player.addEffect(new MobEffectInstance(
                    effectKey, 600, e.getAmplifier(),
                    e.isAmbient(), false, false));

            // 更新冷却时间
            effectCooldowns.put(effectKey, now);
        }
    }

    /**
     * 死亡保护：阻止变身装备掉落
     */
    @SubscribeEvent
    public void onPlayerDeath(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player != serverPlayer) {
            return;
        }

        // 更新缓存
        updateCache();

        // 检查缓存是否有效
        if (cachedFormConfig == null || protectedItemsCache == null) return;

        // 阻止变身装备掉落
        preventArmorDrops(event);
    }


    /**
     * 阻止变身装备掉落
     *
     * @param event 掉落事件
     */
    private void preventArmorDrops(LivingDropsEvent event) {
        // 遍历所有掉落物实体
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ItemStack stack = itemEntity.getItem();

            // 检查物品是否在保护列表中
            if (isProtectedItem(stack)) {
                // 从掉落列表中移除
                iterator.remove();
            }
        }
    }


    /**
     * 检查物品是否受保护
     */
    private boolean isProtectedItem(ItemStack stack) {
        return protectedItemsCache != null &&
                protectedItemsCache.stream().anyMatch(stack::is);
    }
}