package com.qianyunyi.rider.entity.kaijin.gurongi.base;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 基基鲁游戏目标（Gegeru Objective）。
 * <p>
 * 目标击杀数按古朗基集团分级，集团越高目标数越多：
 * <ul>
 *   <li>贝（BE）→ 2 个人类</li>
 *   <li>滋（ZU）→ 5 个人类</li>
 *   <li>魅（ME）→ 8 个人类</li>
 *   <li>葛（GO）→ 10 个人类</li>
 *   <li>拉（RA）→ 15 个人类</li>
 *   <li>奴（NU）→ 20 个人类</li>
 *   <li>恩（N）→ 30 个人类</li>
 * </ul>
 * 同一集团内目标固定，不浮动。
 * <p>
 * 有效目标：林多（玩家、村民）和灾厄村民（掠夺者、卫道士、女巫等）。
 */
public enum GurongiGameObjective {

    KILL_2(2, "基基鲁游戏：杀死 %d 个人类"),
    KILL_3(3, "基基鲁游戏：杀死 %d 个人类"),
    KILL_5(5, "基基鲁游戏：杀死 %d 个人类"),
    KILL_8(8, "基基鲁游戏：杀死 %d 个人类"),
    KILL_10(10, "基基鲁游戏：杀死 %d 个人类"),
    KILL_15(15, "基基鲁游戏：杀死 %d 个人类"),
    KILL_20(20, "基基鲁游戏：杀死 %d 个人类"),
    KILL_30(30, "基基鲁游戏：杀死 %d 个人类");

    private final int targetCount;
    private final String description;

    GurongiGameObjective(int targetCount, String description) {
        this.targetCount = targetCount;
        this.description = description;
    }

    /**
     * @return 完成目标需要击杀的数量
     */
    public int getTargetCount() {
        return targetCount;
    }

    /**
     * 判断给定的实体实例是否匹配此目标。
     * <p>
     * 林多（玩家、村民）和灾厄村民均算作有效目标。
     */
    public boolean matchesTarget(LivingEntity victim) {
        if (victim == null) return false;
        // 林多：玩家或村民
        if (victim instanceof Player) return true;
        if (victim instanceof Villager) return true;
        // 灾厄村民：AbstractIllager 子类（掠夺者、卫道士等）及注册表扩展（含女巫）
        if (victim instanceof AbstractIllager) return true;
        // 通过 GurongiHuntingLaw 综合判断（配置文件扩展）
        return GurongiHuntingLaw.isHuman(victim);
    }

    /**
     * 获取格式化后的描述文本
     */
    public String getDescription() {
        return String.format(description, targetCount);
    }

    /**
     * 根据古朗基集团返回对应的游戏目标。
     * <p>
     * 集团越高，目标击杀数越多。同一集团内目标固定不变。
     *
     * <table border="1">
     *   <tr><th>集团</th><th>目标击杀数</th></tr>
     *   <tr><td>贝（BE）</td><td>2</td></tr>
     *   <tr><td>滋（ZU）</td><td>5</td></tr>
     *   <tr><td>魅（ME）</td><td>8</td></tr>
     *   <tr><td>葛（GO）</td><td>10</td></tr>
     *   <tr><td>拉（RA）</td><td>15</td></tr>
     *   <tr><td>奴（NU）</td><td>20</td></tr>
     *   <tr><td>恩（N）</td><td>无目标（可以随便杀人）</td></tr>
     * </table>
     * <p>
     * <b>恩（N）集团例外</b>：恩集团成员不受基基鲁游戏目标限制，
     * 可以随意杀戮，该方法返回 {@code null}。
     *
     * @param group 古朗基集团
     * @return 对应的游戏目标，恩集团返回 {@code null}
     */
    @Nullable
    public static GurongiGameObjective forGroup(GurongiGroup group) {
        if (group == null) return KILL_5;
        switch (group) {
            case BE: return KILL_2;
            case ZU: return KILL_5;
            case ME: return KILL_8;
            case GO: return KILL_10;
            case RA: return KILL_15;
            case NU: return KILL_20;
            case N:  return null; // 恩集团无游戏目标，不受限制
            default: return KILL_5;
        }
    }

    /**
     * 从字符串反序列化（用于 NBT 存档读取）。
     */
    public static GurongiGameObjective fromString(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}