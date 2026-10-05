package com.qianyunyi.rider.entity.effect;

import com.qianyunyi.rider.network.EntityRegistrationHandler;
import com.qianyunyi.rider.util.GroundShatterEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.Random;

public class FallingBlockEffectEntity extends Entity {
    private static final EntityDataAccessor<Optional<BlockState>> BLOCK_STATE =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);

    private static final EntityDataAccessor<Float> ROTATION_X =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_Y =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_Z =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOVER_TIME =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> STATE =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> EFFECT_TYPE =
            SynchedEntityData.defineId(FallingBlockEffectEntity.class, EntityDataSerializers.BYTE);

    private static final byte STATE_FLYING = 0;
    private static final byte STATE_HOVERING = 1;
    private static final byte STATE_FALLING = 2;
    private static final byte STATE_GROUNDED = 3;
    private static final byte STATE_SINKING = 4;

    private static final int DEFAULT_HOVER_TIME = 100;

    private int life;
    private Vec3 center;
    private Vec3 initialVelocity;
    private double startY;
    private ServerPlayer owner;

    private Vec3 targetPoint;
    private Vec3 launchDirection;
    private double launchSpeed = 1.8;
    private int hoverStartLife;
    private long sharedLaunchTick = -1;

    public FallingBlockEffectEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.startY = 0.0;
        this.fireImmune();
        this.entityData.set(EFFECT_TYPE, (byte) EffectType.FLY_UP.ordinal());
    }

    public FallingBlockEffectEntity(Level level, double x, double y, double z, BlockState state,
                                    EffectType effectType, Vec3 center, int duration, int hoverTime) {
        this(level, x, y, z, state, effectType, center, duration, hoverTime, null);
    }

    public FallingBlockEffectEntity(Level level, double x, double y, double z, BlockState state,
                                    EffectType effectType, Vec3 center, int duration, int hoverTime,
                                    ServerPlayer owner) {
        super(EntityRegistrationHandler.FALLING_BLOCK_EFFECT, level);
        this.setBlockState(state);
        this.setPos(x, y, z);
        this.startY = y;
        this.setDeltaMovement(Vec3.ZERO);
        this.life = 0;
        this.center = center;
        this.entityData.set(EFFECT_TYPE, (byte) effectType.ordinal());
        this.setDuration(duration);
        this.setHoverTime(hoverTime);
        this.setState(STATE_FLYING);
        this.fireImmune();
        this.owner = owner;

        Random random = new Random();
        float rotX = 0, rotY = 0, rotZ = 0;

        if (effectType == EffectType.FLY_UP) {
            rotX = random.nextFloat() * 360;
            rotY = random.nextFloat() * 360;
            rotZ = random.nextFloat() * 360;

            this.initialVelocity = new Vec3(
                    (random.nextDouble() - 0.5) * 0.2,
                    random.nextDouble() * 0.5 + 0.5,
                    (random.nextDouble() - 0.5) * 0.2
            );
        } else if (effectType == EffectType.CRACK_GROUND) {
            double dx = x - center.x;
            double dz = z - center.z;
            double distance = Math.sqrt(dx * dx + dz * dz);

            Vec3 radialVector = new Vec3(dx, 0, dz).normalize();
            float rotationAngle = 15.0f;
            Vec3 rotationAxis = new Vec3(-radialVector.z, 0, radialVector.x).normalize();

            rotX = (float) (rotationAngle * rotationAxis.x);
            rotZ = (float) (rotationAngle * rotationAxis.z);

            float offsetX = (random.nextFloat() - 0.5f) * 0.2f;
            float offsetZ = (random.nextFloat() - 0.5f) * 0.2f;
            double heightOffset = distance * 0.05;
            this.setPos(x + offsetX, y + heightOffset, z + offsetZ);

            rotY = random.nextFloat() * 20 - 10;

            double strength = Mth.clamp(distance / 8.0, 0.1, 1.0);
            double upStrength = strength * 0.25;

            this.initialVelocity = new Vec3(
                    dx * strength * 0.1,
                    upStrength,
                    dz * strength * 0.1
            );
        } else if (effectType == EffectType.GATHER_LAUNCH) {
            rotX = random.nextFloat() * 360;
            rotY = random.nextFloat() * 360;
            rotZ = random.nextFloat() * 360;

            Vec3 toCenter = center.subtract(x, y, z).normalize();
            this.initialVelocity = new Vec3(
                    toCenter.x * 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    Math.abs(toCenter.y) * 0.5 + 0.4 + random.nextDouble() * 0.3,
                    toCenter.z * 0.5 + (random.nextDouble() - 0.5) * 0.3
            );
        }

        this.setRotation(rotX, rotY, rotZ);
        this.setDeltaMovement(initialVelocity);
    }

    public void setTargetPoint(Vec3 targetPoint) {
        this.targetPoint = targetPoint;
    }

    public void setLaunchSpeed(double launchSpeed) {
        this.launchSpeed = launchSpeed;
    }

    public void setSharedLaunchTick(long tick) {
        this.sharedLaunchTick = tick;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BLOCK_STATE, Optional.empty());
        this.entityData.define(ROTATION_X, 0f);
        this.entityData.define(ROTATION_Y, 0f);
        this.entityData.define(ROTATION_Z, 0f);
        this.entityData.define(DURATION, 100);
        this.entityData.define(HOVER_TIME, DEFAULT_HOVER_TIME);
        this.entityData.define(STATE, STATE_FLYING);
        this.entityData.define(EFFECT_TYPE, (byte) EffectType.FLY_UP.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;

        EffectType effType = getEffectType();

        if (effType == EffectType.GATHER_LAUNCH) {
            if (!level().isClientSide) {
                tickGatherLaunch();
            }
            return;
        }

        if (!level().isClientSide && this.getState() == STATE_FALLING) {
            if (GroundShatterEffect.tryDealFallingDamage(this, this.owner, 80.0f)) {
                return;
            }
        }

        byte state = this.getState();
        if (state == STATE_FLYING) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
            if (this.getDeltaMovement().y <= 0) {
                if (effType == EffectType.FLY_UP) {
                    this.setState(STATE_HOVERING);
                    this.setDeltaMovement(Vec3.ZERO);
                } else {
                    this.setState(STATE_GROUNDED);
                    this.setDeltaMovement(Vec3.ZERO);
                }
            }
            if (this.life > 300) this.discard();
        } else if (state == STATE_HOVERING) {
            if (this.life > this.getHoverTime()) {
                this.setState(STATE_FALLING);
                this.setDeltaMovement(new Vec3(0, -0.04, 0));
            }
        } else if (state == STATE_FALLING) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
            if (this.onGround()) {
                this.setState(STATE_GROUNDED);
                this.setDeltaMovement(Vec3.ZERO);
            }
        } else if (state == STATE_GROUNDED) {
            if (this.life > this.getDuration()) this.discard();
            if (effType == EffectType.CRACK_GROUND && this.life > this.getDuration() - 10) {
                this.setState(STATE_SINKING);
            }
        } else if (state == STATE_SINKING) {
            this.setPos(this.getX(), this.getY() - 0.1, this.getZ());
            if (this.getY() < this.startY - 1.0) this.discard();
        }
    }

    private void tickGatherLaunch() {
        byte state = this.getState();

        if (state == STATE_FLYING) {
            Vec3 toCenter = center.subtract(this.position());
            double dist = toCenter.length();

            // 最多聚集 40 tick（2秒），距离足够近时提前切换
            if (dist < 0.8 || this.life > 40) {
                this.setState(STATE_HOVERING);
                this.setDeltaMovement(Vec3.ZERO);
                this.setPos(center.x, center.y, center.z);
                this.hoverStartLife = this.life;
                return;
            }

            // 缓入缓出：前期加速，后期减速
            double gatherProgress = Math.min(1.0, this.life / 25.0);
            double speed = Math.max(0.15, dist * 0.08 * gatherProgress);
            speed = Math.min(speed, 1.2);

            Vec3 velocity = toCenter.normalize().scale(speed);
            this.setDeltaMovement(velocity);
            this.move(MoverType.SELF, this.getDeltaMovement());

            // 方块旋转：向心飞行时缓慢自转
            this.setRotation(
                    this.getRotationX() + 4.0f,
                    this.getRotationY() + 6.0f,
                    this.getRotationZ() + 3.0f
            );

        } else if (state == STATE_HOVERING) {
            // 悬浮时在中心位置轻微浮动
            double bob = Math.sin(this.life * 0.3) * 0.08;
            this.setPos(center.x, center.y + bob, center.z);
            boolean shouldLaunch;
            if (this.sharedLaunchTick > 0) {
                shouldLaunch = this.level().getGameTime() >= this.sharedLaunchTick;
            } else {
                shouldLaunch = this.life - this.hoverStartLife >= this.getHoverTime();
            }
            if (shouldLaunch) {
                this.setState(STATE_FALLING);
                // 发射瞬间：只计算一次初始速度方向，之后由重力自然累积形成抛物线
                Vec3 launchDir;
                if (this.launchDirection != null) {
                    launchDir = this.launchDirection;
                } else if (this.owner != null) {
                    launchDir = this.owner.getLookAngle().normalize();
                } else {
                    launchDir = new Vec3(0, 0, 1);
                }
                if (this.targetPoint != null) {
                    Vec3 toTarget = targetPoint.subtract(center);
                    if (toTarget.lengthSqr() > 0.0001) {
                        launchDir = toTarget.normalize();
                    }
                }
                this.setDeltaMovement(launchDir.scale(launchSpeed));
            }

        } else if (state == STATE_FALLING) {
            // 重力自然累积形成抛物线，不再每 tick 重新瞄准
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.10, 0));
            this.move(MoverType.SELF, this.getDeltaMovement());

            // 飞行时持续旋转
            this.setRotation(
                    this.getRotationX() + 8.0f,
                    this.getRotationY() + 12.0f,
                    this.getRotationZ() + 6.0f
            );

            if (this.onGround() || this.horizontalCollision) {
                this.setState(STATE_GROUNDED);
                this.setDeltaMovement(Vec3.ZERO);
                GroundShatterEffect.playImpactEffect((ServerLevel) level(), this.position(),
                        this.getBoundingBox(), this.owner, this.damageSources());
            }
            if (this.life > 300) this.discard();

        } else if (state == STATE_GROUNDED) {
            if (this.life > 12) this.discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    public void setRotation(float x, float y, float z) {
        this.entityData.set(ROTATION_X, x);
        this.entityData.set(ROTATION_Y, y);
        this.entityData.set(ROTATION_Z, z);
    }

    public BlockState getBlockState() {
        return this.entityData.get(BLOCK_STATE).orElse(null);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(BLOCK_STATE, Optional.ofNullable(state));
    }

    public int getDuration() { return this.entityData.get(DURATION); }
    public void setDuration(int duration) { this.entityData.set(DURATION, duration); }
    public int getHoverTime() { return this.entityData.get(HOVER_TIME); }
    public void setHoverTime(int hoverTime) { this.entityData.set(HOVER_TIME, hoverTime); }
    public byte getState() { return this.entityData.get(STATE); }
    public void setState(byte state) { this.entityData.set(STATE, state); }
    public float getRotationX() { return this.entityData.get(ROTATION_X); }
    public float getRotationY() { return this.entityData.get(ROTATION_Y); }
    public float getRotationZ() { return this.entityData.get(ROTATION_Z); }
    public EffectType getEffectType() { return EffectType.values()[this.entityData.get(EFFECT_TYPE)]; }
    public ServerPlayer getOwner() { return owner; }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("BlockState", 10)) {
            this.setBlockState(NbtUtils.readBlockState(
                    this.level().registryAccess().lookupOrThrow(Registries.BLOCK),
                    compound.getCompound("BlockState")));
        }
        this.setDuration(compound.getInt("Duration"));
        this.setHoverTime(compound.contains("HoverTime") ? compound.getInt("HoverTime") : DEFAULT_HOVER_TIME);
        this.setState(compound.getByte("State"));
        this.life = compound.getInt("Life");
        this.startY = compound.getDouble("StartY");
        this.entityData.set(EFFECT_TYPE, (byte) compound.getInt("EffectType"));
        if (compound.contains("CenterX")) {
            this.center = new Vec3(compound.getDouble("CenterX"), compound.getDouble("CenterY"), compound.getDouble("CenterZ"));
        }
        if (compound.contains("TargetX")) {
            this.targetPoint = new Vec3(compound.getDouble("TargetX"), compound.getDouble("TargetY"), compound.getDouble("TargetZ"));
        }
        this.launchSpeed = compound.contains("LaunchSpeed") ? compound.getDouble("LaunchSpeed") : 3.5;
        this.hoverStartLife = compound.contains("HoverStartLife") ? compound.getInt("HoverStartLife") : 0;
        if (compound.contains("LaunchDirX")) {
            this.launchDirection = new Vec3(compound.getDouble("LaunchDirX"),
                    compound.getDouble("LaunchDirY"), compound.getDouble("LaunchDirZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.put("BlockState", NbtUtils.writeBlockState(this.getBlockState()));
        compound.putInt("Duration", this.getDuration());
        compound.putInt("HoverTime", this.getHoverTime());
        compound.putByte("State", this.getState());
        compound.putInt("Life", this.life);
        compound.putDouble("StartY", this.startY);
        compound.putInt("EffectType", this.getEffectType().ordinal());
        if (this.center != null) {
            compound.putDouble("CenterX", this.center.x);
            compound.putDouble("CenterY", this.center.y);
            compound.putDouble("CenterZ", this.center.z);
        }
        if (this.targetPoint != null) {
            compound.putDouble("TargetX", this.targetPoint.x);
            compound.putDouble("TargetY", this.targetPoint.y);
            compound.putDouble("TargetZ", this.targetPoint.z);
        }
        compound.putDouble("LaunchSpeed", this.launchSpeed);
        compound.putInt("HoverStartLife", this.hoverStartLife);
        if (this.launchDirection != null) {
            compound.putDouble("LaunchDirX", this.launchDirection.x);
            compound.putDouble("LaunchDirY", this.launchDirection.y);
            compound.putDouble("LaunchDirZ", this.launchDirection.z);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public enum EffectType {
        FLY_UP,
        CRACK_GROUND,
        GATHER_LAUNCH
    }
}