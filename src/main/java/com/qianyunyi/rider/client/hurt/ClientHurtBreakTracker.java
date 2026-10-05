package com.qianyunyi.rider.client.hurt;

import net.minecraft.world.entity.player.Player;

/**
 * 纯客户端"破防跟踪器"。
 * <p>
 * 受击镜头摇晃与红色受击遮罩由客户端的 hurtTime 驱动，而生命值也在客户端同一份数据上同步。
 * 因此直接在这里用"受伤瞬间前后的生命值"判断是否破防（实际扣减 > 最大生命值 5%），
 * 与受击画面天然同帧，可彻底避免依赖网络同步 lastHurtFraction 造成的竞态与一次受伤的延迟。
 */
public final class ClientHurtBreakTracker {

    /** 破防阈值：实际扣减的生命值超过最大生命值的这一比例即视为破防 */
    public static final float BREAK_THRESHOLD = 0.05f;

    private static float lastHealth = -1f;
    private static int lastHurtTime = -1;
    private static boolean currentBroken = false;

    private ClientHurtBreakTracker() {
    }

    /**
     * 判断当前（最近一次）受击是否破防。
     * 当检测到 hurtTime 增大（新一轮受击刚触发）时，用上一帧生命值与当前生命值的差值计算。
     *
     * @param player 客户端本地玩家
     * @return true 表示该次受击实际扣减生命值 > 最大生命值 5%（破防，应放行）；false 表示格挡成功
     */
    public static boolean isBrokenHurt(Player player) {
        if (player == null) {
            return false;
        }
        int hurtTime = player.hurtTime;
        float health = player.getHealth();

        // 初始状态：仅记录，不做破防判定
        if (lastHealth < 0f || lastHurtTime < 0) {
            lastHealth = health;
            lastHurtTime = hurtTime;
            return false;
        }

        // 检测到新一轮受击：hurtTime 重新升高（例如 0->10，或被再次打到 10）
        if (hurtTime > lastHurtTime) {
            float loss = lastHealth - health;
            currentBroken = loss > player.getMaxHealth() * BREAK_THRESHOLD;
        }

        lastHurtTime = hurtTime;
        lastHealth = health;
        return currentBroken;
    }

    /** 复位（例如玩家退出/重置时调用） */
    public static void reset() {
        lastHealth = -1f;
        lastHurtTime = -1;
        currentBroken = false;
    }
}