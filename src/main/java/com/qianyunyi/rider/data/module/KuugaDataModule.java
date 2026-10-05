package com.qianyunyi.rider.data.module;

import com.qianyunyi.rider.command.DataField;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.UUID;

/**
 * Kuuga骑士数据模块 - 1.20.1重构版
 * 适配新的BaseDataModule基类
 */
public class KuugaDataModule extends BaseDataModule {

    static {
        RiderDataModuleRegistry.register(KuugaDataModule::new);
    }

    // ========== Kuuga状态数据 ==========
    //是否是试作型亚古鲁
    private boolean isArclePrototype = false;
    // 是否解锁全能形态
    private boolean isMightyKuuga = false;
    // 是否解锁青龙形态
    private boolean isDragonForm = false;
    // 是否解锁天马形态
    private boolean isPegasusForm = false;
    // 是否解锁泰坦形态
    private boolean isTitanForm = false;
    //是否解锁金之力
    private boolean hasGoldPower = false;
    // 是否解锁惊异全能形态
    private boolean isAmazinglyMightyForm = false;
    //是否解锁试作型究极形态
    private boolean isPrototypeUltimateForm = false;
    // 是否解锁究极形态
    private boolean isUltimateForm = false;
    // 是否解锁升华究极形态
    private boolean isRisingUltimateForm = false;
    // 是否解锁超升华究极形态
    private boolean isSuperRisingUltimateForm = false;
    //是否解锁空翻
    private boolean isSomersault = false;
    // 是否使用地之石
    private boolean hasStoneOfEarth = false;
    // 是否处于超自然感官
    private boolean isSupernaturalSensesActive = false;
    // 超自然感官任务ID
    private UUID supernaturalSensesTaskId = null;
    // 是否处于超自然之水
    private boolean isSupernaturalWaterActive = false;
    // 超自然之水任务ID
    private UUID supernaturalWaterTaskId = null;
    private boolean hasSummonedDaguba = false;
    private boolean hasSummonedDaguba2 = false;
    // 是否解锁哥莱姆
    private boolean isGouramUnlocked = false;
    // 哥莱姆最后死亡时间（用于死亡冷却1分钟）
    private long gouramDeathTick = 0;
    // 是否已召唤哥莱姆（召唤标记）
    private boolean isGouramSummoned = false;

    // ========== Kuuga能量数据 ==========
    private float amadamFusion = 0.0f;
    private float amadamFusionMax = 100.0f;
    private float amadamEnergy = 0.0f;
    private float amadamEnergyMax = 1000.0f;
    private float holySpring = 100.0f;
    private float holySpringMax = 100.0f;
    private float superEnergy = 0.0f;
    private float superEnergyMax = 90.0f;

    // 过饱和状态
    private float superChargeSaturation = 0.0f;
    private float superChargeSaturationMax = 2000.0f;
    private boolean isSuperCharging = false;
    private int superChargeTick = 0;

    // 骑士踢使用次数
    private float riderKickCount = 0.0f;

    public KuugaDataModule() {
        super();
    }

    @Override
    public String getModuleName() {
        return "kuuga";
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putBoolean("IsArclePrototype", isArclePrototype);
        nbt.putBoolean("IsMightyKuuga", isMightyKuuga);
        nbt.putBoolean("IsDragonForm", isDragonForm);
        nbt.putBoolean("IsPegasusForm", isPegasusForm);
        nbt.putBoolean("IsTitanForm", isTitanForm);
        nbt.putBoolean("IsAmazinglyMightyForm", isAmazinglyMightyForm);
        nbt.putBoolean("IsPrototypeUltimateForm", isPrototypeUltimateForm);
        nbt.putBoolean("IsUltimateForm", isUltimateForm);
        nbt.putBoolean("IsRisingUltimateForm", isRisingUltimateForm);
        nbt.putBoolean("IsSuperRisingUltimateForm", isSuperRisingUltimateForm);
        nbt.putBoolean("HasGoldPower", hasGoldPower);
        nbt.putBoolean("IsSomersault", isSomersault);
        nbt.putBoolean("HasStoneOfEarth", hasStoneOfEarth);
        nbt.putBoolean("IsSupernaturalSensesActive", isSupernaturalSensesActive);
        if (supernaturalSensesTaskId != null) {
            nbt.putUUID("SupernaturalSensesTaskId", supernaturalSensesTaskId);
        }
        nbt.putBoolean("IsSupernaturalWaterActive", isSupernaturalWaterActive);
        if (supernaturalWaterTaskId != null) {
            nbt.putUUID("SupernaturalWaterTaskId", supernaturalWaterTaskId);
        }
        nbt.putBoolean("HasSummonedDaguba", hasSummonedDaguba);
        nbt.putBoolean("HasSummonedDaguba2", hasSummonedDaguba2);
        nbt.putBoolean("IsGouramUnlocked", isGouramUnlocked);
        nbt.putLong("GouramDeathTick", gouramDeathTick);
        nbt.putBoolean("IsGouramSummoned", isGouramSummoned);

        nbt.putFloat("AmadamFusion", amadamFusion);
        nbt.putFloat("AmadamFusionMax", amadamFusionMax);
        nbt.putFloat("AmadamEnergy", amadamEnergy);
        nbt.putFloat("AmadamEnergyMax", amadamEnergyMax);
        nbt.putFloat("HolySpring", holySpring);
        nbt.putFloat("HolySpringMax", holySpringMax);
        nbt.putFloat("SuperEnergy", superEnergy);
        nbt.putFloat("SuperEnergyMax", superEnergyMax);

        nbt.putFloat("SuperChargeSaturation", superChargeSaturation);
        nbt.putFloat("SuperChargeSaturationMax", superChargeSaturationMax);
        nbt.putBoolean("IsSuperCharging", isSuperCharging);
        nbt.putInt("SuperChargeTick", superChargeTick);

        nbt.putFloat("RiderKickCount", riderKickCount);

        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("IsArclePrototype")) isArclePrototype = nbt.getBoolean("IsArclePrototype");
        if (nbt.contains("IsMightyKuuga")) isMightyKuuga = nbt.getBoolean("IsMightyKuuga");
        if (nbt.contains("IsDragonForm")) isDragonForm = nbt.getBoolean("IsDragonForm");
        if (nbt.contains("IsPegasusForm")) isPegasusForm = nbt.getBoolean("IsPegasusForm");
        if (nbt.contains("IsTitanForm")) isTitanForm = nbt.getBoolean("IsTitanForm");
        if (nbt.contains("IsAmazinglyMightyForm")) isAmazinglyMightyForm = nbt.getBoolean("IsAmazinglyMightyForm");
        if (nbt.contains("IsPrototypeUltimateForm")) isPrototypeUltimateForm = nbt.getBoolean("IsPrototypeUltimateForm");
        if (nbt.contains("IsUltimateForm")) isUltimateForm = nbt.getBoolean("IsUltimateForm");
        if (nbt.contains("IsRisingUltimateForm")) isRisingUltimateForm = nbt.getBoolean("IsRisingUltimateForm");
        if (nbt.contains("IsSuperRisingUltimateForm")) isSuperRisingUltimateForm = nbt.getBoolean("IsSuperRisingUltimateForm");
        if (nbt.contains("HasGoldPower")) hasGoldPower = nbt.getBoolean("HasGoldPower");
        if (nbt.contains("IsSomersault")) isSomersault = nbt.getBoolean("IsSomersault");
        if (nbt.contains("HasStoneOfEarth")) hasStoneOfEarth = nbt.getBoolean("HasStoneOfEarth");
        if (nbt.contains("IsSupernaturalSensesActive")) isSupernaturalSensesActive = nbt.getBoolean("IsSupernaturalSensesActive");
        if (nbt.contains("SupernaturalSensesTaskId")) supernaturalSensesTaskId = nbt.getUUID("SupernaturalSensesTaskId");
        if (nbt.contains("IsSupernaturalWaterActive")) isSupernaturalWaterActive = nbt.getBoolean("IsSupernaturalWaterActive");
        if (nbt.contains("SupernaturalWaterTaskId")) supernaturalWaterTaskId = nbt.getUUID("SupernaturalWaterTaskId");

        if (nbt.contains("HasSummonedDaguba")) hasSummonedDaguba = nbt.getBoolean("HasSummonedDaguba");
        if (nbt.contains("HasSummonedDaguba2")) hasSummonedDaguba2 = nbt.getBoolean("HasSummonedDaguba2");
        if (nbt.contains("IsGouramUnlocked")) isGouramUnlocked = nbt.getBoolean("IsGouramUnlocked");
        if (nbt.contains("GouramDeathTick")) gouramDeathTick = nbt.getLong("GouramDeathTick");
        if (nbt.contains("IsGouramSummoned")) isGouramSummoned = nbt.getBoolean("IsGouramSummoned");

        if (nbt.contains("AmadamFusion")) amadamFusion = nbt.getFloat("AmadamFusion");
        if (nbt.contains("AmadamFusionMax")) amadamFusionMax = nbt.getFloat("AmadamFusionMax");
        if (nbt.contains("AmadamEnergy")) amadamEnergy = nbt.getFloat("AmadamEnergy");
        if (nbt.contains("AmadamEnergyMax")) amadamEnergyMax = nbt.getFloat("AmadamEnergyMax");
        if (nbt.contains("HolySpring")) holySpring = nbt.getFloat("HolySpring");
        if (nbt.contains("HolySpringMax")) holySpringMax = nbt.getFloat("HolySpringMax");
        if (nbt.contains("SuperEnergy")) superEnergy = nbt.getFloat("SuperEnergy");
        if (nbt.contains("SuperEnergyMax")) superEnergyMax = nbt.getFloat("SuperEnergyMax");

        if (nbt.contains("SuperChargeSaturation")) superChargeSaturation = nbt.getFloat("SuperChargeSaturation");
        if (nbt.contains("SuperChargeSaturationMax")) superChargeSaturationMax = nbt.getFloat("SuperChargeSaturationMax");
        if (nbt.contains("IsSuperCharging")) isSuperCharging = nbt.getBoolean("IsSuperCharging");
        if (nbt.contains("SuperChargeTick")) superChargeTick = nbt.getInt("SuperChargeTick");

        if (nbt.contains("RiderKickCount")) riderKickCount = nbt.getFloat("RiderKickCount");
    }

    @Override
    public void resetToDefault() {
        isArclePrototype = false;
        isMightyKuuga = false;
        isDragonForm = false;
        isPegasusForm = false;
        isTitanForm = false;
        isAmazinglyMightyForm = false;
        isPrototypeUltimateForm = false;
        isUltimateForm = false;
        isRisingUltimateForm = false;
        isSuperRisingUltimateForm = false;
        hasGoldPower = false;
        isSomersault = false;
        hasStoneOfEarth = false;
        isSupernaturalSensesActive = false;
        supernaturalSensesTaskId = null;
        isSupernaturalWaterActive = false;
        supernaturalWaterTaskId = null;

        hasSummonedDaguba = false;
        hasSummonedDaguba2 = false;
        isGouramUnlocked = false;
        gouramDeathTick = 0;
        isGouramSummoned = false;

        amadamFusion = 0.0f;
        amadamFusionMax = 100.0f;
        amadamEnergy = 0.0f;
        amadamEnergyMax = 1000.0f;
        holySpring = 100.0f;
        holySpringMax = 100.0f;
        superEnergy = 0.0f;
        superEnergyMax = 90.0f;

        superChargeSaturation = 0.0f;
        superChargeSaturationMax = 2000.0f;
        isSuperCharging = false;
        superChargeTick = 0;

        riderKickCount = 0.0f;
    }

    // ========== Getters & Setters ==========

    public boolean isArclePrototype() { return isArclePrototype; }
    public void setArclePrototype(boolean value) { this.isArclePrototype = value; markDirty(); }

    public boolean isMightyKuuga() { return isMightyKuuga; }
    public void setMightyKuuga(boolean value) { this.isMightyKuuga = value; markDirty(); }

    public boolean isDragonForm() { return isDragonForm; }
    public void setDragonForm(boolean value) { this.isDragonForm = value; markDirty(); }

    public boolean isPegasusForm() { return isPegasusForm; }
    public void setPegasusForm(boolean value) { this.isPegasusForm = value; markDirty(); }

    public boolean isTitanForm() { return isTitanForm; }
    public void setTitanForm(boolean value) { this.isTitanForm = value; markDirty(); }

    public boolean isAmazinglyMightyForm() { return isAmazinglyMightyForm; }
    public void setAmazinglyMightyForm(boolean value) { this.isAmazinglyMightyForm = value; markDirty(); }

    public boolean isPrototypeUltimateForm() { return isPrototypeUltimateForm; }
    public void setPrototypeUltimateForm(boolean value) { this.isPrototypeUltimateForm = value; markDirty(); }

    public boolean isUltimateForm() { return isUltimateForm; }
    public void setUltimateForm(boolean value) { this.isUltimateForm = value; markDirty(); }

    public boolean isRisingUltimateForm() { return isRisingUltimateForm; }
    public void setRisingUltimateForm(boolean value) { this.isRisingUltimateForm = value; markDirty(); }

    public boolean isSuperRisingUltimateForm() { return isSuperRisingUltimateForm; }
    public void setSuperRisingUltimateForm(boolean value) { this.isSuperRisingUltimateForm = value; markDirty(); }

    public boolean isHasGoldPower() { return hasGoldPower; }
    public void setHasGoldPower(boolean value) { this.hasGoldPower = value; markDirty(); }

    public boolean isSomersault() { return isSomersault; }
    public void setSomersault(boolean value) { this.isSomersault = value; markDirty(); }

    public boolean hasStoneOfEarth() { return hasStoneOfEarth; }
    public void setHasStoneOfEarth(boolean value) { this.hasStoneOfEarth = value; markDirty(); }

    public boolean isSupernaturalSensesActive() { return isSupernaturalSensesActive; }
    public void setSupernaturalSensesActive(boolean value) { this.isSupernaturalSensesActive = value; markDirty(); }

    public UUID getSupernaturalSensesTaskId() { return supernaturalSensesTaskId; }
    public void setSupernaturalSensesTaskId(UUID value) { this.supernaturalSensesTaskId = value; markDirty(); }

    public boolean isSupernaturalWaterActive() { return isSupernaturalWaterActive; }
    public void setSupernaturalWaterActive(boolean value) { this.isSupernaturalWaterActive = value; markDirty(); }

    public UUID getSupernaturalWaterTaskId() { return supernaturalWaterTaskId; }
    public void setSupernaturalWaterTaskId(UUID value) { this.supernaturalWaterTaskId = value; markDirty(); }

    public boolean hasSummonedDaguba() { return hasSummonedDaguba; }
    public void setHasSummonedDaguba(boolean value) { this.hasSummonedDaguba = value; markDirty(); }

    public boolean hasSummonedDaguba2() { return hasSummonedDaguba2; }
    public void setHasSummonedDaguba2(boolean value) { this.hasSummonedDaguba2 = value; markDirty(); }

    public boolean isGouramUnlocked() { return isGouramUnlocked; }
    public void setGouramUnlocked(boolean value) { this.isGouramUnlocked = value; markDirty(); }

    public long getGouramDeathTick() { return gouramDeathTick; }
    public void setGouramDeathTick(long value) { this.gouramDeathTick = value; markDirty(); }

    public boolean isGouramSummoned() { return isGouramSummoned; }
    public void setGouramSummoned(boolean value) { this.isGouramSummoned = value; markDirty(); }

    public float getAmadamFusion() { return amadamFusion; }
    public void setAmadamFusion(float value) {
        this.amadamFusion = Math.min(Math.max(value, 0), amadamFusionMax);
        markDirty();
    }

    public float getAmadamFusionMax() { return amadamFusionMax; }
    public void setAmadamFusionMax(float value) { this.amadamFusionMax = Math.max(value, 0); markDirty(); }

    public float getAmadamEnergy() { return amadamEnergy; }
    public void setAmadamEnergy(float value) {
        this.amadamEnergy = Math.min(Math.max(value, 0), amadamEnergyMax);
        markDirty();
    }

    public float getAmadamEnergyMax() { return amadamEnergyMax; }
    public void setAmadamEnergyMax(float value) { this.amadamEnergyMax = Math.max(value, 0); markDirty(); }

    public float getHolySpring() { return holySpring; }
    public float getHolySpringMax() { return holySpringMax; }
    public void setHolySpring(float value) {
        this.holySpring = Math.min(Math.max(value, 0), holySpringMax);
        markDirty();
    }
    public void setHolySpringMax(float value) {
        this.holySpringMax = Math.max(value, 0);
        if (holySpring > holySpringMax) holySpring = holySpringMax;
        markDirty();
    }

    public float getSuperEnergy() { return superEnergy; }
    public void setSuperEnergy(float value) {
        this.superEnergy = Math.min(Math.max(value, 0), superEnergyMax);
        markDirty();
    }
    public float getSuperEnergyMax() { return superEnergyMax; }
    public void setSuperEnergyMax(float value) { this.superEnergyMax = Math.max(value, 0); markDirty(); }

    public float getRiderKickCount() { return riderKickCount; }
    public void setRiderKickCount(float value) {
        if (value >= 10) setSomersault(true);
        this.riderKickCount = Math.min(Math.max(value, 0), 10);
        markDirty();
    }

    // 过饱和
    public float getSuperChargeSaturation() { return superChargeSaturation; }
    public void setSuperChargeSaturation(float value) {
        float maxLimit = getSuperChargeSaturationMax();
        this.superChargeSaturation = Math.min(Math.max(value, 0), maxLimit);
        markDirty();
    }
    public boolean isSuperCharging() { return isSuperCharging; }
    public void setSuperCharging(boolean value) { this.isSuperCharging = value; markDirty(); }
    public int getSuperChargeTick() { return superChargeTick; }
    public void setSuperChargeTick(int value) { this.superChargeTick = value; markDirty(); }
    public float getSuperChargeSaturationMax() { return getSuperEnergyMax() * 0.1f; }

    public void addSuperChargeSaturation(float value) {
        float maxLimit = getSuperChargeSaturationMax();
        float newValue = this.superChargeSaturation + value;
        setSuperChargeSaturation(Math.min(newValue, maxLimit));
    }

    // ========== 便捷业务方法 ==========
    public void addSuperEnergy(float energy) { setSuperEnergy(superEnergy + energy); }
    public void removeSuperEnergy(float energy) { setSuperEnergy(superEnergy - energy); }
    public void addAmadamEnergy(float energy) { setAmadamEnergy(amadamEnergy + energy); }
    public void removeAmadamEnergy(float energy) { setAmadamEnergy(amadamEnergy - energy); }
    public void addAmadamFusion(float fusion) { setAmadamFusion(amadamFusion + fusion); }
    public void addHolySpring(float amount) { setHolySpring(holySpring + amount); }
    public void removeHolySpring(float amount) { setHolySpring(holySpring - amount); }
    public void setHolySpringPercentage(float percentage) { setHolySpring(holySpringMax * Math.min(Math.max(percentage, 0), 1)); }
    public float getHolySpringPercentage() { return holySpringMax > 0 ? holySpring / holySpringMax : 0; }
    public boolean hasEnoughHolySpring(float amount) { return holySpring >= amount; }
    public void addRiderKickCount(float amount) { setRiderKickCount(riderKickCount + amount); }

    // ========== 调试字段 ==========
    @Override
    public List<DataField> getDebugFields() {
        return List.of(
                new DataField("fusion", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getAmadamFusion() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setAmadamFusion(v.floatValue()); },
                        0, 100),
                new DataField("amadam_energy", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getAmadamEnergy() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setAmadamEnergy(v.floatValue()); },
                        0, null),
                new DataField("amadam_max", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getAmadamEnergyMax() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setAmadamEnergyMax(v.floatValue()); },
                        0, null),
                new DataField("holy_spring", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getHolySpring() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setHolySpring(v.floatValue()); },
                        0, null),
                new DataField("super_energy", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getSuperEnergy() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setSuperEnergy(v.floatValue()); },
                        0, null),
                new DataField("super_energy_max", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getSuperEnergyMax() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setSuperEnergyMax(v.floatValue()); },
                        0, null),
                new DataField("rider_kick_count", DataField.DataType.FLOAT,
                        data -> { KuugaDataModule k = data.getKuugaData(); return k != null ? k.getRiderKickCount() : 0; },
                        (data, v) -> { KuugaDataModule k = data.getKuugaData(); if (k != null) k.setRiderKickCount(v.floatValue()); },
                        0, 10)
        );
    }
}