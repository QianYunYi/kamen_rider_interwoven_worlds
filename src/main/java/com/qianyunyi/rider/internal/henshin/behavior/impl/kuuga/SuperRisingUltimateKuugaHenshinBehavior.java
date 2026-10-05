package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga;

import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.ArcleItem;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.HUDControlMessage;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.GroundShatterEffect;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.kuuga.TransformationTaskUtil;
import com.qianyunyi.rider.util.MusicHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SuperRisingUltimateKuugaHenshinBehavior extends BaseKuugaHenshinBehavior {
    private final SoundEvent earthquakeSound = KamenRiderInterwovenWorldsModSounds.EARTHQUAKE.get();
    private final SoundEvent crackedStoneSound = KamenRiderInterwovenWorldsModSounds.CRACKED_STONE.get();
    private static final String IDLE_ANIMATION = "idle";
    private static final String SUMMONS_ANIMATION = "summons";
    private static final String SUMMON_ARCLE_ANIMATION = "summon_arcle";
    private static final String COMPLETE_ANIMATION = "kuuga_super_rising_ultimate_redeye_complete";

    // 添加 activeTasks 列表
    private final List<UUID> activeTasks = new ArrayList<>();

    /**
     * 进入待机状态 (INACTIVE → STANDBY)
     */
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
        HenshinUtil.sendAnimationMessage(serverPlayer, SUMMON_ARCLE_ANIMATION);
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
            tag.putString("ActiveAmadam", "goldAmadam");
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
        DelayedTask.run(serverPlayer.level(), 20, () -> handleHenshin(serverPlayer, config, formConfig));
    }

    /**
     * 完成变身 (STANDBY → ACTIVE)
     */
    @Override
    public void handleHenshin(ServerPlayer serverPlayer, RiderConfig config, FormConfig formConfig) {
        // 获取PlayerDataManager
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);

        // 1. 标记变身中
        dataManager.setHenshining(true);

        // 2. 向客户端广播动画
        HenshinUtil.sendAnimationMessage(serverPlayer, COMPLETE_ANIMATION);
        // 3. 创建方块升空效果
        GroundShatterEffect.createFlyUpEffect(
                serverPlayer.level(),
                serverPlayer.position(),
                10.0, //半径10
                90,   // 持续时间
                60,   // 滞空时间
                0.3f,  // 密度
                10,     // 最大延迟
                serverPlayer
        );

        ItemStack itemBelt = serverPlayer.getItemBySlot(EquipmentSlot.LEGS);
        CompoundTag tag = itemBelt.getOrCreateTag();

        MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
        // 播放变身音效
        SoundEvent transformSound = formConfig.getSounds().get(RiderSoundType.HENSHIN);
        if (transformSound != null) {
            MusicHandler.playSound(serverPlayer, transformSound);
        }

        // 5. 存储当并清除当前装备
        DelayedTask.ChainBuilder chainBuilder = DelayedTask.chain(serverPlayer.level())
                .then(20, () -> {
                    // 给予4.5s的药水效果
                    //serverPlayer.addEffect(new MobEffectInstance(RiderAdventureAffiliatedModMobEffects.CHANGEKUUGA.get(), 55, 1, false, false));
                    TransformationTaskUtil.startSuperRisingFullTransformationEffect(serverPlayer.level(),serverPlayer);
                    // 给予隐身效果60刻
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 1, false, false));
                    FormConfig mightyForm = config.getForm("mighty_form");
                    if (mightyForm != null) {
                        // 保存头盔、胸甲、靴子
                        CompoundTag activeGear = dataManager.getActiveGear();
                        activeGear.put("head", serverPlayer.getItemBySlot(EquipmentSlot.HEAD).save(new CompoundTag()));
                        activeGear.put("chest", serverPlayer.getItemBySlot(EquipmentSlot.CHEST).save(new CompoundTag()));
                        activeGear.put("feet", serverPlayer.getItemBySlot(EquipmentSlot.FEET).save(new CompoundTag()));
                        dataManager.setActiveGear(activeGear);
                        // 清除装备
                        serverPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        //装备全能空我盔甲
                        mightyForm.equipArmorExcludeBelt(serverPlayer);
                        clearAllFormTags(tag);
                        tag.putString("ActiveAmadam", "redAmadam");
                        tag.putString("ActivePanel", "basePanel");
                        tag.putString("ActiveRune",   "baseRune");
                        BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                    }
                // 6. 延迟清除装备和装备升华全能空我盔甲
                }).then(5, () -> {
                    FormConfig mightyForm = config.getForm("rising_mighty_form");
                    if (mightyForm != null) {
                        // 清除装备
                        serverPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        mightyForm.equipArmorExcludeBelt(serverPlayer);
                        clearAllFormTags(tag);
                        tag.putString("ActiveAmadam", "goldAmadam");
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune",   "risingRune");
                        BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                    }
                // 7. 延迟清除装备和装备惊异全能空我盔甲
                }).then(5, () -> {
                    FormConfig mightyForm = config.getForm("amazing_mighty_form");
                    if (mightyForm != null) {
                        // 清除装备
                        serverPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        mightyForm.equipArmorExcludeBelt(serverPlayer);
                        clearAllFormTags(tag);
                        tag.putString("ActiveAmadam", "darkAmadam");
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune",   "risingRune");
                        BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                    }
                // 8. 延迟清除装备和装备究极空我盔甲
                }).then(5, () -> {
                    FormConfig ultimateForm = config.getForm("ultimate_form");
                    if (ultimateForm != null) {
                        // 清除装备
                        serverPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        ultimateForm.equipArmorExcludeBelt(serverPlayer);
                        setArmorNbtTagsStringAndBoolean(serverPlayer,"ActiveEye",true,"redEye");
                        clearAllFormTags(tag);
                        tag.putString("ActiveAmadam", "darkAmadam");
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune",   "risingRune");
                        BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                    }
                // 9. 延迟清除装备和装备升华究极空我盔甲
                }).then(5, () -> {
                    FormConfig risingUltimateForm = config.getForm("rising_ultimate_form");
                    if (risingUltimateForm != null) {
                        // 清除装备
                        serverPlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                        serverPlayer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        risingUltimateForm.equipArmorExcludeBelt(serverPlayer);
                        setArmorNbtTagsStringAndBoolean(serverPlayer,"ActiveEye",true,"redEye");
                        clearAllFormTags(tag);
                        tag.putString("ActiveAmadam", "darkAmadam");
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune",   "risingRune");
                        BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);
                    }
                // 10. 延迟清除装备和最终变身为超升华究极空我
                }).then(10, () -> {
                    // 停止待机音效
                    MusicHandler.stopSound(serverPlayer, formConfig.getSounds().get(RiderSoundType.STANDBY));
                    formConfig.equipArmorExcludeBelt(serverPlayer);
                    clearAllFormTags(tag);
                    tag.putString("ActiveAmadam", "goldAmadam");
                    tag.putString("ActivePanel", "risingPanel");
                    tag.putString("ActiveRune",   "risingRune");
                    BaseAnimatedArmorItem.setAnim(itemBelt, IDLE_ANIMATION);

                    ((ServerLevel)serverPlayer.level()).sendParticles(ParticleTypes.FLASH,
                            serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1);
                    //获取变身前的生命值比例
                    double healthRatio = serverPlayer.getHealth() / serverPlayer.getMaxHealth();
                    // 保存数据
                    HenshinUtil.applyAttributeModifiers(serverPlayer, formConfig);
                    //应用生命值比例
                    serverPlayer.setHealth((float)(serverPlayer.getMaxHealth() * healthRatio));
                    dataManager.setActiveRiderName(config.getRiderName());
                    dataManager.setActiveFormName(formConfig.getFormName());
                    dataManager.setHenshining(false);
                    dataManager.setCurrentState(HenshinState.ACTIVE_AWAKENING);

                    // 注册HUD（仅客户端）
                    NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), formConfig.getFormName(), true), serverPlayer);

                    //注册盔甲守护
                    GlobalListenerManager.updateListenerState(serverPlayer);

                    // 注册攻击监听器
                    RiderAttackEventDispatcher.onFormEnter(serverPlayer, formConfig.getFormName());
                // 9. 延迟创建裂地效果
                }).then(15, () -> {
                    //创建裂地效果
                    GroundShatterEffect.createCrackGroundEffect(
                            serverPlayer.level(),
                            serverPlayer.position(),
                            5,
                            60,
                            1.0f,
                            0
                    );
                    //播放裂地音效
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(), crackedStoneSound, SoundSource.PLAYERS, 5F, 1F);
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