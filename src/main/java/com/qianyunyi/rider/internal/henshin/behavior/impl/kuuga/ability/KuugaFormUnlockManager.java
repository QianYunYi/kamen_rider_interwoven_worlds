package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.ability;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 空我形态解锁管理器
 * 管理青龙（跳跃）、天马（潜行）、泰坦（受击）三种形态的解锁逻辑
 */
public class KuugaFormUnlockManager {

    // ==================== 解锁阈值常量 ====================
    private static final int  DRAGON_JUMP_COUNT    = 10;       // 10次跳跃
    private static final long DRAGON_TIME_WINDOW   = 10000L;   // 10秒内
    private static final long DRAGON_DEBOUNCE      = 200L;     // 200ms防抖
    private static final long PEGASUS_SNEAK_TIME   = 30000L;   // 潜行30秒
    private static final long PEGASUS_CHECK_INTERVAL = 1000L;  // 1秒检查一次
    private static final int  TITAN_DAMAGE_COUNT   = 2;        // 半血受击2次
    private static final String RIDER_NAME = "Kuuga";

    // ==================== 状态字段 ====================
    private ServerPlayer player;

    // 青龙
    private int jumpCount = 0;
    private long firstJumpTime = 0;
    private long lastJumpTime = 0;

    // 天马
    private long sneakStartTime = 0;
    private long lastSneakCheckTime = 0;
    private boolean isSneaking = false;
    private boolean wasHurt = false;

    // 泰坦
    private int halfHealthDamageCount = 0;

    // ==================== 构造与基础 ====================

    public KuugaFormUnlockManager(ServerPlayer player) {
        setPlayer(player);
        KamenRiderInterwovenWorldsMod.LOGGER.info("创建Kuuga形态解锁管理器，玩家: {}", player.getName().getString());
    }

    public void setPlayer(ServerPlayer player) {
        this.player = player;
    }

    // ==================== 公共事件入口 ====================

    /** 处理跳跃事件（青龙形态） */
    public void handleJump() {
        if (isFormUnlocked(KuugaDataModule::isDragonForm, "dragon_form")) return;

        long now = System.currentTimeMillis();
        if (now - lastJumpTime < DRAGON_DEBOUNCE) return;
        lastJumpTime = now;

        // 首次跳跃或超时重置
        if (firstJumpTime == 0 || (now - firstJumpTime) > DRAGON_TIME_WINDOW) {
            firstJumpTime = now;
            jumpCount = 1;
        } else {
            jumpCount++;
        }

        if (jumpCount >= DRAGON_JUMP_COUNT) unlockForm("dragon_form", KuugaDataModule::setDragonForm);
    }

    /** 处理潜行事件（天马形态） */
    public void handleSneak(boolean crouching) {
        if (isFormUnlocked(KuugaDataModule::isPegasusForm, "pegasus_form")) return;

        long now = System.currentTimeMillis();
        if (now - lastSneakCheckTime < PEGASUS_CHECK_INTERVAL) return;
        lastSneakCheckTime = now;

        if (crouching) {
            if (!isSneaking) {
                // 开始潜行
                isSneaking = true;
                sneakStartTime = now;
                wasHurt = false;
            } else if (wasHurt) {
                // 受伤重置计时
                sneakStartTime = now;
                wasHurt = false;
            } else if (now - sneakStartTime >= PEGASUS_SNEAK_TIME) {
                unlockForm("pegasus_form", KuugaDataModule::setPegasusForm);
            }
        } else {
            isSneaking = false;
            sneakStartTime = 0;
        }
    }

    /** 标记受伤（天马潜行中断用） */
    public void onHurt() {
        wasHurt = true;
    }

    /** 处理半血受击事件（泰坦形态） */
    public void handleHalfHealthDamage() {
        if (isFormUnlocked(KuugaDataModule::isTitanForm, "titan_form")) return;

        if (++halfHealthDamageCount >= TITAN_DAMAGE_COUNT) {
            unlockForm("titan_form", KuugaDataModule::setTitanForm);
        }
    }

    /** 重置所有解锁进度 */
    public void resetAll() {
        resetDragon();
        resetPegasus();
        resetTitan();
    }

    // ==================== 私有核心逻辑 ====================

    /**
     * 检查形态是否已解锁或当前已是该形态
     * @param unlockedCheck 数据层解锁状态检查（接收 KuugaDataModule 实例）
     * @param formName      形态名称
     */
    private boolean isFormUnlocked(Predicate<KuugaDataModule> unlockedCheck, String formName) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return true;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null || unlockedCheck.test(kuugaData)) return true;

        // 检查当前是否已是该形态（避免重复处理）
        var current = RiderRegistryHelper.getCurrentFormConfig(player);
        return current.isPresent() && formName.equals(current.get().getFormName());
    }

    /**
     * 解锁指定形态并自动切换
     * @param formName     形态名称
     * @param unlockSetter 数据层解锁标记方法（接收 KuugaDataModule 实例和 boolean 值）
     */
    private void unlockForm(String formName, BiConsumer<KuugaDataModule, Boolean> unlockSetter) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("解锁{}形态...", formName);

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;

        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;

        // 标记解锁
        unlockSetter.accept(kuugaData, true);

        // 获取配置并切换形态
        var riderConfig = RiderRegistryHelper.getConfig(RIDER_NAME).orElseThrow(
                () -> new IllegalStateException("未找到 " + RIDER_NAME + " 配置")
        );
        var formConfig = RiderRegistryHelper.getFormConfig(riderConfig.getRiderName(), formName).orElseThrow(
                () -> new IllegalStateException("未找到形态配置: " + formName)
        );

        if (riderConfig.isSwitchRider()) {
            riderConfig.switchForm(player, formConfig.getFormName());
        }

        // 重置对应进度
        switch (formName) {
            case "dragon_form"  -> resetDragon();
            case "pegasus_form" -> resetPegasus();
            case "titan_form"   -> resetTitan();
        }
    }

    // ==================== 重置方法 ====================

    private void resetDragon() {
        jumpCount = 0;
        firstJumpTime = 0;
        lastJumpTime = 0;
    }

    private void resetPegasus() {
        sneakStartTime = 0;
        isSneaking = false;
        wasHurt = false;
        lastSneakCheckTime = 0;
    }

    private void resetTitan() {
        halfHealthDamageCount = 0;
    }
}