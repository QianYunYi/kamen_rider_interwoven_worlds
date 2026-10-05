
package com.qianyunyi.rider.network;

import com.qianyunyi.rider.block.ShockerOperatingTableBlock;
import com.qianyunyi.rider.block.action.ShockerOperatingTableRightClickAction;
import com.qianyunyi.rider.block.entity.ShockerOperatingTableTileEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

import com.qianyunyi.rider.world.inventory.SurgicalConsoleInterfaceMenu;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SurgicalConsoleInterfaceButtonMessage {
	private final int buttonID, x, y, z;

	public SurgicalConsoleInterfaceButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
	}

	public SurgicalConsoleInterfaceButtonMessage(int buttonID, int x, int y, int z) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static void buffer(SurgicalConsoleInterfaceButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}

	public static void handler(SurgicalConsoleInterfaceButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
            if (entity != null) {
                handleButtonAction(entity, buttonID, x, y, z);
            }
        });
		context.setPacketHandled(true);
	}

	/**
	 * 处理按钮点击
	 * buttonID 0 = 改造按钮
	 */
	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();
		BlockPos pos = new BlockPos(x, y, z);

		// 安全检查
		if (!world.hasChunkAt(pos)) return;
		if (!(entity instanceof ServerPlayer serverPlayer)) return;

		// 获取玩家当前打开的菜单
		AbstractContainerMenu menu = serverPlayer.containerMenu;

		if (!(menu instanceof SurgicalConsoleInterfaceMenu surgicalMenu)) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[网络包] 玩家当前菜单不是 SurgicalConsoleInterfaceMenu，实际是: {}",
					menu.getClass().getName());
			return;
		}

		if (buttonID != 0) return; // 只处理改造按钮

		// ========== 核心修改：确保改造的是手术台上绑定的玩家 ==========

		// 1. 先检查手术台是否已有绑定玩家
		Player boundPlayer = ShockerOperatingTableRightClickAction.getPlayerOnTable(pos);

		if (boundPlayer == null) {
			// 手术台没有绑定玩家，将当前点击按钮的玩家绑定上去
			KamenRiderInterwovenWorldsMod.LOGGER.info("[网络包] 手术台未绑定玩家，将点击者 {} 绑定到手术台", serverPlayer.getName().getString());

			// 检查点击者自己是否已经在其他手术台上
			if (ShockerOperatingTableRightClickAction.isPlayerOnAnyTable(serverPlayer)) {
				KamenRiderInterwovenWorldsMod.LOGGER.warn("[网络包] 玩家 {} 已经在其他手术台上，无法绑定",
						serverPlayer.getName().getString());
				return;
			}

			// 检查方块状态
			if (world.getBlockState(pos).getBlock() instanceof ShockerOperatingTableBlock tableBlock) {
				// 设置方块为占用状态
				world.setBlock(pos, world.getBlockState(pos)
						.setValue(ShockerOperatingTableBlock.OCCUPIED, true)
						.setValue(ShockerOperatingTableBlock.ANIMATION, 1), 3);
			}

			// 绑定玩家到手术台（复用已有的绑定逻辑）
			ShockerOperatingTableRightClickAction.bindPlayerToTable(pos, serverPlayer);

			// 设置 TileEntity 数据
			if (world.getBlockEntity(pos) instanceof ShockerOperatingTableTileEntity tile) {
				tile.setSleepingPlayer(serverPlayer);
			}

			// 强制玩家躺到手术台上
			forcePlayerLieOnTable(serverPlayer, pos);

			boundPlayer = serverPlayer;
		}

		// 2. 确保绑定玩家有效（在线、同维度等）
		if (!(boundPlayer instanceof ServerPlayer targetPlayer) || !targetPlayer.isAlive()) {
			KamenRiderInterwovenWorldsMod.LOGGER.warn("[网络包] 绑定的玩家无效或已离线");
			return;
		}

		// 3. 将 surgicalMenu 的目标玩家改为绑定玩家，然后执行改造
		KamenRiderInterwovenWorldsMod.LOGGER.info("[网络包] 开始改造绑定玩家: {}", targetPlayer.getName().getString());

		// 关键：需要让 menu 知道要改造的是哪个玩家
		// 修改 SurgicalConsoleInterfaceMenu 添加一个设置目标玩家的方法
		surgicalMenu.setTargetPlayer(targetPlayer);
		surgicalMenu.performSurgery();
	}

	/**
	 * 强制玩家躺到手术台上（简化版，不重复设置姿态）
	 */
	private static void forcePlayerLieOnTable(ServerPlayer player, BlockPos pos) {
		player.teleportTo(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
		player.setYRot(0);
		player.setXRot(0);
		player.setDeltaMovement(0, 0, 0);
		player.hurtMarked = true;
		player.hasImpulse = true;
		player.fallDistance = 0;
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		KamenRiderInterwovenWorldsMod.addNetworkMessage(SurgicalConsoleInterfaceButtonMessage.class, SurgicalConsoleInterfaceButtonMessage::buffer, SurgicalConsoleInterfaceButtonMessage::new, SurgicalConsoleInterfaceButtonMessage::handler);
	}
}
