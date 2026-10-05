package com.qianyunyi.rider.data.module;

import com.qianyunyi.rider.data.RiderPlayerData;

/**
 * 数据模块基类 - 1.20.1 Capability 版本
 * 提供脏标记管理和父数据引用
 */
public abstract class BaseDataModule implements IRiderDataModule {

    protected transient RiderPlayerData parentData;
    protected transient boolean dirty = false;

    public BaseDataModule() {
        // 无参构造，用于反序列化
    }

    public void setParentData(RiderPlayerData parentData) {
        this.parentData = parentData;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }
}