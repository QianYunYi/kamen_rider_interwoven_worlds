package com.qianyunyi.rider.data;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.config.common.RiderAssertions;
import com.qianyunyi.rider.data.module.*;
import com.qianyunyi.rider.data.module.*;
import com.qianyunyi.rider.data.enums.HenshinState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.Collections;

public class RiderPlayerData implements INBTSerializable<CompoundTag> {
    // 模块类加载与注册表日志仅在进程生命周期中输出一次
    private static volatile boolean modulesInitialized = false;

    private HenshinState currentState = HenshinState.INACTIVE;
    private String activeRiderName = "";
    private String activeFormName = "";
    private int currentSwitchIndex = 0;
    private boolean isHenshining = false;
    private boolean isSkillActive = false;
    private boolean isKickDashing = false;
    private boolean isSupernaturalEarthActive = false;
    // 最近一次受击时“减免后的伤害”占最大生命值的比例（服务端在伤害事件中写入并同步到客户端）
    private float lastHurtFraction = 0f;
    // 受伤前生命值快照：仅在服务端 LivingHurtEvent→LivingDamageEvent 之间用于计算实际扣血，不参与序列化/同步
    private float hurtHealthBefore = -1f;

    private final List<String> unlockedRiders = new ArrayList<>();
    private String targetRiderName = "";
    private String targetFormName = "";

    private CompoundTag standbyGear = new CompoundTag();
    private CompoundTag activeGear = new CompoundTag();
    // 属性修饰符UUID映射（用于持久化属性修饰符）
    private final Map<String, UUID> attributeUUIDs = new ConcurrentHashMap<>();
    // 加载的模块
    private final Map<String, IRiderDataModule> loadedModules = new ConcurrentHashMap<>();



    // 细粒度脏标记 — 使用 int 位掩码替代 Set<String>，消除字符串哈希和对象分配开销
    private static final int DIRTY_STATE = 1;
    private static final int DIRTY_ACTIVE_RIDER = 1 << 1;
    private static final int DIRTY_ACTIVE_FORM = 1 << 2;
    private static final int DIRTY_SWITCH_INDEX = 1 << 3;
    private static final int DIRTY_IS_HENSHINING = 1 << 4;
    private static final int DIRTY_IS_SKILL_ACTIVE = 1 << 5;
    private static final int DIRTY_IS_KICK_DASHING = 1 << 6;
    private static final int DIRTY_IS_SUPERNATURAL_EARTH = 1 << 7;
    private static final int DIRTY_UNLOCKED_RIDERS = 1 << 8;
    private static final int DIRTY_TARGET_RIDER = 1 << 9;
    private static final int DIRTY_TARGET_FORM = 1 << 10;
    private static final int DIRTY_STANDBY_GEAR = 1 << 11;
    private static final int DIRTY_ACTIVE_GEAR = 1 << 12;
    private static final int DIRTY_ATTRIBUTE_UUIDS = 1 << 13;
    private static final int DIRTY_LAST_HURT_FRACTION = 1 << 14;

    // 脏标记字段名→位掩码反向映射（仅用于 getDirtyFields() 向后兼容）
    private static final String[] DIRTY_FIELD_NAMES = {
            "currentState", "activeRiderName", "activeFormName", "currentSwitchIndex",
            "isHenshining", "isSkillActive", "isKickDashing", "isSupernaturalEarthActive",
            "unlockedRiders", "targetRiderName", "targetFormName",
            "standbyGear", "activeGear", "attributeUUIDs", "lastHurtFraction"
    };
    private static final int[] DIRTY_FIELD_MASKS = {
            DIRTY_STATE, DIRTY_ACTIVE_RIDER, DIRTY_ACTIVE_FORM, DIRTY_SWITCH_INDEX,
            DIRTY_IS_HENSHINING, DIRTY_IS_SKILL_ACTIVE, DIRTY_IS_KICK_DASHING, DIRTY_IS_SUPERNATURAL_EARTH,
            DIRTY_UNLOCKED_RIDERS, DIRTY_TARGET_RIDER, DIRTY_TARGET_FORM,
            DIRTY_STANDBY_GEAR, DIRTY_ACTIVE_GEAR, DIRTY_ATTRIBUTE_UUIDS, DIRTY_LAST_HURT_FRACTION
    };

    private int dirtyFlags = 0;
    private transient boolean dirty = false;
    private transient Player player;
    private transient boolean isActiveRider = false; // 缓存 isActiveRider() 结果，由 recalculateActiveRider() 维护，避免每次事件都做 Capability 查找+字段读取

    private static final String KEY_STATE = "State";
    private static final String KEY_ACTIVE_RIDER = "ActiveRider";
    private static final String KEY_ACTIVE_FORM = "ActiveForm";
    private static final String KEY_SWITCH_INDEX = "SwitchIndex";
    private static final String KEY_IS_HENSHINING = "IsHenshining";
    private static final String KEY_IS_SKILL_ACTIVE = "IsSkillActive";
    private static final String KEY_IS_KICK_DASHING = "IsKickDashing";
    private static final String KEY_IS_SUPERNATURAL_EARTH_ACTIVE = "IsSupernaturalEarthActive";
    private static final String KEY_UNLOCKED_RIDERS = "UnlockedRiders";
    private static final String KEY_TARGET_RIDER = "TargetRider";
    private static final String KEY_TARGET_FORM = "TargetForm";
    private static final String KEY_STANDBY_GEAR = "StandbyGear";
    private static final String KEY_ACTIVE_GEAR = "ActiveGear";
    private static final String KEY_MODULES = "Modules";
    private static final String KEY_ATTRIBUTE_UUIDS = "AttributeUUIDs";
    private static final String KEY_LAST_HURT_FRACTION = "LastHurtFraction";

    public RiderPlayerData() {
        // 显式触发所有已知数据模块类的 static{} 注册块，
        // 确保 RiderDataModuleRegistry 在 createAllModules() 调用前已就绪。
        // （新增模块类时在此处补充一行 Class.forName 调用）
        // 注意：仅在进程生命周期中执行一次，避免每次 new RiderPlayerData 都刷日志。
        if (!modulesInitialized) {
            synchronized (RiderPlayerData.class) {
                if (!modulesInitialized) {
                    String[] knownModules = {
                            "com.qianyunyi.rider.data.module.KuugaDataModule",
                            "com.qianyunyi.rider.data.module.ShowaData"
                    };
                    for (String cn : knownModules) {
                        try {
                            Class.forName(cn);
                        } catch (ClassNotFoundException e) {
                            KamenRiderInterwovenWorldsMod.LOGGER.error(
                                    "[RiderPlayerData] 预加载模块类失败: {}", cn, e);
                        }
                    }
                    KamenRiderInterwovenWorldsMod.LOGGER.info("[RiderPlayerData] 预加载完成，注册表模块数: {}",
                            RiderDataModuleRegistry.getModuleCount());
                    modulesInitialized = true;
                }
            }
        }
        RiderDataModuleRegistry.createAllModules().forEach(this::registerModule);
    }

    public void setPlayer(Player player) {
        this.player = player;
        for (IRiderDataModule module : loadedModules.values()) {
            if (module instanceof BaseDataModule base) {
                base.setParentData(this);
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    private void registerModule(IRiderDataModule module) {
        RiderAssertions.notNull(module, "模块不能为空");
        if (module instanceof BaseDataModule base) {
            base.setParentData(this);
        }
        loadedModules.put(module.getModuleName(), module);
    }

    @SuppressWarnings("unchecked")
    public <T extends IRiderDataModule> T getModule(Class<T> moduleClass) {
        RiderAssertions.notNull(moduleClass, "模块类不能为空");

        String derivedName = getModuleNameFromClass(moduleClass);
        if (derivedName != null) {
            IRiderDataModule cached = loadedModules.get(derivedName);
            if (moduleClass.isInstance(cached)) return (T) cached;
        }

        for (IRiderDataModule m : loadedModules.values()) {
            if (moduleClass.isInstance(m)) return (T) m;
        }

        KamenRiderInterwovenWorldsMod.LOGGER.warn(
                "[RiderPlayerData] 未找到模块: {}", moduleClass.getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends IRiderDataModule> T getModule(String moduleName) {
        return (T) loadedModules.get(moduleName);
    }

    /**
     * 从类名派生模块名（宽松模式）。
     * - FooDataModule -> "foo"（截取 DataModule 后缀并转小写）
     * - ShowaData    -> "showadata"（截取 Data 后缀并转小写）
     * - 其他情况返回 null，调用方回退到 instanceof 遍历
     */
    private String getModuleNameFromClass(Class<?> clazz) {
        if (clazz == null) return null;
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("DataModule")) {
            return simpleName.substring(0, simpleName.length() - 10).toLowerCase(Locale.ROOT);
        }
        if (simpleName.endsWith("Data")) {
            return simpleName.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * @return 已加载模块的只读视图
     */
    public Map<String, IRiderDataModule> getLoadedModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    public KuugaDataModule getKuugaData() {
        return getModule(KuugaDataModule.class);
    }


    public ShowaData getShowaData() {
        return getModule(ShowaData.class);
    }

    public List<String> getUnlockedRiders() {
        return new ArrayList<>(unlockedRiders);
    }

    public void setUnlockedRiders(List<String> riders) {
        RiderAssertions.notNull(riders, "骑士列表不能为空");
        this.unlockedRiders.clear();
        this.unlockedRiders.addAll(riders);
        dirtyFlags |= DIRTY_UNLOCKED_RIDERS;
        markDirty();
    }

    public void addUnlockedRider(String riderName) {
        RiderAssertions.notNull(riderName, "骑士名称不能为空");
        if (!unlockedRiders.contains(riderName)) {
            unlockedRiders.add(riderName);
            dirtyFlags |= DIRTY_UNLOCKED_RIDERS;
            markDirty();
        }
    }

    public HenshinState getCurrentState() { return currentState; }
    public void setCurrentState(HenshinState state) {
        RiderAssertions.notNull(state, "状态不能为空");
        this.currentState = state;
        recalculateActiveRider();
        dirtyFlags |= DIRTY_STATE;
        markDirty();
    }

    public String getActiveRiderName() { return activeRiderName; }
    public void setActiveRiderName(String name) {
        this.activeRiderName = name != null ? name : "";
        recalculateActiveRider();
        dirtyFlags |= DIRTY_ACTIVE_RIDER;
        markDirty();
    }

    public boolean isActiveRider() {
        return isActiveRider;
    }

    private void recalculateActiveRider() {
        this.isActiveRider = currentState != HenshinState.INACTIVE
                && activeRiderName != null && !activeRiderName.isEmpty();
    }

    public String getActiveFormName() { return activeFormName; }
    public void setActiveFormName(String name) {
        this.activeFormName = name != null ? name : "";
        dirtyFlags |= DIRTY_ACTIVE_FORM;
        markDirty();
    }

    public int getCurrentSwitchIndex() { return currentSwitchIndex; }
    public void setCurrentSwitchIndex(int index) {
        this.currentSwitchIndex = index;
        dirtyFlags |= DIRTY_SWITCH_INDEX;
        markDirty();
    }
    public void incrementSwitchIndex() { this.currentSwitchIndex++; dirtyFlags |= DIRTY_SWITCH_INDEX; markDirty(); }

    public boolean isHenshining() { return isHenshining; }
    public void setHenshining(boolean value) { this.isHenshining = value; dirtyFlags |= DIRTY_IS_HENSHINING; markDirty(); }

    public boolean isSkillActive() { return isSkillActive; }
    public void setSkillActive(boolean value) { this.isSkillActive = value; dirtyFlags |= DIRTY_IS_SKILL_ACTIVE; markDirty(); }

    public boolean isKickDashing() { return isKickDashing; }
    public void setKickDashing(boolean value) { this.isKickDashing = value; dirtyFlags |= DIRTY_IS_KICK_DASHING; markDirty(); }

    public boolean isSupernaturalEarthActive() { return isSupernaturalEarthActive; }
    public void setSupernaturalEarthActive(boolean value) { this.isSupernaturalEarthActive = value; dirtyFlags |= DIRTY_IS_SUPERNATURAL_EARTH; markDirty(); }

    public float getLastHurtFraction() { return lastHurtFraction; }
    public void setLastHurtFraction(float value) {
        this.lastHurtFraction = value;
        dirtyFlags |= DIRTY_LAST_HURT_FRACTION;
        markDirty();
    }

    public float getHurtHealthBefore() { return hurtHealthBefore; }
    public void setHurtHealthBefore(float value) { this.hurtHealthBefore = value; }

    public String getTargetRiderName() { return targetRiderName; }
    public void setTargetRiderName(String name) {
        this.targetRiderName = name != null ? name : "";
        dirtyFlags |= DIRTY_TARGET_RIDER;
        markDirty();
    }

    public String getTargetFormName() { return targetFormName; }
    public void setTargetFormName(String name) {
        this.targetFormName = name != null ? name : "";
        dirtyFlags |= DIRTY_TARGET_FORM;
        markDirty();
    }

    public CompoundTag getStandbyGear() { return standbyGear.copy(); }
    public void setStandbyGear(CompoundTag tag) {
        this.standbyGear = tag != null ? tag.copy() : new CompoundTag();
        dirtyFlags |= DIRTY_STANDBY_GEAR;
        markDirty();
    }

    public CompoundTag getActiveGear() { return activeGear.copy(); }
    public void setActiveGear(CompoundTag tag) {
        this.activeGear = tag != null ? tag.copy() : new CompoundTag();
        dirtyFlags |= DIRTY_ACTIVE_GEAR;
        markDirty();
    }

    public void clearGear() {
        standbyGear = new CompoundTag();
        activeGear = new CompoundTag();
        dirtyFlags |= DIRTY_STANDBY_GEAR | DIRTY_ACTIVE_GEAR;
        markDirty();
    }

    // ===== 属性修饰符UUID管理 =====
    /**
     * 获取属性修饰符的UUID
     * @param key 属性键
     * @return UUID，如果不存在返回null
     */
    public UUID getAttributeUUID(String key) {
        return attributeUUIDs.get(key);
    }

    /**
     * 设置属性修饰符的UUID
     * @param key 属性键
     * @param uuid UUID
     */
    public void setAttributeUUID(String key, UUID uuid) {
        attributeUUIDs.put(key, uuid);
        dirtyFlags |= DIRTY_ATTRIBUTE_UUIDS;
        markDirty();
    }

    /**
     * 移除属性修饰符的UUID
     * @param key 属性键
     */
    public void removeAttributeUUID(String key) {
        attributeUUIDs.remove(key);
        dirtyFlags |= DIRTY_ATTRIBUTE_UUIDS;
        markDirty();
    }
    /**
     * 清除所有属性修饰符UUID
     */
    public void clearAttributeUUIDs() {
        attributeUUIDs.clear();
        dirtyFlags |= DIRTY_ATTRIBUTE_UUIDS;
        markDirty();
    }

    // 细粒度脏标记管理 — 使用 int 位掩码，零对象分配
    public boolean isDirty() {
        return dirty || dirtyFlags != 0 || loadedModules.values().stream().anyMatch(IRiderDataModule::isDirty);
    }

    /**
     * 获取脏标记位掩码，供 SyncRiderDataPacket.core() 直接使用位运算
     */
    public int getDirtyFlags() {
        return dirtyFlags;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
        this.dirtyFlags = 0;
        loadedModules.values().forEach(IRiderDataModule::clearDirty);
    }

    /**
     * 获取所有脏字段名集合（向后兼容）
     */
    public Set<String> getDirtyFields() {
        if (dirtyFlags == 0) return Collections.emptySet();
        Set<String> result = new HashSet<>(DIRTY_FIELD_NAMES.length);
        for (int i = 0; i < DIRTY_FIELD_NAMES.length; i++) {
            if ((dirtyFlags & DIRTY_FIELD_MASKS[i]) != 0) {
                result.add(DIRTY_FIELD_NAMES[i]);
            }
        }
        return result;
    }

    public List<IRiderDataModule> getDirtyModules() {
        return loadedModules.values().stream()
                .filter(IRiderDataModule::isDirty)
                .toList();
    }

    /**
     * 批量更新玩家数据
     */
    public void batchUpdate(Runnable operation) {
        boolean wasDirty = this.dirty;
        int initialDirtyFlags = this.dirtyFlags;
        boolean modulesHadDirty = loadedModules.values().stream().anyMatch(IRiderDataModule::isDirty);

        try {
            operation.run();
        } finally {
            if (!wasDirty) {
                boolean modulesNowDirty = loadedModules.values().stream().anyMatch(IRiderDataModule::isDirty);
                this.dirty = (this.dirtyFlags != initialDirtyFlags) || (!modulesHadDirty && modulesNowDirty);
            }
        }
    }

    public void resetToDefault() {
        currentState = HenshinState.INACTIVE;
        activeRiderName = "";
        activeFormName = "";
        currentSwitchIndex = 0;
        isHenshining = false;
        isSkillActive = false;
        isKickDashing = false;
        isSupernaturalEarthActive = false;
        lastHurtFraction = 0f;
        hurtHealthBefore = -1f;
        unlockedRiders.clear();
        targetRiderName = "";
        targetFormName = "";
        standbyGear = new CompoundTag();
        activeGear = new CompoundTag();
        attributeUUIDs.clear();
        loadedModules.values().forEach(IRiderDataModule::resetToDefault);
        recalculateActiveRider();
        markDirty();
    }

    public void tick(Player player) {
        for (IRiderDataModule module : loadedModules.values()) {
            try {
                module.tick(player);
            } catch (Exception e) {
                KamenRiderInterwovenWorldsMod.LOGGER.error("Module tick error: {}", module.getModuleName(), e);
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString(KEY_STATE, currentState.name());
        nbt.putString(KEY_ACTIVE_RIDER, activeRiderName);
        nbt.putString(KEY_ACTIVE_FORM, activeFormName);
        nbt.putInt(KEY_SWITCH_INDEX, currentSwitchIndex);
        nbt.putBoolean(KEY_IS_HENSHINING, isHenshining);
        nbt.putBoolean(KEY_IS_SKILL_ACTIVE, isSkillActive);
        nbt.putBoolean(KEY_IS_KICK_DASHING, isKickDashing);
        nbt.putBoolean(KEY_IS_SUPERNATURAL_EARTH_ACTIVE, isSupernaturalEarthActive);
        nbt.putFloat(KEY_LAST_HURT_FRACTION, lastHurtFraction);

        ListTag list = new ListTag();
        for (String rider : unlockedRiders) {
            list.add(StringTag.valueOf(rider));
        }
        nbt.put(KEY_UNLOCKED_RIDERS, list);

        nbt.putString(KEY_TARGET_RIDER, targetRiderName);
        nbt.putString(KEY_TARGET_FORM, targetFormName);

        nbt.put(KEY_STANDBY_GEAR, standbyGear.copy());
        nbt.put(KEY_ACTIVE_GEAR, activeGear.copy());

        // 序列化属性UUID映射
        CompoundTag attributeUUIDsTag = new CompoundTag();
        for (Map.Entry<String, UUID> entry : attributeUUIDs.entrySet()) {
            attributeUUIDsTag.putString(entry.getKey(), entry.getValue().toString());
        }
        nbt.put(KEY_ATTRIBUTE_UUIDS, attributeUUIDsTag);

        CompoundTag modulesTag = new CompoundTag();
        for (Map.Entry<String, IRiderDataModule> entry : loadedModules.entrySet()) {
            modulesTag.put(entry.getKey(), entry.getValue().serializeNBT());
        }
        nbt.put(KEY_MODULES, modulesTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return;

        try {
            if (nbt.contains(KEY_STATE)) {
                currentState = HenshinState.fromName(nbt.getString(KEY_STATE));
            }
            if (nbt.contains(KEY_ACTIVE_RIDER)) {
                activeRiderName = nbt.getString(KEY_ACTIVE_RIDER);
            }
            if (nbt.contains(KEY_ACTIVE_FORM)) {
                activeFormName = nbt.getString(KEY_ACTIVE_FORM);
            }
            if (nbt.contains(KEY_SWITCH_INDEX)) {
                currentSwitchIndex = nbt.getInt(KEY_SWITCH_INDEX);
            }
            if (nbt.contains(KEY_IS_HENSHINING)) {
                isHenshining = nbt.getBoolean(KEY_IS_HENSHINING);
            }
            if (nbt.contains(KEY_IS_SKILL_ACTIVE)) {
                isSkillActive = nbt.getBoolean(KEY_IS_SKILL_ACTIVE);
            }
            if (nbt.contains(KEY_IS_KICK_DASHING)) {
                isKickDashing = nbt.getBoolean(KEY_IS_KICK_DASHING);
            }
            if (nbt.contains(KEY_IS_SUPERNATURAL_EARTH_ACTIVE)) {
                isSupernaturalEarthActive = nbt.getBoolean(KEY_IS_SUPERNATURAL_EARTH_ACTIVE);
            }
            if (nbt.contains(KEY_LAST_HURT_FRACTION)) {
                lastHurtFraction = nbt.getFloat(KEY_LAST_HURT_FRACTION);
            }

            if (nbt.contains(KEY_UNLOCKED_RIDERS, 9)) {
                unlockedRiders.clear();
                ListTag list = nbt.getList(KEY_UNLOCKED_RIDERS, 8);
                for (int i = 0; i < list.size(); i++) {
                    unlockedRiders.add(list.getString(i));
                }
            }

            if (nbt.contains(KEY_TARGET_RIDER)) {
                targetRiderName = nbt.getString(KEY_TARGET_RIDER);
            }
            if (nbt.contains(KEY_TARGET_FORM)) {
                targetFormName = nbt.getString(KEY_TARGET_FORM);
            }

            if (nbt.contains(KEY_STANDBY_GEAR, 10)) {
                standbyGear = nbt.getCompound(KEY_STANDBY_GEAR);
            }
            if (nbt.contains(KEY_ACTIVE_GEAR, 10)) {
                activeGear = nbt.getCompound(KEY_ACTIVE_GEAR);
            }

            // 反序列化属性UUID映射
            if (nbt.contains(KEY_ATTRIBUTE_UUIDS, 10)) {
                attributeUUIDs.clear();
                CompoundTag attributeUUIDsTag = nbt.getCompound(KEY_ATTRIBUTE_UUIDS);
                for (String key : attributeUUIDsTag.getAllKeys()) {
                    try {
                        UUID uuid = UUID.fromString(attributeUUIDsTag.getString(key));
                        attributeUUIDs.put(key, uuid);
                    } catch (IllegalArgumentException e) {
                        KamenRiderInterwovenWorldsMod.LOGGER.error("无效的属性UUID格式: {}", key);
                    }
                }
            }

            if (nbt.contains(KEY_MODULES, 10)) {
                CompoundTag modulesTag = nbt.getCompound(KEY_MODULES);
                for (String key : modulesTag.getAllKeys()) {
                    IRiderDataModule module = loadedModules.get(key);
                    if (module != null && modulesTag.contains(key, 10)) {
                        module.deserializeNBT(modulesTag.getCompound(key));
                    }
                }
            }

            recalculateActiveRider();
        } catch (Exception e) {
            KamenRiderInterwovenWorldsMod.LOGGER.error("Failed to deserialize RiderPlayerData", e);
            resetToDefault();
        }
    }

}