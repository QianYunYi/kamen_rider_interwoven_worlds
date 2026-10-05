package com.qianyunyi.rider.entity.kaijin.shocker.data;

public class ShockerRank {
    public enum Rank {
        COMBATANT(0, "战斗员"),
        SQUAD_LEADER(1, "战斗员小队长"),
        KAIJIN(2, "怪人"),
        SCIENTIST(3, "科学家"),
        CADRE(4, "干部"),
        LEADER(5, "首领"),
        SHADOW_LEADER(6, "幕后首领");

        private final int id;
        private final String displayName;

        Rank(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public static Rank fromId(int id) {
            for (Rank r : values()) if (r.id == id) return r;
            return COMBATANT;
        }

        public int getId() { return id; }
        public String getDisplayName() { return displayName; }
    }
}