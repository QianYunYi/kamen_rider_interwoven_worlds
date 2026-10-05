package com.qianyunyi.rider.data.module;

import com.qianyunyi.rider.command.DataField;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class ShowaData extends BaseDataModule {

    static {
        RiderDataModuleRegistry.register(ShowaData::new);
    }

    //一号标记
    public boolean isMaskedRider1 = false;
    //樱岛一号标记
    public boolean isMaskedRider1SakurajimaVersion = false;
    //新一号标记
    public boolean isMaskedRiderNew1 = false;
    // 二号标记
    public boolean isMaskedRider2 = false;
    //新二号标记
    public boolean isMaskedRiderNew2 = false;
    // 三号标记
    public boolean isMaskedRider3 = false;
    //V3标记
    public boolean isMaskedRiderV3 = false;
    // 风力值
    public float windPower = 0;
    //风力值上限
    public float maxWindPower = 100;

    public ShowaData() {
        super();
    }

    @Override
    public String getModuleName() {
        return "ShowaData";
    }

    @Override
    public void resetToDefault() {
        isMaskedRider1=false;
        isMaskedRider1SakurajimaVersion=false;
        isMaskedRiderNew1=false;
        isMaskedRider2=false;
        isMaskedRiderNew2=false;
        isMaskedRiderV3=false;
        windPower=0;
        maxWindPower=100;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isMaskedRider1", isMaskedRider1);
        tag.putBoolean("isMaskedRider1SakurajimaVersion", isMaskedRider1SakurajimaVersion);
        tag.putBoolean("isMaskedRiderNew1", isMaskedRiderNew1);
        tag.putBoolean("isMaskedRider2", isMaskedRider2);
        tag.putBoolean("isMaskedRiderNew2", isMaskedRiderNew2);
        tag.putBoolean("isMaskedRider3", isMaskedRider3);
        tag.putBoolean("isMaskedRiderV3", isMaskedRiderV3);
        tag.putFloat("windPower", windPower);
        tag.putFloat("maxWindPower", maxWindPower);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        isMaskedRider1 = nbt.getBoolean("isMaskedRider1");
        isMaskedRider1SakurajimaVersion = nbt.getBoolean("isMaskedRider1SakurajimaVersion");
        isMaskedRiderNew1 = nbt.getBoolean("isMaskedRiderNew1");
        isMaskedRider2 = nbt.getBoolean("isMaskedRider2");
        isMaskedRiderNew2 = nbt.getBoolean("isMaskedRiderNew2");
        isMaskedRider3 = nbt.getBoolean("isMaskedRider3");
        isMaskedRiderV3 = nbt.getBoolean("isMaskedRiderV3");
        windPower = nbt.getFloat("windPower");
        maxWindPower = nbt.getFloat("maxWindPower");
    }

    // ========== Getter/Setter方法 ==========
    //形态标记
    public boolean isMaskedRider1() {
        return isMaskedRider1;
    }

    public void setMaskedRider1(boolean value) {
        this.isMaskedRider1 = value;
    }
    //樱岛一号标记
    public boolean isMaskedRider1SakurajimaVersion() {
        return isMaskedRider1SakurajimaVersion;
    }
    //新一号标记
    public boolean isMaskedRiderNew1() {
        return isMaskedRiderNew1;
    }
    // 二号标记
    public boolean isMaskedRider2() {
        return isMaskedRider2;
    }
    //新二号标记
    public boolean isMaskedRiderNew2() {
        return isMaskedRiderNew2;
    }
    // 三号标记
    public boolean isMaskedRider3() {
        return isMaskedRider3;
    }
    //V3标记
    public boolean isMaskedRiderV3() {
        return isMaskedRiderV3;
    }

    //风力值
    public float getWindPower() {
        return windPower;
    }

    //樱岛一号
    public void setMaskedRider1SakurajimaVersion(boolean isMaskedRider1SakurajimaVersion) {
        this.isMaskedRider1SakurajimaVersion = isMaskedRider1SakurajimaVersion;
    }

    //新一号
    public void setMaskedRiderNew1(boolean isMaskedRiderNew1) {
        this.isMaskedRiderNew1 = isMaskedRiderNew1;
    }

    // 二号
    public void setMaskedRider2(boolean isMaskedRider2) {
        this.isMaskedRider2 = isMaskedRider2;
    }

    //新二号
    public void setMaskedRiderNew2(boolean isMaskedRiderNew2) {
        this.isMaskedRiderNew2 = isMaskedRiderNew2;
    }

    // 三号
    public void setMaskedRider3(boolean isMaskedRider3) {
        this.isMaskedRider3 = isMaskedRider3;
    }

    // V3
    public void setMaskedRiderV3(boolean isMaskedRiderV3) {
        this.isMaskedRiderV3 = isMaskedRiderV3;
    }

    // SETTER风力值
    public void setWindPower(float windPower) {
        this.windPower = windPower;
    }

    public float getMaxWindPower() {
        return maxWindPower;
    }

    public void setMaxWindPower(float value) {
        this.maxWindPower = value;
    }

    // ========== 调试字段 ==========
    @Override
    public List<DataField> getDebugFields() {
        return List.of(
                new DataField("wind_power", DataField.DataType.FLOAT,
                        data -> { ShowaData s = data.getShowaData(); return s != null ? s.getWindPower() : 0; },
                        (data, v) -> { ShowaData s = data.getShowaData(); if (s != null) s.setWindPower(v.floatValue()); },
                        0, null),
                new DataField("max_wind_power", DataField.DataType.FLOAT,
                        data -> { ShowaData s = data.getShowaData(); return s != null ? s.getMaxWindPower() : 0; },
                        (data, v) -> { ShowaData s = data.getShowaData(); if (s != null) s.setMaxWindPower(v.floatValue()); },
                        0, null),
                new DataField("masked_rider_1", DataField.DataType.BOOL,
                        data -> { ShowaData s = data.getShowaData(); return s != null && s.isMaskedRider1() ? 1 : 0; },
                        (data, v) -> { ShowaData s = data.getShowaData(); if (s != null) s.setMaskedRider1(v.intValue() != 0); }),
                new DataField("masked_rider_v3", DataField.DataType.BOOL,
                        data -> { ShowaData s = data.getShowaData(); return s != null && s.isMaskedRiderV3() ? 1 : 0; },
                        (data, v) -> { ShowaData s = data.getShowaData(); if (s != null) s.setMaskedRiderV3(v.intValue() != 0); })
        );
    }
}