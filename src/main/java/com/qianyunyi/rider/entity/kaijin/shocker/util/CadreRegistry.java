package com.qianyunyi.rider.entity.kaijin.shocker.util;



import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;

import java.util.*;

public class CadreRegistry {
    private static final Map<Integer, List<Class<? extends ShockerBase>>> ORG_CADRES = new HashMap<>();
    private static final Random RANDOM = new Random();

    // 注册所有组织干部
    public static void registerAll() {
        // 修卡干部
        //register(ShockerOrg.ORG_SHOCKER, ColonelSolEntity.class);
        //register(ShockerOrg.ORG_SHOCKER, DoctorDeathEntity.class);

        // 盖尔修卡
        // register(ShockerOrg.ORG_GEL_SHOCKER, 你的干部.class);

        // 迪斯特龙
        // register(ShockerOrg.ORG_DESTRO, ...);

        // GOD机关
        // register(ShockerOrg.ORG_GOVERNMENT_OF_DARK_NESS, ...);

        // 加兰达
        // register(ShockerOrg.ORG_GARANDER, ...);

        // 迪尔扎
        // register(ShockerOrg.ORG_DELZER, ...);
    }

    private static void register(int orgId, Class<? extends ShockerBase> cadreClass) {
        ORG_CADRES.computeIfAbsent(orgId, k -> new ArrayList<>()).add(cadreClass);
    }

    // 随机抽取干部
    public static Class<? extends ShockerBase> getRandomCadreByOrg(int orgId) {
        List<Class<? extends ShockerBase>> list = ORG_CADRES.getOrDefault(orgId, Collections.emptyList());
        if (list.isEmpty()) return null;
        return list.get(RANDOM.nextInt(list.size()));
    }
}