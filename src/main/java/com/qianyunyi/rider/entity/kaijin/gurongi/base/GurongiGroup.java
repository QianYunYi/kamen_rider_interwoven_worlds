package com.qianyunyi.rider.entity.kaijin.gurongi.base;

/**
 * 古朗基集团枚举
 * 对应七个集团，按等级从低到高排列：
 * 贝（Be）- 最低等级
 * 滋（Zu）
 * 魅（Me）
 * 葛（Go）
 * 拉（Ra）
 * 奴（Nu）- 特殊集团
 * 恩（N）- 最高等级
 */
public enum GurongiGroup {
    BE("贝"),
    ZU("滋"),
    ME("魅"),
    GO("葛"),
    RA("拉"),
    NU("奴"),
    N("恩");

    private final String chineseName;

    GurongiGroup(String chineseName) {
        this.chineseName = chineseName;
    }

    /**
     * 获取集团的中文名称
     */
    public String getChineseName() {
        return chineseName;
    }

    /**
     * 获取集团的英文缩写
     */
    public String getAbbreviation() {
        return this.name();
    }

    /**
     * 根据字符串匹配集团（不区分大小写）
     */
    public static GurongiGroup fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        for (GurongiGroup group : values()) {
            if (group.name().equals(upper) || group.getChineseName().equals(value.trim())) {
                return group;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return chineseName + "(" + name() + ")";
    }
}