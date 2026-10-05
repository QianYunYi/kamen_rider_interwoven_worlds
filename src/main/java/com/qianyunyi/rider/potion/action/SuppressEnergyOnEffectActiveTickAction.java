package com.qianyunyi.rider.potion.action;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

import java.util.Random;

public class SuppressEnergyOnEffectActiveTickAction {
    private static final Random random = new Random();

    // 存储初始生命值的键
    private static final String INITIAL_HEALTH_KEY = "suppress_energy_initial_health";

    // 形态常量
    private static final String FORM_TAG_KEY = "form";
    private static final String BASE_FORM = "base";
    private static final String MIGHTY_FORM = "mighty_form";

    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null) return;
        if (!(entity instanceof LivingEntity livingEntity)) return;

        // 检查是否为服务器端世界
        if (!(world instanceof ServerLevel serverLevel)) return;

        // 获取实体的持久数据
        CompoundTag persistentData = livingEntity.getPersistentData();

        // 如果是第一次激活效果，记录初始生命值
        if (!persistentData.contains(INITIAL_HEALTH_KEY)) {
            persistentData.putFloat(INITIAL_HEALTH_KEY, livingEntity.getHealth());
        }

        // 获取记录的初始生命值
        float initialHealth = persistentData.getFloat(INITIAL_HEALTH_KEY);

        // 如果当前生命值大于初始生命值，重置为初始值
        if (livingEntity.getHealth() > initialHealth) {
            livingEntity.setHealth(initialHealth);
        }

        // 检查形态标签，如果不是base和mighty_form，播放闪电效果
        if (persistentData.contains(FORM_TAG_KEY)) {
            String form = persistentData.getString(FORM_TAG_KEY);
            if (!BASE_FORM.equals(form) && !MIGHTY_FORM.equals(form)) {
                // 获取闪电粒子类型
                SimpleParticleType lightningParticle = KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get();

                // 生成闪电粒子
                generateLightningParticles(serverLevel, livingEntity, lightningParticle);

                // 播放电流音效
                SoundEvent currentSound = KamenRiderInterwovenWorldsModSounds.CURRENT.get();
                if (currentSound != null) {
                    float pitch = 1.0F + (world.getRandom().nextFloat() - 0.5F) * 0.2F;
                    world.playSound(null,
                            entity.getOnPos(),
                            currentSound,
                            SoundSource.PLAYERS,
                            1.0F,
                            pitch);
                }
            }
        }
    }

    private static void generateLightningParticles(ServerLevel level, LivingEntity entity,
                                                   SimpleParticleType particleType) {
        // 粒子数量 - 减少到2-4个
        int particleCount = 2 + random.nextInt(3); // 2-4个粒子

        // 获取实体碰撞箱大小
        double width = entity.getBbWidth();
        double height = entity.getBbHeight();

        // 根据实体大小确定粒子数量
        if (width > height) {
            // 宽度大于高度，增加粒子数量
            particleCount = (int) (particleCount * 1.5);
        } else {
            // 高度大于宽度，减少粒子数量
            particleCount = (int) (particleCount * 0.75);
        }

        for (int i = 0; i < particleCount; i++) {
            // 在更大的范围内生成粒子（增加分散度）
            double offsetX = (random.nextDouble() - 0.5) * width * 2.0;
            double offsetY = random.nextDouble() * height * 1.5;
            double offsetZ = (random.nextDouble() - 0.5) * width * 2.0;

            // 计算粒子位置
            double particleX = entity.getX() + offsetX;
            double particleY = entity.getY() + offsetY;
            double particleZ = entity.getZ() + offsetZ;

            // 添加轻微随机运动
            double motionX = (random.nextDouble() - 0.5) * 0.05;
            double motionY = (random.nextDouble() - 0.5) * 0.05;
            double motionZ = (random.nextDouble() - 0.5) * 0.05;

            // 生成粒子
            level.sendParticles(
                    particleType,
                    particleX, particleY, particleZ,
                    particleCount, // 粒子数量
                    motionX, motionY, motionZ, // 运动速度
                    0.02 // 额外速度
            );
        }
    }
}