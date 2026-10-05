package com.qianyunyi.rider.util;


import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import com.qianyunyi.rider.item.base.BaseAnimationItem;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.AnimationMessage;
import com.qianyunyi.rider.internal.registry.config.AttributeModifierConfig;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.*;

public class HenshinUtil {
    // 存储每个玩家当前播放的声音
    private static final Map<UUID, SoundEvent> PLAYING_SOUNDS = new HashMap<>();

    /* -------------装备操纵-------------- */
    /**
     * 检查玩家是否持有指定的主副手道具
     *
     * @param player 玩家实例
     * @param mainItem 主手道具（可为空）
     * @param offhandItem 副手道具（可为空）
     * @return 如果玩家持有指定的主副手道具，返回 true；否则返回 false
     */
    public static boolean isHoldingItems(Player player, Item mainItem, Item offhandItem) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 检查主手
        if (mainItem != null && !mainHand.getItem().equals(mainItem)) {
            return false;
        }

        // 检查副手
        return offhandItem == null || offHand.getItem().equals(offhandItem);
    }

    /**
     * 移除主手物品
     *
     * @param player 玩家实例
     * @param item 要移除的物品
     */
    public static void removeMainhandItem(Player player, Item item) {
        ItemStack mainHandStack = player.getMainHandItem();
        if (!mainHandStack.isEmpty() && mainHandStack.getItem() == item) {
            if (mainHandStack.getCount() > 1) {
                mainHandStack.shrink(1);
            } else {
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        } else {
            // 如果主手不是指定物品，尝试从背包中移除
            removeItemFromInventory(player, item);
        }
    }

    /**
     * 移除副手物品
     *
     * @param player 玩家实例
     * @param item 要移除的物品
     */
    public static void removeOffhandItem(Player player, Item item) {
        ItemStack offHandStack = player.getOffhandItem();
        if (!offHandStack.isEmpty() && offHandStack.getItem() == item) {
            if (offHandStack.getCount() > 1) {
                offHandStack.shrink(1);
            } else {
                player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        } else {
            // 如果副手不是指定物品，尝试从背包中移除
            removeItemFromInventory(player, item);
        }
    }

    /**
     * 从背包中移除指定物品
     *
     * @param player 玩家实例
     * @param item 要移除的物品
     */
    public static void removeItemFromInventory(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                if (stack.getCount() > 1) {
                    stack.shrink(1);
                } else {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return; // 移除一个后返回
            }
        }
    }

    /**
     * 从背包中移除指定全部物品
     *
     * @param player 玩家实例
     * @param item 要移除的物品
     */
    public static void removeAllItemFromInventory(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }


    // 保存玩家装备（包括主副手）
    public static CompoundTag savePlayerGear(Player player) {
        CompoundTag gearData = new CompoundTag();

        // 保存盔甲
        for (int i = 0; i < FormConfig.ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(FormConfig.ARMOR_SLOTS[i]);
            CompoundTag slotTag = new CompoundTag();
            if (!stack.isEmpty()) {
                stack.save(slotTag);
            }
            gearData.put("slot" + i, slotTag);
        }

        // 保存主手物品
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            CompoundTag mainHandTag = new CompoundTag();
            mainHand.save(mainHandTag);
            gearData.put("mainhand", mainHandTag);
        }

        // 保存副手物品
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            CompoundTag offHandTag = new CompoundTag();
            offHand.save(offHandTag);
            gearData.put("offhand", offHandTag);
        }

        return gearData;
    }

    /**
     * 获取玩家待机前存储的装备
     *
     * @param player 玩家实例
     * @return 备用装备数据
     */
    public static   CompoundTag getStandbyGear(Player player) {
        return RiderPlayerVariablesProvider.get(player).getStandbyGear();
    }

    /**
     * 获取玩家变身前装备
     *
     * @param player 玩家实例
     * @return 当前装备数据
     */
    public static CompoundTag getActiveGear(Player player) {
        return RiderPlayerVariablesProvider.get(player).getActiveGear();
    }

    /**
     * 恢复待机状态装备
     */
    public static void restoreStandbyGear(Player player) {
        CompoundTag standbyGear = HenshinUtil.getStandbyGear(player);
        KamenRiderInterwovenWorldsMod.LOGGER.info("恢复待机前的装备: {}", standbyGear);
        if (standbyGear != null) {
            if (standbyGear.contains("legs")) {
                player.setItemSlot(EquipmentSlot.LEGS,
                        ItemStack.of(standbyGear.getCompound("legs")));
            }
            if (standbyGear.contains("mainhand")) {
                restoreItemToSlot(player, EquipmentSlot.MAINHAND, ItemStack.of(standbyGear.getCompound("mainhand")));
            }
            if (standbyGear.contains("offhand")) {
                restoreItemToSlot(player, EquipmentSlot.OFFHAND, ItemStack.of(standbyGear.getCompound("offhand")));
            }
        }
    }

    /**
     * 恢复玩家全部的装备
     *
     * @param player 玩家实例
     */
    public static void restorePlayerGear(Player player) {
        CompoundTag activeGear = RiderPlayerVariablesProvider.get(player).getActiveGear();
        KamenRiderInterwovenWorldsMod.LOGGER.info("恢复变身前的装备: {}", activeGear);

        if (activeGear.contains("head")) {
            player.setItemSlot(EquipmentSlot.HEAD,
                    ItemStack.of(activeGear.getCompound("head")));
        }
        if (activeGear.contains("chest")) {
            player.setItemSlot(EquipmentSlot.CHEST,
                    ItemStack.of(activeGear.getCompound("chest")));
        }
        if (activeGear.contains("feet")) {
            player.setItemSlot(EquipmentSlot.FEET,
                    ItemStack.of(activeGear.getCompound("feet")));
        }

        restoreStandbyGear(player);
    }

    /**
     * 恢复物品到指定槽位
     *
     * @param player 玩家实例
     * @param slot 装备槽位
     * @param item 要恢复的物品
     */
    public static void restoreItemToSlot(Player player, EquipmentSlot slot, ItemStack item) {
        if (item.isEmpty()) return;

        // 如果槽位为空，直接放置
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, item);
            return;
        }

        // 尝试放入背包
        if (player.getInventory().add(item)) {
            return;
        }

        // 背包满了则生成掉落物
        if (player.level().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            player.spawnAtLocation(item);
        }
    }

    /**
     * 检查玩家是否装备了全套盔甲
     *
     * @param player 玩家实例
     * @param armorSet 盔甲数组
     * @return 如果玩家装备了全套盔甲，返回 true；否则返回 false
     */
    public static boolean isFullyEquipped(Player player, Item[] armorSet) {
        for (int i = 0; i < FormConfig.ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(FormConfig.ARMOR_SLOTS[i]);
            if (stack.isEmpty() || stack.getItem() != armorSet[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测某个物品是否来自某个全套盔甲
     * @param item 物品
     * @param armorSet 盔甲数组
     * @return 如果物品来自盔甲数组，返回它的槽位；否则返回 null
     */
    public static EquipmentSlot getArmorSlot(Item item, Item[] armorSet) {
        for (int i = 0; i < FormConfig.ARMOR_SLOTS.length; i++) {
            if (armorSet[i] == item) {
                return FormConfig.ARMOR_SLOTS[i];
            }
        }
        return null;
    }

    /**
     * 检测玩家的当前的头盔、胸甲和鞋子槽位是否为空
     * @param player 玩家实例
     * @return 如果玩家的任意除腰带位装备槽位为空，返回 true；否则返回 false
     */
    public static boolean isEmptyArmorSlots(Player player) {
        // 定义需要检查的装备槽位：头盔、胸甲、鞋子
        EquipmentSlot[] relevantSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET};

        for (EquipmentSlot slot : relevantSlots) {
            if (player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否装备了对应的盔甲
     *
     * @param player 玩家实例
     * @param armorSet 盔甲数组
     * @return 如果玩家装备了对应的头盔、胸甲和鞋子，返回 true；否则返回 false
     */
    public static boolean isEquippedArmor(Player player, Item[] armorSet) {
        // 定义需要检查的装备槽位：头盔、胸甲、鞋子
        EquipmentSlot[] relevantSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET};
        // 对应需要检查的盔甲数组索引
        int[] armorIndices = {0, 1, 3};
        KamenRiderInterwovenWorldsMod.LOGGER.info("armorSet: {}", (Object) armorSet);

        for (int i = 0; i < relevantSlots.length; i++) {
            ItemStack stack = player.getItemBySlot(relevantSlots[i]);
            KamenRiderInterwovenWorldsMod.LOGGER.info("当前槽位: {}, 槽位物品: {}", relevantSlots[i], stack.getItem());
            if (stack.isEmpty() || stack.getItem() != armorSet[armorIndices[i]]) {
                KamenRiderInterwovenWorldsMod.LOGGER.info("槽位期待的装:{}", armorSet[armorIndices[i]]);
                KamenRiderInterwovenWorldsMod.LOGGER.info("槽位物品与盔甲数组不匹配");
                return false;
            }
        }

        return true;
    }

    /**
     * 发送消息给玩家
     *
     * @param player 玩家实例
     * @param message 消息内容
     * @param color 消息颜色
     */
    public static void sendMessage(Player player, String message, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(message).withStyle(color), true);
    }

    /**
     * 发送配置消息给玩家
     *
     * @param player 玩家实例
     * @param riderName 骑士名称
     * @param message 消息内容
     */
    public static void sendConfigMessage(Player player,
                                         String riderName,
                                         String message) {
        String formatted = String.format("[%s] %s", riderName, message);
        sendMessage(player, formatted, ChatFormatting.GOLD);
    }

    // 发送动画消息
    public static void sendAnimationMessage(Player player, String animationName) {
        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendToTrackingPlayers(
                    new AnimationMessage(animationName, player.getUUID(), true),
                    sp
            );
        }
    }

    /**
     * 检查玩家是否装备了腰带
     * @param player 玩家
     * @param expectedBelt 预期的腰带物品（可为null，如果为null则只检查是否有腰带）
     * @return 是否装备了指定的腰带
     */
    public static boolean hasBeltEquipped(Player player, Item expectedBelt) {
        ItemStack beltStack = player.getItemBySlot(EquipmentSlot.LEGS);
        if (beltStack.isEmpty()) {
            return false;
        }

        if (expectedBelt != null) {
            return beltStack.getItem() == expectedBelt;
        }

        return true;
    }

    /**
     * 获取玩家当前装备的腰带物品
     * @param player 玩家
     * @return 腰带物品，如果没有装备则返回null
     */
    public static Item getEquippedBelt(Player player) {
        ItemStack beltStack = player.getItemBySlot(EquipmentSlot.LEGS);
        return beltStack.isEmpty() ? null : beltStack.getItem();
    }

    /**
     * 装备腰带并播放动画
     * @param player 玩家
     * @param formConfig 形态配置
     * @param animation 动画名称
     */
    public static void equipBeltAndPlayAnimation(Player player, FormConfig formConfig, String animation) {
        if (player instanceof ServerPlayer sp) {
            ItemStack beltItem = new ItemStack(formConfig.getBeltItem());
            if (beltItem.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(beltItem, animation);
                // 装备腰带
                sp.setItemSlot(EquipmentSlot.LEGS, beltItem);
            }
        }
    }

    /**
     * 装备腰带并播放动画
     * @param player 玩家
     * @param beltItem 腰带物品
     * @param animation 动画名称
     */
    public static void equipBeltAndPlayAnimation(Player player, ItemStack beltItem, String animation) {
        if (player instanceof ServerPlayer sp) {
            if (beltItem.getItem() instanceof BaseAnimatedArmorItem) {
                // 装备腰带
                sp.setItemSlot(EquipmentSlot.LEGS, beltItem);
                BaseAnimatedArmorItem.setAnim(beltItem, animation);
            }
        }
    }

    /**
     * 装备头盔，胸甲，靴子并播放动画
     * @param player 玩家
     * @param formConfig 形态配置
     * @param animation 动画名称
     */
    public static void equipHelmetChestplateBootsAndPlayAnimation(Player player, FormConfig formConfig, String animation) {
        if (player instanceof ServerPlayer sp) {
            //获取当前形态装备
            Item[] armorSet = formConfig.getArmorSetExcludeBelt();
            //转换为ItemStack
            ItemStack helmet = new ItemStack(armorSet[0]);
            ItemStack chestplate = new ItemStack(armorSet[1]);
            ItemStack boots = new ItemStack(armorSet[2]);
            if (helmet.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(helmet, animation);
            }
            sp.setItemSlot(EquipmentSlot.HEAD, helmet);
            if (chestplate.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(chestplate, animation);
            }
            sp.setItemSlot(EquipmentSlot.CHEST, chestplate);
            if (boots.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(boots, animation);
            }
            sp.setItemSlot(EquipmentSlot.FEET, boots);
        }
    }

    /*
     * 装备头盔，胸甲，靴子并播放动画
     * @param player 玩家
     * @param Item[] 装备的物品数组，顺序为头盔、胸甲、靴子
     * @param animation 动画名称
     */
     public static void equipHelmetChestplateBootsAndPlayAnimation(Player player, ItemStack[] armorSet, String animation) {
        if (player instanceof ServerPlayer sp) {
            if (armorSet.length != 3) {
                throw new IllegalArgumentException("armorSet must contain 3 items: helmet, chestplate, boots");
            }
            ItemStack helmet = armorSet[0];
            ItemStack chestplate = armorSet[1];
            ItemStack boots = armorSet[2];
            if (helmet.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(helmet, animation);
            }
            sp.setItemSlot(EquipmentSlot.HEAD, helmet);
            if (chestplate.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(chestplate, animation);
            }
            sp.setItemSlot(EquipmentSlot.CHEST, chestplate);
            if (boots.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(boots, animation);
            }
            sp.setItemSlot(EquipmentSlot.FEET, boots);
        }
    }

    /*
     * 装备头盔，胸甲，靴子并播放动画
     * @param player 玩家
     * @param itemStack 装备的物品数组，顺序为头盔、胸甲、靴子
     * @param animation 动画名称
     */
    public static void equipHelmetChestplateBootsAndPlayAnimation(Player player, ItemStack helmet, ItemStack chestplate, ItemStack boots, String animation) {
        if (player instanceof ServerPlayer sp) {
            if (helmet.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(helmet, animation);
            }
            sp.setItemSlot(EquipmentSlot.HEAD, helmet);
            if (chestplate.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(chestplate, animation);
            }
            sp.setItemSlot(EquipmentSlot.CHEST, chestplate);
            if (boots.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(boots, animation);
            }
            sp.setItemSlot(EquipmentSlot.FEET, boots);
        }
    }

    /** 仅发送动画给除腰带以外的装备 */
    public static void equipHelmetChestplateBootsAndPlayAnimation(Player player, String animation) {
        if (player instanceof ServerPlayer sp) {
            //获取当前形态装备
            ItemStack helmet = sp.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack chestplate = sp.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack boots = sp.getItemBySlot(EquipmentSlot.FEET);
            if (helmet.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(helmet, animation);
            }
            if (chestplate.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(chestplate, animation);
            }
            if (boots.getItem() instanceof BaseAnimatedArmorItem) {
                BaseAnimatedArmorItem.setAnim(boots, animation);
            }
        }
    }


    /**
     * 装备物品并播放动画
     * @param player 玩家
     * @param itemStack 物品栈
     * @param animation 动画名称
     */
    public static void equipWeaponAndPlayAnimation(Player player, EquipmentSlot slot, ItemStack itemStack, String animation) {
        if (player instanceof ServerPlayer sp) {
            if (itemStack.getItem() instanceof BaseAnimationItem) {
                BaseAnimationItem.setAnim(itemStack, animation);
                //装备物品
                sp.setItemSlot(slot, itemStack);
            }else if (itemStack.getItem() instanceof BaseSwordAnimationItem) {
                BaseSwordAnimationItem.setAnim(itemStack, animation);
                //装备物品
                sp.setItemSlot(slot, itemStack);
            }
        }
    }


    /**
     * 修改槽位物品的动画
     * @param player 玩家
     * @param animation 动画名称
     */
    public static void equipWeaponAndPlayAnimation(Player player, EquipmentSlot slot,  String animation) {
        if (player instanceof ServerPlayer sp) {
            ItemStack itemBySlot = sp.getItemBySlot(slot);
            if (itemBySlot.getItem() instanceof BaseAnimationItem) {
                BaseAnimationItem.setAnim(itemBySlot, animation);
            }else if (itemBySlot.getItem() instanceof BaseSwordAnimationItem) {
                BaseSwordAnimationItem.setAnim(itemBySlot, animation);
            }
        }
    }

    /**
     * 为所有盔甲部件设置NBT标签
     * 根据攻击次数设置不同的变身阶段标签
     */
    public static void setArmorNbtTagsBoolean(ServerPlayer serverPlayer,String tags,Boolean bl) {
        // 清除之前的变身标签
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack itemStack = serverPlayer.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
                CompoundTag tag = itemStack.getOrCreateTag();
                // 设置当前阶段的标签
                tag.putString(tags, bl.toString());
            }
        }
    }

    /**
     * 为所有盔甲部件设置NBT标签
     * 根据攻击次数设置不同的变身阶段标签
     */
    public static void setArmorNbtTagsString(ServerPlayer serverPlayer,String tags,String bone) {
        // 清除之前的变身标签
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack itemStack = serverPlayer.getItemBySlot(slot);
            if (!itemStack.isEmpty()) {
                CompoundTag tag = itemStack.getOrCreateTag();
                // 设置当前阶段的标签
                tag.putString(tags, bone);
            }
        }
    }


    /* -------------属性修饰符管理-------------- */

    /**
     * 应用属性修饰符
     *
     * @param player 玩家实例
     * @param formConfig 形态配置
     */
    public static void applyAttributeModifiers(Player player, FormConfig formConfig) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        List<AttributeModifierConfig> modifiers = formConfig.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) return;

        for (AttributeModifierConfig modifierConfig : modifiers) {
            Attribute attribute = modifierConfig.getAttribute();
            String key = attribute.getDescriptionId() + ":" + formConfig.getFormName();

            UUID uuid = data.getAttributeUUID(key);
            if (uuid == null) {
                uuid = UUID.randomUUID();
                data.setAttributeUUID(key, uuid);
            }

            AttributeModifier modifierInstance = new AttributeModifier(
                    uuid,
                    key,
                    modifierConfig.getAmount(),
                    modifierConfig.getOperation()
            );

            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                // 检查是否已经存在相同的修饰符
                if (attributeInstance.hasModifier(modifierInstance)) {
                    // 如果存在，移除旧的修饰符
                    attributeInstance.removeModifier(modifierInstance);
                }
                // 添加新的修饰符
                attributeInstance.addTransientModifier(modifierInstance);
                KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 添加属性修饰符: {} 值: {}", key, modifierConfig.getAmount());
            }
        }
    }
    /**
     * 移除属性修饰符
     *
     * @param player 玩家实例
     * @param formConfig 形态配置
     */
    public static void removeAttributeModifiers(Player player, FormConfig formConfig) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        List<AttributeModifierConfig> modifiers = formConfig.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) return;

        for (AttributeModifierConfig modifier : modifiers) {
            Attribute attribute = modifier.getAttribute();
            String key = attribute.getDescriptionId() + ":" + formConfig.getFormName();
            UUID uuid = data.getAttributeUUID(key);

            if (uuid != null && player.getAttribute(attribute) != null) {
                Objects.requireNonNull(player.getAttribute(attribute)).removeModifier(uuid);
                KamenRiderInterwovenWorldsMod.LOGGER.info("[Kamen Rider] 移除属性修饰符: {}", key);
                data.removeAttributeUUID(key);
            }
        }
    }

    public static void removeBuffLists(Player player , List<MobEffectInstance> createBuffList) {
        createBuffList.forEach(buff -> {
            player.removeEffect(buff.getEffect());
        });
    }

}