package com.qianyunyi.rider.world.inventory;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMenus;
import com.qianyunyi.rider.init.RiderAdventureAffiliatedModTags;
import com.qianyunyi.rider.internal.surgery.SurgeryEvent;
import com.qianyunyi.rider.internal.surgery.SurgeryRecipe;
import com.qianyunyi.rider.internal.surgery.SurgeryRecipeManager;
import com.qianyunyi.rider.item.base.SurgeryBlueprintItem;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SurgicalConsoleInterfaceMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity; // 打开GUI的玩家（操作者）
	public int x, y, z;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;

	// ========== 新增：目标玩家（被改造者） ==========
	private ServerPlayer targetPlayer; // 实际被改造的玩家，默认是操作者自己

	private final DataSlot buttonState = DataSlot.standalone();
	private final DataSlot surgeryProgress = DataSlot.standalone();
	private final DataSlot canPerformSurgery = DataSlot.standalone();

	public SurgicalConsoleInterfaceMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(KamenRiderInterwovenWorldsModMenus.SURGICAL_CONSOLE_INTERFACE.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(10);

		// 默认目标玩家是打开GUI的玩家
		if (inv.player instanceof ServerPlayer sp) {
			this.targetPlayer = sp;
		}

		this.addDataSlot(buttonState);
		this.addDataSlot(surgeryProgress);
		this.addDataSlot(canPerformSurgery);

		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
		}

		// 注意：使用独立的 10 槽 ItemStackHandler（0-8: 材料槽，9: 图纸槽）
		// 不绑定 TileEntity 的 ITEM_HANDLER capability，因为 TileEntity 只有9槽会导致越界

		// 9个材料槽
		int[] gridX = {30, 48, 66};
		int[] gridY = {25, 43, 61};
		int slotIndex = 0;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				this.customSlots.put(slotIndex, this.addSlot(
						new SlotItemHandler(internal, slotIndex, gridX[col], gridY[row]) {
							@Override
							public boolean mayPlace(ItemStack stack) {
								return true;
							}
						}
				));
				slotIndex++;
			}
		}

		// 图纸槽 - 检查物品是否继承 SurgeryBlueprintItem 或带有图纸标签
		this.customSlots.put(9, this.addSlot(
				new SlotItemHandler(internal, 9, 117, 37) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return stack.getItem() instanceof SurgeryBlueprintItem
								|| stack.is(RiderAdventureAffiliatedModTags.SURGERY_BLUEPRINTS);
					}
				}
		));

		// 玩家背包
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, 8 + si * 18, 142));
	}

	// ========== 新增：设置目标玩家 ==========
	public void setTargetPlayer(ServerPlayer target) {
		this.targetPlayer = target;
		KamenRiderInterwovenWorldsMod.LOGGER.info("[改造菜单] 目标玩家已设置为: {}", target.getName().getString());
	}

	public ServerPlayer getTargetPlayer() {
		return this.targetPlayer;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public boolean checkRecipeMatch() {
		ItemStack[] materials = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			materials[i] = internal.getStackInSlot(i);
		}
		ItemStack blueprint = internal.getStackInSlot(9);

		if (blueprint.isEmpty()) {
			canPerformSurgery.set(0);
			return false;
		}

		boolean match = SurgeryRecipeManager.matches(world, materials, blueprint);
		canPerformSurgery.set(match ? 1 : 0);
		return match;
	}

	/**
	 * 触发改造手术 - 改造 targetPlayer（绑定在手术台上的玩家）
	 */
	public void performSurgery() {
		// 使用 targetPlayer 而不是 entity
		if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[改造菜单] 目标玩家无效，无法执行改造");
			return;
		}

		ServerPlayer serverPlayer = this.targetPlayer;

		if (!checkRecipeMatch()) return;

		ItemStack[] materials = new ItemStack[9];
		for (int i = 0; i < 9; i++) {
			materials[i] = internal.getStackInSlot(i);
		}
		ItemStack blueprint = internal.getStackInSlot(9);

		SurgeryRecipe recipe = SurgeryRecipeManager.getMatchingRecipe(world, materials, blueprint);
		if (recipe == null) return;

		// 检查目标玩家的装备（不是操作者的装备！）
		if (!serverPlayer.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				|| !serverPlayer.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				|| !serverPlayer.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				|| !serverPlayer.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
			TitleUtils.sendTitleMessage(serverPlayer, "§c请先脱下所有装备再进行改造！", ChatFormatting.RED);
			// 也通知操作者
			if (this.entity instanceof ServerPlayer operator && operator != serverPlayer) {
				TitleUtils.sendTitleMessage(operator, "§c目标玩家未脱装备，无法改造！", ChatFormatting.RED);
			}
			return;
		}

		// 关闭目标玩家的容器（如果打开着的话）
		serverPlayer.closeContainer();
		serverPlayer.containerMenu = serverPlayer.inventoryMenu;

		// 关闭操作者的GUI
		if (this.entity instanceof ServerPlayer operator && operator != serverPlayer) {
			operator.closeContainer();
			operator.containerMenu = operator.inventoryMenu;
		}

		buttonState.set(1);

		// 消耗材料
		for (int i = 0; i < 9; i++) {
			int cost = recipe.getMaterialCost(i);
			if (cost > 0) {
				ItemStack mat = internal.getStackInSlot(i);
				if (!mat.isEmpty()) {
					mat.shrink(cost);
					if (mat.isEmpty()) {
						internal.extractItem(i, 1, false);
					}
				}
			}
		}

		// 消耗图纸
		blueprint.shrink(1);
		if (blueprint.isEmpty()) {
			internal.extractItem(9, 1, false);
		}

		// 触发事件，传入 targetPlayer
		MinecraftForge.EVENT_BUS.post(new SurgeryEvent.Start(serverPlayer, recipe, this));
	}

	public void finishSurgery(SurgeryRecipe recipe) {
		// 同样使用 targetPlayer
		if (this.targetPlayer == null || !this.targetPlayer.isAlive()) return;

		ServerPlayer serverPlayer = this.targetPlayer;

		ItemStack result = recipe.getResultItem(serverPlayer.level().registryAccess());
		if (!result.isEmpty()) {
			serverPlayer.getInventory().placeItemBackInInventory(result);
		}

		RiderPlayerData data = RiderPlayerVariablesProvider.get(serverPlayer);

		serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
				SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);

		buttonState.set(2);
		surgeryProgress.set(100);

		DelayedTask.run(serverPlayer.level(), 40, () -> {
			buttonState.set(0);
			surgeryProgress.set(0);
		});
	}

	public int getButtonState() { return buttonState.get(); }
	public int getSurgeryProgress() { return surgeryProgress.get(); }
	public boolean canPerformSurgery() { return canPerformSurgery.get() == 1; }
	public void setSurgeryProgress(int progress) { surgeryProgress.set(progress); }

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (index < 10) {
				if (!this.moveItemStackTo(itemstack1, 10, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(itemstack1, itemstack);
			} else {
				if (itemstack1.getItem() instanceof com.qianyunyi.rider.item.base.SurgeryBlueprintItem
						|| itemstack1.is(RiderAdventureAffiliatedModTags.SURGERY_BLUEPRINTS)) {
					if (!this.moveItemStackTo(itemstack1, 9, 10, false)) {
						if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
							return ItemStack.EMPTY;
						}
					}
				} else {
					if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
						return ItemStack.EMPTY;
					}
				}
			}

			if (itemstack1.getCount() == 0) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(playerIn, itemstack1);
			checkRecipeMatch();
		}
		return itemstack;
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
			if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
				for (int j = 0; j < internal.getSlots(); ++j) {
					playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
				}
			} else {
				for (int i = 0; i < internal.getSlots(); ++i) {
					playerIn.getInventory().placeItemBackInInventory(
							internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
				}
			}
		}
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		checkRecipeMatch();
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}
}