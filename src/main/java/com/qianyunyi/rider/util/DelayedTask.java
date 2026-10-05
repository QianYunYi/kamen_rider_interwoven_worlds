package com.qianyunyi.rider.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 延迟任务类
 */
public class DelayedTask {
    private static final Map<ServerLevel, Queue<Task>> TASK_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, Task> ACTIVE_TASKS = new ConcurrentHashMap<>();
    private static final AtomicLong TASK_ID_GENERATOR = new AtomicLong();
    private static boolean handlerRegistered = false;

    static {
        registerHandler();
    }

    // 注册事件处理程序
    private static void registerHandler() {
        if (!handlerRegistered) {
            MinecraftForge.EVENT_BUS.register(new DelayedTaskHandler());
            MinecraftForge.EVENT_BUS.register(new WorldUnloadHandler());
            handlerRegistered = true;
        }
    }

    /**
     * 创建任务链构建器
     */
    public static ChainBuilder chain(Level level) {
        return new ChainBuilder(level);
    }

    /**
     * 运行延迟任务
     * @return 任务ID（可用于取消）
     */
    public static UUID run(Level level, int delayTicks, Runnable task) {
        return run(level, delayTicks, (l) -> task.run());
    }

    /**
     * 运行延迟任务
     * @return 任务ID（可用于取消）
     */
    public static UUID run(Level level, int delayTicks, Consumer<Level> task) {
        // 确保只在服务端调度任务
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        UUID taskId = new UUID(TASK_ID_GENERATOR.incrementAndGet(), 0);
        Task newTask = new Task(taskId, delayTicks, task);

        TASK_MAP.computeIfAbsent(serverLevel, k -> new ConcurrentLinkedQueue<>())
                .add(newTask);
        ACTIVE_TASKS.put(taskId, newTask);

        return taskId;
    }

    /**
     * 取消指定任务
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public static boolean cancel(UUID taskId) {
        if (taskId == null) return false;

        Task task = ACTIVE_TASKS.remove(taskId);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    /**
     * 取消所有任务
     */
    public static void cancelAll() {
        ACTIVE_TASKS.values().forEach(Task::cancel);
        ACTIVE_TASKS.clear();
        TASK_MAP.values().forEach(Queue::clear);
    }

    /**
     * 添加重复任务（无限次数）
     */
    public static UUID repeat(Level level, int interval, Consumer<Level> task) {
        return repeat(level, interval, Integer.MAX_VALUE, task, null);
    }

    /**
     * 添加重复任务（指定次数）
     */
    public static UUID repeat(Level level, int interval, int repetitions, Consumer<Level> task) {
        return repeat(level, interval, repetitions, task, null);
    }

    /**
     * 添加重复任务（指定次数 + 完成回调）
     * @param level 服务器等级
     * @param interval 间隔 ticks
     * @param repetitions 重复次数
     * @param task 任务
     * @param onComplete 完成回调
     * @return 任务ID（可用于取消）
     */
    public static UUID repeat(Level level, int interval, int repetitions, Consumer<Level> task, Consumer<Level> onComplete) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        UUID taskId = new UUID(TASK_ID_GENERATOR.incrementAndGet(), 0);
        UUID[] subTaskId = new UUID[1];
        boolean[] cancelled = new boolean[1];

        Consumer<Level> repeatedTask = new Consumer<Level>() {
            int count = 0;

            @Override
            public void accept(Level l) {
                if (cancelled[0]) {
                    ACTIVE_TASKS.remove(taskId);
                    return;
                }
                if (count >= repetitions) {
                    if (onComplete != null) {
                        onComplete.accept(l);
                    }
                    ACTIVE_TASKS.remove(taskId);
                    return;
                }

                task.accept(l);
                count++;

                if (!cancelled[0]) {
                    subTaskId[0] = DelayedTask.run(l, interval, this);
                }
            }
        };

        subTaskId[0] = DelayedTask.run(level, interval, repeatedTask);
        ACTIVE_TASKS.put(taskId, new Task(taskId, repetitions, task) {
            @Override
            public void cancel() {
                cancelled[0] = true;
                if (subTaskId[0] != null) {
                    DelayedTask.cancel(subTaskId[0]);
                }
                super.cancel();
            }
        });

        return taskId;
    }

    private static class Task {
        private final UUID id;
        private int ticksRemaining;
        private final Consumer<Level> action;
        private boolean isCancelled = false;

        public Task(UUID id, int delay, Consumer<Level> action) {
            this.id = id;
            this.ticksRemaining = delay;
            this.action = action;
        }

        public boolean tick(ServerLevel level) {
            if (isCancelled) {
                return true;
            }

            if (--ticksRemaining <= 0) {
                action.accept(level);
                ACTIVE_TASKS.remove(id);
                return true;
            }
            return false;
        }

        public void cancel() {
            this.isCancelled = true;
            ACTIVE_TASKS.remove(id);
        }
    }

    private static class DelayedTaskHandler {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                for (Map.Entry<ServerLevel, Queue<Task>> entry : TASK_MAP.entrySet()) {
                    ServerLevel level = entry.getKey();
                    Queue<Task> tasks = entry.getValue();

                    tasks.removeIf(task -> task.tick(level));
                }
            }
        }
    }

    private static class WorldUnloadHandler {
        @SubscribeEvent
        public void onWorldUnload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                // 取消该世界的所有任务
                TASK_MAP.getOrDefault(serverLevel, new ConcurrentLinkedQueue<>())
                        .forEach(Task::cancel);
                TASK_MAP.remove(serverLevel);
            }
        }
    }

    /**
     * 链式任务构建器
     */
    public static class ChainBuilder {
        private final Level level;
        private final List<TaskDefinition> tasks = new ArrayList<>();
        private UUID chainId;
        private Consumer<Level> onComplete;
        private Consumer<Level> onCancel;
        private UUID currentTaskId;

        public ChainBuilder(Level level) {
            this.level = level;
        }

        public ChainBuilder then(int delayTicks, Runnable task) {
            return then(delayTicks, l -> task.run());
        }

        public ChainBuilder then(int delayTicks, Consumer<Level> task) {
            tasks.add(new TaskDefinition(delayTicks, task));
            return this;
        }

        public ChainBuilder onComplete(Runnable callback) {
            return onComplete(l -> callback.run());
        }

        public ChainBuilder onComplete(Consumer<Level> callback) {
            this.onComplete = callback;
            return this;
        }

        public ChainBuilder onCancel(Runnable callback) {
            return onCancel(l -> callback.run());
        }

        public ChainBuilder onCancel(Consumer<Level> callback) {
            this.onCancel = callback;
            return this;
        }

        public UUID start() {
            if (tasks.isEmpty()) {
                return null;
            }
            chainId = UUID.randomUUID();
            ACTIVE_TASKS.put(chainId, new Task(chainId, 0, l -> {})); // 占位任务
            scheduleNextTask(0);
            return chainId;
        }

        private void scheduleNextTask(int index) {
            if (index >= tasks.size()) {
                if (onComplete != null) onComplete.accept(level);
                ACTIVE_TASKS.remove(chainId);
                return;
            }

            TaskDefinition taskDef = tasks.get(index);
            currentTaskId = run(level, taskDef.delayTicks, l -> {
                taskDef.task.accept(l);
                scheduleNextTask(index + 1);
            });
        }

        public void cancel() {
            if (currentTaskId != null) {
                DelayedTask.cancel(currentTaskId);
            }
            if (onCancel != null) {
                // 正确传递 level 参数
                onCancel.accept(level);
            }
            if (chainId != null) {
                ACTIVE_TASKS.remove(chainId);
            }
        }
    }

    private static class TaskDefinition {
        final int delayTicks;
        final Consumer<Level> task;

        TaskDefinition(int delayTicks, Consumer<Level> task) {
            this.delayTicks = delayTicks;
            this.task = task;
        }
    }
}