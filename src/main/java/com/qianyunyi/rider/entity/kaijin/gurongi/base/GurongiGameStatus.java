package com.qianyunyi.rider.entity.kaijin.gurongi.base;

/**
 * 古朗基基基鲁游戏（Gegeru）状态机。
 * <p>
 * 正常流程：
 * NONE → REGISTERED → WAITING → ACTIVE → COMPLETED → PROMOTED
 * <p>
 * 异常流程：
 * <ul>
 *   <li>REGISTERED / WAITING 状态非法杀人 → DISQUALIFIED</li>
 *   <li>ACTIVE 超时未完成 → FAIL</li>
 *   <li>DISQUALIFIED 无法恢复</li>
 * </ul>
 * <p>
 * 状态说明：
 * <ul>
 *   <li><b>NONE</b> - 没有基基鲁资格</li>
 *   <li><b>REGISTERED</b> - 已被授予基基鲁资格，已注册但游戏未开始</li>
 *   <li><b>WAITING</b> - 等待轮到自己的游戏回合</li>
 *   <li><b>ACTIVE</b> - 正在进行基基鲁游戏</li>
 *   <li><b>COMPLETED</b> - 已完成游戏目标</li>
 *   <li><b>PROMOTED</b> - 已晋级下一阶段</li>
 *   <li><b>DISQUALIFIED</b> - 失去游戏权（因违规）</li>
 *   <li><b>FAIL</b> - 游戏失败（如超时）</li>
 * </ul>
 */
public enum GurongiGameStatus {

    /** 没有基基鲁资格 */
    NONE,

    /** 已被授予基基鲁资格，已注册但游戏未正式开始 */
    REGISTERED,

    /** 等待轮到自己的游戏回合 */
    WAITING,

    /** 正在进行基基鲁游戏 */
    ACTIVE,

    /** 已完成游戏目标 */
    COMPLETED,

    /** 已晋级下一阶段 */
    PROMOTED,

    /** 失去游戏权（因违规：提前杀人、抢杀等） */
    DISQUALIFIED,

    /** 逃脱导致失去游戏权（Zu-Gooma-Gu 专属，逃跑即放弃游戏规则） */
    ESCAPED,

    /** 游戏失败（如超时未完成） */
    FAIL
}