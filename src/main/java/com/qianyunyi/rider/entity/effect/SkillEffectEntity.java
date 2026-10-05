package com.qianyunyi.rider.entity.effect;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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


import java.util.List;

/**
 * 特殊效果实体类
 */
public class SkillEffectEntity extends Projectile implements GeoEntity {
    // 实体数据定义
    public static final EntityDataAccessor<Integer> DEAD_TIME =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_DYING =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> AUTO_CLEAR =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> ANIMATION =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> FOLLOW_OWNER =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    // 新增模型位置字段
    public static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.STRING);

    // 新增碰撞相关字段
    public static final EntityDataAccessor<Boolean> ENABLE_COLLISION =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENABLE_COLLISION_DAMAGE =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> COLLISION_DAMAGE =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLLISION_BOX_WIDTH =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> COLLISION_BOX_HEIGHT =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);

    // 新增发光贴图相关字段
    public static final EntityDataAccessor<String> GLOW_TEXTURE =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ENABLE_GLOW =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> GLOW_COLOR_R =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_G =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_B =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_COLOR_A =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GLOW_INTENSITY =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);

    // 新增伤害所有者字段
    public static final EntityDataAccessor<Integer> DAMAGE_OWNER_ID =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.INT);

    // 新增可视距离字段 (实体能够看到/感知到此特效的最大距离)
    public static final EntityDataAccessor<Float> VIEW_DISTANCE =
            SynchedEntityData.defineId(SkillEffectEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 构造方法
    public SkillEffectEntity(EntityType<? extends SkillEffectEntity> type, Level world) {
        super(type, world);
        setupDefaults();
    }

    public SkillEffectEntity(EntityType<? extends SkillEffectEntity> type, double x, double y, double z, Level world) {
        this(type, world);
        this.setPos(x, y, z);
    }

    public SkillEffectEntity(EntityType<? extends SkillEffectEntity> type, LivingEntity owner, Level world) {
        this(type, owner.getX(), owner.getY() - 0.1F, owner.getZ(), world);
        this.setOwner(owner);
        setDamageOwner(owner); // 默认伤害所有者与实体所有者相同
    }

    // 带模型、纹理和动画的构造函数
    public SkillEffectEntity(EntityType<? extends SkillEffectEntity> type, LivingEntity owner, Level world,
                             String modelLocation, String textureLocation, String animationName) {
        this(type, owner, world);
        setModelLocation(modelLocation);
        setTextureLocation(textureLocation);
        setAnimationName(animationName);
    }

    // 带发光贴图的构造函数
    public SkillEffectEntity(EntityType<? extends SkillEffectEntity> type, LivingEntity owner, Level world,
                             String modelLocation, String textureLocation, String animationName,
                             String glowTextureLocation, boolean enableGlow) {
        this(type, owner, world, modelLocation, textureLocation, animationName);
        setGlowTextureLocation(glowTextureLocation);
        setEnableGlow(enableGlow);
    }

    private void setupDefaults() {
        setNoGravity(true);
        setInvulnerable(true);
        // 设置默认模型、纹理和动画
        setModelLocation("special_effect");
        setTextureLocation("special_effect");
        setAnimationName("idle");

        // 默认关闭碰撞和伤害
        setEnableCollision(false);
        setEnableCollisionDamage(false);
        setCollisionDamage(0.0f);
        setCollisionBoxSize(1.0f, 1.0f); // 默认碰撞箱大小

        // 默认伤害所有者ID为-1（无所有者）
        entityData.set(DAMAGE_OWNER_ID, -1);

        // 默认发光设置
        setGlowTextureLocation(""); // 默认无发光贴图
        setEnableGlow(false);
        setGlowColor(1.0f, 1.0f, 1.0f, 1.0f); // 默认白色
        setGlowIntensity(1.0f); // 默认强度

        // 默认可视距离为20.0（类似Minecraft的默认渲染距离）
        setViewDistance(20.0f);
    }

    // 网络同步
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // 实体数据定义
    @Override
    protected void defineSynchedData() {
        entityData.define(DEAD_TIME, 0);
        entityData.define(IS_DYING, false);
        entityData.define(AUTO_CLEAR, false);
        entityData.define(ANIMATION, "idle");
        entityData.define(TEXTURE, "special_effect");
        entityData.define(FOLLOW_OWNER, true);
        entityData.define(MODEL, "special_effect");

        // 新增碰撞相关数据
        entityData.define(ENABLE_COLLISION, false);
        entityData.define(ENABLE_COLLISION_DAMAGE, false);
        entityData.define(COLLISION_DAMAGE, 0.0f);
        entityData.define(COLLISION_BOX_WIDTH, 1.0f);
        entityData.define(COLLISION_BOX_HEIGHT, 1.0f);
        entityData.define(DAMAGE_OWNER_ID, -1);

        // 新增发光贴图相关数据
        entityData.define(GLOW_TEXTURE, "");
        entityData.define(ENABLE_GLOW, false);
        entityData.define(GLOW_COLOR_R, 1.0f);
        entityData.define(GLOW_COLOR_G, 1.0f);
        entityData.define(GLOW_COLOR_B, 1.0f);
        entityData.define(GLOW_COLOR_A, 1.0f);
        entityData.define(GLOW_INTENSITY, 1.0f);

        // 新增可视距离数据
        entityData.define(VIEW_DISTANCE, 20.0f);
    }


    @Override
    public boolean shouldBeSaved() {
        // 特效实体应该始终保存，除非明确设置不保存
        return true;
    }

    // ===== 新增可视距离相关方法 =====

    /**
     * 获取实体可视距离（其他实体能看到此特效的最大距离）
     */
    public float getViewDistance() {
        return entityData.get(VIEW_DISTANCE);
    }

    /**
     * 设置实体可视距离
     */
    public void setViewDistance(float distance) {
        entityData.set(VIEW_DISTANCE, Math.max(0.0f, distance));
    }

    /**
     * 检查指定实体是否在此特效的可视距离内
     */
    public boolean isEntityInViewRange(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        // 计算距离
        double dx = getX() - entity.getX();
        double dy = getY() - entity.getY();
        double dz = getZ() - entity.getZ();
        double distanceSqr = dx * dx + dy * dy + dz * dz;

        float viewDistance = getViewDistance();
        return distanceSqr <= (viewDistance * viewDistance);
    }

    /**
     * 获取可视范围内的所有实体
     */
    public List<Entity> getEntitiesInViewRange() {
        float viewDistance = getViewDistance();
        AABB viewBox = new AABB(
                getX() - viewDistance, getY() - viewDistance, getZ() - viewDistance,
                getX() + viewDistance, getY() + viewDistance, getZ() + viewDistance
        );

        return level().getEntities(this, viewBox);
    }

    /**
     * 获取可视范围内的所有活体实体
     */
    public List<LivingEntity> getLivingEntitiesInViewRange() {
        List<Entity> entities = getEntitiesInViewRange();
        List<LivingEntity> livingEntities = new java.util.ArrayList<>();

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                // 排除自己和伤害所有者
                if (!isDamageOwner(entity)) {
                    livingEntities.add(livingEntity);
                }
            }
        }

        return livingEntities;
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

    // 新增模型位置方法
    public String getModelLocation() {
        return entityData.get(MODEL);
    }

    public void setModelLocation(String location) {
        entityData.set(MODEL, location);
    }

    public boolean shouldFollowOwner() {
        return entityData.get(FOLLOW_OWNER);
    }

    // 是否跟随主人
    public void setFollowOwner(boolean follow) {
        entityData.set(FOLLOW_OWNER, follow);
    }

    // ===== 新增碰撞相关方法 =====
    public boolean isEnableCollision() {
        return entityData.get(ENABLE_COLLISION);
    }

    // 设置是否启用碰撞
    public void setEnableCollision(boolean enable) {
        entityData.set(ENABLE_COLLISION, enable);
    }

    // 获取是否启用碰撞伤害
    public boolean isEnableCollisionDamage() {
        return entityData.get(ENABLE_COLLISION_DAMAGE);
    }

    // 设置是否启用碰撞伤害
    public void setEnableCollisionDamage(boolean enable) {
        entityData.set(ENABLE_COLLISION_DAMAGE, enable);
    }

    // 获取碰撞伤害
    public float getCollisionDamage() {
        return entityData.get(COLLISION_DAMAGE);
    }

    // 设置碰撞伤害
    public void setCollisionDamage(float damage) {
        entityData.set(COLLISION_DAMAGE, damage);
    }

    // 设置碰撞箱宽度
    public void setCollisionBoxWidth(float width) {
        entityData.set(COLLISION_BOX_WIDTH, width);
    }

    // 设置碰撞箱高度
    public void setCollisionBoxHeight(float height) {
        entityData.set(COLLISION_BOX_HEIGHT, height);
    }

    // 获取碰撞箱宽度
    public float getCollisionBoxWidth() {
        return entityData.get(COLLISION_BOX_WIDTH);
    }

    // 获取碰撞箱高度
    public float getCollisionBoxHeight() {
        return entityData.get(COLLISION_BOX_HEIGHT);
    }

    /**
     * 设置碰撞箱大小
     */
    public void setCollisionBoxSize(float width, float height) {
        entityData.set(COLLISION_BOX_WIDTH, width);
        entityData.set(COLLISION_BOX_HEIGHT, height);
    }

    // ===== 伤害所有者相关方法 =====
    /**
     * 获取伤害所有者ID
     */
    public int getDamageOwnerId() {
        return entityData.get(DAMAGE_OWNER_ID);
    }

    /**
     * 设置伤害所有者ID
     */
    public void setDamageOwnerId(int entityId) {
        entityData.set(DAMAGE_OWNER_ID, entityId);
    }

    /**
     * 获取伤害所有者实体
     */
    public LivingEntity getDamageOwner() {
        int ownerId = getDamageOwnerId();
        if (ownerId == -1) return null;

        Entity entity = level().getEntity(ownerId);
        return entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

    /**
     * 设置伤害所有者
     */
    public void setDamageOwner(LivingEntity owner) {
        if (owner != null) {
            setDamageOwnerId(owner.getId());
        } else {
            setDamageOwnerId(-1);
        }
    }

    /**
     * 检查实体是否是伤害所有者
     */
    public boolean isDamageOwner(Entity entity) {
        return entity != null && entity.getId() == getDamageOwnerId();
    }

    // ===== 碰撞检测方法（替代getBoundingBox重写） =====

    /**
     * 获取用于碰撞检测的AABB
     */
    public AABB getCollisionBoundingBox() {
        if (!isEnableCollision()) {
            // 禁用碰撞时返回空AABB
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        float width = getCollisionBoxWidth();
        float height = getCollisionBoxHeight();
        double halfWidth = width / 2.0;

        return new AABB(
                getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + height, getZ() + halfWidth
        );
    }

    /**
     * 获取用于伤害检测的AABB
     */
    public AABB getDamageBoundingBox() {
        if (!isEnableCollisionDamage()) {
            // 禁用伤害时返回空AABB
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        float width = getCollisionBoxWidth();
        float height = getCollisionBoxHeight();
        double halfWidth = width / 2.0;

        return new AABB(
                getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + height, getZ() + halfWidth
        );
    }

    @Override
    public boolean canBeCollidedWith() {
        return isEnableCollision();
    }

    @Override
    public boolean isPickable() {
        return isEnableCollision();
    }

    private void updateCustomBoundingBox() {
        float width = getCollisionBoxWidth();
        float height = getCollisionBoxHeight();
        double halfWidth = width / 2.0;
        setBoundingBox(new AABB(
                getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + height, getZ() + halfWidth
        ));
    }

    // ===== 核心逻辑 =====
    @Override
    public void tick() {
        // 死亡状态处理
        if (isDying()) {
            if (getDeadTime() > 0) {
                setDeadTime(getDeadTime() - 1);
            } else {
                discard();
                return;
            }
        }

        // 自动清除无主实体
        if (shouldAutoClear() && getOwner() == null) {
            discard();
            return;
        }

        // 移动逻辑
        if (!shouldFollowOwner()) {
            // 独立移动模式 - 自己处理重力、贴地和方块碰撞
            moveWithVelocity();
        } else if (getOwner() instanceof LivingEntity owner) {
            // 跟随模式
            updatePositionToOwner(owner);
        }

        // 碰撞伤害检测
        if (isEnableCollisionDamage() && getCollisionDamage() > 0) {
            checkCollisionDamage();
        }

        // 将速度清零后调用 super.tick()，避免 Projectile.tick() 造成双重移动
        // super.tick() 中的 gravity 会被 setNoGravity(true) 阻止，move 因 velocity 为零不产生位移
        Vec3 savedVelocity = getDeltaMovement();
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        // 恢复我们的速度（super.tick 可能把它清零了）
        if (!shouldFollowOwner()) {
            setDeltaMovement(savedVelocity);
        }
    }

    /**
     * 检查并应用碰撞伤害
     */
    private void checkCollisionDamage() {
        if (level().isClientSide) return;

        // 获取伤害检测区域内的所有实体
        AABB damageBox = getDamageBoundingBox().inflate(0.1); // 稍微扩大一点检测范围
        List<Entity> entities = level().getEntities(this, damageBox);

        for (Entity entity : entities) {
            // 跳过自己、伤害所有者、已死亡的实体和非生物实体
            if (entity == this ||
                    isDamageOwner(entity) ||
                    !entity.isAlive() ||
                    !(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            // 应用伤害
            applyCollisionDamage(livingEntity);
        }
    }

    /**
     * 应用碰撞伤害
     */
    private void applyCollisionDamage(LivingEntity target) {
        float damage = getCollisionDamage();

        // 创建伤害源
        DamageSource damageSource;
        LivingEntity damageOwner = getDamageOwner();

        if (damageOwner != null) {
            damageSource = damageOwner.damageSources().mobProjectile(this, damageOwner);
        } else {
            damageSource = level().damageSources().magic();
        }

        // 应用伤害
        if (target.hurt(damageSource, damage)) {
            // 伤害成功应用后的逻辑
            onCollisionDamageApplied(target, damage);
        }
    }

    /**
     * 碰撞伤害应用后的回调
     */
    protected void onCollisionDamageApplied(LivingEntity target, float damage) {
        // 可以被子类重写以添加特殊效果
        // 例如：击退效果、粒子效果等

        // 默认击退效果
        if (getDeltaMovement().lengthSqr() > 0.1) {
            Vec3 knockback = getDeltaMovement().normalize().scale(0.5);
            target.push(knockback.x, 0.2, knockback.z);
        }
    }

    private void moveWithVelocity() {
        Vec3 motion = getDeltaMovement();

        // 施加重力让实体下落贴地（原版重力值 -0.08）
        double gravity = -0.08;
        double newY = motion.y + gravity;
        // 限制最大下落速度
        if (newY < -0.5) newY = -0.5;
        motion = new Vec3(motion.x, newY, motion.z);

        // 使用 move() 方法进行方块碰撞检测，不会穿透墙壁或地面
        move(MoverType.SELF, motion);

        // 移动后需要重新设置碰撞箱大小，因为 move() 内部可能用默认尺寸重置了 bbox
        updateCustomBoundingBox();

        // 移动后根据碰撞结果更新下一帧的速度
        // Entity.move() 会更新 onGround()、horizontalCollision、verticalCollision 等标志
        if (onGround()) {
            // 贴地时，Y速度归零（不再继续下落）
            setDeltaMovement(motion.x * 0.95, 0, motion.z * 0.95);
        } else if (verticalCollision) {
            // 撞到天花板，Y速度也归零
            setDeltaMovement(motion.x * 0.95, 0, motion.z * 0.95);
        } else {
            // 空中或水平碰撞，保持原有的速度，水平方向稍微衰减
            double newX = horizontalCollision ? 0 : motion.x * 0.98;
            double newZ = horizontalCollision ? 0 : motion.z * 0.98;
            setDeltaMovement(newX, motion.y, newZ);
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // 如果启用碰撞，使用正常的移动逻辑（会检测碰撞）
        if (isEnableCollision()) {
            // 使用正常的移动逻辑，会检测碰撞
            super.move(type, movement);
            // 移动后重新设置碰撞箱大小（super.move 内部可能会调整 bbox）
            updateCustomBoundingBox();
        } else {
            // 禁用碰撞时直接移动，忽略方块碰撞
            this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        // 设置位置后立即用自定义尺寸更新碰撞箱
        updateCustomBoundingBox();
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        super.moveTo(x, y, z, yRot, xRot);
        updateCustomBoundingBox();
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return isEnableCollision() && super.canCollideWith(entity);
    }

    // 位置跟随逻辑
    protected void updatePositionToOwner(LivingEntity owner) {
        Vec3 offset = calculatePositionOffset(owner);
        setPos(
                owner.getX() + offset.x,
                owner.getY() + offset.y,
                owner.getZ() + offset.z
        );
        setRot(owner.getYRot(), owner.getXRot());
    }

    protected Vec3 calculatePositionOffset(LivingEntity owner) {
        return Vec3.ZERO;
    }

    // 位置更新方法
    public void updatePosition(Entity target) {
        updatePosition(target, Entity::setPos);
    }

    // 清除特效实体
    public void clearEffect() {
        setOwner(null);
        setDamageOwner(null);
        setAutoClear(true);
        setDying(true);
        setDeadTime(0);
    }

    protected void updatePosition(Entity target, PositionSetter setter) {
        if (getOwner() != null) {
            Entity owner = getOwner();
            setRot(owner.getYRot(), owner.getXRot());
            double yOffset = owner.getY();
            setter.setPosition(this, owner.getX(), yOffset, owner.getZ());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @FunctionalInterface
    protected interface PositionSetter {
        void setPosition(Entity entity, double x, double y, double z);
    }

    // ===== 动画系统 =====

    private PlayState animationPredicate(AnimationState<SkillEffectEntity> event) {
        // 获取当前要播放的动画名称
        String animationName = getAnimationName();

        // 如果动画名称不为空，则播放该动画
        if (animationName != null && !animationName.isEmpty()) {
            event.getController().setAnimation(
                    RawAnimation.begin().thenPlay(animationName)
            );
            return PlayState.CONTINUE;
        }

        // 如果没有指定动画，则停止播放
        return PlayState.STOP;
    }

    // ===== 实用方法 =====
    public void refreshEffect(int newLifetime, String newAnimation) {
        setDeadTime(newLifetime);
        setAnimationName(newAnimation);
    }

    public void spawnAtEntity(LivingEntity entity) {
        setPos(entity.getX(), entity.getY(), entity.getZ());
        setOwner(entity);
        setDamageOwner(entity); // 默认伤害所有者与实体所有者相同
        level().addFreshEntity(this);
    }

    // 创建特效实体的构建器方法
    public static SkillEffectEntity create(EntityType<? extends SkillEffectEntity> type,
                                           LivingEntity owner, Level world,
                                           String model, String texture, String animation) {
        return new SkillEffectEntity(type, owner, world, model, texture, animation);
    }

    // ===== 发光贴图相关方法 =====

    /**
     * 获取发光贴图位置
     */
    public String getGlowTextureLocation() {
        return entityData.get(GLOW_TEXTURE);
    }

    /**
     * 设置发光贴图位置
     */
    public void setGlowTextureLocation(String location) {
        entityData.set(GLOW_TEXTURE, location != null ? location : "");
    }

    /**
     * 检查是否启用发光效果
     */
    public boolean isEnableGlow() {
        return entityData.get(ENABLE_GLOW) && hasGlowTexture();
    }

    /**
     * 设置是否启用发光效果
     */
    public void setEnableGlow(boolean enable) {
        entityData.set(ENABLE_GLOW, enable);
    }

    /**
     * 检查是否有发光贴图
     */
    public boolean hasGlowTexture() {
        String glowTexture = getGlowTextureLocation();
        return glowTexture != null && !glowTexture.isEmpty();
    }

    /**
     * 获取发光颜色（RGBA）
     */
    public float[] getGlowColor() {
        return new float[] {
                entityData.get(GLOW_COLOR_R),
                entityData.get(GLOW_COLOR_G),
                entityData.get(GLOW_COLOR_B),
                entityData.get(GLOW_COLOR_A)
        };
    }

    /**
     * 设置发光颜色（RGBA，0.0-1.0）
     */
    public void setGlowColor(float red, float green, float blue, float alpha) {
        entityData.set(GLOW_COLOR_R, Math.max(0.0f, Math.min(1.0f, red)));
        entityData.set(GLOW_COLOR_G, Math.max(0.0f, Math.min(1.0f, green)));
        entityData.set(GLOW_COLOR_B, Math.max(0.0f, Math.min(1.0f, blue)));
        entityData.set(GLOW_COLOR_A, Math.max(0.0f, Math.min(1.0f, alpha)));
    }

    /**
     * 设置发光颜色（RGB，Alpha默认为1.0）
     */
    public void setGlowColor(float red, float green, float blue) {
        setGlowColor(red, green, blue, 1.0f);
    }

    /**
     * 获取发光强度
     */
    public float getGlowIntensity() {
        return entityData.get(GLOW_INTENSITY);
    }

    /**
     * 设置发光强度（0.0-10.0）
     */
    public void setGlowIntensity(float intensity) {
        entityData.set(GLOW_INTENSITY, Math.max(0.0f, Math.min(10.0f, intensity)));
    }

    /**
     * 快速启用发光效果
     */
    public void enableGlowEffect(String glowTexture, float intensity) {
        setGlowTextureLocation(glowTexture);
        setEnableGlow(true);
        setGlowIntensity(intensity);
    }

    /**
     * 快速启用发光效果（带颜色）
     */
    public void enableGlowEffect(String glowTexture, float red, float green, float blue, float intensity) {
        setGlowTextureLocation(glowTexture);
        setEnableGlow(true);
        setGlowColor(red, green, blue);
        setGlowIntensity(intensity);
    }

    /**
     * 禁用发光效果
     */
    public void disableGlowEffect() {
        setEnableGlow(false);
    }

    // ===== 碰撞相关实用方法 =====

    /**
     * 快速启用碰撞和伤害
     */
    public void enableCollisionWithDamage(float damage, float boxWidth, float boxHeight, LivingEntity damageOwner) {
        setEnableCollision(true);
        setEnableCollisionDamage(true);
        setCollisionDamage(damage);
        setCollisionBoxSize(boxWidth, boxHeight);
        setDamageOwner(damageOwner);
    }

    /**
     * 仅启用碰撞（无伤害）
     */
    public void enableCollisionOnly(float boxWidth, float boxHeight) {
        setEnableCollision(true);
        setEnableCollisionDamage(false);
        setCollisionBoxSize(boxWidth, boxHeight);
    }

    /**
     * 禁用所有碰撞相关功能
     */
    public void disableCollision() {
        setEnableCollision(false);
        setEnableCollisionDamage(false);
        setCollisionDamage(0.0f);
    }

    /**
     * 设置碰撞箱为球形（宽度和高度相同）
     */
    public void setSphericalCollisionBox(float radius) {
        setCollisionBoxSize(radius * 2, radius * 2);
    }

    /**
     * 设置碰撞箱为圆柱形（特定半径和高度）
     */
    public void setCylindricalCollisionBox(float radius, float height) {
        setCollisionBoxSize(radius * 2, height);
    }

    /**
     * 设置伤害所有者与实体所有者相同
     */
    public void setDamageOwnerAsEntityOwner() {
        if (getOwner() instanceof LivingEntity owner) {
            setDamageOwner(owner);
        }
    }

    /**
     * 设置独立的伤害所有者
     */
    public void setIndependentDamageOwner(LivingEntity damageOwner) {
        setDamageOwner(damageOwner);
    }
}