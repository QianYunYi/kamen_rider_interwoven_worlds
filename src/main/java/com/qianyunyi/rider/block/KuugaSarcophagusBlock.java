package com.qianyunyi.rider.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.material.FluidState;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Collections;

import com.qianyunyi.rider.block.entity.KuugaSarcophagusTileEntity;
import com.qianyunyi.rider.block.action.KuugaSarcophagusRightClickAction;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlockEntities;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModBlocks;

public class KuugaSarcophagusBlock extends BaseEntityBlock implements EntityBlock {
	public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, (int) 1);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public KuugaSarcophagusBlock() {
		super(BlockBehaviour.Properties.of()

				.sound(SoundType.STONE).strength(2f, 10f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return KamenRiderInterwovenWorldsModBlockEntities.KUUGA_SARCOPHAGUS.get().create(blockPos, blockState);
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
		builder.add(ANIMATION, FACING);
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
	public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
		if (player.getInventory().getSelected().getItem() instanceof TieredItem tieredItem)
			return tieredItem.getTier().getLevel() >= 3;
		return false;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		LootParams params = builder.create(LootContextParamSets.BLOCK);
		BlockEntity blockEntity = params.getParamOrNull(LootContextParams.BLOCK_ENTITY);

		ItemStack drop = new ItemStack(this, 1);

		if (blockEntity instanceof KuugaSarcophagusTileEntity tileEntity) {
			CompoundTag blockEntityTag = tileEntity.getPersistentDataForItem();
			CompoundTag blockEntityNbt = new CompoundTag();
			blockEntityNbt.put("persistent_data", blockEntityTag);
			drop.getOrCreateTag().put("BlockEntityTag", blockEntityNbt);
		}

		return Collections.singletonList(drop);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid) {
		boolean retval = super.onDestroyedByPlayer(blockstate, world, pos, entity, willHarvest, fluid);
		// ArcleObtainProcedure.execute(entity);
		return retval;
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state,
							@Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);

		if (world.getBlockEntity(pos) instanceof KuugaSarcophagusTileEntity tileEntity) {
			if (stack.hasTag()) {
				CompoundTag itemTag = stack.getTag();
				if (itemTag != null && itemTag.contains("BlockEntityTag")) {
					CompoundTag blockEntityTag = itemTag.getCompound("BlockEntityTag");
					tileEntity.loadFromItemNBT(blockEntityTag);
				}
			}
		}
	}

	@Override
	public InteractionResult use(BlockState blockstate, Level world, BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
		super.use(blockstate, world, pos, entity, hand, hit);
		KuugaSarcophagusRightClickAction.execute(world, pos.getX(), pos.getY(), pos.getZ(), entity);
		return InteractionResult.SUCCESS;
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerRenderLayer() {
		ItemBlockRenderTypes.setRenderLayer(KamenRiderInterwovenWorldsModBlocks.KUUGA_SARCOPHAGUS.get(), renderType -> renderType == RenderType.cutout());
	}
}