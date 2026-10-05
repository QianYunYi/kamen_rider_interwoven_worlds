package com.qianyunyi.rider.entity.kaijin.damage;

import com.qianyunyi.rider.config.AdventureConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = "kamen_rider_interwoven_worlds")
public class SoulLikeBossBattleEvent {

    // 存储BOSS的延迟检查任务
    private static final Map<UUID, BossResetTask> bossResetTasks = new HashMap<>();

    // 存储最近死亡的玩家位置
    private static final Map<UUID, PlayerDeathInfo> recentPlayerDeaths = new HashMap<>();

    // 延迟任务队列（替代 scheduleOnMain，全版本兼容）
    private static final Queue<Runnable> delayedTasks = new LinkedList<>();
    private static int delayTimer = 0;

    /**
     * 玩家死亡信息
     */
    private static class PlayerDeathInfo {
        public final ResourceLocation dimension;
        public final double x;
        public final double y;
        public final double z;
        public final long deathTime; // 死亡时间戳（tick数）

        public PlayerDeathInfo(ServerPlayer player) {
            this.dimension = player.level().dimension().location();
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.deathTime = player.level().getGameTime();
        }

        /**
         * 检查死亡信息是否过期（30秒后过期）
         */
        public boolean isExpired(long currentTime) {
            return currentTime - deathTime > 600; // 600tick = 30秒
        }
    }

    /**
     * BOSS重置任务信息
     */
    private static class BossResetTask {
        private final UUID bossId;
        private final ResourceLocation dimension;
        private int remainingTicks;
        private final int lastX;
        private final int lastY;
        private final int lastZ;
        private final UUID playerCausedBy; // 触发此任务的玩家ID

        public BossResetTask(LivingEntity boss, UUID playerCausedBy, int delayTicks) {
            this.bossId = boss.getUUID();
            this.dimension = boss.level().dimension().location();
            this.remainingTicks = delayTicks;
            this.lastX = (int) boss.getX();
            this.lastY = (int) boss.getY();
            this.lastZ = (int) boss.getZ();
            this.playerCausedBy = playerCausedBy;
        }

        public void tick() {
            this.remainingTicks--;
        }

        public boolean isReady() {
            return this.remainingTicks <= 0;
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() &&
                AdventureConfig.SOUL_LIKE_BOSS_BATTLE_ON &&
                event.getEntity() instanceof ServerPlayer player) {

            // 记录玩家死亡信息
            recentPlayerDeaths.put(player.getUUID(), new PlayerDeathInfo(player));

            // 【修复】全版本兼容 延迟1秒(20tick) 执行检查
            Level world = player.level();
            MinecraftServer server = world.getServer();
            if (server != null) {
                addDelayedTask(() -> {
                    ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
                    if (currentPlayer == null || currentPlayer.isDeadOrDying()) {
                        checkAndScheduleBossReset(world, player);
                    }
                }, 20);
            }
        }
    }

    /**
     * 添加延迟任务（全版本通用，安全无阻塞）
     */
    private static void addDelayedTask(Runnable task, int delayTicks) {
        new Thread(() -> {
            try {
                // 毫秒延迟：delayTicks * 50（1 tick = 50ms）
                Thread.sleep(delayTicks * 50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // 延迟结束后加入主线程队列执行
            synchronized (delayedTasks) {
                delayedTasks.add(task);
            }
        }).start();
    }

    /**
     * 玩家克隆事件（真正的死亡重生）
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            Level world = player.level();
            world.getServer().execute(() -> checkAndScheduleBossReset(world, player));
        }
    }

    /**
     * 检查并安排BOSS重置
     */
    private static void checkAndScheduleBossReset(Level world, ServerPlayer player) {
        if (!validPlayergameMode(player)) {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        List<String> soulLikeBossEntityList = AdventureConfig.SOUL_LIKE_BOSS_BATTLE_ENTITY;
        if (soulLikeBossEntityList == null || soulLikeBossEntityList.isEmpty()) {
            return;
        }

        AABB searchArea = new AABB(
                playerX - AdventureConfig.BOSS_BATTLE_DISTANCE,
                playerY - 8.0D,
                playerZ - AdventureConfig.BOSS_BATTLE_DISTANCE,
                playerX + AdventureConfig.BOSS_BATTLE_DISTANCE,
                playerY + 8.0D,
                playerZ + AdventureConfig.BOSS_BATTLE_DISTANCE
        );

        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, searchArea);

        for (LivingEntity boss : nearbyEntities) {
            ResourceLocation entityRegName = EntityType.getKey(boss.getType());
            if (soulLikeBossEntityList.contains(entityRegName.toString())) {
                scheduleBossResetCheck(boss, player.getUUID());
            }
        }
    }

    /**
     * 服务器tick事件
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // 执行所有到期的延迟任务（主线程安全）
            synchronized (delayedTasks) {
                while (!delayedTasks.isEmpty()) {
                    delayedTasks.poll().run();
                }
            }

            long currentTime = server.overworld().getGameTime();
            recentPlayerDeaths.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));

            Iterator<Map.Entry<UUID, BossResetTask>> iterator = bossResetTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                BossResetTask task = iterator.next().getValue();
                task.tick();
                if (task.isReady()) {
                    iterator.remove();
                    checkAndResetBoss(task, server);
                }
            }
        }
    }

    /**
     * 为BOSS安排重置检查任务
     */
    private static void scheduleBossResetCheck(LivingEntity boss, UUID playerCausedBy) {
        UUID bossId = boss.getUUID();

        if (bossResetTasks.containsKey(bossId)) {
            BossResetTask existingTask = bossResetTasks.get(bossId);
            if (existingTask.playerCausedBy.equals(playerCausedBy)) {
                existingTask.remainingTicks = 100;
                return;
            }
        }

        BossResetTask task = new BossResetTask(boss, playerCausedBy, 100);
        bossResetTasks.put(bossId, task);
    }

    /**
     * 检查并重置BOSS状态
     */
    private static void checkAndResetBoss(BossResetTask task, MinecraftServer server) {
        Level world = getWorldByDimension(server, task.dimension);
        if (world == null) return;

        LivingEntity boss = null;
        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class,
                new AABB(task.lastX - 32, task.lastY - 32, task.lastZ - 32,
                        task.lastX + 32, task.lastY + 32, task.lastZ + 32))) {
            if (entity.getUUID().equals(task.bossId)) {
                boss = entity;
                break;
            }
        }

        if (boss == null || !boss.isAlive()) return;

        ServerPlayer causingPlayer = server.getPlayerList().getPlayer(task.playerCausedBy);
        if (causingPlayer != null &&
                causingPlayer.level().dimension().location().equals(task.dimension) &&
                causingPlayer.distanceToSqr(task.lastX, task.lastY, task.lastZ) <=
                        AdventureConfig.BOSS_BATTLE_DISTANCE * AdventureConfig.BOSS_BATTLE_DISTANCE) {
            if (validPlayergameMode(causingPlayer) && !causingPlayer.isDeadOrDying()) {
                return;
            }
        }

        AABB searchArea = new AABB(
                boss.getX() - AdventureConfig.BOSS_BATTLE_DISTANCE,
                boss.getY() - 8.0D,
                boss.getZ() - AdventureConfig.BOSS_BATTLE_DISTANCE,
                boss.getX() + AdventureConfig.BOSS_BATTLE_DISTANCE,
                boss.getY() + 8.0D,
                boss.getZ() + AdventureConfig.BOSS_BATTLE_DISTANCE
        );

        List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(ServerPlayer.class, searchArea);
        for (ServerPlayer player : nearbyPlayers) {
            if (validPlayergameMode(player) && !player.isDeadOrDying()) {
                return;
            }
        }

        // 重置BOSS
        boss.removeAllEffects();
        if (boss.getLastHurtByMobTimestamp() > 20) {
            boss.setHealth(boss.getMaxHealth());
        }
    }

    private static Level getWorldByDimension(MinecraftServer server, ResourceLocation dimension) {
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    public static boolean validPlayergameMode(ServerPlayer player) {
        GameType playerGameMode = player.gameMode.getGameModeForPlayer();
        if (AdventureConfig.PLAYER_GAME_MODE == null) return false;

        for (String gameMode : AdventureConfig.PLAYER_GAME_MODE) {
            if (gameMode == null) continue;
            switch (gameMode.toLowerCase()) {
                case "survival": if (playerGameMode == GameType.SURVIVAL) return true; break;
                case "adventure": if (playerGameMode == GameType.ADVENTURE) return true; break;
                case "creative": if (playerGameMode == GameType.CREATIVE) return true; break;
                case "spectator": if (playerGameMode == GameType.SPECTATOR) return true; break;
            }
        }
        return false;
    }
}