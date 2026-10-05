package com.qianyunyi.rider.util;

public class SnowStormManager {
    public static final SnowStormManager INSTANCE = new SnowStormManager();
    // 全局暴风雪开关（true=开启，false=关闭）
    private boolean globalStorm = false;

    private SnowStormManager() {}

    // 开启全局暴风雪
    public void startGlobalStorm() {
        globalStorm = true;
    }

    // 关闭全局暴风雪
    public void stopGlobalStorm() {
        globalStorm = false;
    }

    // 获取当前是否开启
    public boolean getGlobalStorm() {
        return globalStorm;
    }
}