package com.qianyunyi.rider.util.kuuga;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.WeaponType;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.TitanSwordItem;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;

public class MorphingPowerUtil {

    private static final String ORIGINAL_ITEM_TAG = "OriginalItemData";
    private static final String WEAPON_TYPE_TAG = "weaponType";
    private static final String IS_MORPH_WEAPON_TAG = "IsMorphWeapon";

    // 青龙形态 青龙棍
    public static void tryMorphStick(FormConfig cfg, Player player) {
        ItemStack m = player.getMainHandItem();
        if (eligibleStick(m) && !isMorphWeapon(m)) {
            morph(m, cfg, EquipmentSlot.MAINHAND, "telescopic", player);
            player.level().playSound(null, player.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.DRAGON_ROD_FOYLA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (player.isCrouching() && isMorphWeapon(m)) {
            // 如果是变形武器，归还
            ItemStack restored = restore(m);
            if (!restored.isEmpty()) {
                player.setItemSlot(EquipmentSlot.MAINHAND, restored);
                HenshinUtil.sendAnimationMessage(player, "weapon_changes");
            }
        }
    }

    // 天马形态 天马弩
    public static void tryMorphCrossbow(FormConfig cfg, Player player) {
        ItemStack m = player.getMainHandItem();
        if (m.getItem() instanceof CrossbowItem && !isMorphWeapon(m)) {
            morph(m, cfg, EquipmentSlot.MAINHAND, null, player);
            player.level().playSound(null, player.blockPosition(),
                    KamenRiderInterwovenWorldsModSounds.BOWGUN_ACTIFING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (player.isCrouching() && isMorphWeapon(m)) {
            // 如果是变形武器，归还
            ItemStack restored = restore(m);
            if (!restored.isEmpty()) {
                player.setItemSlot(EquipmentSlot.MAINHAND, restored);
                HenshinUtil.sendAnimationMessage(player, "weapon_changes");
            }
        }
    }

    // 泰坦形态 泰坦剑双持
    public static void tryMorphSwordDual(FormConfig cfg, Player player) {
        boolean isMainHandMorph = false;
        boolean isOffhandMorph = false;

        // 处理主手
        boolean mainHandProcessed = handleHand(cfg, player, EquipmentSlot.MAINHAND, "telescopic");
        KamenRiderInterwovenWorldsMod.LOGGER.info("处理主手");
        if (mainHandProcessed) {
            isMainHandMorph = isMorphWeapon(player.getMainHandItem());
        }

        // 如果满足双持条件，处理副手
        boolean canDualWield = canDualWield(player);
        if (canDualWield) {
            // 检查副手是否为剑
            ItemStack offhand = player.getOffhandItem();
            if (offhand.getItem() instanceof SwordItem) {
                KamenRiderInterwovenWorldsMod.LOGGER.info("处理副手");
                boolean offhandProcessed = handleHand(cfg, player, EquipmentSlot.OFFHAND, "telescopic");
                if (offhandProcessed) {
                    isOffhandMorph = isMorphWeapon(player.getOffhandItem());
                }
            }
            // 副手不是剑时，不处理副手，但仍需播放主手动画（不提前return）
        }

        // 播放动画
        if (mainHandProcessed || (canDualWield && isOffhandMorph)) {
            // 变形为泰坦剑时播放转化音效
            if (isMainHandMorph || isOffhandMorph) {
                player.level().playSound(null, player.blockPosition(),
                        KamenRiderInterwovenWorldsModSounds.TITAN_FOYLA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (isMainHandMorph && isOffhandMorph) {
                // 主副手都变形了，播放双持动画
                HenshinUtil.sendAnimationMessage(player, "dual_wield_weapon_changes");
            } else {
                // 只有主手或单手持变形，播放单持动画
                HenshinUtil.sendAnimationMessage(player, "weapon_changes");
            }
        }
    }

    // 单只手处理逻辑
    private static boolean handleHand(FormConfig cfg, Player player, EquipmentSlot slot, String anim) {
        ItemStack handItem = player.getItemBySlot(slot);
        String weaponType = getWeaponType(handItem);

        if (handItem.isEmpty()) {
            return false;
        }

        if (isMorphWeapon(handItem)) {
            // 是变形武器，归还
            ItemStack restored = restore(handItem);
            if (!restored.isEmpty()) {
                player.setItemSlot(slot, restored);
                return true;
            }
        } else if (weaponType != null && canMorphWeapon(player, weaponType)) {
            // 不是变形武器，尝试变形
            morph(handItem, cfg, slot, anim, player, false);
            return true;
        }

        return false;
    }

    // 双持条件
    private static boolean canDualWield(Player player) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        return dataManager.getKuugaData().getAmadamFusion()>=50;
    }

    // 变形条件检查
    private static boolean canMorphWeapon(Player player, String weaponType) {
        int count = countWeaponType(player, weaponType);
        int max = getMaxAllowedCount(weaponType, player);
        return count < max;
    }

    // 最大数量获取
    private static int getMaxAllowedCount(String weaponType, Player player) {
        if ("sword".equals(weaponType) && canDualWield(player)) {
            return 2;
        }
        return 1;
    }

    /* 变形核心方法 - 保持简单 */
    private static void morph(ItemStack orig, FormConfig cfg, EquipmentSlot slot, String anim, Player player) {
        morph(orig, cfg, slot, anim, player, true);
    }

    /* 变形核心方法，控制是否播放动画 */
    private static void morph(ItemStack orig, FormConfig cfg, EquipmentSlot slot, String anim, Player player, boolean playAnimation) {
        if (orig.isEmpty()) return;

        // 获取武器类型字符串
        String weaponTypeStr = getWeaponType(orig);
        if (weaponTypeStr == null) return;

        // 根据武器类型获取对应的 WeaponType
        WeaponType weaponType = getWeaponTypeByString(weaponTypeStr);
        if (weaponType == null) return;

        // 从配置中获取对应的武器
        Map<WeaponType, Item> weapons = cfg.getWeapons();
        Item subItem = weapons.get(weaponType);

        if (subItem == null) {
            // 如果没有对应类型的武器，尝试使用默认的剑
            weaponType = WeaponType.SUBSTITUTE_SWORD;
            subItem = weapons.get(weaponType);
        }

        if (subItem == null) return;

        ItemStack sub = new ItemStack(subItem);
        CompoundTag tag = sub.getOrCreateTag();

        // 保存原物品信息
        CompoundTag originalTag = new CompoundTag();
        orig.save(originalTag);
        tag.put(ORIGINAL_ITEM_TAG, originalTag);

        // 标记武器类型
        tag.putString(WEAPON_TYPE_TAG, weaponTypeStr);

        // 标记为变形武器
        tag.putBoolean(IS_MORPH_WEAPON_TAG, true);

        // 复制附魔
        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(orig));
        // 青龙棍（stick类型）始终自带忠诚III和激流III
        if ("stick".equals(weaponTypeStr)) {
            enchantments.put(Enchantments.LOYALTY, 3);
            enchantments.put(Enchantments.RIPTIDE, 3);
        }
        EnchantmentHelper.setEnchantments(enchantments, sub);

        // 设置到槽位
        player.setItemSlot(slot, sub);

        // 播放动画（如果要求播放）
        if (playAnimation) {
            HenshinUtil.sendAnimationMessage(player, "weapon_changes");
        }

        // 根据武器类型设置不同的动画（普通泰坦剑跳过telescopic，直接idle）
        if (subItem instanceof TitanSwordItem) {
            HenshinUtil.equipWeaponAndPlayAnimation(player, slot, sub, "idle");
        } else {
            String morphAnim = getMorphAnimationByWeaponType(weaponTypeStr);
            if (morphAnim == null && anim != null) {
                morphAnim = anim; // 使用传入的动画
            }
            HenshinUtil.equipWeaponAndPlayAnimation(player, slot, sub, morphAnim != null ? morphAnim : "telescopic");

            // 延迟30刻恢复默认动作
            DelayedTask.run(player.level(), 30, () -> {
                HenshinUtil.equipWeaponAndPlayAnimation(player, slot, "idle");
            });
        }
    }

    // 根据武器类型获取 WeaponType 枚举
    private static WeaponType getWeaponTypeByString(String weaponType) {
        switch (weaponType) {
            case "sword":
                return WeaponType.SUBSTITUTE_SWORD;
            case "stick":
                return WeaponType.SUBSTITUTE_WAND;
            case "crossbow":
                return WeaponType.SUBSTITUTE_CROSSBOW;
            default:
                return null;
        }
    }

    // 根据武器类型获取变形动画
    private static String getMorphAnimationByWeaponType(String weaponType) {
        switch (weaponType) {
            case "stick":
                return "telescopic";
            case "sword":
                return "telescopic";
            case "crossbow":
                return "load";
            default:
                return "telescopic";
        }
    }

    // 归还所有武器
    public static void doRestoreAll(Player player) {
        if (player == null) return;

        // 先归还手中的
        restoreHand(player, EquipmentSlot.MAINHAND);
        restoreHand(player, EquipmentSlot.OFFHAND);

        // 再归还背包中的
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isMorphWeapon(stack)) {
                ItemStack restored = restore(stack);
                if (!restored.isEmpty()) {
                    player.getInventory().setItem(i, restored);
                }
            }
        }
    }

    // 手部归还
    private static void restoreHand(Player player, EquipmentSlot slot) {
        ItemStack item = player.getItemBySlot(slot);
        if (isMorphWeapon(item)) {
            ItemStack restored = restore(item);
            if (!restored.isEmpty()) {
                player.setItemSlot(slot, restored);
            }
        }
    }

    // 判断工具方法
    public static boolean eligibleStick(ItemStack s) {
        return s.getItem() instanceof TridentItem || s.getItem() instanceof FishingRodItem
                || s.getItem() instanceof ShovelItem || s.getItem() == Items.STICK;
    }

    public static boolean isMorphWeapon(ItemStack s) {
        if (s.isEmpty() || s.getTag() == null) return false;
        return s.getTag().getBoolean(IS_MORPH_WEAPON_TAG);
    }

    // 武器类型获取
    public static String getWeaponType(ItemStack stack) {
        if (stack.isEmpty()) return null;

        Item item = stack.getItem();
        if (item instanceof SwordItem) {
            return "sword";
        } else if (item instanceof CrossbowItem) {
            return "crossbow";
        } else if (eligibleStick(stack)) {
            return "stick";
        }
        return null;
    }

    // 武器计数
    public static int countWeaponType(Player player, String weaponType) {
        int count = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isMorphWeapon(stack) && weaponType.equals(getWeaponType(stack))) {
                count++;
            }
        }

        return count;
    }

    // 武器归还
    public static ItemStack restore(ItemStack morphWeapon) {
        if (!isMorphWeapon(morphWeapon)) {
            return ItemStack.EMPTY;
        }

        CompoundTag tag = morphWeapon.getTag();
        if (tag == null || !tag.contains(ORIGINAL_ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        CompoundTag originalTag = tag.getCompound(ORIGINAL_ITEM_TAG);
        ItemStack original = ItemStack.of(originalTag);

        return original.isEmpty() ? ItemStack.EMPTY : original;
    }
}