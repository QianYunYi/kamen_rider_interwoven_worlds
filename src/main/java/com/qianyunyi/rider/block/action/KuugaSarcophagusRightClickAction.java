package com.qianyunyi.rider.block.action;

import com.qianyunyi.rider.block.entity.KuugaSarcophagusTileEntity;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGameManager;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.network.AddonWorldData;

import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class KuugaSarcophagusRightClickAction {
    // 存储活动任务ID
    private static final Set<UUID> activeTaskIds = ConcurrentHashMap.newKeySet();

    // 任务添加器
    private static final Consumer<UUID> taskAdder = taskId -> {
        if (taskId != null) {
            activeTaskIds.add(taskId);
        }
    };

    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        ItemStack mainHandItem = entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;

        // 如果玩家主手是空的
        if (mainHandItem.isEmpty()) {
            handleEmptyHandInteraction(world, pos, entity);
        }
    }

    /**
     * 处理空手与石棺的交互
     */
    private static void handleEmptyHandInteraction(LevelAccessor world, BlockPos pos, Entity entity) {
        // 获取BlockEntity
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof KuugaSarcophagusTileEntity tileEntity)) return;

        // 从TileEntity的持久化NBT中读取值
        boolean isOpen = tileEntity.isOpen();
        boolean hasArcle = tileEntity.hasArcle();

        if (!isOpen) {
            // 如果石棺是关闭的，则打开它
            tileEntity.setOpen(true);

            // ★ 通知基基鲁游戏管理器：石棺已打开（延时启动游戏阶段）
            if (world instanceof ServerLevel serverLevel) {
                GurongiGameManager manager = GurongiGameManager.get(serverLevel);
                manager.onSarcophagusOpened(serverLevel, (int) serverLevel.getGameTime());
            }
        } else {
            if (!hasArcle) {
                // 如果石棺没有Arcle，则设置为true并给予玩家石化Arcle物品
                tileEntity.setArcle(true);
                givePetrifyArcle((LivingEntity) entity);

                // 添加ArcleObtainProcedure的逻辑
                if (entity instanceof ServerPlayer serverPlayer) {
                    // 第一条消息
                    TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                            Component.literal("指尖传来").withStyle(ChatFormatting.GRAY),
                            Component.literal("远古的").withStyle(ChatFormatting.DARK_GRAY),
                            Component.literal("悸动").withStyle(ChatFormatting.RED),
                            Component.literal("。").withStyle(ChatFormatting.GRAY)
                    );

                    // 计算第一条消息的总显示时间
                    int firstMessageDelay = calculateTotalDelay(
                            "指尖传来", "远古的", "悸动", "。"
                    );

                    // 延迟后显示第二条消息
                    taskAdder.accept(DelayedTask.run(serverPlayer.level(), firstMessageDelay + 20, level -> {
                        TitleUtils.sendPhasedTitleMessage(serverPlayer, taskAdder,
                                Component.literal("沉睡的").withStyle(ChatFormatting.DARK_GRAY),
                                Component.literal("不只是").withStyle(ChatFormatting.GRAY),
                                Component.literal("力量").withStyle(ChatFormatting.WHITE),
                                Component.literal("，还有与之相随的").withStyle(ChatFormatting.GRAY),
                                Component.literal("黑暗").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)
                        );
                    }));
                }
            } else {
                // 如果石棺已经取走Arcle，则关闭石棺
                tileEntity.setOpen(false);
            }
        }
    }



    /**
     * 给予玩家石化Arcle物品
     */
    private static void givePetrifyArcle(LivingEntity entity) {
        ItemStack arcleStack = new ItemStack(KamenRiderInterwovenWorldsModItems.PETRIFY_ARCLE_ITEM.get());
        arcleStack.setCount(1);
        entity.setItemInHand(InteractionHand.MAIN_HAND, arcleStack);

        if (entity instanceof Player player) {
            player.getInventory().setChanged();
        }
    }



    /**
     * 检查玩家是否持有空白卡片
     */
    private static boolean isHoldingBlankCard(ItemStack itemStack) {
        return ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString().equals("kamen_rider:blankcard");
    }

    /**
     * 检查玩家是否持有空白手表
     */
    private static boolean isHoldingRideWatch(ItemStack itemStack) {
        return ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString().equals("rider_expansion:ride_watch");
    }


    /**
     * 计算消息的总显示时间
     */
    private static int calculateTotalDelay(String... phrases) {
        int totalDelay = 0;
        for (String phrase : phrases) {
            totalDelay += phrase.length() * TitleUtils.DEFAULT_TYPEWRITER_DELAY;
            totalDelay += TitleUtils.DEFAULT_TYPEWRITER_DELAY * 3;
        }
        return totalDelay;
    }
}