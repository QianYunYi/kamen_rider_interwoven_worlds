package com.qianyunyi.rider.entity.kaijin.shocker.data;



import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;

import java.util.*;

public class ShockerOrg {
    public static final int ORG_SHOCKER = 0;
    public static final int ORG_GEL_SHOCKER = 1;
    public static final int ORG_DESTRO = 2;
    public static final int ORG_GOVERNMENT_OF_DARK_NESS = 3;
    public static final int ORG_GARANDER = 4;
    public static final int ORG_DELZER = 5;
    public static final int ORG_DAI_SHOCKER = 6;

    private static final Map<Integer, String> ORG_NAMES = new HashMap<>();
    private static final Map<Integer, Map<Integer, ShockerRelationship>> ORG_RELATIONS = new HashMap<>();

    static {
        ORG_NAMES.put(ORG_SHOCKER, "修卡");
        ORG_NAMES.put(ORG_GEL_SHOCKER, "盖尔修卡");
        ORG_NAMES.put(ORG_DESTRO, "迪斯特龙");
        ORG_NAMES.put(ORG_GOVERNMENT_OF_DARK_NESS, "GOD机关");
        ORG_NAMES.put(ORG_GARANDER, "加兰达");
        ORG_NAMES.put(ORG_DELZER, "迪尔扎");
        ORG_NAMES.put(ORG_DAI_SHOCKER, "大修卡");

        // 初始化组织关系
        for (int org : Arrays.asList(ORG_SHOCKER,
                ORG_GEL_SHOCKER,
                ORG_DESTRO,
                ORG_GOVERNMENT_OF_DARK_NESS,
                ORG_GARANDER, ORG_DELZER,
                ORG_DAI_SHOCKER)) {
            ORG_RELATIONS.put(org, new HashMap<>());
        }
        // 初始化组织关系，友好关系
        for (int org : ORG_RELATIONS.keySet()) {
            ORG_RELATIONS.get(org).put(org, ShockerRelationship.FRIENDLY);
        }
        // 初始化组织关系，中立关系
        for (int a : ORG_RELATIONS.keySet()) {
            for (int b : ORG_RELATIONS.keySet()) {
                if (a != b && !ORG_RELATIONS.get(a).containsKey(b)) {
                    ORG_RELATIONS.get(a).put(b, ShockerRelationship.NEUTRAL);
                }
            }
        }
    }

    public static ShockerRelationship getRelationship(int a, int b) {
        return a == b ? ShockerRelationship.FRIENDLY :
                ORG_RELATIONS.getOrDefault(a, Collections.emptyMap()).getOrDefault(b, ShockerRelationship.NEUTRAL);
    }

    public static boolean areFriendly(ShockerBase a, ShockerBase b) {
        return getRelationship(a.getOrgId(), b.getOrgId()) == ShockerRelationship.FRIENDLY;
    }

    public static String getOrgName(int id) {
        return ORG_NAMES.getOrDefault(id, "未知组织");
    }
}