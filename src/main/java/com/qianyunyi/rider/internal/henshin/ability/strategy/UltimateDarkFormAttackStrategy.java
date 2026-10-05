package com.qianyunyi.rider.internal.henshin.ability.strategy;

import com.qianyunyi.rider.config.AdventureConfig;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.SkillType;
import com.qianyunyi.rider.api.rider.behavior.IRiderSkill;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.StartAutoAimPacket;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UltimateDarkFormAttackStrategy extends AbstractKuugaFormAttackStrategy {

    private static final class Constants {
        static final String ATTACK_ANIM = "punch";
        static final int ATTACK_ANIM_DURATION = 15;
        static final int TELEPORT_INTERVAL = 200;
        static final double TELEPORT_RANGE = 15.0;
        static final int AUTO_ATTACK_INTERVAL = 10;
        static final double AUTO_ATTACK_RANGE = 2.0;
        static final int SUPERNATURAL_FIRE_INTERVAL = 20;
        static final double SUPERNATURAL_FIRE_RANGE = 20.0;
        static final double SUPERNATURAL_FIRE_ANGLE = 30.0;
        static final int TARGET_UPDATE_INTERVAL = 20;
        static final int MOVE_UPDATE_INTERVAL = 5;
        static final int LOOK_AT_INTERVAL = 5;
        static final int DODGE_COOLDOWN_TICKS = 200;
        static final double DODGE_CHANCE = 0.2;
        static final double MIN_DODGE_DISTANCE = 1.0;
        static final double MAX_DODGE_DISTANCE = 3.0;
        static final int MOVE_DURATION_TICKS = 20;
        static final int RIDER_PUNCH_COOLDOWN = 30;
        static final int ABILITY_COOLDOWN = 20;
    }

    public static final class DarkFormState {
        public int attackAnimTimer;
        public boolean isAttacking;
        public int teleportTimer;
        public int autoAttackTimer;
        public int superNaturalFireTimer;
        public int dodgeCooldown;
        public LivingEntity currentTarget;
        public int targetUpdateTimer;
        public int moveTimer;
        public int lookAtTimer;
        public Vec3 moveDirection = Vec3.ZERO;
        public int moveDuration;
        public double moveSpeed = 1.0;
        public int riderPunchCooldown;
        public int abilityCooldown;

        public void reset() {
            attackAnimTimer = 0;
            isAttacking = false;
            teleportTimer = 0;
            autoAttackTimer = 0;
            superNaturalFireTimer = 0;
            dodgeCooldown = 0;
            currentTarget = null;
            targetUpdateTimer = 0;
            moveTimer = 0;
            lookAtTimer = 0;
            moveDirection = Vec3.ZERO;
            moveDuration = 0;
            moveSpeed = 1.0;
            riderPunchCooldown = 0;
            abilityCooldown = 0;
        }
    }

    private static final Map<UUID, DarkFormState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Random> RANDOMS = new ConcurrentHashMap<>();

    @Override
    public void onFormEnter(ServerPlayer player) {
        super.onFormEnter(player);
        STATES.put(player.getUUID(), new DarkFormState());
        RANDOMS.put(player.getUUID(), new Random());
    }

    @Override
    public void onFormLeave(ServerPlayer player) {
        super.onFormLeave(player);
        STATES.remove(player.getUUID());
        RANDOMS.remove(player.getUUID());
    }

    @Override
    public void onPlayerTick(ServerPlayer player, TickEvent.PlayerTickEvent event) {
        super.onPlayerTick(player, event);
        if (event.side == LogicalSide.CLIENT) return;

        DarkFormState state = STATES.get(player.getUUID());
        if (state == null) return;

        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        if (dataManager == null) return;
        if (dataManager.getKuugaData().getHolySpring() >= 50) return;

        if (AdventureConfig.IS_EMPTY_MY_BLACK_EYE_BLINDNESS.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 4, false, false));
        }

        updateCooldowns(state);
        boolean canMoveAndAttack = canMoveAndAttack(state);
        updateAnimationState(state);
        if (state.dodgeCooldown > 0) state.dodgeCooldown--;
        updateSuperNaturalFire(player, state);
        updateLookAtTarget(player, state);

        if (canMoveAndAttack) {
            updateTeleport(player, state);
            updateAI(player, state);
        }
    }

    @Override
    protected void specialDamageHandling(ServerPlayer player, LivingHurtEvent event) {
        super.specialDamageHandling(player, event);
        if (event.getSource().getEntity() != null && event.getSource().getEntity().getUUID().equals(player.getUUID())) {
            LivingEntity target = event.getEntity();
            if (target != null) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 4, false, false));
            }
        }
    }

    @Override
    public void onLivingAttack(ServerPlayer player, LivingAttackEvent event) {
        if (event.getEntity().getUUID().equals(player.getUUID())) {
            DamageSource source = event.getSource();
            Entity sourceEntity = source.getDirectEntity();

            if (sourceEntity instanceof Projectile projectile) {
                deflectProjectile(player, projectile);
                event.setCanceled(true);
                return;
            }

            DarkFormState state = STATES.get(player.getUUID());
            Random random = RANDOMS.get(player.getUUID());
            if (state != null && random != null && canDodge(state, random)) {
                performDodgeTeleport(player, sourceEntity, state, random);
                event.setCanceled(true);
                return;
            }
        }
        super.onLivingAttack(player, event);
    }

    private void updateCooldowns(DarkFormState state) {
        if (state.riderPunchCooldown > 0) state.riderPunchCooldown--;
        if (state.abilityCooldown > 0) state.abilityCooldown--;
    }

    private boolean canMoveAndAttack(DarkFormState state) {
        return state.riderPunchCooldown <= 0 && state.abilityCooldown <= 0;
    }

    private void updateAnimationState(DarkFormState state) {
        if (state.isAttacking) {
            state.attackAnimTimer--;
            if (state.attackAnimTimer <= 0) state.isAttacking = false;
        }
    }

    private void updateSuperNaturalFire(ServerPlayer player, DarkFormState state) {
        state.superNaturalFireTimer++;
        if (state.superNaturalFireTimer >= Constants.SUPERNATURAL_FIRE_INTERVAL) {
            state.superNaturalFireTimer = 0;
            triggerSuperNaturalFire(player);
        }
    }

    private void updateLookAtTarget(ServerPlayer player, DarkFormState state) {
        state.lookAtTimer++;
        if (state.lookAtTimer >= Constants.LOOK_AT_INTERVAL && state.currentTarget != null && state.currentTarget.isAlive()) {
            state.lookAtTimer = 0;
            lookAtTarget(player, state.currentTarget);
        }
    }

    private void updateTeleport(ServerPlayer player, DarkFormState state) {
        state.teleportTimer++;
        if (state.teleportTimer >= Constants.TELEPORT_INTERVAL) {
            state.teleportTimer = 0;
            performRandomTeleport(player, state);
        }
    }

    private void updateAI(ServerPlayer player, DarkFormState state) {
        state.targetUpdateTimer++;
        if (state.targetUpdateTimer >= Constants.TARGET_UPDATE_INTERVAL) {
            state.targetUpdateTimer = 0;
            updateCurrentTarget(player, state);
        }
        state.moveTimer++;
        if (state.moveTimer >= Constants.MOVE_UPDATE_INTERVAL) {
            state.moveTimer = 0;
            processMovementAndAttack(player, state);
        }
        applyMovement(player, state);
    }

    private void processMovementAndAttack(ServerPlayer player, DarkFormState state) {
        if (state.currentTarget != null && state.currentTarget.isAlive()) {
            NetworkHandler.sendToPlayer(new StartAutoAimPacket(200), player);
            double distance = player.distanceTo(state.currentTarget);
            if (distance > Constants.AUTO_ATTACK_RANGE) {
                if (!state.isAttacking) moveTowardsTarget(player, state.currentTarget, state);
            } else {
                state.moveDirection = Vec3.ZERO;
                state.autoAttackTimer++;
                if (state.autoAttackTimer >= Constants.AUTO_ATTACK_INTERVAL) {
                    state.autoAttackTimer = 0;
                    if (!state.isAttacking) attackTarget(player, state.currentTarget, state);
                }
            }
        } else {
            findNewTarget(player, state);
        }
    }

    private void applyMovement(ServerPlayer player, DarkFormState state) {
        if (state.moveDuration > 0 && !state.moveDirection.equals(Vec3.ZERO)) {
            Vec3 velocity = state.moveDirection.scale(state.moveSpeed);
            player.setDeltaMovement(velocity);
            player.hurtMarked = true;
            state.moveDuration--;
        }
    }

    private void lookAtTarget(ServerPlayer player, LivingEntity target) {
        if (player == null || target == null || target.level().isClientSide) return;
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 playerPos = player.getEyePosition();
        Vec3 lookVec = targetPos.subtract(playerPos).normalize();
        double dx = lookVec.x, dz = lookVec.z, dy = lookVec.y;
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    private void moveTowardsTarget(ServerPlayer player, LivingEntity target, DarkFormState state) {
        if (player == null || target == null || !target.isAlive()) return;
        Vec3 toTarget = target.position().subtract(player.position()).normalize();
        state.moveDirection = new Vec3(toTarget.x, 0, toTarget.z);
        state.moveDuration = Constants.MOVE_DURATION_TICKS;
    }

    private void attackTarget(ServerPlayer player, LivingEntity target, DarkFormState state) {
        if (player == null || target == null || target.level().isClientSide) return;
        lookAtTarget(player, target);
        state.isAttacking = true;
        state.attackAnimTimer = Constants.ATTACK_ANIM_DURATION;
        state.moveDirection = Vec3.ZERO;
        HenshinUtil.sendAnimationMessage(player, Constants.ATTACK_ANIM);
        DamageSources damageSources = player.level().damageSources();
        DamageSource damageSource = damageSources.playerAttack(player);
        float damage = (float) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
        target.hurt(damageSource, damage);
        Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.5);
        target.push(knockback.x, 0.3, knockback.z);
    }

    private void updateCurrentTarget(ServerPlayer player, DarkFormState state) {
        if (player == null) return;
        if (state.currentTarget == null || !state.currentTarget.isAlive()) {
            findNewTarget(player, state);
        } else if (player.distanceToSqr(state.currentTarget) > 400.0) {
            state.currentTarget = null;
            findNewTarget(player, state);
        }
    }

    private void findNewTarget(ServerPlayer player, DarkFormState state) {
        if (player == null || player.level().isClientSide) return;
        List<LivingEntity> nearbyEntities = getNearbyEntities(player, 20.0);
        if (nearbyEntities.isEmpty()) { state.currentTarget = null; return; }
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : nearbyEntities) {
            double distance = entity.distanceToSqr(player);
            if (distance < nearestDistance) { nearestDistance = distance; nearest = entity; }
        }
        state.currentTarget = nearest;
        if (state.currentTarget != null) lookAtTarget(player, state.currentTarget);
    }

    private List<LivingEntity> getNearbyEntities(ServerPlayer player, double range) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof Player p && p.isCreative()));
    }

    private void deflectProjectile(ServerPlayer player, Projectile projectile) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 newMotion = lookVec.scale(projectile.getDeltaMovement().length());
        projectile.setDeltaMovement(newMotion);
        projectile.setYRot(player.getYRot());
        projectile.setXRot(player.getXRot());
    }

    private boolean canDodge(DarkFormState state, Random random) {
        return state.dodgeCooldown <= 0 && !(random.nextDouble() > Constants.DODGE_CHANCE);
    }

    private void performDodgeTeleport(ServerPlayer player, Entity attacker, DarkFormState state, Random random) {
        if (player == null || player.level().isClientSide) return;
        state.dodgeCooldown = Constants.DODGE_COOLDOWN_TICKS;
        Vec3 dodgeDirection = calculateDodgeDirection(player, attacker, random);
        double dodgeDistance = Constants.MIN_DODGE_DISTANCE + random.nextDouble() * (Constants.MAX_DODGE_DISTANCE - Constants.MIN_DODGE_DISTANCE);
        Vec3 dodgeVec = dodgeDirection.normalize().scale(dodgeDistance);
        Vec3 targetPos = player.position().add(dodgeVec);
        if (player.randomTeleport(targetPos.x, targetPos.y, targetPos.z, true)) {
            state.moveDirection = Vec3.ZERO;
        }
    }

    private Vec3 calculateDodgeDirection(ServerPlayer player, Entity attacker, Random random) {
        if (attacker != null) {
            Vec3 toPlayer = player.position().subtract(attacker.position()).normalize();
            return random.nextBoolean()
                    ? toPlayer.cross(new Vec3(0, 1, 0)).normalize()
                    : toPlayer.cross(new Vec3(0, 1, 0)).normalize().scale(-1);
        }
        double angle = (random.nextDouble() < 0.5 ? 90.0 : 270.0) * Math.PI / 180.0;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    private void performRandomTeleport(ServerPlayer player, DarkFormState state) {
        if (player == null || player.level().isClientSide) return;
        Random random = RANDOMS.get(player.getUUID());
        if (random == null) return;
        List<LivingEntity> nearbyEntities = getNearbyEntities(player, Constants.TELEPORT_RANGE);
        if (nearbyEntities.isEmpty()) return;
        LivingEntity targetEntity = nearbyEntities.get(random.nextInt(nearbyEntities.size()));
        Vec3 targetPos = targetEntity.position();
        double targetWidth = targetEntity.getBoundingBox().getXsize();
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = targetWidth + 1.0 + random.nextDouble();
        Vec3 teleportPos = new Vec3(
                targetPos.x + Math.cos(angle) * distance,
                targetPos.y,
                targetPos.z + Math.sin(angle) * distance);
        if (player.randomTeleport(teleportPos.x, teleportPos.y, teleportPos.z, true)) {
            state.moveDirection = Vec3.ZERO;
            state.abilityCooldown = Constants.ABILITY_COOLDOWN;
            lookAtTarget(player, targetEntity);
            executeRiderPunch(player, state);
        }
    }

    private void executeRiderPunch(ServerPlayer player, DarkFormState state) {
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        if (dataManager == null || dataManager.isSkillActive()) return;
        String riderName = dataManager.getActiveRiderName();
        String activeFormName = dataManager.getActiveFormName();
        if (riderName == null || activeFormName == null) return;
        var form = RiderRegistryHelper.getConfig(riderName).orElseThrow().getForm(activeFormName);
        if (form == null) return;
        IRiderSkill riderPunch = form.getSkill(SkillType.RIDER_PUNCH);
        if (riderPunch != null && riderPunch.isAvailable(player)) {
            state.riderPunchCooldown = Constants.RIDER_PUNCH_COOLDOWN;
            state.moveDirection = Vec3.ZERO;
            riderPunch.skill(player);
        }
        for (IRiderSkill skill : form.getAbilities()) {
            if (!skill.isAvailable(player)) continue;
            skill.skill(player);
            break;
        }
    }

    private void triggerSuperNaturalFire(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 eyePos = player.getEyePosition();
        AABB searchArea = player.getBoundingBox().inflate(Constants.SUPERNATURAL_FIRE_RANGE);
        double angleThresholdRad = Math.toRadians(Constants.SUPERNATURAL_FIRE_ANGLE);
        List<Entity> nearbyEntities = player.level().getEntities(player, searchArea,
                entity -> entity != player && entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator());
        for (Entity entity : nearbyEntities) {
            Vec3 toEntity = entity.position().subtract(eyePos).normalize();
            double dot = lookVec.dot(toEntity);
            double angle = Math.acos(dot);
            if (angle <= angleThresholdRad && isVisible(player, entity, eyePos)) {
                entity.setSecondsOnFire(10);
            }
        }
    }

    private boolean isVisible(ServerPlayer player, Entity entity, Vec3 startPos) {
        if (player == null || player.level().isClientSide) return false;
        Vec3 endPos = entity.position().add(0, entity.getEyeHeight() * 0.5, 0);
        ClipContext clipContext = new ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        net.minecraft.world.phys.BlockHitResult blockHitResult = player.level().clip(clipContext);
        if (blockHitResult.getType() == HitResult.Type.MISS) return true;
        return startPos.distanceTo(blockHitResult.getLocation()) >= startPos.distanceTo(endPos);
    }
}