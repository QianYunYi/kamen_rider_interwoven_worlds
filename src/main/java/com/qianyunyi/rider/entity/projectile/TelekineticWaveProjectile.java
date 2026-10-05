package com.qianyunyi.rider.entity.projectile;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * 恩·伽米奥·杰达的念力波弹射物
 * 纯渲染器效果，无实体贴图，发射金色/绿色闪电状能量波
 */
public class TelekineticWaveProjectile extends AbstractArrow {

	private static final EntityDataAccessor<Float> DAMAGE_AMOUNT = SynchedEntityData.defineId(
			TelekineticWaveProjectile.class, EntityDataSerializers.FLOAT);

	private static final float BASE_DAMAGE = 120.0f;
	private static final int MAX_LIFETIME = 100; // 5秒后自动消失
	private int lifetime = 0;

	public TelekineticWaveProjectile(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.setCritArrow(false);
		this.setBaseDamage(BASE_DAMAGE);
	}

	public TelekineticWaveProjectile(Level level, LivingEntity shooter) {
		super(KamenRiderInterwovenWorldsModEntities.TELEKINETIC_WAVE_PROJECTILE.get(), shooter, level);
		this.setNoGravity(true);
		this.setCritArrow(false);
		this.setBaseDamage(BASE_DAMAGE);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DAMAGE_AMOUNT, BASE_DAMAGE);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.level().isClientSide) {
			spawnTrailParticles();
		}

		lifetime++;
		if (lifetime > MAX_LIFETIME && !this.level().isClientSide) {
			this.discard();
		}
	}

	/**
	 * 客户端粒子尾迹效果
	 */
	private void spawnTrailParticles() {
		Vec3 pos = this.position();
		double speed = this.getDeltaMovement().length();

		// 金色核心粒子
		this.level().addParticle(
				new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.45f), 1.5f),
				pos.x, pos.y, pos.z,
				0, 0, 0
		);

		// 绿色环绕粒子
		if (this.random.nextFloat() < 0.3f) {
			double offsetX = (random.nextDouble() - 0.5) * 0.3;
			double offsetY = (random.nextDouble() - 0.5) * 0.3;
			double offsetZ = (random.nextDouble() - 0.5) * 0.3;
			this.level().addParticle(
					new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.2f), 1.0f),
					pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
					0, 0, 0
			);
		}

		// 金色外层粒子拖尾
		if (speed > 0.5 && random.nextFloat() < 0.2f) {
			Vec3 back = pos.subtract(this.getDeltaMovement().normalize().scale(0.5));
			this.level().addParticle(
					new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.1f), 0.8f),
					back.x, back.y, back.z,
					(random.nextDouble() - 0.5) * 0.1,
					(random.nextDouble() - 0.5) * 0.1,
					(random.nextDouble() - 0.5) * 0.1
			);
		}
	}

	@Override
	protected void onHitEntity(@NotNull EntityHitResult result) {
		super.onHitEntity(result);
		Entity target = result.getEntity();

		if (this.level().isClientSide) return;

		// 命中时产生爆炸粒子效果
		if (this.level() instanceof ServerLevel serverLevel) {
			Vec3 hitPos = result.getLocation();
			// 金色爆炸粒子
			for (int i = 0; i < 15; i++) {
				serverLevel.sendParticles(
						new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.45f), 2.0f),
						hitPos.x, hitPos.y, hitPos.z,
						1, 0.3, 0.3, 0.3, 0.1
				);
			}
			// 绿色散射粒子
			for (int i = 0; i < 8; i++) {
				serverLevel.sendParticles(
						new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.2f), 1.5f),
						hitPos.x, hitPos.y, hitPos.z,
						1, 0.5, 0.5, 0.5, 0.05
				);
			}
			// 普通爆炸粒子
			serverLevel.sendParticles(ParticleTypes.EXPLOSION,
					hitPos.x, hitPos.y, hitPos.z,
					1, 0, 0, 0, 0);
		}

		this.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
		this.discard();
	}

	@Override
	protected void onHitBlock(@NotNull BlockHitResult result) {
		super.onHitBlock(result);

		if (!this.level().isClientSide) {
			// 命中地面也产生粒子效果
			if (this.level() instanceof ServerLevel serverLevel) {
				Vec3 hitPos = result.getLocation();
				for (int i = 0; i < 10; i++) {
					serverLevel.sendParticles(
							new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.45f), 1.5f),
							hitPos.x, hitPos.y, hitPos.z,
							1, 0.2, 0.2, 0.2, 0.1
					);
				}
			}
			this.discard();
		}
	}

	@Override
	public void setBaseDamage(double damage) {
		super.setBaseDamage(damage);
		this.entityData.set(DAMAGE_AMOUNT, (float) damage);
	}

	public float getDamageAmount() {
		return this.entityData.get(DAMAGE_AMOUNT);
	}

	@Override
	protected @NotNull ItemStack getPickupItem() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putFloat("DamageAmount", this.getDamageAmount());
		compound.putInt("Lifetime", this.lifetime);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DamageAmount")) {
			this.setBaseDamage(compound.getFloat("DamageAmount"));
		}
		if (compound.contains("Lifetime")) {
			this.lifetime = compound.getInt("Lifetime");
		}
	}
}