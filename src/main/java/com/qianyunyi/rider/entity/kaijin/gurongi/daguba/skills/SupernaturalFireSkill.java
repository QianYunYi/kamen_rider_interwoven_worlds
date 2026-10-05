package com.qianyunyi.rider.entity.kaijin.gurongi.daguba.skills;

import com.qianyunyi.rider.entity.kaijin.gurongi.daguba.NDagubaZebaEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.util.DelayedTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SupernaturalFireSkill extends AbstractDagubaSkill {

    public SupernaturalFireSkill(NDagubaZebaEntity entity) {
        super(entity);
    }

    @Override
    public boolean canExecute(LivingEntity target) {
        return target != null && target.isAlive() && entity.hasLineOfSight(target);
    }

    @Override
    public int getCooldown() {
        return 600;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public double getMaxRange() {
        return 32.0;
    }

    @Override
    protected void onStart() {
        entity.setAnimation("supernatural");

        double range = 32.0;
        AABB area = new AABB(
                entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range
        );

        DelayedTask.run(entity.level(), 20, () -> {
            for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (target == entity) continue;

                if (target instanceof ServerPlayer serverPlayer) {
                    if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) continue;
                } else if (target instanceof Player player) {
                    if (player.isCreative()) continue;
                }

                if (entity.getSensing().hasLineOfSight(target)) {
                    target.hurt(entity.damageSources().mobAttack(entity),
                            (float) (entity.getAttributeValue(Attributes.ATTACK_DAMAGE)) * 2.0f);
                    target.setSecondsOnFire(10);
                    target.addEffect(new MobEffectInstance(
                            KamenRiderInterwovenWorldsModMobEffects.SUPERNATURAL_FIRE.get(),
                            100, 8, false, false, true));
                    Vec3 vec3 = entity.position().subtract(target.position()).normalize();
                    target.push(vec3.x * 0.5, 0.2, vec3.z * 0.5);
                }
            }
            onFinish();
        });
    }
}