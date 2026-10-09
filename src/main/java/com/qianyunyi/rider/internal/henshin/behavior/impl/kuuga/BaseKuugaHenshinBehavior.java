package com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga;

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
import com.qianyunyi.rider.internal.registry.impl.kuuga.KuugaRiderRegistry;
import com.qianyunyi.rider.internal.henshin.behavior.BaseHenshinBehavior;
import com.qianyunyi.rider.internal.henshin.ability.RiderAttackEventDispatcher;
import com.qianyunyi.rider.event.listener.GlobalListenerManager;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.MusicHandler;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public class BaseKuugaHenshinBehavior extends BaseHenshinBehavior {

    protected static final String ANIM_IDLE      = "idle";
    protected static final String ANIM_SUMMONS   = "summons";
    protected static final String ANIM_ARCLE     = "summon_arcle";

    protected static final String[] AMADAM_TAGS = {"redAmadam", "blueAmadam", "greenAmadam", "purpleAmadam", "goldAmadam", "darkAmadam"};
    protected static final String[] PANEL_TAGS  = {"basePanel", "risingPanel"};
    protected static final String[] RUNE_TAGS   = {"baseRune", "risingRune"};

    @Override
    public void enterStandbyState(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        data.setHenshining(true);

        CompoundTag standby = HenshinUtil.getStandbyGear(player);
        standby.put("legs", player.getItemBySlot(EquipmentSlot.LEGS).save(new CompoundTag()));

        HenshinUtil.sendAnimationMessage(player, ANIM_ARCLE);
        playSound(player, form.getSounds().get(RiderSoundType.STANDBY));

        equipArcle(player, form, "redAmadam", "basePanel", "baseRune", ANIM_SUMMONS);

        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), form.getFormName(), true), player);

        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(form.getFormName());
        data.setStandbyGear(standby);
        data.setCurrentState(HenshinState.STANDBY_AWAKENING);
        data.setHenshining(false);

        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), form.getFormName(), true), player);

        this.handleUnForm(player, config, form);
    }

    @Override
    public void handleQuickHenshin(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        CompoundTag standby = HenshinUtil.getStandbyGear(player);
        CompoundTag active  = HenshinUtil.getActiveGear(player);

        if (!data.getActiveRiderName().equals(config.getRiderName())) {
            saveGearSlots(player, active, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET);
        }

        if (player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
            form.equipArmor(player);
        } else {
            form.equipArmorExcludeBelt(player);
        }

        applyFormColors(form.getFormName(), player.getItemBySlot(EquipmentSlot.LEGS));
        stopSound(player, form.getSounds().get(RiderSoundType.STANDBY));
        playSound(player, form.getSounds().get(RiderSoundType.HENSHIN));

        applyHealthRatio(player, form);

        data.setActiveRiderName(config.getRiderName());
        data.setActiveFormName(form.getFormName());
        data.setStandbyGear(standby);
        data.setActiveGear(active);
        data.setCurrentState(HenshinState.ACTIVE_AWAKENING);

        GlobalListenerManager.updateListenerState(player);
        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), form.getFormName(), true), player);
        registerAttackListener(form, player);
    }

    @Override
    public void handleHenshin(ServerPlayer player, RiderConfig config, FormConfig form) {
    }

    @Override
    public void handleUnForm(ServerPlayer player, RiderConfig config, FormConfig form) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data.getCurrentState() == HenshinState.STANDBY) return;

        form.removeArmor(player);
        HenshinUtil.restorePlayerGear(player);
        data.clearGear();

        stopAllSounds(player, form);
        MusicHandler.playSound(player, form.getSounds().get(RiderSoundType.UNFORM));

        if (data.getKuugaData().isSupernaturalSensesActive()) {
            MusicHandler.stopLoopingSound(player);
            SpecialEffectsUtil.clearHighlightNearbyEnemies(30.0F, player);

            UUID taskId = data.getKuugaData().getSupernaturalSensesTaskId();
            if (taskId != null) {
                DelayedTask.cancel(taskId);
            }

            data.getKuugaData().setSupernaturalSensesActive(false);
            data.getKuugaData().setSupernaturalSensesTaskId(null);
        }

        HenshinUtil.removeBuffLists(player, form.getEffects());
        HenshinUtil.removeAttributeModifiers(player, form);
        player.setHealth(player.getMaxHealth());

        data.setCurrentState(HenshinState.INACTIVE);
        data.setActiveRiderName("");
        data.setActiveFormName("");

        GlobalListenerManager.updateListenerState(player);
        NetworkHandler.sendToPlayer(new HUDControlMessage(config.getRiderName(), form.getFormName(), false), player);
        RiderAttackEventDispatcher.onFormLeave(player);
    }

    @Override
    public boolean canTransform(ServerPlayer player) {
        return false;
    }

    protected void equipArcle(ServerPlayer player, FormConfig form,
                              String amadam, String panel, String rune, String anim) {
        ItemStack belt = form.getBeltItem().getDefaultInstance();
        if (!(belt.getItem() instanceof ArcleItem)) return;

        CompoundTag tag = belt.getOrCreateTag();
        tag.putBoolean("ActiveAmadam", true);
        tag.putBoolean("ActivePanel", true);
        tag.putBoolean("ActiveRune", true);
        tag.putString("ActiveAmadam", amadam);
        tag.putString("ActivePanel", panel);
        tag.putString("ActiveRune", rune);

        HenshinUtil.equipBeltAndPlayAnimation(player, belt, anim);
    }

    protected void applyFormColors(String formName, ItemStack belt) {
        String[] colors = KuugaRiderRegistry.getFormColors(formName);
        if (colors == null || !(belt.getItem() instanceof ArcleItem)) return;

        applyArcleColors(belt, colors[0], colors[1], colors[2]);
        BaseAnimatedArmorItem.setAnim(belt, ANIM_IDLE);
    }

    public static void applyArcleColors(ItemStack belt, String amadam, String panel, String rune) {
        if (!(belt.getItem() instanceof ArcleItem)) return;
        CompoundTag tag = belt.getOrCreateTag();
        tag.putString("ActiveAmadam", amadam);
        tag.putString("ActivePanel", panel);
        tag.putString("ActiveRune", rune);
    }

    protected void clearAllFormTags(CompoundTag tag) {
        clearTags(tag, AMADAM_TAGS);
        clearTags(tag, PANEL_TAGS);
        clearTags(tag, RUNE_TAGS);
    }

    private void clearTags(CompoundTag tag, String[] keys) {
        for (String key : keys) tag.remove(key);
    }

    protected void saveGearSlots(ServerPlayer player, CompoundTag target, EquipmentSlot... slots) {
        for (EquipmentSlot slot : slots) {
            target.put(slot.name().toLowerCase(), player.getItemBySlot(slot).save(new CompoundTag()));
        }
    }

    protected void applyHealthRatio(ServerPlayer player, FormConfig form) {
        float healthRatio = player.getHealth() / player.getMaxHealth();
        HenshinUtil.applyAttributeModifiers(player, form);
        player.setHealth(player.getMaxHealth() * healthRatio);
    }

    protected void playSound(ServerPlayer player, SoundEvent sound) {
        if (sound != null) MusicHandler.playSound(player, sound);
    }

    protected void stopSound(ServerPlayer player, SoundEvent sound) {
        if (sound != null) MusicHandler.stopSound(player, sound);
    }

    protected void stopAllSounds(ServerPlayer player, FormConfig form) {
        Map<RiderSoundType, SoundEvent> sounds = form.getSounds();
        stopSound(player, sounds.get(RiderSoundType.DRIVER));
        stopSound(player, sounds.get(RiderSoundType.STANDBY));
        stopSound(player, sounds.get(RiderSoundType.HENSHIN));
    }

    // ← 改动3: 整个方法替换
    protected void registerAttackListener(FormConfig form, ServerPlayer player) {
        RiderAttackEventDispatcher.onFormEnter(player, form.getFormName());
    }

    protected void setArmorNbtTagsStringAndBoolean(ServerPlayer player, String tags, boolean value, String bone) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(tags, value);
            tag.putString(tags, bone);
        }
    }

    protected void setArmorNbtTags(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stack.getOrCreateTag().putString("henshin5", "true");
            }
        }
    }
}