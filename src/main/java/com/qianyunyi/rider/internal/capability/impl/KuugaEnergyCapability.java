package com.qianyunyi.rider.internal.capability.impl;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.api.rider.behavior.IHenshinBehavior;
import com.qianyunyi.rider.internal.capability.api.IKuugaEnergyCapability;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.GrowingFormKuugaHenshinBehavior;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.event.listener.rider.kuuga.DagubaSummon2Listener;
import com.qianyunyi.rider.event.listener.rider.kuuga.DagubaSummonListener;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.impl.kuuga.KuugaRiderRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;

public class KuugaEnergyCapability implements IKuugaEnergyCapability {

    private final ServerPlayer player;
    private boolean active = false;
    private long lastCheckTime = 0;
    private long lastTickTime = 0;
    private long lastDamageTime = 0; // 上次造成伤害的时间戳
    // ========== 达古巴事件启动标记，防止重复触发 ==========
    private boolean dagubaEvent1Started = false;
    private boolean dagubaEvent2Started = false;

    // ========== 过饱和配置 ==========
    private static final float MAX_SUPER_ENERGY_LIMIT = 15000.0F; // 能量上限封顶
    private static final float CONVERT_PER_SECOND = 10.0F;        // 每秒转化速度

    public KuugaEnergyCapability(Player player) {
        this.player = (player instanceof ServerPlayer) ? (ServerPlayer) player : null;
        if (this.player != null) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player != player
                || event.side == LogicalSide.CLIENT) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 1000) {
            if (active) {
                tickEnergy(now);
            }
            return;
        }

        lastCheckTime = now;
        autoUpdateActiveState();
    }

    private void autoUpdateActiveState() {
        boolean shouldActive = shouldBeActive();

        if (shouldActive && !active) {
            active = true;
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[KuugaEnergy] 自动激活：{}", player.getName().getString());
        } else if (!shouldActive && active) {
            active = false;
            KamenRiderInterwovenWorldsMod.LOGGER.debug("[KuugaEnergy] 自动关闭：{}", player.getName().getString());
        }
    }

    public boolean shouldBeActive() {
        if (player == null || !player.isAlive()) return false;
        KuugaDataModule kuugaData = RiderPlayerVariablesProvider.get(player).getKuugaData();
        return kuugaData != null && kuugaData.isMightyKuuga();
    }

    @Override
    public void tick() {
        if (!active || player == null) return;
        long now = System.currentTimeMillis();
        tickEnergy(now);
    }

    /**
     * 每秒执行一次能量逻辑
     */
    private void tickEnergy(long now) {
        if (now - lastTickTime < 1000) return;
        lastTickTime = now;

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;

        KuugaDataModule kuuga = data.getKuugaData();
        if (kuuga == null) return;

        // ========== 统一执行：过饱和能量转化 ==========
        convertEnergy(kuuga);

        // ========== 无论是否变身，都检查达古巴事件 ==========
        checkDagubaEvents(data);

        // 非变身状态
        boolean isKuugaTransformed = false;
        if (data.getCurrentState() == HenshinState.INACTIVE
                || data.getCurrentState() == HenshinState.STANDBY
                || data.getCurrentState() == HenshinState.STANDBY_AWAKENING) {
            handleNonTransformed(data);
        }
        // 空我变身状态
        else if ("Kuuga".equals(data.getActiveRiderName())) {
            isKuugaTransformed = true;
            handleTransformed(data);
        }

        // ========== 融合度处理：基于战斗状态，仅变身时战斗才能增长 ==========
        handleFusion(kuuga, isKuugaTransformed);
    }

    /**
     * 统一检查达古巴触发
     */
    private void checkDagubaEvents(RiderPlayerData data) {
        KuugaDataModule kuuga = data.getKuugaData();

        // 达古巴事件1
        if (kuuga.getAmadamFusion() >= 75.0f && !kuuga.hasSummonedDaguba()) {
            dagubaSummonEvent(kuuga);
        }

        // 达古巴事件2
        if (kuuga.getAmadamFusion() >= 100.0f && !kuuga.hasSummonedDaguba2()) {
            dagubaSummonEvent2(kuuga);
        }
    }

    /**
     * 非变身状态逻辑
     */
    private void handleNonTransformed(RiderPlayerData data) {
        KuugaDataModule kuuga = data.getKuugaData();

        // 生命恢复
        if (player.getHealth() < player.getMaxHealth()) {
            float heal = kuuga.getAmadamEnergyMax() * 0.0001f;
            if (kuuga.getAmadamEnergy() >= heal) {
                player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                kuuga.removeAmadamEnergy(heal);
            }
        }
        // 金之力：能量转化
        else if (kuuga.isHasGoldPower()
                && kuuga.getAmadamEnergy() > kuuga.getAmadamEnergyMax() * 0.5f
                && kuuga.getSuperEnergy() < kuuga.getSuperEnergyMax()) {
            float costAma = kuuga.getAmadamEnergyMax() * 0.01f;
            float gainSuper = kuuga.getSuperEnergyMax() * 0.01f;
            if (kuuga.getAmadamEnergy() >= costAma) {
                kuuga.removeAmadamEnergy(costAma);
                kuuga.addSuperEnergy(gainSuper);
            }
        }
        // 能量自然恢复
        else {
            float regen = kuuga.getAmadamEnergyMax() * 0.01f;
            kuuga.addAmadamEnergy(regen);
        }
    }

    /**
     * 变身状态逻辑
     */
    private void handleTransformed(RiderPlayerData data) {
        KuugaDataModule kuuga = data.getKuugaData();
        String form = data.getActiveFormName();

        // 形态能量消耗
        switch (form) {
            case "base" -> consumeBase(data, kuuga);
            case "mighty_form", "dragon_form", "pegasus_form", "titan_form" -> consumeBaseMighty(data, kuuga);
            case "rising_mighty_form", "rising_dragon_form", "rising_pegasus_form", "rising_titan_form" ->
                    consumeRising(data, kuuga, form);
            case "amazing_mighty_form" -> consumeAmazing(data, kuuga);
            case "ultimate_form", "ultimate_form_dark_eye" -> consumeUltimate(data, kuuga);
            case "rising_ultimate_form", "rising_ultimate_form_dark_eye" -> consumeRisingUltimate(data, kuuga);
            case "super_rising_ultimate_form" -> consumeSuperRisingUltimate(data, kuuga);
        }
    }

    /**
     * 监听玩家造成伤害事件，用于融合度战斗状态追踪
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (player == null || !player.isAlive()) return;
        // 只处理玩家作为攻击者造成伤害的情况
        if (event.getSource().getEntity() == player) {
            lastDamageTime = System.currentTimeMillis();
        }
    }

    /**
     * 融合度处理：基于战斗状态动态调整
     * - 增长：仅在变身状态下战斗时有效（10秒内造成过伤害），效率为原来的10倍
     * - 下降：无论是否变身，非战斗状态都会缓慢下降（约3小时从100%→0%）
     * - 到达100%融合度后不再增减
     */
    private void handleFusion(KuugaDataModule kuuga, boolean isKuugaTransformed) {
        // 如果到达100%融合度，则不会再增加和减少
        if (kuuga.getAmadamFusion() >= 100.0f) {
            if (!kuuga.isUltimateForm()) {
                kuuga.setUltimateForm(true);
            }
            return;
        }

        long now = System.currentTimeMillis();
        // 10秒内造成过伤害则视为战斗状态
        boolean inCombat = (now - lastDamageTime) < 10000;

        if (inCombat && isKuugaTransformed) {
            // 仅变身状态下战斗才能增长
            float add = kuuga.getAmadamFusionMax() * 0.000556f;
            kuuga.addAmadamFusion(add);
        } else if (!inCombat) {
            // 非战斗状态：融合度缓慢下降（无论是否变身）
            float decrease = kuuga.getAmadamFusionMax() * 0.000093f;
            kuuga.setAmadamFusion(kuuga.getAmadamFusion() - decrease);
        }

        // 更新能量上限
        kuuga.setAmadamEnergyMax(1000 + (kuuga.getAmadamFusion() * 10));

        // 如果升华能量大于当前上限，下降当前的升华能量到当前的上限
        if (kuuga.getAmadamEnergy() > kuuga.getAmadamEnergyMax()) {
            kuuga.setAmadamEnergy(kuuga.getAmadamEnergyMax());
        }
    }

    private void dagubaSummonEvent(KuugaDataModule kuuga) {
        if (dagubaEvent1Started) return;       // ← 新增：已启动则跳过
        if (kuuga.isAmazinglyMightyForm()) {
            dagubaEvent1Started = true;         // ← 新增：标记已启动
            KamenRiderInterwovenWorldsMod.LOGGER.info("[空我能量] 触发达古巴第一次事件！");
            startDagubaSummonEvent1();
        }
    }

    private void dagubaSummonEvent2(KuugaDataModule kuuga) {
        if (dagubaEvent2Started) return;       // ← 新增：已启动则跳过
        dagubaEvent2Started = true;             // ← 新增：标记已启动
        KamenRiderInterwovenWorldsMod.LOGGER.info("[空我能量] 触发达古巴第二次事件！");
        startDagubaSummonEvent2();
    }

    // 启动事件
    private void startDagubaSummonEvent1() {
        DagubaSummonListener listener = new DagubaSummonListener();
        listener.initialize(player);
        listener.register();
    }

    private void startDagubaSummonEvent2() {
     DagubaSummon2Listener listener = new DagubaSummon2Listener();
        listener.initialize(player);
        listener.register();
    }
    

    /**
     * 过饱和能量转化：全局唯一执行点
     */
    private void convertEnergy(KuugaDataModule kuuga) {
        if (kuuga.getSuperChargeSaturation() <= 0.01f) {
            kuuga.setSuperCharging(false);
            kuuga.setSuperChargeSaturation(0);
            return;
        }

        kuuga.setSuperCharging(true);

        // 转化
        float saturation = kuuga.getSuperChargeSaturation();
        float convert = Math.min(saturation, CONVERT_PER_SECOND);

        // 能量上限有封顶
        float newMax = kuuga.getSuperEnergyMax() + convert;
        newMax = Math.min(newMax, MAX_SUPER_ENERGY_LIMIT);

        kuuga.setSuperEnergyMax(newMax);
        kuuga.setSuperChargeSaturation(saturation - convert);
    }

    // ========================= 消耗逻辑 =========================
    private void consumeBase(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 0.5f;
        if (checkEnergy(data, cost, "base"))
            kuuga.removeAmadamEnergy(cost);
    }

    private void consumeBaseMighty(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 1.0f;
        if (checkEnergy(data, cost, data.getActiveFormName()))
            kuuga.removeAmadamEnergy(cost);
    }

    private void consumeRising(RiderPlayerData data, KuugaDataModule kuuga, String form) {
        float cost = 3.0f;
        if (kuuga.isAmazinglyMightyForm()) {
            cost = 1;
            if (checkSuper(data, cost, form)) {
                kuuga.removeSuperEnergy(cost);
            }
        } else if (checkSuper(data, cost, form)) {
            kuuga.removeSuperEnergy(cost);
        }
    }

    private void consumeAmazing(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 1;
        if (checkSuper(data, cost, "amazing_mighty_form")) {
            kuuga.removeSuperEnergy(cost);
        }
    }

    private void consumeUltimate(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 1;
        if (checkEnergy(data, cost, "ultimate_form"))
            kuuga.removeAmadamEnergy(cost);
    }

    private void consumeRisingUltimate(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 1;
        if (checkSuper(data, cost, "rising_ultimate_form")) {
            kuuga.removeSuperEnergy(cost);
        }
    }

    private void consumeSuperRisingUltimate(RiderPlayerData data, KuugaDataModule kuuga) {
        float cost = 1;
        if (checkSuper(data, cost, "super_rising_ultimate_form")) {
            kuuga.removeSuperEnergy(cost);
        }
    }

    // ========================= 检查能量 =========================
    private boolean checkEnergy(RiderPlayerData data, float cost, String form) {
        if (data.getKuugaData().getAmadamEnergy() < cost) {
            doDegrade(data, form);
            return false;
        }
        return true;
    }

    private boolean checkEnergyAlways(KuugaDataModule kuuga, float cost) {
        return kuuga.getAmadamEnergy() >= cost;
    }

    private boolean checkSuper(RiderPlayerData data, float cost, String form) {
        if (data.getKuugaData().getSuperEnergy() < cost) {
            doSuperDegrade(data, form);
            return false;
        }
        return true;
    }

    // ========================= 退化 =========================
    private void doDegrade(RiderPlayerData data, String form) {
        RiderConfig rider = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElseThrow();
        FormConfig formConfig = rider.getForm(data.getActiveFormName());
        if ("base".equals(form)) {
            IHenshinBehavior behavior = formConfig.getBehavior();
            behavior.handleUnForm(player, rider, formConfig);
        } else {
            GrowingFormKuugaHenshinBehavior.switchToBaseForm(player);
        }
    }

    private void doSuperDegrade(RiderPlayerData data, String form) {
        RiderConfig rider = RiderRegistryHelper.getConfig(data.getActiveRiderName()).orElseThrow();
        // 从主线形态列表中查找当前形态的前一个（退化方向）
        List<String> mainForms = KuugaRiderRegistry.MAIN_FORMS;
        int idx = mainForms.indexOf(form);
        if (idx > 0) {
            String back = mainForms.get(idx - 1);
            rider.switchForm(player, back);
        }
    }

    // 玩家登出注销
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == player) {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    @Override
    public void activate() {
        this.active = true;
    }

    @Override
    public void deactivate() {
        this.active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}