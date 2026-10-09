package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga;

import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.HUDControlMessage;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 空我初生形态变身行为
 * 继承BaseKuugaHenshinBehavior，复用基类装备/颜色/音效工具
 * 负责完整变身动画链及形态切换
 */
public class GrowingFormKuugaHenshinBehavior extends BaseKuugaHenshinBehavior {

    // ==================== 动画常量（子类特有） ====================
    private static final String COMPLETE_ANIMATION = "genuine_kuuga_henshin";
    private static final String ANIM_HENSHIN   = "henshin";

    // 亚玛达姆融合度阈值：超过此值切换为全能形态
    private static final float AMADAM_THRESHOLD = 1.0f;

    // 活跃任务追踪，用于取消/清理
    private final List<UUID> activeTasks = new ArrayList<>();

    // ==================== 待机状态（覆写：支持融合度判定） ====================

    @Override
    public void enterStandbyState(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        data.setHenshining(true);

        // 保存玩家腿部装备（待机装备）
        CompoundTag standby = HenshinUtil.getStandbyGear(player);
        standby.put("legs", player.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        data.setStandbyGear(standby);

        // 播放亚玛达姆灵石召唤动画
        HenshinUtil.sendAnimationMessage(player, COMPLETE_ANIMATION);

        // 融合度达标则切换为全能形态配置
        boolean isMighty = data.getKuugaData().getAmadamFusion() > AMADAM_THRESHOLD;
        FormConfig targetForm = isMighty ? config.getForm("mighty_form") : form;

        // 装备腰带并设置NBT（复用基类方法）
        String amadamColor = isMighty ? "redAmadam" : "orangeAmadam";
        equipArcle(player, targetForm, amadamColor, "basePanel", "baseRune", ANIM_SUMMONS);

        // 同步HUD与状态
        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), targetForm.getFormName(), true), player);
        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(targetForm.getFormName());
        data.setCurrentState(HenshinState.STANDBY);
        data.setHenshining(false);
        handleHenshin(player, config, targetForm);
    }

    // ==================== 快速变身（覆写：简化装备流程） ====================

    @Override
    public void handleQuickHenshin(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);

        // 若已装备目标装甲，跳过装备流程
        if (!HenshinUtil.isEquippedArmor(player, form.getArmorSet())) {
            // 复用基类：保存装备 → 装备装甲 → 打标记 → 重置腰带
            saveCurrentGear(player, data);
            form.equipArmor(player);
            resetArcleNbt(player, ANIM_IDLE);
        }
        setArmorNbtTags(player);

        // 应用属性并同步状态（复用基类血量比例保持）
        applyHealthRatio(player, form);
        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(form.getFormName());
        data.setCurrentState(HenshinState.ACTIVE);

        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), form.getFormName(), true), player);
        GlobalListenerManager.updateListenerState(player);
        registerAttackListener(form, player);
    }

    // ==================== 完整变身（带动画/音效链） ====================

    @Override
    public void handleHenshin(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        data.setHenshining(true);

        // 构建延迟任务链：保存装备 → 音效+腰带重置 → 装备装甲 → 隐身 → 属性应用+状态切换
        DelayedTask.ChainBuilder chain = DelayedTask.chain(player.level())
                .then(89, () -> saveActiveGear(player, data))
                .then(1,  () -> playTransformSoundAndResetArcle(player, form))
                .then(1,  () -> equipArmorAndAnimate(player, form))
                .then(20, () -> player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false)))
                .then(1,  () -> finalizeHenshin(player, data, config, form));

        UUID chainId = chain.start();
        chain.onComplete(() -> activeTasks.remove(chainId))
                .onCancel(() -> cancelHenshin(player, data, chainId));

        if (chainId != null) activeTasks.add(chainId);
    }

    // ==================== 形态切换：强制初生 ====================

    public static void switchToBaseForm(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        String riderName = data.getActiveRiderName();
        if (riderName == null || riderName.isEmpty()) return;

        RiderConfig config = RiderRegistryHelper.getConfig(riderName).orElse(null);
        // 玩家可能在延迟任务期间切换了骑士或登出，配置可能已不存在
        if (config != null) {
            // 解除当前形态
            FormConfig current = config.getForm(data.getActiveFormName());
            if (current != null) current.getBehavior().handleUnForm(player, config, current);
        }

        // 已是初生则跳过
        if ("base".equals(data.getActiveFormName())) return;

        // 切换到初生形态（快速变身模式）
        FormConfig base = config.getForm("base");
        if (base != null) base.getBehavior().handleQuickHenshin(player, config, base);
    }

    // ==================== 私有工具方法（子类特有） ====================

    /** 保存当前装备到待机/活跃槽位 */
    private void saveCurrentGear(ServerPlayer player, RiderPlayerData data) {
        CompoundTag standby = HenshinUtil.getStandbyGear(player);
        CompoundTag active  = HenshinUtil.getActiveGear(player);

        active.put("head",  player.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
        active.put("chest", player.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
        standby.put("legs", player.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        active.put("feet",  player.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));

        data.setStandbyGear(standby);
        data.setActiveGear(active);
    }

    /** 仅保存活跃装备（头盔/胸甲/靴子） */
    private void saveActiveGear(ServerPlayer player, RiderPlayerData data) {
        CompoundTag active = data.getActiveGear();
        active.put("head",  player.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
        active.put("chest", player.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
        active.put("feet",  player.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));
        data.setActiveGear(active);
    }

    /** 播放变身音效并重置腰带 */
    private void playTransformSoundAndResetArcle(ServerPlayer player, FormConfig form) {
        stopSound(player, form.getSounds().get(RiderSoundType.STANDBY));
        SoundEvent sound = form.getSounds().get(RiderSoundType.HENSHIN);
        if (sound != null) playSound(player, sound);
        resetArcleNbt(player, ANIM_IDLE);
    }

    /** 装备装甲并播放变身动画 */
    private void equipArmorAndAnimate(ServerPlayer player, FormConfig form) {
        HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(player, form, ANIM_HENSHIN);
        setArmorNbtTags(player);
    }

    /** 最终化变身：应用属性、同步状态、注册监听器 */
    private void finalizeHenshin(ServerPlayer player, RiderPlayerData data, RiderConfig config, FormConfig form) {
        applyHealthRatio(player, form);

        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(form.getFormName());
        data.setHenshining(false);
        data.setCurrentState(HenshinState.ACTIVE);
        player.setInvulnerable(false);

        GlobalListenerManager.updateListenerState(player);
        registerAttackListener(form, player);
    }

    /** 取消变身：恢复无敌状态、清理标记 */
    private void cancelHenshin(ServerPlayer player, RiderPlayerData data, UUID taskId) {
        player.setInvulnerable(false);
        data.setHenshining(false);
        activeTasks.remove(taskId);
    }

    /** 重置腰带NBT并设空闲动画（初生专用：橙色灵石） */
    private void resetArcleNbt(ServerPlayer player, String anim) {
        // 复用基类清除方法
        clearAllFormTags(player.getItemBySlot(EquipmentSlot.LEGS).getOrCreateTag());
        // 重新应用初生颜色
        applyArcleColors(player.getItemBySlot(EquipmentSlot.LEGS), "orangeAmadam", "basePanel", "baseRune");
        // 设置动画
        net.minecraft.world.item.ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        BaseAnimatedArmorItem.setAnim(belt, anim);
    }
}