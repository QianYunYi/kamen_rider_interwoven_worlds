package com.qianyunyi.rider.entity.kaijin.gurongi.base;

/**
 * 目标类型枚举
 * <p>
 * 古朗基狩猎目标分为两类：
 * <ul>
 *   <li>{@link #LINTO} — 林多（玩家、村民）</li>
 *   <li>{@link #ILLAGER} — 灾厄村民（掠夺者、卫道士、唤魔者等）</li>
 * </ul>
 * 可通过 {@link GurongiHuntingLaw#registerLintoEntityType} 和
 * {@link GurongiHuntingLaw#registerIllagerEntityType} 扩展。
 */
public enum TargetType {

    /**
     * 林多（Linto）—— 玩家和村民
     */
    LINTO,

    /**
     * 灾厄村民（Illager）
     */
    ILLAGER
}