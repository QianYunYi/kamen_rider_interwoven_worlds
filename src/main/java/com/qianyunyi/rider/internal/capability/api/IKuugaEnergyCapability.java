package com.qianyunyi.rider.internal.capability.api;

public interface IKuugaEnergyCapability {
    void tick(); // 每秒执行一次能量逻辑
    void activate();
    void deactivate();
    boolean isActive();
}