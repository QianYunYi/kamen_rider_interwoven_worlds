package com.qianyunyi.rider.block.action;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.block.ShockerOperatingTableBlock;
import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ShockerOperatingTableRightClickAction {

	// 用于存储玩家在改造台上的信息
	private static final Map<BlockPos, Player> PLAYERS_ON_TABLE = new HashMap<>();
	// 用于存储玩家在改造台上的位置信息
	private static final Map<Player, BlockPos> PLAYER_TO_TABLE = new HashMap<>();

	static {
		MinecraftForge.EVENT_BUS.register(ShockerOperatingTableRightClickAction.class);
	}

	public static void bindPlayerToTable(BlockPos pos, Player player) {
		// 绑定前先解绑该玩家可能存在的旧绑定
		unbindPlayerFromTable(player);

		PLAYERS_ON_TABLE.put(pos, player);
		PLAYER_TO_TABLE.put(player, pos);
		TitleUtils.sendTitleMessage((ServerPlayer) player, "你已躺上改造台，任意移动键位离开。", ChatFormatting.GREEN);
	}

	public static void unbindPlayerFromTable(Player player) {
		BlockPos pos = PLAYER_TO_TABLE.remove(player);
		if (pos != null) {
			PLAYERS_ON_TABLE.remove(pos);
		}
	}

	/**
	 * 强制让玩家离开改造台（恢复站立状态）
	 */
	public static void forceWakeUp(Player player) {
		if (!PLAYER_TO_TABLE.containsKey(player)) return;

		BlockPos pos = PLAYER_TO_TABLE.get(player);

		// 停止动画
		HenshinUtil.sendAnimationMessage(player, "get_up");

		// 放置玩家到改造台旁边
		BlockPos wakeUpPos = pos.above();
		player.teleportTo(wakeUpPos.getX() + 0.5, wakeUpPos.getY() + 1.0, wakeUpPos.getZ() + 0.5);
		player.setDeltaMovement(0, 0, 0);

		// 恢复站立姿态
		player.setPose(Pose.STANDING);

		// 重置方块状态
		if (!player.level().isClientSide()) {
			player.level().setBlock(pos,
					player.level().getBlockState(pos)
							.setValue(ShockerOperatingTableBlock.ANIMATION, 0)
							.setValue(ShockerOperatingTableBlock.OCCUPIED, false),
					3
			);

			BlockEntity be = player.level().getBlockEntity(pos);
			if (be instanceof ShockerOperatingTableTileEntity tile) {
				//tile.setSleepingPlayer(null);
			}
		}

		unbindPlayerFromTable(player);
		TitleUtils.sendTitleMessage((ServerPlayer) player, "已离开改造台", ChatFormatting.YELLOW);
	}

	/**
	 * 每 tick 检查：保持躺姿
	 */
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		if (event.player.level().isClientSide()) return;

		Player player = event.player;
		if (!PLAYER_TO_TABLE.containsKey(player)) return;

		BlockPos pos = PLAYER_TO_TABLE.get(player);

		// 检查改造台是否还存在
		if (!(player.level().getBlockState(pos).getBlock() instanceof ShockerOperatingTableBlock)) {
			forceWakeUp(player);
			return;
		}

		// ====== 固定姿态 + 禁止移动 ======
		player.setDeltaMovement(0, 0, 0);
		player.hurtMarked = true;
		player.hasImpulse = true;
		player.fallDistance = 0;
		player.setYRot(0);
		player.setXRot(0);

		HenshinUtil.sendAnimationMessage(player, "sleep");

		// ====== 修复：允许小范围浮动，只在玩家主动跑远时才踢走 ======
		double expectedX = pos.getX() + 0.5;
		double expectedY = pos.getY() + 0.8;
		double expectedZ = pos.getZ() + 0.5;

		double dx = player.getX() - expectedX;
		double dz = player.getZ() - expectedZ;
		double distSq = dx * dx + dz * dz;

		// 玩家主动跑远 > 0.1 格才离开
		if (distSq > 0.1) {
			forceWakeUp(player);
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.connection.send(new ClientboundTeleportEntityPacket(player));
			}
		}
	}

	// ========== 新增：玩家退出游戏时清除绑定 ==========
	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (PLAYER_TO_TABLE.containsKey(player)) {
			KamenRiderInterwovenWorldsMod.LOGGER.info("[改造台] 玩家 {} 退出游戏，清除手术台绑定", player.getName().getString());

			BlockPos pos = PLAYER_TO_TABLE.get(player);

			// 重置方块状态
			if (!player.level().isClientSide()) {
				if (player.level().getBlockState(pos).getBlock() instanceof ShockerOperatingTableBlock) {
					player.level().setBlock(pos,
							player.level().getBlockState(pos)
									.setValue(ShockerOperatingTableBlock.ANIMATION, 0)
									.setValue(ShockerOperatingTableBlock.OCCUPIED, false),
							3
					);
				}

				BlockEntity be = player.level().getBlockEntity(pos);
				if (be instanceof ShockerOperatingTableTileEntity tile) {
					//tile.setSleepingPlayer(null);
				}
			}

			// 清除绑定
			unbindPlayerFromTable(player);
		}
	}

	// ========== 新增：玩家死亡时清除绑定 ==========
	@SubscribeEvent
	public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;

		if (PLAYER_TO_TABLE.containsKey(player)) {
			KamenRiderInterwovenWorldsMod.LOGGER.info("[改造台] 玩家 {} 死亡，清除手术台绑定", player.getName().getString());

			BlockPos pos = PLAYER_TO_TABLE.get(player);

			if (!player.level().isClientSide()) {
				if (player.level().getBlockState(pos).getBlock() instanceof ShockerOperatingTableBlock) {
					player.level().setBlock(pos,
							player.level().getBlockState(pos)
									.setValue(ShockerOperatingTableBlock.ANIMATION, 0)
									.setValue(ShockerOperatingTableBlock.OCCUPIED, false),
							3
					);
				}

				BlockEntity be = player.level().getBlockEntity(pos);
				if (be instanceof ShockerOperatingTableTileEntity tile) {
					//tile.setSleepingPlayer(null);
				}
			}

			unbindPlayerFromTable(player);
		}
	}

	public static Player getPlayerOnTable(BlockPos pos) {
		return PLAYERS_ON_TABLE.get(pos);
	}

	public static boolean isTableOccupied(BlockPos pos) {
		return PLAYERS_ON_TABLE.containsKey(pos);
	}

	public static boolean isPlayerOnAnyTable(Player player) {
		return PLAYER_TO_TABLE.containsKey(player);
	}
}