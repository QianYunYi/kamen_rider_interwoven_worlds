package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.switching;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.api.rider.IFormSwitchBehavior;
import com.qianyunyi.rider.item.ArcleItem;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.internal.registry.impl.kuuga.KuugaRiderRegistry;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.BaseKuugaHenshinBehavior;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.kuuga.TransformationTaskUtil;
import com.qianyunyi.rider.util.MusicHandler;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.KuugaFormSwitchCooldownMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;

import java.util.*;

public class KuugaSwitchBehavior implements IFormSwitchBehavior {

    private static final int QUICK_SWITCH_TICKS = 20;  // 快速切换的持续时间
    private static final Random RANDOM = new Random();

    // 高级形态列表
    private static final Set<String> ADVANCED_FORMS = Set.of(
            "amazing_mighty_form", "ultimate_form", "ultimate_form_dark_eye",
            "rising_ultimate_form", "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    // 究极形态列表
    private static final Set<String> ULTIMATE_FORMS = Set.of(
            "ultimate_form", "ultimate_form_dark_eye"
    );

    // 升华究极形态列表
    private static final Set<String> RISING_ULTIMATE_FORMS = Set.of(
            "rising_ultimate_form", "rising_ultimate_form_dark_eye",
            "super_rising_ultimate_form"
    );

    @Override
    public void handleFormSwitch(Player player, RiderConfig riderConfig,
                                 FormConfig currentForm, FormConfig targetForm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(serverPlayer);
        if (dataManager == null) return;

        String fromForm = currentForm.getFormName();
        String toForm = targetForm.getFormName();

        // 判断是否为同级切换（同阶派生 或 黑目/红目对等切换）
        boolean isSameLevel = isSameLevelSwitch(fromForm, toForm);

        dataManager.setHenshining(true);

        // 播放变身音效
        SoundEvent transformSound = targetForm.getSounds().get(RiderSoundType.HENSHIN);
        if (transformSound != null) {
            MusicHandler.playSound(serverPlayer, transformSound);
        }

        // 根据关系决定切换方式
        if (isSameLevel) {
            // 同级形态切换：快速切换，无粒子特效
            executeQuickSwitch(serverPlayer, currentForm, targetForm, dataManager, false);
        } else {
            // 主线进阶/退化切换：有粒子特效
            executeMainLineSwitch(serverPlayer, currentForm, targetForm, dataManager);
        }
    }

    /**
     * 判断两个形态是否为同级切换
     * 同级 = 同阶轮询组（如四基础派生）或黑目/红目对等切换
     */
    private static boolean isSameLevelSwitch(String fromForm, String toForm) {
        if (fromForm.equals(toForm)) return true;

        // 检查是否在同级轮询映射中
        for (List<String> cycleList : KuugaRiderRegistry.FORM_CYCLE_MAP.values()) {
            if (cycleList.contains(fromForm) && cycleList.contains(toForm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 执行快速切换（同级和退化）
     * @param isRegression 是否为退化切换
     */
    private void executeQuickSwitch(ServerPlayer player, FormConfig currentForm,
                                    FormConfig targetForm, RiderPlayerData data, boolean isRegression) {
        String targetFormName = targetForm.getFormName();

        // 检查形态类型并执行相应的切换逻辑
        if (isRisingUltimateForm(targetFormName)) {
            executeRisingUltimateFormSwitch(player, currentForm, targetForm, data, isRegression);
        } else if (isUltimateForm(targetFormName)) {
            executeUltimateFormSwitch(player, currentForm, targetForm, data, isRegression);
        } else {
            executeStandardQuickSwitch(player, currentForm, targetForm, data, isRegression);
        }
    }

    /**
     * 执行标准快速切换（非究极形态）
     */
    private void executeStandardQuickSwitch(ServerPlayer player, FormConfig currentForm,
                                            FormConfig targetForm, RiderPlayerData data, boolean isRegression) {
        double healthRatio = player.getHealth() / player.getMaxHealth();

        // 给予短暂的无敌效果
        player.invulnerableTime = QUICK_SWITCH_TICKS;

        DelayedTask.chain(player.level())
                .then(5, () -> {
                    // 切换腰带颜色
                    switchBeltAmadam(player, targetForm);
                })
                .then(5, () -> {
                    // 切换所有防具
                    switchAllArmor(player, targetForm);
                })
                .then(5, () -> {
                    // 更新属性和监听器
                    completeQuickSwitch(player, currentForm, targetForm, data, healthRatio);
                })
                .onComplete(() -> finishSwitch(player, data))
                .onCancel(() -> cancelSwitch(player, data))
                .start();
    }

    /**
     * 究极形态切换
     */
    private void executeUltimateFormSwitch(ServerPlayer player, FormConfig currentForm,
                                           FormConfig targetForm, RiderPlayerData data, boolean isRegression) {
        double healthRatio = player.getHealth() / player.getMaxHealth();

        // 给予短暂的无敌效果
        player.invulnerableTime = QUICK_SWITCH_TICKS;

        DelayedTask.chain(player.level())
                .then(5, () -> {
                    TransformationTaskUtil.startUltimateTransformationEffect(player.level(),player);
                    // 创建并配置究极形态腰带
                    ItemStack itemBelt = new ItemStack(KamenRiderInterwovenWorldsModItems.ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS.get());
                    CompoundTag tag = itemBelt.getOrCreateTag();

                    // 根据神圣之泉值设置眼睛颜色
                    if (data.getKuugaData().getHolySpring() >= 50) {
                        tag.putBoolean("ActiveEye", true);
                        tag.putString("ActiveEye", "redEye");
                    } else {
                        tag.putBoolean("ActiveEye", true);
                        tag.putString("ActiveEye", "darkEye");
                    }

                    // 装备腰带并播放动画
                    HenshinUtil.equipBeltAndPlayAnimation(player, itemBelt, "henshin");
                })
                .then(40, () -> {
                    // 应用腰带效果
                    ItemStack itemBelt = new ItemStack(targetForm.getBeltItem());
                    if (itemBelt.getItem() instanceof ArcleItem) {
                        CompoundTag tag = itemBelt.getOrCreateTag();
                        tag.putBoolean("ActiveAmadam", true);
                        tag.putBoolean("ActivePanel", true);
                        tag.putBoolean("ActiveRune", true);
                        tag.putString("ActiveAmadam", "darkAmadam");
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune", "risingRune");

                        // 装备腰带
                        player.setItemSlot(EquipmentSlot.LEGS, itemBelt);
                    }
                })
                .then(1, () -> {
                    // 切换所有防具
                    switchAllArmor(player, targetForm);
                    if (data.getKuugaData().getHolySpring() >= 50) {
                        setArmorNbtTagsStringAndBoolean(player, "ActiveEye", true, "redEye");
                    } else {
                        setArmorNbtTagsStringAndBoolean(player, "ActiveEye", true, "darkEye");
                    }
                    // 更新属性和监听器
                    completeQuickSwitch(player, currentForm, targetForm, data, healthRatio);
                })
                .onComplete(() -> finishSwitch(player, data))
                .onCancel(() -> cancelSwitch(player, data))
                .start();
    }

    /**
     * 升华究极形态切换
     */
    private void executeRisingUltimateFormSwitch(ServerPlayer player, FormConfig currentForm,
                                                 FormConfig targetForm, RiderPlayerData data, boolean isRegression) {
        double healthRatio = player.getHealth() / player.getMaxHealth();
        String targetFormName = targetForm.getFormName();

        // 给予短暂的无敌效果
        player.invulnerableTime = QUICK_SWITCH_TICKS;

        DelayedTask.chain(player.level())
                .then(5, () -> {
                    ItemStack itemBelt = null;
                    // 根据目标形态选择相应的腰带模型
                    if (targetFormName.equals("rising_ultimate_form") || targetFormName.equals("rising_ultimate_form_dark_eye")) {
                        TransformationTaskUtil.startRisingTransformationEffect(player.level(),player);
                        itemBelt = new ItemStack(KamenRiderInterwovenWorldsModItems.RISING_ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS.get());
                        CompoundTag tag = itemBelt.getOrCreateTag();
                        // 根据神圣之泉值设置眼睛颜色
                        if (data.getKuugaData().getHolySpring() >= 50) {
                            tag.putBoolean("ActiveEye", true);
                            tag.putString("ActiveEye", "redEye");
                        } else {
                            tag.putBoolean("ActiveEye", true);
                            tag.putString("ActiveEye", "darkEye");
                        }
                    } else if (targetFormName.equals("super_rising_ultimate_form")) {
                        TransformationTaskUtil.startSuperRisingTransformationEffect(player.level(),player);
                        itemBelt = new ItemStack(KamenRiderInterwovenWorldsModItems.SUPER_RISING_ULTIMATE_FORM_KUUGA_EFFECT_LEGGINGS.get());
                    }
                    // 装备腰带并播放动画
                    HenshinUtil.equipBeltAndPlayAnimation(player, itemBelt, "henshin");
                })
                .then(31, () -> {
                    // 应用腰带效果
                    ItemStack itemBelt = new ItemStack(targetForm.getBeltItem());
                    if (itemBelt.getItem() instanceof ArcleItem) {
                        CompoundTag tag = itemBelt.getOrCreateTag();
                        tag.putBoolean("ActiveAmadam", true);
                        tag.putBoolean("ActivePanel", true);
                        tag.putBoolean("ActiveRune", true);
                        if (targetFormName.equals("super_rising_ultimate_form")) {
                            tag.putString("ActiveAmadam", "goldAmadam");
                        }else {
                            tag.putString("ActiveAmadam", "darkAmadam");
                        }
                        tag.putString("ActivePanel", "risingPanel");
                        tag.putString("ActiveRune", "risingRune");

                        // 装备腰带
                        player.setItemSlot(EquipmentSlot.LEGS, itemBelt);
                        // 切换所有防具
                        switchAllArmor(player, targetForm);
                        if (data.getKuugaData().getHolySpring() >= 50) {
                            setArmorNbtTagsStringAndBoolean(player, "ActiveEye", true, "redEye");
                        } else {
                            setArmorNbtTagsStringAndBoolean(player, "ActiveEye", true, "darkEye");
                        }
                    }
                    // 更新属性和监听器
                    completeQuickSwitch(player, currentForm, targetForm, data, healthRatio);
                })
                .onComplete(() -> finishSwitch(player, data))
                .onCancel(() -> cancelSwitch(player, data))
                .start();
    }

    /**
     * 执行进阶切换（有粒子特效）
     */
    private void executeAdvancementSwitch(ServerPlayer player, FormConfig currentForm,
                                          FormConfig targetForm, RiderPlayerData data) {
        String targetFormName = targetForm.getFormName();

        // 检查形态类型并执行相应的切换逻辑
        if (isRisingUltimateForm(targetFormName)) {
            executeRisingUltimateFormSwitch(player, currentForm, targetForm, data, false);
        } else if (isUltimateForm(targetFormName)) {
            executeUltimateFormSwitch(player, currentForm, targetForm, data, false);
        } else {
            executeStandardAdvancementSwitch(player, currentForm, targetForm, data);
        }
    }

    /**
     * 执行标准进阶切换（非究极形态）
     */
    private void executeStandardAdvancementSwitch(ServerPlayer player, FormConfig currentForm,
                                                  FormConfig targetForm, RiderPlayerData data) {

        double healthRatio = player.getHealth() / player.getMaxHealth();
        boolean isAdvancedForm = isAdvancedForm(targetForm.getFormName());

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            executeStandardQuickSwitch(player, currentForm, targetForm, data, false);
            return;
        }

        // 应用切换效果
        applyAdvancementEffects(player);

        DelayedTask.chain(player.level())
                .then(10, () -> generateLightningParticles(serverLevel, player, ParticleType.STANDBY))
                .then(20, () -> {
                    switchBeltAmadam(player, targetForm);
                })
                .then(30, () -> {
                    generateLightningParticles(serverLevel, player, ParticleType.SHOULDER);
                    switchChestplate(player, targetForm);
                })
                .then(40, () -> {
                    generateLightningParticles(serverLevel, player,
                            isAdvancedForm ? ParticleType.BOTH_LEGS : ParticleType.RIGHT_LEG);
                    switchAllArmor(player, targetForm);
                    completeAdvancementSwitch(player, currentForm, targetForm, data, healthRatio);
                })
                .onComplete(() -> finishSwitch(player, data))
                .onCancel(() -> cancelSwitch(player, data))
                .start();
    }

    /**
     * 执行主线形态切换
     */
    private void executeMainLineSwitch(ServerPlayer player, FormConfig currentForm,
                                       FormConfig targetForm, RiderPlayerData data) {
        KamenRiderInterwovenWorldsMod.LOGGER.info("执行主线切换: {} -> {}",
                currentForm.getFormName(), targetForm.getFormName());

        // 主线形态之间也使用进阶切换，有粒子特效
        executeAdvancementSwitch(player, currentForm, targetForm, data);
    }

    private boolean isAdvancedForm(String formName) {
        return ADVANCED_FORMS.contains(formName);
    }

    private boolean isUltimateForm(String formName) {
        return ULTIMATE_FORMS.contains(formName);
    }

    private boolean isRisingUltimateForm(String formName) {
        return RISING_ULTIMATE_FORMS.contains(formName);
    }

    private void applyAdvancementEffects(ServerPlayer player) {
        player.invulnerableTime = 100;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false));
    }

    private void switchBeltAmadam(ServerPlayer player, FormConfig targetForm) {
        ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!(belt.getItem() instanceof ArcleItem)) return;

        String[] colors = KuugaRiderRegistry.getFormColors(targetForm.getFormName());
        if (colors != null) {
            BaseKuugaHenshinBehavior.applyArcleColors(belt, colors[0], colors[1], colors[2]);
        }
    }

    private void switchChestplate(ServerPlayer player, FormConfig targetForm) {
        var item = targetForm.getArmorForSlot(EquipmentSlot.CHEST);
        if (item != null) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(item));
        }
    }

    private void switchAllArmor(ServerPlayer player, FormConfig targetForm) {
        switchArmorSlot(player, targetForm, EquipmentSlot.HEAD);
        switchArmorSlot(player, targetForm, EquipmentSlot.CHEST);
        switchArmorSlot(player, targetForm, EquipmentSlot.FEET);
    }

    private void switchArmorSlot(ServerPlayer player, FormConfig form, EquipmentSlot slot) {
        var item = form.getArmorForSlot(slot);
        if (item != null) {
            player.setItemSlot(slot, new ItemStack(item));
        }
    }

    private void completeQuickSwitch(ServerPlayer player, FormConfig currentForm,
                                     FormConfig targetForm, RiderPlayerData data, double healthRatio) {
        HenshinUtil.removeAttributeModifiers(player, currentForm);
        HenshinUtil.removeBuffLists(player, currentForm.getEffects());

        HenshinUtil.applyAttributeModifiers(player, targetForm);

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 1, false, false));

        player.setHealth((float)(player.getMaxHealth() * healthRatio));

        data.setActiveFormName(targetForm.getFormName());
        data.setCurrentState(HenshinState.ACTIVE_AWAKENING);
        RiderAttackEventDispatcher.onFormLeave(player);
        RiderAttackEventDispatcher.onFormEnter(player, targetForm.getFormName());
    }

    private void completeAdvancementSwitch(ServerPlayer player, FormConfig currentForm,
                                           FormConfig targetForm, RiderPlayerData data, double healthRatio) {
        // 移除缓慢和抗性提升效果
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

        // 完成切换
        completeQuickSwitch(player, currentForm, targetForm, data, healthRatio);
    }

    private void finishSwitch(ServerPlayer player, RiderPlayerData data) {
        data.setHenshining(false);
        GlobalListenerManager.updateListenerState(player);
        // 切换完成后进入冷却，冷却内无法再次打开形态切换轮盘
        NetworkHandler.sendToPlayer(new KuugaFormSwitchCooldownMessage(), player);
    }

    private void cancelSwitch(ServerPlayer player, RiderPlayerData data) {
        data.setHenshining(false);
        player.invulnerableTime = 0;
    }

    private enum ParticleType { STANDBY, SHOULDER, RIGHT_LEG, LEFT_LEG, BOTH_LEGS }

    private static void generateLightningParticles(ServerLevel world, Player entity, ParticleType type) {
        List<ParticlePosition> positions = switch (type) {
            case STANDBY -> List.of(new ParticlePosition(0, entity.getBbHeight() / 2.0, 0));
            case SHOULDER -> List.of(new ParticlePosition(0, entity.getBbHeight() / 2.0 + 0.5, 0));
            case RIGHT_LEG -> List.of(new ParticlePosition(entity.getBbWidth() / 2.0, 0.2, 0));
            case LEFT_LEG -> List.of(new ParticlePosition(-entity.getBbWidth() / 2.0, 0.2, 0));
            case BOTH_LEGS -> List.of(
                    new ParticlePosition(entity.getBbWidth() / 2.0, 0.2, 0),
                    new ParticlePosition(-entity.getBbWidth() / 2.0, 0.2, 0)
            );
        };

        positions.forEach(pos -> generateParticles(world, entity, pos));
    }

    private static void generateParticles(ServerLevel world, Player entity, ParticlePosition pos) {
        double centerX = entity.getX() + pos.x;
        double centerY = entity.getY() + pos.y;
        double centerZ = entity.getZ() + pos.z;

        for (int i = 0; i < 3; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI;

            double dirX = Math.sin(phi) * Math.cos(theta);
            double dirY = Math.cos(phi);
            double dirZ = Math.sin(phi) * Math.sin(theta);

            double radius = 0.3;
            double particleX = centerX + dirX * radius;
            double particleY = centerY + dirY * radius;
            double particleZ = centerZ + dirZ * radius;

            world.sendParticles(
                    (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
                    particleX, particleY, particleZ, 5,
                    dirX * 0.1, dirY * 0.1, dirZ * 0.1, 0.05
            );
        }
    }

    private static record ParticlePosition(double x, double y, double z) {}

    /**
     * 为所有盔甲部件设置NBT标签
     * 根据神圣之泉值设置不同的眼睛颜色标签
     */
    private void setArmorNbtTagsStringAndBoolean(ServerPlayer serverPlayer, String tags, Boolean bl, String bone) {
        // 为所有盔甲部件设置NBT标签
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack itemStack = serverPlayer.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
                CompoundTag tag = itemStack.getOrCreateTag();
                // 设置标签
                tag.putBoolean(tags, bl);
                tag.putString(tags, bone);
            }
        }
    }
}