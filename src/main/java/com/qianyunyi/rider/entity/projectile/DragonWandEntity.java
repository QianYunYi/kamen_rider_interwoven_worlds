package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.network.EntityRegistrationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DragonWandEntity extends AbstractArrow implements GeoEntity {
    private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(DragonWandEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(DragonWandEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected ItemStack wandItem = new ItemStack(KamenRiderInterwovenWorldsModItems.DRAGON_WAND.get());
    private boolean dealtDamage = false;
    public int clientSideReturnTridentTickCount = 0;

    private boolean isInGround = false;
    private int inGroundTime = 0;
    private BlockPos lastPos = null;

    public DragonWandEntity(EntityType<? extends DragonWandEntity> type, Level level) {
        super(type, level);
        this.setBaseDamage(8.0D);
    }

    public DragonWandEntity(Level level, LivingEntity shooter, ItemStack stack) {
        this(EntityRegistrationHandler.DRAGON_WAND_ENTITY, level, shooter, stack);
    }

    protected DragonWandEntity(EntityType<? extends DragonWandEntity> type, Level level, LivingEntity shooter, ItemStack stack) {
        super(type, shooter, level);
        this.wandItem = stack.copy();
        byte loyalty = (byte) EnchantmentHelper.getLoyalty(stack);
        boolean foil = stack.hasFoil();

        this.entityData.set(ID_LOYALTY, loyalty);
        this.entityData.set(ID_FOIL, foil);

        this.setBaseDamage(8.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ID_LOYALTY, (byte) 0);
        this.entityData.define(ID_FOIL, false);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return this.wandItem.copy();
    }

    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        if (this.inGround) {
            this.isInGround = true;
            this.inGroundTime++;

            if (this.inGroundTime > 4) {
                this.dealtDamage = true;
            }
        } else {
            this.isInGround = false;
            this.inGroundTime = 0;
        }

        int loyalty = this.entityData.get(ID_LOYALTY);
        if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && this.getOwner() != null) {
            if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive()
                    || (owner instanceof Player player && player.isSpectator())) {
                if (!this.level().isClientSide && this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }
                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 vector3d = owner.getEyePosition().subtract(this.position());
                this.setPos(this.getX(), this.getY() + vector3d.y * 0.015D * (double) loyalty, this.getZ());
                double d0 = 0.05D * (double) loyalty;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(vector3d.normalize().scale(d0)));

                if (this.clientSideReturnTridentTickCount == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }
                ++this.clientSideReturnTridentTickCount;

                this.isInGround = false;
                this.inGround = false;
                this.inGroundTime = 0;
            }
        }

        if (this.isInGround) {
            this.lastPos = this.blockPosition();
        }

        super.tick();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result.getType() == HitResult.Type.BLOCK) {
            this.playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.0F, 1.0F);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        float damage = 8.0F;

        if (entity instanceof LivingEntity livingEntity) {
            damage += EnchantmentHelper.getDamageBonus(this.wandItem, livingEntity.getMobType());
        }

        Entity owner = this.getOwner();
        DamageSource damageSource = this.level().damageSources().trident(this, owner != null ? owner : this);
        this.dealtDamage = true;
        this.level().playSound(null, this.blockPosition(),
                KamenRiderInterwovenWorldsModSounds.ROD_DRAGON_STRIKE.get(), this.getSoundSource(), 1.0F, 1.0F);

        if (entity.hurt(damageSource, damage)) {
            if (entity instanceof LivingEntity livingEntity1) {
                if (owner instanceof LivingEntity livingEntity2) {
                    EnchantmentHelper.doPostHurtEffects(livingEntity1, livingEntity2);
                    EnchantmentHelper.doPostDamageEffects(livingEntity2, livingEntity1);
                }
                this.doPostHurtEffects(livingEntity1);
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && (this.isNoPhysics() || this.inGround) && this.shakeTime <= 0) {
            boolean flag = this.pickup == Pickup.ALLOWED
                    || this.pickup == Pickup.CREATIVE_ONLY && player.getAbilities().instabuild
                    || this.isNoPhysics() && this.ownedBy(player);

            if (this.pickup == Pickup.ALLOWED && !player.getInventory().add(this.getPickupItem())) {
                flag = false;
            }

            if (flag) {
                player.take(this, 1);
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("WandItem", this.wandItem.save(new CompoundTag()));
        compound.putBoolean("DealtDamage", this.dealtDamage);
        compound.putBoolean("InGround", this.isInGround);
        compound.putInt("InGroundTime", this.inGroundTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("WandItem", 10)) {
            this.wandItem = ItemStack.of(compound.getCompound("WandItem"));
        }
        this.dealtDamage = compound.getBoolean("DealtDamage");
        this.isInGround = compound.getBoolean("InGround");
        this.inGroundTime = compound.getInt("InGroundTime");

        this.entityData.set(ID_LOYALTY, (byte) EnchantmentHelper.getLoyalty(this.wandItem));
        this.entityData.set(ID_FOIL, this.wandItem.hasFoil());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends DragonWandEntity> PlayState predicate(AnimationState<E> event) {
        if (event.getController().getCurrentAnimation() == null) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public byte getLoyalty() {
        return this.entityData.get(ID_LOYALTY);
    }

    public boolean hasFoil() {
        return this.entityData.get(ID_FOIL);
    }

    public boolean isInGroundState() {
        return this.isInGround;
    }

    public int getInGroundTime() {
        return this.inGroundTime;
    }

    @Nullable
    public BlockPos getGroundPosition() {
        return this.lastPos;
    }
}