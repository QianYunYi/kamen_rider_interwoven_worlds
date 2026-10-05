package com.qianyunyi.rider.item;

import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.entity.projectile.PegasusBowgunProjectile;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.item.base.BaseCrossbowAnimationItem;
import com.qianyunyi.rider.item.renderer.RisingPegasusCrossbowItemRenderer;
import com.qianyunyi.rider.network.NetworkHandler;
import com.qianyunyi.rider.network.packet.StartAutoAimPacket;
import com.qianyunyi.rider.network.packet.StopAutoAimPacket;
import com.qianyunyi.rider.network.packet.UpdateItemDataPacket;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * 升华天马弩 - Rising Pegasus Crossbow
 * 三阶段状态机：装填(15tick) → 充能(15tick) → 射击(5发)
 * 空我形态充能时消耗 10% 亚玛达姆能量上限
 */
public class RisingPegasusCrossbowItem extends BaseCrossbowAnimationItem {

	// ==================== 常量 ====================

	/** 装填时长（15刻 = 0.75秒） */
	public static final int LOAD_DURATION = 15;
	/** 充能时长（15刻 = 0.75秒） */
	public static final int CHARGE_DURATION = 15;
	/** 弩箭飞行速度 */
	public static final float PROJECTILE_SPEED = 12.0F;
	/** 弩箭伤害系数 */
	public static final float BULLET_DAMAGE = 1.0F;
	/** 最大连射次数 */
	public static final int MAX_SHOTS = 5;

	// NBT 标签
	private static final String TAG_CHARGED = "RisingPegasusCharged";
	private static final String TAG_LOADING = "RisingPegasusLoading";
	private static final String TAG_LOAD_PROGRESS = "RisingPegasusLoadProgress";
	private static final String TAG_CHARGING = "RisingPegasusCharging";
	private static final String TAG_CHARGE_PROGRESS = "RisingPegasusChargeProgress";
	private static final String TAG_LOADED = "RisingPegasusLoaded";
	private static final String TAG_SHOOT = "RisingPegasusShoot";
	private static final String TAG_REMAINING_SHOTS = "RisingPegasusRemainingShots";

	// 动画名称
	public static final String ANIM_IDLE = "idle";
	public static final String ANIM_LOAD = "load";
	public static final String ANIM_LOAD_IDLE = "load_idle";
	public static final String ANIM_SHOOT = "shoot";

	// 音效
	private final SoundEvent SOUND_LOAD = KamenRiderInterwovenWorldsModSounds.PEGASUS_BOWGUN_LOAD.get();
	private final SoundEvent SOUND_CHARGE = KamenRiderInterwovenWorldsModSounds.PEGASUS_BOWGUN_CHARGE.get();
	private final SoundEvent SOUND_SHOOT = KamenRiderInterwovenWorldsModSounds.PEGASUS_BOWGUN_SHOOT.get();

	// 动画缓存（避免每 tick 重复写入 NBT）
	private String lastAnim = ANIM_IDLE;

	// ==================== 构造 ====================

	public RisingPegasusCrossbowItem() {
		super(new Item.Properties()
				.stacksTo(1)
				.fireResistant()
				.rarity(Rarity.UNCOMMON)
				.durability(600));
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return false;
	}

	// ==================== NBT 状态读取/写入 ====================

	public static boolean isCharged(ItemStack stack)   { return stack.getOrCreateTag().getBoolean(TAG_CHARGED); }
	public static void setCharged(ItemStack stack, boolean v) { stack.getOrCreateTag().putBoolean(TAG_CHARGED, v); }

	public static boolean isLoading(ItemStack stack)   { return stack.getOrCreateTag().getBoolean(TAG_LOADING); }
	public static void setLoading(ItemStack stack, boolean v) { stack.getOrCreateTag().putBoolean(TAG_LOADING, v); }

	public static int getLoadProgress(ItemStack stack) { return stack.getOrCreateTag().getInt(TAG_LOAD_PROGRESS); }
	public static void setLoadProgress(ItemStack stack, int v) { stack.getOrCreateTag().putInt(TAG_LOAD_PROGRESS, v); }

	public static boolean isCharging(ItemStack stack)  { return stack.getOrCreateTag().getBoolean(TAG_CHARGING); }
	public static void setCharging(ItemStack stack, boolean v) { stack.getOrCreateTag().putBoolean(TAG_CHARGING, v); }

	public static int getChargeProgress(ItemStack stack) { return stack.getOrCreateTag().getInt(TAG_CHARGE_PROGRESS); }
	public static void setChargeProgress(ItemStack stack, int v) { stack.getOrCreateTag().putInt(TAG_CHARGE_PROGRESS, v); }

	public static boolean isLoaded(ItemStack stack)    { return stack.getOrCreateTag().getBoolean(TAG_LOADED); }
	public static void setLoaded(ItemStack stack, boolean v) { stack.getOrCreateTag().putBoolean(TAG_LOADED, v); }

	public static boolean isShoot(ItemStack stack)     { return stack.getOrCreateTag().getBoolean(TAG_SHOOT); }
	public static void setShoot(ItemStack stack, boolean v) { stack.getOrCreateTag().putBoolean(TAG_SHOOT, v); }

	public static int getRemainingShots(ItemStack stack) { return stack.getOrCreateTag().getInt(TAG_REMAINING_SHOTS); }
	public static void setRemainingShots(ItemStack stack, int v) { stack.getOrCreateTag().putInt(TAG_REMAINING_SHOTS, v); }

	// ==================== 动画状态管理 ====================

	/**
	 * 更新动画状态，只在状态变化时写入 NBT
	 * 避免每 tick 写入导致 GeckoLib 动画重载和性能开销
	 */
	private void updateAnimationState(ItemStack stack) {
		if (isLoading(stack) || isShoot(stack)) {
			return;
		}
		String newAnim;
		if (isCharging(stack) || isCharged(stack) || isLoaded(stack)) {
			newAnim = ANIM_LOAD_IDLE;
		} else {
			newAnim = ANIM_IDLE;
		}
		if (!newAnim.equals(lastAnim) || !newAnim.equals(getAnim(stack))) {
			setAnim(stack, newAnim);
			lastAnim = newAnim;
		}
	}

	// ==================== 装填阶段 ====================

	/** 开始装填（需长按右键，显示 CROSSBOW 动画） */
	private void startLoading(ItemStack stack, Player player) {
		if (!isLoading(stack) && !isCharging(stack) && !isCharged(stack) && !isLoaded(stack) && !isShoot(stack)) {
			setLoading(stack, true);
			setLoadProgress(stack, 0);
			setAnim(stack, ANIM_LOAD);
		}
	}

	/**
	 * 装填完成：切换到已装填状态
	 */
	private void finishLoading(ItemStack stack, Player player) {
		if (isLoading(stack)) {
			setLoading(stack, false);
			setLoadProgress(stack, 0);
			setLoaded(stack, true);
			setAnim(stack, ANIM_LOAD_IDLE);

			player.level().playSound(null, player.blockPosition(),
					SOUND_LOAD, SoundSource.PLAYERS, 1.0F, 1.0F);
			if (player.level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.END_ROD,
						player.getX(), player.getY() + 1.0, player.getZ(),
						2, 0.3, 0.3, 0.3, 0.05);
			}
		}
	}

	/** 取消装填 */
	private void cancelLoading(ItemStack stack, Player player) {
		if (isLoading(stack) && !isCharging(stack)) {
			setLoading(stack, false);
			setLoadProgress(stack, 0);
			setAnim(stack, ANIM_IDLE);
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.CROSSBOW_LOADING_START,
					SoundSource.PLAYERS, 0.3F, 1.5F);
		}
	}

	// ==================== 充能阶段 ====================

	/**
	 * 开始充能（点击即忘，无需长按）
	 * 空我形态消耗 10% 亚玛达姆能量上限
	 */
	private void startCharging(ItemStack stack, Player player) {
		if (isLoaded(stack) && !isCharging(stack) && !isCharged(stack)) {
			// 空我形态 → 检查并消耗亚玛达姆能量
			if (!player.level().isClientSide()) {
				RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
				if (data != null && "Kuuga".equals(data.getActiveRiderName())) {
					KuugaDataModule kuuga = data.getKuugaData();
					if (kuuga != null) {
						float cost = kuuga.getAmadamEnergyMax() * 0.1f;
						if (kuuga.getAmadamEnergy() >= cost) {
							kuuga.removeAmadamEnergy(cost);
						} else {
							player.displayClientMessage(
									Component.literal("§c亚玛达姆能量不足，无法充能！"), true);
							return;
						}
					}
				}
			}

			setCharging(stack, true);
			setChargeProgress(stack, 0);
			setLoaded(stack, false);
			setAnim(stack, ANIM_LOAD_IDLE);

			player.level().playSound(null, player.blockPosition(),
					SOUND_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	/** 充能完成：开启自动瞄准，设置连射次数 */
	private void finishCharging(ItemStack stack, Player player) {
		if (isCharging(stack)) {
			setCharging(stack, false);
			setCharged(stack, true);
			setChargeProgress(stack, 0);
			setRemainingShots(stack, MAX_SHOTS);
			setAnim(stack, ANIM_LOAD_IDLE);

			player.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), 1.0F, 1.5F);

			// 判断是否是超自然感管状态
			if (RiderPlayerVariablesProvider.get(player).getKuugaData().isSupernaturalSensesActive()
					&& !player.level().isClientSide()) {
				NetworkHandler.sendToPlayer(new StartAutoAimPacket(600), (ServerPlayer) player);
			}
			player.level().playSound(null, player.blockPosition(),
					SOUND_LOAD, SoundSource.PLAYERS, 0.8F, 1.2F);

			if (player.level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.END_ROD,
						player.getX(), player.getY() + 1.0, player.getZ(),
						3, 0.3, 0.3, 0.3, 0.05);
			}
		}
	}

	/** 取消充能，回到已装填状态 */
	private void cancelCharging(ItemStack stack, Player player) {
		if (isCharging(stack)) {
			setCharging(stack, false);
			setChargeProgress(stack, 0);
			setLoaded(stack, true);
			setAnim(stack, ANIM_LOAD_IDLE);
			player.level().playSound(null, player.blockPosition(),
					SoundEvents.CROSSBOW_LOADING_START,
					SoundSource.PLAYERS, 0.3F, 1.5F);
		}
	}

	// ==================== 射击阶段 ====================

	private void shoot(ItemStack stack, Player player) {
		if (!isCharged(stack)) return;

		setShoot(stack, true);
		setAnim(stack, ANIM_SHOOT);

		// 减少剩余射击次数
		int remaining = getRemainingShots(stack);
		if (remaining > 0) {
			setRemainingShots(stack, remaining - 1);
		}

		// 检查是否还有剩余射击次数
		if (getRemainingShots(stack) <= 0) {
			// 没有剩余射击次数，重置所有状态
			setCharged(stack, false);
			setCharging(stack, false);
			setLoaded(stack, false);
			setLoading(stack, false);
			setLoadProgress(stack, 0);
			setChargeProgress(stack, 0);
		}

		player.level().playSound(null, player.blockPosition(),
				SOUND_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

		if (player.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 1.0, player.getZ(),
					3, 0.5, 0.5, 0.5, 0.1);
		}

		AbstractArrow projectile = new PegasusBowgunProjectile(player.level(), player, BULLET_DAMAGE);
		projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
				0.0F, PROJECTILE_SPEED, 1.0F);

		// 微调子弹生成位置：从玩家眼睛向右偏移，模拟弩的枪口位置
		Vec3 look = player.getLookAngle();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		projectile.setPos(
				projectile.getX() + right.x * 0.2,
				projectile.getY() + 0.05,
				projectile.getZ() + right.z * 0.2
		);

		setShoot(stack, false);
		player.level().addFreshEntity(projectile);

		// 如果弹药用尽，取消自瞄
		if (getRemainingShots(stack) <= 0 && player instanceof ServerPlayer serverPlayer) {
			NetworkHandler.sendToPlayer(new StopAutoAimPacket(), serverPlayer);
		}
	}

	// ==================== 物品交互 ====================

	/**
	 * 右键交互（客户端+服务端均执行）
	 *
	 * 状态机：
	 *   空闲 → 右键 → 装填（UseAnim.CROSSBOW，需长按）
	 *   装填完成(15tick) → 已装填（CROSSBOW 自然关闭，松手）
	 *   已装填 → 再次右键 → 充能（点击即忘，无需长按）
	 *   充能完成(15tick) → 已充能（自动瞄准）
	 *   已充能 → 右键 → 射击（最多5连发）
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		// 充能中 → 拒绝操作
		if (isCharging(stack)) {
			return InteractionResultHolder.fail(stack);
		}

		if (isCharged(stack)) {
			// 已充能 → 射击（仅服务端执行逻辑）
			if (!level.isClientSide) {
				shoot(stack, player);
			}
			return InteractionResultHolder.success(stack);
		}

		if (isLoaded(stack) && !isCharging(stack) && !isCharged(stack)) {
			// 已装填 → 开始充能（点击即忘，不 startUsingItem，无需长按）
			if (!level.isClientSide) {
				startCharging(stack, player);
			}
			return InteractionResultHolder.consume(stack);
		}

		if (!isLoading(stack) && !isCharging(stack) && !isCharged(stack) && !isLoaded(stack)) {
			// 空闲 → 开始装填（需长按，显示 CROSSBOW 动画）
			if (!level.isClientSide) {
				startLoading(stack, player);
			}
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}

		return InteractionResultHolder.consume(stack);
	}

	/**
	 * 松手释放（仅装填阶段有效，因为只有装填调用了 startUsingItem）
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player) || level.isClientSide) return;

		int useDuration = this.getUseDuration(stack) - timeLeft;

		// 装填中 → 提前松手则取消
		if (isLoading(stack)) {
			if (useDuration < LOAD_DURATION) {
				cancelLoading(stack, player);
			}
			return;
		}
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public @NotNull UseAnim getUseAnimation(ItemStack stack) {
		return isLoading(stack) ? UseAnim.CROSSBOW : UseAnim.NONE;
	}

	// ==================== 每 tick 更新 ====================

	/**
	 * 每 tick 调用（服务端跟踪进度，双端同步动画）
	 * 网络包每 5 tick 发送一次以减少带宽
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
		if (!level.isClientSide && entity instanceof Player player) {
			// ---- 装填进度 ----
			if (isLoading(stack)) {
				int progress = getLoadProgress(stack);
				if (progress < LOAD_DURATION) {
					setLoadProgress(stack, progress + 1);

					if (progress == LOAD_DURATION / 2) {
						level.playSound(null, player.blockPosition(),
								SoundEvents.CROSSBOW_LOADING_MIDDLE,
								SoundSource.PLAYERS, 0.5F, 1.0F);
					}

					if (progress % 5 == 0 && player instanceof ServerPlayer sp) {
						NetworkHandler.sendToTrackingPlayers(
								new UpdateItemDataPacket(slot, stack.getTag()), sp);
					}
				} else {
					finishLoading(stack, player);
					if (player instanceof ServerPlayer sp) {
						NetworkHandler.sendToTrackingPlayers(
								new UpdateItemDataPacket(slot, stack.getTag()), sp);
					}
				}
			}

			// ---- 充能进度 ----
			if (isCharging(stack)) {
				int progress = getChargeProgress(stack);
				if (progress < CHARGE_DURATION) {
					setChargeProgress(stack, progress + 1);

					if (progress % 5 == 0 && player instanceof ServerPlayer sp) {
						NetworkHandler.sendToTrackingPlayers(
								new UpdateItemDataPacket(slot, stack.getTag()), sp);
					}
				} else {
					finishCharging(stack, player);
					if (player instanceof ServerPlayer sp) {
						NetworkHandler.sendToTrackingPlayers(
								new UpdateItemDataPacket(slot, stack.getTag()), sp);
					}
				}
			}
		}

		// 双端均更新动画状态
		updateAnimationState(stack);
	}

	// ==================== 工具提示 ====================

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		if (isCharged(stack)) {
			int remaining = getRemainingShots(stack);
			tooltip.add(Component.literal("§a已充能完成 - 剩余射击: §e" + remaining + "/" + MAX_SHOTS));
		} else if (isCharging(stack)) {
			float pct = (float) getChargeProgress(stack) / CHARGE_DURATION * 100;
			tooltip.add(Component.literal("§6充能中: §e" + (int) pct + "%"));
		} else if (isLoaded(stack)) {
			tooltip.add(Component.literal("§e已装填，右键开始充能"));
		} else if (isLoading(stack)) {
			float pct = (float) getLoadProgress(stack) / LOAD_DURATION * 100;
			tooltip.add(Component.literal("§7装填中: §e" + (int) pct + "%"));
		} else {
			tooltip.add(Component.literal("§7右键装填 - §6§l升华天马弩"));
		}

		// 升华形态的独特描述
		tooltip.add(Component.literal("§6§l升华天马弩"));
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		super.initializeClient(consumer);
		consumer.accept(new IClientItemExtensions() {
			private final BlockEntityWithoutLevelRenderer renderer = new RisingPegasusCrossbowItemRenderer();
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}
}