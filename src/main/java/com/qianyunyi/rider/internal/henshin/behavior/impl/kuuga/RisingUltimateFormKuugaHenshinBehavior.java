package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga;

import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.item.ArcleItem;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.HUDControlMessage;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RisingUltimateFormKuugaHenshinBehavior extends BaseKuugaHenshinBehavior {

    private static final String IDLE_ANIMATION = "idle";
    private static final String SUMMONS_ANIMATION = "summons";
    private static final String HENSHIN_ANIMATION = "henshin";
    private static final String GENUINE_KUUGA_HENSHIN_ANIMATION = "genuine_kuuga_henshin";
    private static final String GOLD_IDLE_ANIMATION = "gold_idle";
    private static final String DARK_IDLE_ANIMATION = "dark_idle";

    // 添加 activeTasks 列表
    private final List<UUID> activeTasks = new ArrayList<>();

    @Override
    public void enterStandbyState(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {

        //获取玩家数据
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        // 1. 标记变身中
        dataManager.setHenshining(true);
        // 2. 保存玩家装备
        CompoundTag standbyGear = HenshinUtil.getStandbyGear(serverPlayer);
        standbyGear.put("legs", serverPlayer.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));
        dataManager.setStandbyGear(standbyGear);

        //3. 播放变身动画
        HenshinUtil.sendAnimationMessage(serverPlayer, GENUINE_KUUGA_HENSHIN_ANIMATION);

        // 播放待机音效
        SoundEvent standbySound = formConfig.getSounds().get(RiderSoundType.STANDBY);
        if (standbySound != null) {
            MusicHandler.playSound(serverPlayer, standbySound);
        }
        //4. 装备腰带并播放动画
        ItemStack itemBelt = formConfig.getBeltItem().getDefaultInstance();
        if (itemBelt.getItem() instanceof ArcleItem) {
            CompoundTag tag = itemBelt.getOrCreateTag();
            tag.putBoolean("ActiveAmadam", true);
            tag.putBoolean("ActivePanel", true);
            tag.putBoolean("ActiveRune", true);
            tag.putString("ActiveAmadam", "darkAmadam");
            tag.putString("ActivePanel", "risingPanel");
            tag.putString("ActiveRune",   "risingRune");

            HenshinUtil.equipBeltAndPlayAnimation(serverPlayer, itemBelt, SUMMONS_ANIMATION);
        }

        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), formConfig.getFormName(), true), serverPlayer);

        dataManager.setActiveRiderName(config.getRiderName());
        dataManager.setActiveFormName(formConfig.getFormName());

        // 保存数据
        dataManager.setStandbyGear(standbyGear);
        dataManager.setCurrentState(HenshinState.STANDBY_AWAKENING);
        dataManager.setHenshining(false);
        handleHenshin(serverPlayer, config, formConfig);
    }

    @Override
    public void handleHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        // 1. 标记变身中
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        dataManager.setHenshining(true);

        // 创建任务链构建器
        DelayedTask.ChainBuilder chainBuilder = DelayedTask.chain(serverPlayer.level())
                .then(89, () -> {
                    //3. 保存玩家装备
                    CompoundTag activeGear = dataManager.getActiveGear();
                    activeGear.put("head", serverPlayer.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
                    activeGear.put("chest", serverPlayer.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
                    activeGear.put("feet", serverPlayer.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));
                    dataManager.setActiveGear(activeGear);
                }).then(1, () -> {
                    MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
                    // 播放变身音效
                    SoundEvent transformSound = formConfig.getSounds().get(RiderSoundType.HENSHIN);
                    if (transformSound != null) {
                        MusicHandler.playSound(serverPlayer, transformSound);
                    }

                    ItemStack itemBelt = serverPlayer.getItemBySlot(EquipmentSlot.LEGS);
                    CompoundTag tag = itemBelt.getOrCreateTag();
                    clearAllFormTags(tag);
                    tag.putBoolean("ActiveAmadam", true);
                    tag.putBoolean("ActivePanel", true);
                    tag.putBoolean("ActiveRune", true);
                    tag.putString("ActiveAmadam", "goldAmadam");
                    tag.putString("ActivePanel", "risingPanel");
                    tag.putString("ActiveRune",   "risingRune");
                    BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                }).then(1, () -> {
                    //5. 装备头盔、胸甲和靴子并播放动画
                    HenshinUtil.equipHelmetChestplateBootsAndPlayAnimation(serverPlayer, formConfig, HENSHIN_ANIMATION);
                    //6. 切换形态颜色
                    if (dataManager.getKuugaData().getHolySpring()>=50) {
                        setArmorNbtTagsStringAndBoolean(serverPlayer,"ActiveEye",true,"redEye");
                    }else {
                        setArmorNbtTagsStringAndBoolean(serverPlayer,"ActiveEye",true,"darkEye");
                    }
                }).then(20, () -> {
                    //给予自身隐身效果
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));
                }).then(10, () -> {
                    //8. 应用形态属性修饰符
                    //获取变身前的生命值比例
                    double healthRatio = serverPlayer.getHealth() / serverPlayer.getMaxHealth();
                    // 保存数据
                    HenshinUtil.applyAttributeModifiers(serverPlayer, formConfig);
                    //应用生命值比例
                    serverPlayer.setHealth((float)(serverPlayer.getMaxHealth() * healthRatio));
                    //9. 保存形态信息
                    dataManager.setActiveRiderName(config.getRiderName());
                    dataManager.setActiveFormName(formConfig.getFormName());
                    dataManager.setHenshining(false);
                    dataManager.setCurrentState(HenshinState.ACTIVE_AWAKENING);

                    serverPlayer.setInvulnerable(false);

                    GlobalListenerManager.updateListenerState(serverPlayer);

                    //注册攻击监听器
                    RiderAttackEventDispatcher.onFormEnter(serverPlayer, formConfig.getFormName());
                });

        // 使用final变量在lambda中引用
        final UUID chainId = chainBuilder.start();

        // 设置回调
        chainBuilder.onComplete(() -> activeTasks.remove(chainId)).onCancel(() -> {
            // 这里不需要参数，因为我们在 ChainBuilder 内部处理了 level
            serverPlayer.setInvulnerable(false);
            dataManager.setHenshining(false);
            activeTasks.remove(chainId);
        });

        if (chainId != null) {
            activeTasks.add(chainId);
        }
    }
}