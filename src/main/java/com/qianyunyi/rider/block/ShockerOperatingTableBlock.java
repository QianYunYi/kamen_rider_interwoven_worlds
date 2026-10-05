
package com.qianyunyi.rider.block;

import com.qianyunyi.rider.block.action.ShockerOperatingTableRightClickAction;
import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;
import com.qianyunyi.rider.world.inventory.SurgicalConsoleInterfaceMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Collections;

import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlockEntities;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class ShockerOperatingTableBlock extends BaseEntityBlock implements EntityBlock {
	public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, 1);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");

    public ShockerOperatingTableBlock() {
		super(BlockBehaviour.Properties.of()

				.sound(SoundType.METAL).strength(1f, 10f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ANIMATION, 0).setValue(OCCUPIED, false));
	}


	// -----------------------------------------------------------------------------------------
	// 右键逻辑
	// -----------------------------------------------------------------------------------------
	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (world.isClientSide)
			return InteractionResult.SUCCESS;

		if (player.isShiftKeyDown()) {
			return tryLieOnTable(world, pos, player, state);
		}

		if (state.getValue(OCCUPIED)) {
			player.displayClientMessage(Component.literal("§c改造台正被使用中！"), true);
			return InteractionResult.SUCCESS;
		}

		NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("修卡改造控制台");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
				buf.writeBlockPos(pos);
				return new SurgicalConsoleInterfaceMenu(id, inventory, buf);
			}
		}, pos);

		return InteractionResult.SUCCESS;
	}

	// -----------------------------------------------------------------------------------------
	// 躺上改造台
	// -----------------------------------------------------------------------------------------
	private InteractionResult tryLieOnTable(Level world, BlockPos pos, Player player, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return InteractionResult.SUCCESS;

		// ========== 检查手术台是否已被其他玩家绑定 ==========
		Player existingPlayer = ShockerOperatingTableRightClickAction.getPlayerOnTable(pos);
		if (existingPlayer != null && existingPlayer != player && existingPlayer.isAlive()) {
			player.displayClientMessage(Component.literal("§c改造台已被 " + existingPlayer.getName().getString() + " 占用！"), true);
			return InteractionResult.SUCCESS;
		}


		if (state.getValue(OCCUPIED)) {
			player.displayClientMessage(Component.literal("§c改造台已被占用！"), true);
			return InteractionResult.SUCCESS;
		}

		if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				|| !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				|| !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				|| !player.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
			player.displayClientMessage(Component.literal("§c请先脱下所有装备再躺上手术台！"), true);
			return InteractionResult.SUCCESS;
		}

		// 强制躺到改造台上 - 只在这里设置一次，不在tick里重复设置
		forceLieOnTable(serverPlayer, pos);

		// 设置方块状态
		world.setBlock(pos, state.setValue(OCCUPIED, true).setValue(ANIMATION, 1), 3);

		// 绑定玩家到改造台
		ShockerOperatingTableRightClickAction.bindPlayerToTable(pos, player);

		// 设置 TileEntity 数据
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof ShockerOperatingTableTileEntity tile) {
			tile.setSleepingPlayer(player);
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * 强制躺姿 - 关键：只调用一次，不在tick里重复
	 */
	private void forceLieOnTable(ServerPlayer player, BlockPos pos) {
		try {
			// 精准设置位置 + 同步客户端
			player.teleportTo(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
			player.setYRot(0);
			player.setXRot(0);

			// 停止所有运动
			player.setDeltaMovement(0, 0, 0);
			player.hurtMarked = true;
			player.hasImpulse = true;
			player.fallDistance = 0;

			// 强制同步位置到客户端
			player.connection.send(new ClientboundTeleportEntityPacket(player));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return KamenRiderInterwovenWorldsModBlockEntities.SHOCKER_OPERATING_TABLE.get().create(blockPos, blockState);
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ANIMATION, FACING, OCCUPIED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}
}