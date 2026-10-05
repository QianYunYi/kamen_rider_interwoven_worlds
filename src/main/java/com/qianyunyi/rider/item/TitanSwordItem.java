package com.qianyunyi.rider.item;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.combat.skill.kuuga.TitanSwordComboHandler;
import com.qianyunyi.rider.item.base.BaseSwordAnimationItem;
import com.qianyunyi.rider.item.renderer.TitanSwordItemRenderer;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import java.util.function.Consumer;

public class TitanSwordItem extends BaseSwordAnimationItem {

	public TitanSwordItem() {
		super(
				Tiers.NETHERITE,
				15,
				-2.4F,
				new Item.Properties()
						.stacksTo(1)
						.rarity(Rarity.EPIC)
		);
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return false;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			ItemStack stack = player.getItemInHand(hand);
			if (isNormalTitanSword(stack)
					&& !"idle_open".equals(BaseSwordAnimationItem.getDefaultAnim(stack))
					&& !"telescopic".equals(BaseSwordAnimationItem.getAnim(stack))) {
				openTitanSword(sp);
			}
		}
		return InteractionResultHolder.success(player.getItemInHand(hand));
	}

	private static final int TELESCOPIC_DURATION = 10; // telescopic动画时长 0.5秒=10刻

	/**
	 * 播放泰坦剑开剑音效，播放telescopic动画，结束后切换为idle_open
	 */
	private static void openTitanSword(ServerPlayer sp) {
		sp.level().playSound(null, sp.blockPosition(),
				KamenRiderInterwovenWorldsModSounds.OPEN_TITAN_SWORD.get(), SoundSource.PLAYERS, 1.0F, 1.4F);

		ItemStack mainHand = sp.getMainHandItem();
		if (isNormalTitanSword(mainHand)) {
			ItemStack openingMain = mainHand;
			HenshinUtil.equipWeaponAndPlayAnimation(sp, EquipmentSlot.MAINHAND, mainHand, "telescopic");
			DelayedTask.run(sp.level(), TELESCOPIC_DURATION, () -> {
				ItemStack currentMain = sp.getMainHandItem();
				if (currentMain == openingMain && isNormalTitanSword(currentMain)) {
					BaseSwordAnimationItem.setDefaultAnim(currentMain, "idle_open");
					HenshinUtil.equipWeaponAndPlayAnimation(sp, EquipmentSlot.MAINHAND, currentMain, "idle_open");
				}
			});
		}

		ItemStack offHand = sp.getOffhandItem();
		if (isNormalTitanSword(offHand)) {
			ItemStack openingOff = offHand;
			HenshinUtil.equipWeaponAndPlayAnimation(sp, EquipmentSlot.OFFHAND, offHand, "telescopic");
			DelayedTask.run(sp.level(), TELESCOPIC_DURATION, () -> {
				ItemStack currentOff = sp.getOffhandItem();
				if (currentOff == openingOff && isNormalTitanSword(currentOff)) {
					BaseSwordAnimationItem.setDefaultAnim(currentOff, "idle_open");
					HenshinUtil.equipWeaponAndPlayAnimation(sp, EquipmentSlot.OFFHAND, currentOff, "idle_open");
				}
			});
		}
	}

	/**
	 * 仅普通泰坦剑生效，升华/究极不生效
	 */
	private static boolean isNormalTitanSword(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof TitanSwordItem;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] hurtEnemy called, attacker={}, target={}",
				attacker, target);
		if (attacker instanceof ServerPlayer sp) {
			KamenRiderInterwovenWorldsMod.LOGGER.info("[TitanSword] hurtEnemy: calling executeCombo for {}", sp.getName().getString());
			TitanSwordComboHandler.executeCombo(sp);
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final BlockEntityWithoutLevelRenderer renderer = new TitanSwordItemRenderer();

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}
}