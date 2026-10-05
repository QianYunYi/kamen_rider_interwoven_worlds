package com.qianyunyi.rider.entity.kaijin.gurongi.base;

/**
 * 基基鲁游戏（Gegeru）的阶段枚举。
 * <p>
 * 古朗基的游戏分为以下几个阶段，对应不同集团的参赛资格：
 * <ul>
 *   <li><b>AUDITION（海选）</b> - 滋（Zu）集团的游戏阶段</li>
 *   <li><b>SELECTION（决选）</b> - 魅（Me）集团的游戏阶段</li>
 *   <li><b>SEMI_FINAL（半决赛）</b> - 葛（Go）集团的游戏阶段，
 *       也被称为黑暗游戏（gerizagibasu_gegeru）</li>
 *   <li><b>WHITE_DARK_DUEL（准决赛）</b> - 半准决赛成功后，
 *       与0号（恩集团）进行一对一的战斗"白暗的游戏"。
 *       将0号击倒后可继承其变身腰带。</li>
 *   <li><b>FINAL（决赛）</b> - 最终阶段的冠军挑战</li>
 * </ul>
 */
public enum GurongiGameStage {

    /** 无游戏阶段 */
    NONE,

    /** 海选（Audition）- 滋集团的游戏 */
    AUDITION,

    /** 决选（Selection）- 魅集团的游戏 */
    SELECTION,

    /** 半决赛/黑暗游戏（gerizagibasu_gegeru）- 葛集团的游戏 */
    SEMI_FINAL,

    /** 准决赛/白暗的游戏（white_dark_duel）- 与0号一对一战斗 */
    WHITE_DARK_DUEL,

    /** 决赛 - 冠军挑战 */
    FINAL
}