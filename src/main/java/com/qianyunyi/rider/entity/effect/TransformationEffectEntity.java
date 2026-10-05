package com.qianyunyi.rider.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * 变身特效实体类 - 用于制作变身动画
 * 不继承Projectile，不移动，始终跟随玩家
 */
public class TransformationEffectEntity extends Entity implements GeoEntity {
    // 实体数据定义
    public static final EntityDataAccessor<Integer> DEAD_TIME =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> AUTO_CLEAR =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> ANIMATION =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.STRING);

    // 位置偏移（本地坐标：x=右，y=上，z=前）
    public static final EntityDataAccessor<Float> OFFSET_X =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> OFFSET_Y =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> OFFSET_Z =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);

    // 缩放
    public static final EntityDataAccessor<Float> SCALE_X =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SCALE_Y =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SCALE_Z =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);

    // 旋转
    public static final EntityDataAccessor<Float> ROTATION_SPEED_X =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ROTATION_SPEED_Y =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ROTATION_SPEED_Z =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);

    // 发光贴图相关字段
    public static final EntityDataAccessor<String> GLOW_TEXTURE =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ENABLE_GLOW =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> GLOW_COLOR_R =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_G =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_B =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_A =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_INTENSITY =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.FLOAT);

    // 所有者ID
    public static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(TransformationEffectEntity.class, EntityDataSerializers.INT);

    // 所有者UUID，用于防止实体ID复用导致的误判（仅服务端使用）
    private UUID ownerUUID = null;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 构造方法
    public TransformationEffectEntity(EntityType<? extends TransformationEffectEntity> type, Level world) {
        super(type, world);
        setupDefaults();
    }

    public TransformationEffectEntity(EntityType<? extends TransformationEffectEntity> type, double x, double y, double z, Level world) {
        this(type, world);
        this.setPos(x, y, z);
    }

    public TransformationEffectEntity(EntityType<? extends TransformationEffectEntity> type, LivingEntity owner, Level world) {
        this(type, owner.getX(), owner.getY(), owner.getZ(), world);
        setOwner(owner);
    }

    // 带模型、纹理和动画的构造函数
    public TransformationEffectEntity(EntityType<? extends TransformationEffectEntity> type, LivingEntity owner, Level world,
                                      String modelLocation, String textureLocation, String animationName) {
        this(type, owner, world);
        setModelLocation(modelLocation);
        setTextureLocation(textureLocation);
        setAnimationName(animationName);
    }

    // 带发光贴图的构造函数
    public TransformationEffectEntity(EntityType<? extends TransformationEffectEntity> type, LivingEntity owner, Level world,
                                      String modelLocation, String textureLocation, String animationName,
                                      String glowTextureLocation, boolean enableGlow) {
        this(type, owner, world, modelLocation, textureLocation, animationName);
        setGlowTextureLocation(glowTextureLocation);
        setEnableGlow(enableGlow);
    }

    private void setupDefaults() {
        setNoGravity(true);
        setInvulnerable(true);

        // 默认模型、纹理和动画
        setModelLocation("transformation_effect");
        setTextureLocation("transformation_effect");
        setAnimationName("transform");

        // 默认位置偏移
        setOffset(0.0f, 0.0f, 0.0f);

        // 默认缩放
        setScale(1.0f, 1.0f, 1.0f);

        // 默认旋转速度（设为0，完全跟随玩家朝向）
        setRotationSpeed(0.0f, 0.0f, 0.0f);

        // 默认发光设置
        setGlowTextureLocation("");
        setEnableGlow(false);
        setGlowColor(1.0f, 1.0f, 1.0f, 1.0f);
        setGlowIntensity(1.0f);
    }

    // 网络同步
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 实体数据定义
    @Override
    protected void defineSynchedData() {
        entityData.define(DEAD_TIME, 60);
        entityData.define(IS_DYING, false);
        entityData.define(AUTO_CLEAR, true);
        entityData.define(ANIMATION, "transform");
        entityData.define(TEXTURE, "transformation_effect");
        entityData.define(MODEL, "transformation_effect");

        // 位置偏移
        entityData.define(OFFSET_X, 0.0f);
        entityData.define(OFFSET_Y, 1.0f);
        entityData.define(OFFSET_Z, 0.0f);

        // 缩放
        entityData.define(SCALE_X, 1.0f);
        entityData.define(SCALE_Y, 1.0f);
        entityData.define(SCALE_Z, 1.0f);

        // 旋转（默认速度为0，完全跟随玩家朝向）
        entityData.define(ROTATION_SPEED_X, 0.0f);
        entityData.define(ROTATION_SPEED_Y, 0.0f);
        entityData.define(ROTATION_SPEED_Z, 0.0f);

        // 发光贴图
        entityData.define(GLOW_TEXTURE, "");
        entityData.define(ENABLE_GLOW, false);
        entityData.define(GLOW_COLOR_R, 1.0f);
        entityData.define(GLOW_COLOR_G, 1.0f);
        entityData.define(GLOW_COLOR_B, 1.0f);
        entityData.define(GLOW_COLOR_A, 1.0f);
        entityData.define(GLOW_INTENSITY, 1.0f);

        // 所有者
        entityData.define(OWNER_ID, -1);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ===== 状态管理方法 =====
    public boolean isDying() {
        return entityData.get(IS_DYING);
    }

    public void setDying(boolean flag) {
        entityData.set(IS_DYING, flag);
    }

    public int getDeadTime() {
        return entityData.get(DEAD_TIME);
    }

    public void setDeadTime(int ticks) {
        entityData.set(DEAD_TIME, ticks);
    }

    public boolean shouldAutoClear() {
        return entityData.get(AUTO_CLEAR);
    }

    public void setAutoClear(boolean flag) {
        entityData.set(AUTO_CLEAR, flag);
    }

    public String getAnimationName() {
        return entityData.get(ANIMATION);
    }

    public void setAnimationName(String name) {
        entityData.set(ANIMATION, name);
    }

    public String getTextureLocation() {
        return entityData.get(TEXTURE);
    }

    public void setTextureLocation(String location) {
        entityData.set(TEXTURE, location);
    }

    public String getModelLocation() {
        return entityData.get(MODEL);
    }

    public void setModelLocation(String location) {
        entityData.set(MODEL, location);
    }

    // ===== 位置偏移方法 =====
    public void setOffset(float x, float y, float z) {
        entityData.set(OFFSET_X, x);
        entityData.set(OFFSET_Y, y);
        entityData.set(OFFSET_Z, z);
    }

    public float getOffsetX() {
        return entityData.get(OFFSET_X);
    }

    public float getOffsetY() {
        return entityData.get(OFFSET_Y);
    }

    public float getOffsetZ() {
        return entityData.get(OFFSET_Z);
    }

    public Vec3 getOffset() {
        return new Vec3(getOffsetX(), getOffsetY(), getOffsetZ());
    }

    // ===== 缩放方法 =====
    public void setScale(float x, float y, float z) {
        entityData.set(SCALE_X, x);
        entityData.set(SCALE_Y, y);
        entityData.set(SCALE_Z, z);
    }

    public void setScale(float scale) {
        setScale(scale, scale, scale);
    }

    public float getScaleX() {
        return entityData.get(SCALE_X);
    }

    public float getScaleY() {
        return entityData.get(SCALE_Y);
    }

    public float getScaleZ() {
        return entityData.get(SCALE_Z);
    }

    public Vec3 getScale() {
        return new Vec3(getScaleX(), getScaleY(), getScaleZ());
    }

    // ===== 旋转方法 =====
    public void setRotationSpeed(float x, float y, float z) {
        entityData.set(ROTATION_SPEED_X, x);
        entityData.set(ROTATION_SPEED_Y, y);
        entityData.set(ROTATION_SPEED_Z, z);
    }

    public float getRotationSpeedX() {
        return entityData.get(ROTATION_SPEED_X);
    }

    public float getRotationSpeedY() {
        return entityData.get(ROTATION_SPEED_Y);
    }

    public float getRotationSpeedZ() {
        return entityData.get(ROTATION_SPEED_Z);
    }

    public Vec3 getRotationSpeed() {
        return new Vec3(getRotationSpeedX(), getRotationSpeedY(), getRotationSpeedZ());
    }

    // ===== 所有者相关方法 =====
    public int getOwnerId() {
        return entityData.get(OWNER_ID);
    }

    public void setOwnerId(int entityId) {
        entityData.set(OWNER_ID, entityId);
    }

    public LivingEntity getOwner() {
        int ownerId = getOwnerId();
        if (ownerId == -1) return null;

        Entity entity = level().getEntity(ownerId);
        if (entity instanceof LivingEntity living) {
            if (ownerUUID != null && entity instanceof Player player) {
                if (!player.getUUID().equals(ownerUUID)) {
                    return null;
                }
            }
            return living;
        }
        return null;
    }

    public void setOwner(LivingEntity owner) {
        if (owner != null) {
            setOwnerId(owner.getId());
            ownerUUID = owner.getUUID();
        } else {
            setOwnerId(-1);
            ownerUUID = null;
        }
    }

    // ===== 碰撞相关 =====
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_20052_) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_20139_) {
    }

    // ===== 核心逻辑 =====
    @Override
    public void tick() {
        super.tick();

        if (isDying()) {
            if (getDeadTime() > 0) {
                setDeadTime(getDeadTime() - 1);
            } else {
                discard();
                return;
            }
        }

        if (shouldAutoClear() && getOwner() == null) {
            discard();
            return;
        }

        LivingEntity owner = getOwner();
        if (owner != null) {
            updatePositionToOwner(owner);
        }

        applyRotation();
    }

    /**
     * 更新位置到所有者
     * 偏移量跟随玩家朝向（yaw + pitch）旋转，以玩家为轴心
     */
    protected void updatePositionToOwner(LivingEntity owner) {
        Vec3 offset = getOffset();

        float yaw = owner.getYRot();
        float pitch = owner.getXRot();

        // 第一步：绕 Y 轴（水平）旋转
        double yawRad = Math.toRadians(yaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double rx = offset.x * cosYaw - offset.z * sinYaw;
        double rz = offset.x * sinYaw + offset.z * cosYaw;
        double ry = offset.y;

        // 第二步：绕玩家右轴（X 轴）旋转，让偏移跟随俯仰角
        double pitchRad = Math.toRadians(-pitch);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double rotatedY = ry * cosPitch - rz * sinPitch;
        double rotatedZ = ry * sinPitch + rz * cosPitch;

        Vec3 ownerPos = owner.position();
        double x = ownerPos.x + rx;
        double y = ownerPos.y + rotatedY;
        double z = ownerPos.z + rotatedZ;

        // 直接设置位置，同时更新旧位置，防止客户端渲染插值导致的延迟
        setPosRaw(x, y, z);
        xo = x;
        yo = y;
        zo = z;
        this.setBoundingBox(this.makeBoundingBox());

        // 同步旋转，同时更新旧旋转，消除旋转插值延迟
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    /**
     * 应用自旋
     * 在跟随玩家朝向的基础上，叠加自旋效果
     */
    protected void applyRotation() {
        Vec3 rotationSpeed = getRotationSpeed();

        if (rotationSpeed.lengthSqr() > 0) {
            // 在玩家朝向基础上叠加自旋
            // 先保存旧旋转值用于插值
            yRotO = getYRot();
            xRotO = getXRot();
            setYRot(getYRot() + (float) rotationSpeed.y);
            setXRot(getXRot() + (float) rotationSpeed.x);
        }
    }

    /**
     * 阻止客户端插值延迟——既然我们每 tick 自己算位置，不需要服务端插值
     */
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        // 不做任何事，位置由 tick() 中的 updatePositionToOwner 直接控制
    }

    @Override
    public void lerpHeadTo(float yaw, int ticks) {
        // 不做任何事
    }

    // ===== 动画系统 =====
    private PlayState animationPredicate(AnimationState<TransformationEffectEntity> event) {
        String animationName = getAnimationName();

        if (animationName != null && !animationName.isEmpty()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(animationName));
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ===== 实用方法 =====
    public void refreshEffect(int newLifetime, String newAnimation) {
        setDeadTime(newLifetime);
        setAnimationName(newAnimation);
    }

    public void spawnAtEntity(LivingEntity entity) {
        setPos(entity.getX(), entity.getY(), entity.getZ());
        setOwner(entity);
        level().addFreshEntity(this);
    }

    public void spawnAtEntityWithOffset(LivingEntity entity, float offsetX, float offsetY, float offsetZ) {
        setOffset(offsetX, offsetY, offsetZ);
        spawnAtEntity(entity);
    }

    public static TransformationEffectEntity create(EntityType<? extends TransformationEffectEntity> type,
                                                    LivingEntity owner, Level world,
                                                    String model, String texture, String animation) {
        return new TransformationEffectEntity(type, owner, world, model, texture, animation);
    }

    public static TransformationEffectEntity createWithOffset(EntityType<? extends TransformationEffectEntity> type,
                                                              LivingEntity owner, Level world,
                                                              String model, String texture, String animation,
                                                              float offsetX, float offsetY, float offsetZ) {
        TransformationEffectEntity effect = new TransformationEffectEntity(type, owner, world, model, texture, animation);
        effect.setOffset(offsetX, offsetY, offsetZ);
        return effect;
    }

    // ===== 发光贴图相关方法 =====
    public String getGlowTextureLocation() {
        return entityData.get(GLOW_TEXTURE);
    }

    public void setGlowTextureLocation(String location) {
        entityData.set(GLOW_TEXTURE, location != null ? location : "");
    }

    public boolean isEnableGlow() {
        return entityData.get(ENABLE_GLOW) && hasGlowTexture();
    }

    public void setEnableGlow(boolean enable) {
        entityData.set(ENABLE_GLOW, enable);
    }

    public boolean hasGlowTexture() {
        String glowTexture = getGlowTextureLocation();
        return glowTexture != null && !glowTexture.isEmpty();
    }

    public float[] getGlowColor() {
        return new float[] {
                entityData.get(GLOW_COLOR_R),
                entityData.get(GLOW_COLOR_G),
                entityData.get(GLOW_COLOR_B),
                entityData.get(GLOW_COLOR_A)
        };
    }

    public void setGlowColor(float red, float green, float blue, float alpha) {
        entityData.set(GLOW_COLOR_R, Math.max(0.0f, Math.min(1.0f, red)));
        entityData.set(GLOW_COLOR_G, Math.max(0.0f, Math.min(1.0f, green)));
        entityData.set(GLOW_COLOR_B, Math.max(0.0f, Math.min(1.0f, blue)));
        entityData.set(GLOW_COLOR_A, Math.max(0.0f, Math.min(1.0f, alpha)));
    }

    public void setGlowColor(float red, float green, float blue) {
        setGlowColor(red, green, blue, 1.0f);
    }

    public float getGlowIntensity() {
        return entityData.get(GLOW_INTENSITY);
    }

    public void setGlowIntensity(float intensity) {
        entityData.set(GLOW_INTENSITY, Math.max(0.0f, Math.min(10.0f, intensity)));
    }

    // ===== 特效控制方法 =====
    public void startTransformation(int durationTicks) {
        setDeadTime(durationTicks);
        setDying(false);
        setAnimationName("transform");
    }

    public void endTransformation(int fadeOutTicks) {
        setDeadTime(fadeOutTicks);
        setDying(true);
        setAnimationName("fade_out");
    }

    public void endTransformationImmediately() {
        discard();
    }

    public void setScaleWithProportion(float scale) {
        setScale(scale);
    }

    public void setPositionOffset(float x, float y, float z) {
        setOffset(x, y, z);
    }

    public void setFootOffset(float x, float y, float z) {
        setOffset(x, y, z);
    }

    public void setEyeOffset(float x, float y, float z) {
        setOffset(x, y + 1.62f, z);
    }
}