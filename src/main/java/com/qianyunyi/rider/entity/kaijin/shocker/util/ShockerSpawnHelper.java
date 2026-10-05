package com.qianyunyi.rider.entity.kaijin.shocker.util;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.entity.kaijin.shocker.base.ShockerBase;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShockerSpawnHelper {

    // 自动映射：实体类 → EntityType
    private static final Map<Class<? extends ShockerBase>, EntityType<? extends ShockerBase>> CLASS_TO_TYPE = new HashMap<>();

    // 在Mod初始化时注册所有干部
    public static void registerCadreTypes() {
       // put(ColonelSolEntity.class, KamenRiderInterwovenWorldsModEntities.COLONEL_SOL);
       //put(DoctorDeathEntity.class, KamenRiderInterwovenWorldsModEntities.DOCTOR_DEATH);
    }

    private static <T extends ShockerBase> void put(Class<T> clazz, RegistryObject<EntityType<T>> typeObject) {
        CLASS_TO_TYPE.put(clazz, typeObject.get());
    }

    public static void trySpawnRandomCadreOnDeath(ShockerBase deadEntity, float chance) {
        Level level = deadEntity.level();
        if (level.isClientSide()) return;

        // 概率
        if (deadEntity.getRandom().nextFloat() >= chance) return;

        // 获取干部
        int orgId = deadEntity.getOrgId();
        Class<? extends ShockerBase> cadreClass = CadreRegistry.getRandomCadreByOrg(orgId);
        if (cadreClass == null) return;

        // 32格检测
        AABB box = deadEntity.getBoundingBox().inflate(32);
        List<? extends LivingEntity> existing = level.getEntitiesOfClass(cadreClass, box, LivingEntity::isAlive);
        if (!existing.isEmpty()) return;

        try {
            //  自动匹配构造
            EntityType<? extends ShockerBase> type = CLASS_TO_TYPE.get(cadreClass);
            if (type == null) return;

            ShockerBase cadre = cadreClass.getConstructor(EntityType.class, Level.class)
                    .newInstance(type, level);

            cadre.setPos(deadEntity.getX(), deadEntity.getY(), deadEntity.getZ());
            cadre.setOrgId(orgId);

            if (deadEntity.getLastHurtByMob() != null) {
                cadre.setTarget(deadEntity.getLastHurtByMob());
            }

            // 核心修复：手动调用 finalizeSpawn 初始化
            if (level instanceof ServerLevelAccessor serverLevel) {
                DifficultyInstance difficulty = serverLevel.getCurrentDifficultyAt(cadre.blockPosition());
                // 最后一个参数为null，完全复刻刷怪蛋生成逻辑
                cadre.finalizeSpawn(serverLevel, difficulty, MobSpawnType.MOB_SUMMONED, null, null);
            }

            level.addFreshEntity(cadre);
            KamenRiderInterwovenWorldsMod.LOGGER.info("✅ 自动生成干部：" + cadreClass.getSimpleName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}