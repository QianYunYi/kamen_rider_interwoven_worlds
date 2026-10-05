package com.qianyunyi.rider.util.kuuga;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于延迟任务的形态切换粒子效果工具类
 */
public class TransformationTaskUtil {
    private static final Random random = new Random();
    private static final ConcurrentMap<UUID, TransformationTask> ACTIVE_TRANSFORMATIONS = new ConcurrentHashMap<>();

    /**
     * 开始执行形态切换效果
     * @return 任务ID，可用于取消效果
     */
    public static UUID startTransformationEffect(Level world, LivingEntity entity, TransformationEffectData data) {
        if (entity == null || world == null || world.isClientSide) return null;

        UUID taskId = UUID.randomUUID();
        TransformationTask task = new TransformationTask(taskId, world, entity, data);
        ACTIVE_TRANSFORMATIONS.put(taskId, task);

        // 开始效果
        task.start();

        return taskId;
    }

    /**
     * 开始执行默认形态切换效果
     */
    public static UUID startDefaultTransformationEffect(Level world, LivingEntity entity) {
        return startTransformationEffect(world, entity, TransformationEffectData.DEFAULT);
    }

    /**
     * 开始执行究极形态切换效果（缩小版）
     */
    public static UUID startUltimateTransformationEffect(Level world, LivingEntity entity) {
        TransformationEffectData data = new TransformationEffectData.Builder()
                .withTotalDuration(60)
                .withLightningIntensity(1.5f)
                .withDustIntensity(1.5f)
                .withDustColor(new Vector3f(0f, 0f, 0f)) // 黑色尘埃
                .withDustRadiusMultiplier(0.8f) // 缩小一圈
                .withDustVerticalOffset(-0.1f) // 下降一格高度
                .withPushRadius(12)
                .withPushStrength(7)
                .withExtinguishFire(true)
                .build();
        return startTransformationEffect(world, entity, data);
    }

    /**
     * 开始执行升华形态切换效果（缩小版）
     */
    public static UUID startRisingTransformationEffect(Level world, LivingEntity entity) {
        TransformationEffectData data = new TransformationEffectData.Builder()
                .withTotalDuration(80)
                .withLightningIntensity(2.0f)
                .withDustIntensity(1.0f)
                .withDustColor(new Vector3f(0.9f, 0.7f, 0f)) // 金色尘埃
                .withDustRadiusMultiplier(0.8f) // 缩小一圈
                .withDustVerticalOffset(-0.1f) // 下降一格高度
                .withPushRadius(15)
                .withPushStrength(8)
                .withExtinguishFire(true)
                .build();
        return startTransformationEffect(world, entity, data);
    }

    /**
     * 开始执行超升华形态切换效果（缩小版）
     */
    public static UUID startSuperRisingTransformationEffect(Level world, LivingEntity entity) {
        TransformationEffectData data = new TransformationEffectData.Builder()
                .withTotalDuration(100)
                .withLightningIntensity(3.0f)
                .withDustIntensity(2.0f)
                .withDustColor(new Vector3f(0.9f, 0.7f, 0f)) // 金色尘埃
                .withDustRadiusMultiplier(0.8f) // 缩小一圈
                .withDustVerticalOffset(-0.1f) // 下降一格高度
                .withPushRadius(20)
                .withPushStrength(10)
                .withExtinguishFire(true)
                .withSpecialLightning(true) // 特殊闪电
                .build();
        return startTransformationEffect(world, entity, data);
    }

    /**
     * 开始执行超升华形态切换效果（原大小版）
     */
    public static UUID startSuperRisingFullTransformationEffect(Level world, LivingEntity entity) {
        TransformationEffectData data = new TransformationEffectData.Builder()
                .withTotalDuration(100)
                .withLightningIntensity(3.0f)
                .withDustIntensity(2.0f)
                .withDustColor(new Vector3f(0.9f, 0.7f, 0f)) // 金色尘埃
                .withDustRadiusMultiplier(1.0f) // 保持原大小
                .withDustVerticalOffset(1.0f) // 原始高度（向上1格）
                .withPushRadius(20)
                .withPushStrength(10)
                .withExtinguishFire(true)
                .withSpecialLightning(true) // 特殊闪电
                .build();
        return startTransformationEffect(world, entity, data);
    }

    /**
     * 取消形态切换效果
     */
    public static boolean cancelTransformationEffect(UUID taskId) {
        if (taskId == null) return false;

        TransformationTask task = ACTIVE_TRANSFORMATIONS.remove(taskId);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    /**
     * 取消实体的所有形态切换效果
     */
    public static int cancelAllTransformationEffects(LivingEntity entity) {
        int count = 0;
        for (TransformationTask task : ACTIVE_TRANSFORMATIONS.values()) {
            if (task.entity.equals(entity)) {
                task.cancel();
                ACTIVE_TRANSFORMATIONS.remove(task.taskId);
                count++;
            }
        }
        return count;
    }

    /**
     * 形态切换任务
     */
    private static class TransformationTask {
        private final UUID taskId;
        private final Level world;
        private final LivingEntity entity;
        private final TransformationEffectData data;
        private UUID chainId;
        private int currentTick = 0;

        public TransformationTask(UUID taskId, Level world, LivingEntity entity, TransformationEffectData data) {
            this.taskId = taskId;
            this.world = world;
            this.entity = entity;
            this.data = data;
        }

        public void start() {
            if (world.isClientSide) return;

            // 熄灭火焰
            if (data.extinguishFire) {
                entity.clearFire();
            }

            // 创建任务链
            chainId = DelayedTask.chain(world)
                    .then(0, this::tickEffect)  // 立即执行第一刻
                    .then(1, this::tickEffect)  // 第二刻
                    .then(1, this::tickEffect)  // 第三刻
                    .then(1, this::tickEffect)  // 继续...
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)  // 第10刻
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)  // 第20刻
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)  // 第30刻
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)  // 第40刻
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)
                    .then(1, this::tickEffect)  // 第50刻
                    .onComplete(() -> ACTIVE_TRANSFORMATIONS.remove(taskId))
                    .onCancel(() -> ACTIVE_TRANSFORMATIONS.remove(taskId))
                    .start();
        }

        private void tickEffect(Level world) {
            if (world.isClientSide || !(world instanceof ServerLevel serverWorld)) {
                cancel();
                return;
            }

            if (!entity.isAlive() || entity.isRemoved()) {
                cancel();
                return;
            }

            // 熄灭火焰
            if (data.extinguishFire) {
                entity.clearFire();
            }

            // 生成闪电粒子
            if (data.generateLightning && currentTick < data.totalDuration) {
                generateLightningParticles(serverWorld, entity, currentTick, data);
            }

            // 生成尘埃粒子
            if (data.generateDust && currentTick >= 20 && currentTick < data.totalDuration) {
                generateDustParticles(serverWorld, entity, currentTick, data);
            }

            // 球形推力效果
            if (data.createPushEffect && currentTick == 20) {
                SpecialEffectsUtil.createSphericalPushEffect(
                        serverWorld,
                        entity.position(),
                        data.pushRadius,
                        data.pushStrength,
                        entity
                );
            }

            currentTick++;
        }

        public void cancel() {
            if (chainId != null) {
                DelayedTask.cancel(chainId);
            }
            ACTIVE_TRANSFORMATIONS.remove(taskId);
        }
    }

    /**
     * 生成闪电粒子
     */
    private static void generateLightningParticles(ServerLevel world, LivingEntity entity, int elapsedTime, TransformationEffectData data) {
        int lightningCount = calculateLightningCount(elapsedTime, data.lightningIntensity);
        if (lightningCount <= 0) return;

        Vec3 center = new Vec3(
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2.0 + 0.5,
                entity.getZ()
        );

        double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * 1.5;

        for (int i = 0; i < lightningCount; i++) {
            generateSingleLightningParticle(world, center, radius, elapsedTime, data);

            if (data.specialLightning && elapsedTime > data.totalDuration * 0.3) {
                generateSpecialLightningEffect(world, center, radius, elapsedTime, i, data);
            }
        }
    }

    /**
     * 生成单个闪电粒子
     */
    private static void generateSingleLightningParticle(ServerLevel world, Vec3 center, double radius, int elapsedTime, TransformationEffectData data) {
        double theta = random.nextDouble() * Math.PI * 2;
        double phi = random.nextDouble() * Math.PI;

        double dirX = Math.sin(phi) * Math.cos(theta);
        double dirY = Math.cos(phi);
        double dirZ = Math.sin(phi) * Math.sin(theta);

        double startX = center.x;
        double startY = center.y;
        double startZ = center.z;

        double endX = center.x + dirX * radius;
        double endY = center.y + dirY * radius;
        double endZ = center.z + dirZ * radius;

        double progress = (elapsedTime % 10) / 10.0;
        double particleX = startX + (endX - startX) * progress;
        double particleY = startY + (endY - startY) * progress;
        double particleZ = startZ + (endZ - startZ) * progress;

        double motionX = dirX * 0.5;
        double motionY = dirY * 0.5;
        double motionZ = dirZ * 0.5;

        world.sendParticles(
                (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
                particleX, particleY, particleZ,
                1,
                motionX, motionY, motionZ,
                0.1
        );

        if (progress > 0.2 && progress < 0.8) {
            double trailProgress = Math.max(0.0, progress - 0.1);
            double trailX = startX + (endX - startX) * trailProgress;
            double trailY = startY + (endY - startY) * trailProgress;
            double trailZ = startZ + (endZ - startZ) * trailProgress;

            world.sendParticles(
                    (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
                    trailX, trailY, trailZ,
                    1,
                    0, 0, 0,
                    0.05
            );
        }
    }

    /**
     * 生成特殊闪电效果
     */
    private static void generateSpecialLightningEffect(ServerLevel world, Vec3 center, double radius, int elapsedTime, int index, TransformationEffectData data) {
        int ringCount = 3;
        for (int ring = 0; ring < ringCount; ring++) {
            double angle = 2 * Math.PI * index / ringCount + (elapsedTime * 0.1);
            double x = center.x + Math.cos(angle) * radius * (0.5 + ring * 0.5);
            double z = center.z + Math.sin(angle) * radius * (0.5 + ring * 0.5);

            world.sendParticles(
                    (ParticleOptions) KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
                    x, center.y, z,
                    0, 0, 0, 0, 0
            );
        }
    }

    /**
     * 生成尘埃粒子
     */
    private static void generateDustParticles(ServerLevel world, LivingEntity entity, int elapsedTime, TransformationEffectData data) {
        int dustCount = calculateDustCount(elapsedTime, data.dustIntensity);
        if (dustCount <= 0) return;

        Vec3 center = new Vec3(
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2.0 + data.dustVerticalOffset, // 使用配置的高度偏移
                entity.getZ()
        );

        double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * data.dustRadiusMultiplier; // 使用配置的半径乘数

        for (int i = 0; i < dustCount; i++) {
            generateDustParticle(world, center, radius, data);
        }
    }

    /**
     * 生成单个尘埃粒子
     */
    private static void generateDustParticle(ServerLevel world, Vec3 center, double radius, TransformationEffectData data) {
        double theta = random.nextDouble() * Math.PI * 2;
        double phi = random.nextDouble() * Math.PI;

        double xOffset = radius * Math.sin(phi) * Math.cos(theta);
        double yOffset = radius * Math.cos(phi);
        double zOffset = radius * Math.sin(phi) * Math.sin(theta);

        double particleX = center.x + xOffset;
        double particleY = center.y + yOffset;
        double particleZ = center.z + zOffset;

        DustParticleOptions particleOptions = new DustParticleOptions(
                data.dustColor,
                1.2f + random.nextFloat() * 0.6f
        );

        double motionX = -xOffset * 0.01;
        double motionY = -yOffset * 0.01;
        double motionZ = -zOffset * 0.01;

        world.sendParticles(
                particleOptions,
                particleX, particleY, particleZ,
                2,
                motionX, motionY, motionZ,
                0.1
        );
    }

    /**
     * 计算闪电粒子数量
     */
    private static int calculateLightningCount(int elapsedTime, float intensity) {
        int baseCount = 0;

        if (elapsedTime < 10) {
            baseCount = 1;
        } else if (elapsedTime < 20) {
            baseCount = 1;
        } else if (elapsedTime < 30) {
            baseCount = 4;
        } else if (elapsedTime < 40) {
            baseCount = 6;
        } else if (elapsedTime < 50) {
            baseCount = 2;
        } else {
            baseCount = 1;
        }

        return (int) (baseCount * intensity);
    }

    /**
     * 计算尘埃粒子数量
     */
    private static int calculateDustCount(int elapsedTime, float intensity) {
        int baseCount = 0;

        if (elapsedTime < 20) {
            baseCount = 0;
        } else if (elapsedTime < 25) {
            baseCount = 1;
        } else if (elapsedTime < 30) {
            baseCount = 5;
        } else if (elapsedTime < 35) {
            baseCount = 10;
        } else if (elapsedTime < 40) {
            baseCount = 20;
        } else if (elapsedTime < 45) {
            baseCount = 30;
        } else {
            baseCount = 40;
        }

        return (int) (baseCount * intensity);
    }

    /**
     * 效果数据类
     */
    public static class TransformationEffectData {
        public static final TransformationEffectData DEFAULT = new Builder()
                .withTotalDuration(50)
                .withLightningIntensity(1.0f)
                .withDustIntensity(1.0f)
                .withDustColor(new Vector3f(0.0f, 0.0f, 0.0f))
                .withDustRadiusMultiplier(1.0f) // 默认大小
                .withDustVerticalOffset(1.0f) // 默认向上1格
                .withPushRadius(10)
                .withPushStrength(5)
                .withExtinguishFire(true)
                .withGenerateLightning(true)
                .withGenerateDust(true)
                .withCreatePushEffect(true)
                .withSpecialLightning(false)
                .build();

        public final int totalDuration;
        public final float lightningIntensity;
        public final float dustIntensity;
        public final Vector3f dustColor;
        public final float dustRadiusMultiplier; // 尘埃球体半径乘数
        public final float dustVerticalOffset;   // 尘埃球体垂直偏移
        public final int pushRadius;
        public final int pushStrength;
        public final boolean extinguishFire;
        public final boolean generateLightning;
        public final boolean generateDust;
        public final boolean createPushEffect;
        public final boolean specialLightning;

        private TransformationEffectData(Builder builder) {
            this.totalDuration = builder.totalDuration;
            this.lightningIntensity = builder.lightningIntensity;
            this.dustIntensity = builder.dustIntensity;
            this.dustColor = builder.dustColor;
            this.dustRadiusMultiplier = builder.dustRadiusMultiplier;
            this.dustVerticalOffset = builder.dustVerticalOffset;
            this.pushRadius = builder.pushRadius;
            this.pushStrength = builder.pushStrength;
            this.extinguishFire = builder.extinguishFire;
            this.generateLightning = builder.generateLightning;
            this.generateDust = builder.generateDust;
            this.createPushEffect = builder.createPushEffect;
            this.specialLightning = builder.specialLightning;
        }

        public static class Builder {
            private int totalDuration = 50;
            private float lightningIntensity = 1.0f;
            private float dustIntensity = 1.0f;
            private Vector3f dustColor = new Vector3f(0.0f, 0.0f, 0.0f);
            private float dustRadiusMultiplier = 1.0f; // 默认大小
            private float dustVerticalOffset = 1.0f;   // 默认向上1格
            private int pushRadius = 10;
            private int pushStrength = 5;
            private boolean extinguishFire = true;
            private boolean generateLightning = true;
            private boolean generateDust = true;
            private boolean createPushEffect = true;
            private boolean specialLightning = false;

            public Builder withTotalDuration(int totalDuration) {
                this.totalDuration = totalDuration;
                return this;
            }

            public Builder withLightningIntensity(float intensity) {
                this.lightningIntensity = intensity;
                return this;
            }

            public Builder withDustIntensity(float intensity) {
                this.dustIntensity = intensity;
                return this;
            }

            public Builder withDustColor(Vector3f color) {
                this.dustColor = color;
                return this;
            }

            public Builder withDustRadiusMultiplier(float multiplier) {
                this.dustRadiusMultiplier = multiplier;
                return this;
            }

            public Builder withDustVerticalOffset(float offset) {
                this.dustVerticalOffset = offset;
                return this;
            }

            public Builder withPushRadius(int radius) {
                this.pushRadius = radius;
                return this;
            }

            public Builder withPushStrength(int strength) {
                this.pushStrength = strength;
                return this;
            }

            public Builder withExtinguishFire(boolean extinguish) {
                this.extinguishFire = extinguish;
                return this;
            }

            public Builder withGenerateLightning(boolean generate) {
                this.generateLightning = generate;
                return this;
            }

            public Builder withGenerateDust(boolean generate) {
                this.generateDust = generate;
                return this;
            }

            public Builder withCreatePushEffect(boolean create) {
                this.createPushEffect = create;
                return this;
            }

            public Builder withSpecialLightning(boolean special) {
                this.specialLightning = special;
                return this;
            }

            public TransformationEffectData build() {
                return new TransformationEffectData(this);
            }
        }
    }
}