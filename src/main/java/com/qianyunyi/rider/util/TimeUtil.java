package com.qianyunyi.rider.util;

import net.minecraft.server.level.ServerLevel;

public class TimeUtil {

    // 我的世界一天总刻数 = 24000
    private static final int DAY_TICKS = 24000;
    // 夜晚开始时间（默认 13000 左右开始天黑，18000 完全黑夜）
    private static final int NIGHT_START = 13000;
    private static final int MID_NIGHT = 18000;

    /**
     * 平滑过渡到【当天】的夜晚
     * 不会跳天数，只会在当前天内把时间拉到夜晚
     */
    public static void setToNightSmooth(ServerLevel level) {
        long currentTime = level.getDayTime();
        long currentTimeInDay = currentTime % DAY_TICKS;

        // 如果已经是晚上，不处理
        if (currentTimeInDay >= NIGHT_START) {
            return;
        }

        // 计算：当前天 + 夜晚时间（18000）
        long targetTime = (currentTime - currentTimeInDay) + MID_NIGHT;
        level.setDayTime(targetTime);
    }

    /**
     * 强制立刻黑夜（保留原版逻辑，备用）
     */
    public static void setToNightInstant(ServerLevel level) {
        level.setDayTime(MID_NIGHT);
    }
}