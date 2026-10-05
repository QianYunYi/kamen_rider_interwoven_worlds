package com.qianyunyi.rider.internal.registry.config;

import com.qianyunyi.rider.api.rider.behavior.IHUDManager;
import com.qianyunyi.rider.api.rider.behavior.IHenshinBehavior;
import com.qianyunyi.rider.api.rider.behavior.IHenshinStateListener;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.data.enums.FormType;
import com.qianyunyi.rider.data.enums.RiderSoundType;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.data.enums.WeaponType;
import com.qianyunyi.rider.api.rider.IFormSwitchBehavior;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 形态配置类 - 保留所有原有功能
 */
public class FormConfig {
    // 形态名称
    private final String formName;
    // 形态类型
    FormType formType;
    // 装备配置
    private final Item[] armorSet;

    /**
     * 装备槽位
     */
    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    // 变身条件
    private final Predicate<ServerPlayer> transformCondition;
    // 变身道具
    private final Item mainHenshinItem;
    private final Item offhandHenshinItem;
    // 音效
    private final Map<RiderSoundType, SoundEvent> sounds;
    // 变身行为
    private final IHenshinBehavior behavior;
    // HUD界面
    private final Supplier<IHUDManager> hudManagerSupplier;
    // 变身状态刻
    private final IHenshinStateListener henshinStateListener;
    // 属性修饰符
    private final List<AttributeModifierConfig> attributeModifiers;
    // 状态效果
    private final List<MobEffectInstance> effects = new ArrayList<>();
    // 允许使用的技能
    private final Map<SkillType, IRiderSkill> allowedSkills;
    // 允许使用的能力列表
    private final List<IRiderSkill> abilities = new ArrayList<>();
    // 武器
    private final Map<WeaponType, Item> weapons;
    // 变身后给予的物品
    private final Item[] grantedItem;
    // 可切换形态
    private final List<String> switchableForms = new ArrayList<>();
    // 形态切换行为
    private IFormSwitchBehavior switchBehavior;
    protected static final EquipmentSlot[] SLOTS = ARMOR_SLOTS;


    // 构造方法
    public FormConfig(String formName,
                      Item[] armorSet,
                      Predicate<ServerPlayer> transformCondition,
                      Item mainHenshinItem,
                      Item offhandHenshinItem,
                      Map<RiderSoundType, SoundEvent> sounds,
                      FormType formType,
                      IHenshinBehavior behavior,
                      Supplier<IHUDManager> hudManagerSupplier,
                      IHenshinStateListener henshinStateListener,
                      List<AttributeModifierConfig> attributeModifiers,
                      List<MobEffectInstance> effects,
                      Map<SkillType, IRiderSkill> allowedSkills,
                      List<IRiderSkill> abilities,
                      Map<WeaponType, Item> weapons,
                      Item[] grantedItem) {
        this.formName = formName;
        this.armorSet = armorSet;
        this.transformCondition = transformCondition;
        this.mainHenshinItem = mainHenshinItem;
        this.offhandHenshinItem = offhandHenshinItem;
        this.sounds = sounds;
        this.formType = formType;
        this.behavior = behavior;
        this.hudManagerSupplier = hudManagerSupplier;
        this.henshinStateListener = henshinStateListener;
        this.attributeModifiers = attributeModifiers;
        this.effects.addAll(effects);
        this.allowedSkills = allowedSkills;
        this.abilities.addAll(abilities);
        this.weapons = Objects.requireNonNullElseGet(weapons, HashMap::new);
        this.grantedItem = grantedItem;
    }

    // 添加可切换形态
    public void addSwitchableForm(String formName) {
        if (!switchableForms.contains(formName)) {
            switchableForms.add(formName);
        }
    }

    // 检查是否符合变身条件
    public boolean canTransform(ServerPlayer serverPlayer) {
        return transformCondition.test(serverPlayer);
    }

    // 装备操作
    /**
     * 装备腰带
     * @param serverPlayer 玩家实例
     */
    public void equipBelt(ServerPlayer serverPlayer) {
        Item belt = getBeltItem();
        if (belt != null) {
            serverPlayer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(belt));
        }
    }

    /**
     * 装备形态装备
     * @param serverPlayer 玩家实例
     */
    public void equipArmor(ServerPlayer serverPlayer) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (armorSet[i] != null) {
                serverPlayer.setItemSlot(SLOTS[i], new ItemStack(armorSet[i]));
            }
        }
        // 装备武器
        if (weapons.get(WeaponType.DEFAULT) != null) {
            HenshinUtil.restoreItemToSlot(serverPlayer, EquipmentSlot.MAINHAND, weapons.get(WeaponType.DEFAULT).getDefaultInstance());
        }
    }

    /**
     * 移除形态装备
     * @param serverPlayer 玩家实例
     */
    public void removeArmor(ServerPlayer serverPlayer) {
        // 只移除当前形态的装备
        for (int i = 0; i < SLOTS.length; i++) {
            Item currentItem = serverPlayer.getItemBySlot(SLOTS[i]).getItem();
            Item formItem = armorSet[i];

            // 只有当当前装备是形态装备时才移除
            if (currentItem == formItem) {
                serverPlayer.setItemSlot(SLOTS[i], ItemStack.EMPTY);
            }
        }
    }

    /**
     * 根据槽位获取对应的装备物品
     * @param slot 装备槽位
     * @return 对应的装备物品
     */
    public Item getArmorForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> armorSet[0];
            case CHEST -> armorSet[1];
            case LEGS -> armorSet[2];
            case FEET -> armorSet[3];
            default -> null;
        };
    }

    /**
     * 获取腰带物品（对应盔甲套装中的腿部装备）
     */
    public Item getBeltItem() {
        return armorSet[2];
    }

    public Item[] getArmorSetExcludeBelt() {
        return new Item[]{armorSet[0], armorSet[1], armorSet[3]};
    }

    /**
     * 装备除腰带外的装备
     */
    public void equipArmorExcludeBelt(ServerPlayer serverPlayer) {
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != EquipmentSlot.LEGS) {
                serverPlayer.setItemSlot(SLOTS[i], new ItemStack(armorSet[i]));
            }
        }
    }

    // ==================== Getter 方法 ====================

    public String getFormName() {
        return formName;
    }

    public FormType getFormType() {
        return formType;
    }

    public Item[] getArmorSet() {
        return armorSet;
    }

    public Predicate<ServerPlayer> getTransformCondition() {
        return transformCondition;
    }

    public Item getMainHenshinItem() {
        return mainHenshinItem;
    }

    public Item getOffhandHenshinItem() {
        return offhandHenshinItem;
    }

    public Map<RiderSoundType, SoundEvent> getSounds() {
        return sounds;
    }

    public IHenshinBehavior getBehavior() {
        return behavior;
    }

    public Supplier<IHUDManager> getHudManagerSupplier() {
        return hudManagerSupplier;
    }

    public IHenshinStateListener getHenshinStateListener() {
        return henshinStateListener;
    }

    public List<AttributeModifierConfig> getAttributeModifiers() {
        return attributeModifiers;
    }

    public List<MobEffectInstance> getEffects() {
        return effects;
    }

    public boolean canUseSkill(ServerPlayer serverPlayer, SkillType skillTypeToUse) {
        return allowedSkills.containsKey(skillTypeToUse);
       }

    public IRiderSkill getSkill(SkillType skillType) {
        return allowedSkills.get(skillType);
    }

    public Map<SkillType, IRiderSkill> getAllowedSkills() {
        return allowedSkills;
    }

    public List<IRiderSkill> getAbilities() {
        return abilities;
    }

    public Map<WeaponType, Item> getWeapons() {
        return weapons;
    }

    public Item[] getGrantedItem() {
        return grantedItem;
    }

    public List<String> getSwitchableForms() {
        return switchableForms;
    }

    public IFormSwitchBehavior getSwitchBehavior() {
        return switchBehavior;
    }

    public void setSwitchBehavior(IFormSwitchBehavior switchBehavior) {
        this.switchBehavior = switchBehavior;
    }

}