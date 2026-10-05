package com.qianyunyi.rider.internal.surgery;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.block.ShockerOperatingTableBlock;
import com.qianyunyi.rider.block.action.ShockerOperatingTableRightClickAction;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModItems;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.TitleUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 改造手术事件处理器
 * 使用 @Mod.EventBusSubscriber 确保自动注册到 Forge 事件总线
 */
@Mod.EventBusSubscriber(modid = KamenRiderInterwovenWorldsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurgeryEventHandler {

    private static final Map<UUID, SurgeryProgressData> surgeryProgressMap = new HashMap<>();

    public static class SurgeryProgressData {
        public final BlockPos tablePos;
        public final int totalTicks;
        public int currentTick = 0;
        public boolean isCompleted = false;
        public final SurgeryRecipe recipe;
        public final ServerPlayer player;

        public SurgeryProgressData(ServerPlayer player, BlockPos tablePos, int totalTicks, SurgeryRecipe recipe) {
            this.player = player;
            this.tablePos = tablePos;
            this.totalTicks = totalTicks;
            this.recipe = recipe;
        }
    }

    @SubscribeEvent
    public static void onSurgeryStart(SurgeryEvent.Start event) {
        ServerPlayer player = event.getPlayer();
        SurgeryRecipe recipe = event.getRecipe();
        var menu = event.getMenu();

        KamenRiderInterwovenWorldsMod.LOGGER.info("[改造事件] ========== onSurgeryStart 被调用 ==========");
        KamenRiderInterwovenWorldsMod.LOGGER.info("[改造事件] 玩家: {}", player.getName().getString());

        BlockPos tablePos = new BlockPos(menu.x, menu.y, menu.z);
        LevelAccessor world = player.level();
        BlockState blockState = world.getBlockState(tablePos);

        if (!(blockState.getBlock() instanceof ShockerOperatingTableBlock)) {
            tablePos = player.blockPosition();
        }

        // 绑定束缚装备
        player.setItemSlot(EquipmentSlot.CHEST,
                new ItemStack(KamenRiderInterwovenWorldsModItems.FULL_BODY_RESTRAINTS_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS,
                new ItemStack(KamenRiderInterwovenWorldsModItems.FULL_BODY_RESTRAINTS_LEGGINGS.get()));

        // 锁定位置 + 躺平
        player.setPos(tablePos.getX() + 0.5, tablePos.getY() + 0.7, tablePos.getZ() + 0.5);
        player.setYRot(0);
        player.setXRot(0);
        player.startSleeping(tablePos);

        // 锁定玩家
        player.setInvulnerable(true);
        player.noPhysics = true;
        player.getAbilities().flying = false;
        player.getAbilities().mayfly = false;
        player.setSprinting(false);
        player.setJumping(false);

        world.playSound(null, tablePos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (blockState.hasProperty(ShockerOperatingTableBlock.ANIMATION)) {
            world.setBlock(tablePos, blockState.setValue(ShockerOperatingTableBlock.ANIMATION, 1)
                    .setValue(ShockerOperatingTableBlock.OCCUPIED, true), 3);
        }

        int duration = recipe != null ? recipe.getDuration() : 200;
        SurgeryProgressData data = new SurgeryProgressData(player, tablePos, duration, recipe);
        surgeryProgressMap.put(player.getUUID(), data);

        TitleUtils.sendTitleMessage(player, "改造手术开始...", ChatFormatting.DARK_RED);
        player.displayClientMessage(Component.literal("§e改造开始...不要乱动！"), true);
    }

    /**
     * 玩家 tick 事件
     * @param event 事件
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide())
            return;

        ServerPlayer player = (ServerPlayer) event.player;
        UUID playerId = player.getUUID();

        if (!surgeryProgressMap.containsKey(playerId)) return;

        SurgeryProgressData data = surgeryProgressMap.get(playerId);
        LevelAccessor world = player.level();
        BlockPos tablePos = data.tablePos;


        // 强制锁位置
        player.setPos(tablePos.getX() + 0.5, tablePos.getY() + 0.7, tablePos.getZ() + 0.5);
        player.setYRot(0);
        player.setXRot(0);
        player.setJumping(false);
        player.setSprinting(false);
        player.startSleeping(tablePos);

        HenshinUtil.sendAnimationMessage(player, "being_modified");

        BlockState blockState = world.getBlockState(tablePos);
        boolean isTableValid = blockState.getBlock() instanceof ShockerOperatingTableBlock;
        if (!isTableValid) {
            stopSurgery(player, false);
            return;
        }

        data.currentTick++;

        if (data.currentTick % 20 == 0) {
            int seconds = data.currentTick / 20;
            int totalSeconds = data.totalTicks / 20;
            float progress = (float) data.currentTick / data.totalTicks;
            StringBuilder bar = new StringBuilder("§a[");
            int bars = (int) (progress * 20);
            for (int i = 0; i < 20; i++) bar.append(i < bars ? "█" : "░");
            bar.append("] §e").append(seconds).append("s/").append(totalSeconds).append("s");
            player.displayClientMessage(Component.literal(bar.toString()), true);
        }

        if (data.currentTick >= data.totalTicks) {
            data.isCompleted = true;
            completeSurgery(player);
            stopSurgery(player, true);
        }
    }

    /**
     * 完成改造手术
     * @param player 玩家
     */
    private static void completeSurgery(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        TitleUtils.sendTitleMessage(player, "改造完成！", ChatFormatting.GREEN);

        SurgeryProgressData data = surgeryProgressMap.get(player.getUUID());
        if (data != null && data.recipe != null) {
            MinecraftForge.EVENT_BUS.post(new SurgeryEvent.Complete(player, data.recipe, null));
        }
    }

    /**
     * 停止改造手术
     * @param player 玩家
     * @param completed 是否完成手术
     */
    public static void stopSurgery(ServerPlayer player, boolean completed) {
        UUID playerId = player.getUUID();
        SurgeryProgressData data = surgeryProgressMap.remove(playerId);
        if (data == null) return;

        LevelAccessor world = player.level();
        BlockPos tablePos = data.tablePos;

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);

        player.stopSleeping();
        player.setInvulnerable(false);
        player.noPhysics = false;
        player.getAbilities().mayfly = player.isCreative();
        player.getAbilities().flying = false;

        ShockerOperatingTableRightClickAction.forceWakeUp(player);

        BlockState blockState = world.getBlockState(tablePos);
        if (blockState.getBlock() instanceof ShockerOperatingTableBlock &&
                blockState.hasProperty(ShockerOperatingTableBlock.ANIMATION)) {
            world.setBlock(tablePos, blockState.setValue(ShockerOperatingTableBlock.ANIMATION, 0)
                    .setValue(ShockerOperatingTableBlock.OCCUPIED, false), 3);
        }

        world.playSound(null, tablePos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (completed) {
            player.displayClientMessage(Component.literal("§6§l改造完成！"), true);
        } else {
            player.displayClientMessage(Component.literal("§c改造中断"), true);
        }
    }

    /**
     * 改造手术完成事件
     * @param event 事件
     */
    @SubscribeEvent
    public static void onSurgeryComplete(SurgeryEvent.Complete event) {
        ServerPlayer player = event.getPlayer();
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
    }

    public static boolean isPlayerInSurgery(ServerPlayer player) {
        return surgeryProgressMap.containsKey(player.getUUID());
    }
}