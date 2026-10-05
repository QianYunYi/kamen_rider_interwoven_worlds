package com.qianyunyi.rider.client.effect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.WeakReference;

public class YellowLightningRenderer {

    private static final Map<UUID, LightningState> ACTIVE_STATES = new ConcurrentHashMap<>();

    private static final float[] WHITE_HOT    = {1.00f, 1.00f, 0.98f};
    private static final float[] CORE_COLOR   = {1.00f, 0.95f, 0.45f};
    private static final float[] INNER_GLOW   = {1.00f, 0.80f, 0.10f};
    private static final float[] OUTER_GLOW   = {1.00f, 0.55f, 0.00f};
    private static final float[] FAR_GLOW     = {1.00f, 0.35f, 0.00f};
    // 绿色闪电颜色
    private static final float[] GREEN_CORE   = {0.20f, 0.95f, 0.20f};
    private static final float[] GREEN_GLOW   = {0.10f, 0.60f, 0.10f};
    private static final float[] GREEN_FAR    = {0.05f, 0.30f, 0.05f};

    private static final Random RAND = new Random();

    private static final float V_HEIGHT     = 2.8f;
    private static final float V_BASE_R     = 0.3f;
    private static final float V_TOP_R      = 1.8f;

    // 锥形闪电束参数
    private static final float BEAM_LENGTH   = 24.0f;
    private static final float CONE_ANGLE_DEG = 25.0f;

    public static class LightningState {
        public enum Phase { CHARGING, FLYING, LANDING, CHARGING_RING, INACTIVE }
        public Phase phase = Phase.INACTIVE;
        public UUID playerId;
        public WeakReference<Entity> entityRef = new WeakReference<>(null);
        public int tick;
        public long gameStartTick;
        public int duration;
        public double landX, landY, landZ;
        public double dirX, dirY, dirZ; // 发射方向（仅FLYING使用）
        public final List<BoltInstance> bolts = new ArrayList<>();
        public final List<SparkInstance> sparks = new ArrayList<>();
        public int nextBurstTick = 0;
    }

    public static class BoltInstance {
        public static final byte TYPE_MAIN = 0;         // 主闪电（最粗）
        public static final byte TYPE_THICK_BRANCH = 1; // 粗分支
        public static final byte TYPE_BRANCH = 2;       // 细分支
        public static final byte TYPE_GREEN = 3;         // 绿色外围闪电
        public static final byte TYPE_RING = 4;          // 蓄力环身闪电

        public final byte type;
        public final List<Vec3> path;
        public final int spawnTick;
        public final int emergeDur;
        public final int holdDur;
        public final int retractDur;
        public final int fadeDur;
        public final int totalLife;
        public final float flickerPhase;

        public BoltInstance(List<Vec3> path, int spawnTick,
                            int emergeDur, int holdDur, int retractDur, int fadeDur,
                            float flickerPhase) {
            this(path, spawnTick, emergeDur, holdDur, retractDur, fadeDur, flickerPhase, TYPE_BRANCH);
        }

        public BoltInstance(List<Vec3> path, int spawnTick,
                            int emergeDur, int holdDur, int retractDur, int fadeDur,
                            float flickerPhase, byte type) {
            this.path = path;
            this.spawnTick = spawnTick;
            this.emergeDur = emergeDur;
            this.holdDur = holdDur;
            this.retractDur = retractDur;
            this.fadeDur = fadeDur;
            this.totalLife = emergeDur + holdDur + retractDur + fadeDur;
            this.flickerPhase = flickerPhase;
            this.type = type;
        }

        public float visibleFraction(int tick) {
            int age = tick - spawnTick;
            if (age < 0) return 0;
            if (age < emergeDur) return (float) age / emergeDur;
            if (age < emergeDur + holdDur) return 1f;
            if (age < emergeDur + holdDur + retractDur) {
                int rAge = age - emergeDur - holdDur;
                return 1f - (float) rAge / retractDur;
            }
            return 0;
        }

        public float alpha(int tick) {
            int age = tick - spawnTick;
            if (age < 0 || age > totalLife) return 0;

            if (age < emergeDur) {
                float t = (float) age / emergeDur;
                return t * t;
            }

            if (age < emergeDur + holdDur) {
                int hAge = age - emergeDur;
                float base = (float) Math.sin(hAge * 3.8 + flickerPhase);
                float fast = (float) Math.sin(hAge * 7.5 + flickerPhase * 2.1);
                float val = 0.45f + 0.35f * base + 0.20f * fast;
                return Math.max(0.15f, Math.min(1f, val));
            }

            if (age < emergeDur + holdDur + retractDur) {
                int rAge = age - emergeDur - holdDur;
                float t = (float) rAge / retractDur;
                return (1f - t) * (1f - t) * 0.7f;
            }

            int fAge = age - emergeDur - holdDur - retractDur;
            float t = (float) fAge / fadeDur;
            return (1f - t) * (1f - t) * 0.3f;
        }
    }

    public static class SparkInstance {
        public final Vec3 pos;
        public final int spawnTick;
        public final int lifetime;
        public final float size;

        public SparkInstance(Vec3 pos, int spawnTick, int lifetime, float size) {
            this.pos = pos;
            this.spawnTick = spawnTick;
            this.lifetime = lifetime;
            this.size = size;
        }

        public float alpha(int currentTick) {
            int age = currentTick - spawnTick;
            if (age < 0 || age > lifetime) return 0;
            return 1f - (float) age / lifetime;
        }
    }

    public static void setState(UUID entityId, int phaseOrdinal, double x, double y, double z,
                            double dirX, double dirY, double dirZ,
                            int duration, long gameTime) {
        LightningState.Phase phase = LightningState.Phase.values()[Math.min(phaseOrdinal, 4)];
        if (phase == LightningState.Phase.INACTIVE) {
            ACTIVE_STATES.remove(entityId);
            return;
        }
        LightningState s = ACTIVE_STATES.computeIfAbsent(entityId, k -> new LightningState());
        s.phase = phase;
        s.playerId = entityId;
        s.duration = duration;
        s.gameStartTick = gameTime;
        s.tick = 0;
        s.bolts.clear();
        s.sparks.clear();
        s.nextBurstTick = 0;
        s.dirX = dirX;
        s.dirY = dirY;
        s.dirZ = dirZ;
        if (phase == LightningState.Phase.LANDING) {
            s.landX = x;
            s.landY = y;
            s.landZ = z;
        }

        // 缓存实体引用：从当前客户端世界中查找实体
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = findEntityByUuid(mc.level, entityId);
            if (entity != null) {
                s.entityRef = new WeakReference<>(entity);
            }
        }
    }

    public static void clearState(UUID playerId) {
        ACTIVE_STATES.remove(playerId);
    }

    /**
     * 在客户端世界中按UUID查找实体
     * 使用适中的搜索半径避免区块坐标整数溢出
     */
    @Nullable
    private static Entity findEntityByUuid(Level level, UUID uuid) {
        // 方法1：通过玩家的位置构建合理范围的AABB搜索（高效，覆盖已加载实体）
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double range = 256.0; // 256格半径足够覆盖所有已加载实体
            Vec3 center = mc.player.position();
            AABB searchBox = new AABB(
                    center.x - range, center.y - range, center.z - range,
                    center.x + range, center.y + range, center.z + range);
            List<Entity> found = level.getEntities(
                    EntityTypeTest.forClass(Entity.class),
                    searchBox,
                    e -> e.getUUID().equals(uuid));
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }

        // 方法2：保守的全局搜索，使用不会溢出的范围
        // 区块坐标 = blockPos >> 4 = (int) x / 16
        // Integer.MAX_VALUE = 2147483647, 乘以16 ≈ 3.4e10 仍安全
        double safeRange = 2.0e7; // 2000万格，远超任何实际加载距离
        AABB globalBox = new AABB(-safeRange, -safeRange, -safeRange, safeRange, safeRange, safeRange);
        List<Entity> found = level.getEntities(
                EntityTypeTest.forClass(Entity.class),
                globalBox,
                e -> e.getUUID().equals(uuid));
        if (!found.isEmpty()) {
            return found.get(0);
        }

        return null;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) return;
        if (ACTIVE_STATES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();
        long gameTime = mc.level.getGameTime();
        Level level = mc.level;

        ACTIVE_STATES.entrySet().removeIf(e -> {
            LightningState s = e.getValue();
            if (s.phase == LightningState.Phase.LANDING)
                return (gameTime - s.gameStartTick) > s.duration;
            if (s.phase == LightningState.Phase.INACTIVE) return true;
            Entity ent = s.entityRef.get();
            if (ent == null) {
                ent = findEntityByUuid(level, s.playerId);
                if (ent != null) {
                    s.entityRef = new WeakReference<>(ent);
                }
            }
            if (ent == null || !ent.isAlive()) return true;
            if ((gameTime - s.gameStartTick) > 600) return true;
            return false;
        });
        if (ACTIVE_STATES.isEmpty()) return;

        for (LightningState s : ACTIVE_STATES.values()) s.tick++;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        for (LightningState s : ACTIVE_STATES.values()) {
            // 统一实体查找：entityRef → getPlayerByUUID → 全量UUID搜索
            Entity ent = s.entityRef.get();
            if (ent == null) {
                ent = level.getPlayerByUUID(s.playerId);
            }
            if (ent == null) {
                ent = findEntityByUuid(level, s.playerId);
                if (ent != null) {
                    s.entityRef = new WeakReference<>(ent);
                }
            }
            // LANDING阶段不依赖实体，其余阶段需要实体存在
            if (s.phase != LightningState.Phase.LANDING && ent == null) continue;

            if (s.phase == LightningState.Phase.FLYING && (s.dirX != 0 || s.dirY != 0 || s.dirZ != 0)) {
                renderBeamLightning(poseStack, buf, camPos, ent, s, partialTick);
            } else if (s.phase == LightningState.Phase.CHARGING_RING) {
                renderChargingRingLightning(poseStack, buf, camPos, ent, s, partialTick);
            } else if (s.phase == LightningState.Phase.CHARGING) {
                renderBodyLightning(poseStack, buf, camPos, ent, s, partialTick, false);
            } else if (s.phase == LightningState.Phase.FLYING) {
                renderBodyLightning(poseStack, buf, camPos, ent, s, partialTick, true);
            } else if (s.phase == LightningState.Phase.LANDING) {
                renderGroundArcs(poseStack, buf, camPos, s, level, partialTick);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderBodyLightning(PoseStack poseStack, BufferBuilder buf,
                                            Vec3 camPos, Entity entity, LightningState state,
                                            float partialTick, boolean isFlying) {
        double px = entity.xo + (entity.getX() - entity.xo) * partialTick;
        double py = entity.yo + (entity.getY() - entity.yo) * partialTick;
        double pz = entity.zo + (entity.getZ() - entity.zo) * partialTick;

        int tick = state.tick;
        state.bolts.removeIf(b -> tick - b.spawnTick > b.totalLife);
        state.sparks.removeIf(sp -> tick - sp.spawnTick > sp.lifetime);

        RAND.setSeed(tick * 31L + state.playerId.hashCode());

        if (tick >= state.nextBurstTick) {
            int darkGap = 2 + RAND.nextInt(7);
            state.nextBurstTick = tick + darkGap;

            int boltCount = 2 + RAND.nextInt(4);
            float burstHeight = V_HEIGHT * (0.7f + RAND.nextFloat() * 0.6f);
            float burstTopR = V_TOP_R * (0.5f + RAND.nextFloat() * 1.0f);

            for (int i = 0; i < boltCount; i++) {
                int staggerDelay = RAND.nextInt(3);
                int spawnTick = tick + staggerDelay;

                double baseAngle = RAND.nextDouble() * 2.0 * Math.PI;
                double baseX = Math.cos(baseAngle) * V_BASE_R * (0.5 + RAND.nextDouble());
                double baseZ = Math.sin(baseAngle) * V_BASE_R * (0.5 + RAND.nextDouble());
                Vec3 base = new Vec3(baseX, 0.05 + RAND.nextDouble() * 0.1, baseZ);

                double topAngle = baseAngle + (RAND.nextDouble() - 0.5) * 1.2;
                double topX = Math.cos(topAngle) * burstTopR * (0.6 + RAND.nextDouble() * 0.4);
                double topZ = Math.sin(topAngle) * burstTopR * (0.6 + RAND.nextDouble() * 0.4);
                Vec3 top = new Vec3(topX, burstHeight * (0.8 + RAND.nextDouble() * 0.2), topZ);

                int segs = 8 + RAND.nextInt(8);
                List<Vec3> path = generateBoltPath(base, top, segs, 0.3f + RAND.nextFloat() * 0.5f);
                float flickerPhase = RAND.nextFloat() * (float) Math.PI * 2;

                int emerge = 3 + RAND.nextInt(3);
                int hold = 3 + RAND.nextInt(4);
                int retract = 2 + RAND.nextInt(3);
                int fade = 2 + RAND.nextInt(2);
                state.bolts.add(new BoltInstance(path, spawnTick, emerge, hold, retract, fade, flickerPhase));

                state.sparks.add(new SparkInstance(base, spawnTick, 3 + RAND.nextInt(3), 0.04f + RAND.nextFloat() * 0.05f));
                state.sparks.add(new SparkInstance(top, spawnTick, 3 + RAND.nextInt(3), 0.03f + RAND.nextFloat() * 0.04f));

                if (RAND.nextFloat() < 0.45f) {
                    int restrikeDelay = emerge + hold + RAND.nextInt(2);
                    int rsTick = spawnTick + restrikeDelay;
                    List<Vec3> rsPath = mutatePath(path, 0.18f);
                    int rsEmerge = 1 + RAND.nextInt(2);
                    int rsHold = 2 + RAND.nextInt(3);
                    int rsRetract = 1 + RAND.nextInt(2);
                    int rsFade = 1 + RAND.nextInt(2);
                    state.bolts.add(new BoltInstance(rsPath, rsTick, rsEmerge, rsHold, rsRetract, rsFade,
                            flickerPhase + 1.8f));
                    state.sparks.add(new SparkInstance(top, rsTick, 2 + RAND.nextInt(2), 0.03f));
                }
            }
        }

        while (state.bolts.size() > 60) state.bolts.remove(0);
        while (state.sparks.size() > 80) state.sparks.remove(0);

        poseStack.pushPose();
        poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        if (isFlying) {
            float pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
            poseStack.mulPose(Axis.XP.rotationDegrees(-65 + pitch));
        }

        Matrix4f mat = poseStack.last().pose();
        Vec3 cam = new Vec3(camPos.x - px, camPos.y - py, camPos.z - pz);

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (SparkInstance sp : state.sparks) {
            float a = sp.alpha(tick);
            if (a <= 0.01f) continue;
            float sz = sp.size * (0.5f + 0.5f * a);
            renderSpark(buf, mat, sp.pos, sz, WHITE_HOT, a * 0.9f, cam);
            renderSpark(buf, mat, sp.pos, sz * 2f, INNER_GLOW, a * 0.35f, cam);
        }

        for (BoltInstance bolt : state.bolts) {
            float a = bolt.alpha(tick);
            float vf = bolt.visibleFraction(tick);
            if (a <= 0.01f || vf <= 0.01f) continue;

            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.12f, FAR_GLOW,   a * 0.12f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.07f, OUTER_GLOW, a * 0.30f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.04f, INNER_GLOW, a * 0.55f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.02f, CORE_COLOR, a * 0.85f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.008f, WHITE_HOT,  a, cam);
        }

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();
    }

    /**
     * 渲染蓄力阶段环身绿色闪电：随机分支短弧环绕实体，非完美圆形
     */
    private static void renderChargingRingLightning(PoseStack poseStack, BufferBuilder buf,
                                                    Vec3 camPos, Entity entity, LightningState state,
                                                    float partialTick) {
        double px = entity.xo + (entity.getX() - entity.xo) * partialTick;
        double py = entity.yo + (entity.getY() - entity.yo) * partialTick;
        double pz = entity.zo + (entity.getZ() - entity.zo) * partialTick;

        int tick = state.tick;
        state.bolts.removeIf(b -> tick - b.spawnTick > b.totalLife);
        state.sparks.removeIf(sp -> tick - sp.spawnTick > sp.lifetime);

        RAND.setSeed(tick * 31L + state.playerId.hashCode());

        if (tick >= state.nextBurstTick) {
            int gap = 1 + RAND.nextInt(3);
            state.nextBurstTick = tick + gap;

            float bodyHeight = Math.max(entity.getBbHeight(), 1.6f);

            // 随机分支弧线环绕实体（替代圆形环）
            // 每条弧是起点到终点的曲折闪电路径，覆盖60~150度弧度
            int arcCount = 8 + RAND.nextInt(6); // 8~13条随机短弧
            for (int i = 0; i < arcCount; i++) {
                float heightRatio = 0.1f + RAND.nextFloat() * 0.7f;
                float ringHeight = bodyHeight * heightRatio;
                float envFactor = (float) Math.sin(heightRatio * Math.PI);
                float ringRadius = 1.0f + envFactor * 0.8f;
                // 随时间整体缓慢旋转
                float rotOffset = tick * 0.03f + i * 0.5f;

                double startAngle = RAND.nextDouble() * 2.0 * Math.PI;
                double arcLen = (Math.PI / 3) + RAND.nextDouble() * (Math.PI / 1.8); // 60~130度

                Vec3 start = new Vec3(
                        Math.cos(startAngle + rotOffset) * ringRadius * (0.85 + RAND.nextDouble() * 0.3),
                        ringHeight + (RAND.nextDouble() - 0.5) * 0.1,
                        Math.sin(startAngle + rotOffset) * ringRadius * (0.85 + RAND.nextDouble() * 0.3));
                Vec3 end = new Vec3(
                        Math.cos(startAngle + rotOffset + arcLen) * ringRadius * (0.85 + RAND.nextDouble() * 0.3),
                        ringHeight + (RAND.nextDouble() - 0.5) * 0.1,
                        Math.sin(startAngle + rotOffset + arcLen) * ringRadius * (0.85 + RAND.nextDouble() * 0.3));

                int segs = 3 + RAND.nextInt(5);
                float displace = 0.2f + RAND.nextFloat() * 0.35f;
                List<Vec3> arcPath = generateBoltPath(start, end, segs, displace);

                float fp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(arcPath, tick + RAND.nextInt(2),
                        2, 3 + RAND.nextInt(3), 1, 1, fp,
                        BoltInstance.TYPE_RING));
            }

            // 绿色闪烁火花
            int sparkCount = 5 + RAND.nextInt(5);
            for (int s = 0; s < sparkCount; s++) {
                double angle = RAND.nextDouble() * 2.0 * Math.PI;
                float hRatio = 0.1f + RAND.nextFloat() * 0.7f;
                float sRadius = 1.0f + (float) Math.sin(hRatio * Math.PI) * 0.8f;
                double sx = Math.cos(angle) * sRadius * (0.9 + RAND.nextDouble() * 0.2);
                double sz = Math.sin(angle) * sRadius * (0.9 + RAND.nextDouble() * 0.2);
                double sy = bodyHeight * hRatio;
                state.sparks.add(new SparkInstance(new Vec3(sx, sy, sz), tick,
                        2 + RAND.nextInt(3), 0.04f + RAND.nextFloat() * 0.05f));
            }
        }

        while (state.bolts.size() > 60) state.bolts.remove(0);
        while (state.sparks.size() > 80) state.sparks.remove(0);

        // ===== 渲染 =====
        poseStack.pushPose();
        poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

        Matrix4f mat = poseStack.last().pose();
        Vec3 cam = new Vec3(camPos.x - px, camPos.y - py, camPos.z - pz);

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // 绿色火花
        for (SparkInstance sp : state.sparks) {
            float a = sp.alpha(tick);
            if (a <= 0.01f) continue;
            float sz = sp.size * (0.5f + 0.5f * a);
            renderSpark(buf, mat, sp.pos, sz, GREEN_CORE, a * 0.7f, cam);
            renderSpark(buf, mat, sp.pos, sz * 2f, GREEN_GLOW, a * 0.3f, cam);
        }

        // 环身绿色闪电短弧（TYPE_RING）
        for (BoltInstance bolt : state.bolts) {
            float a = bolt.alpha(tick);
            float vf = bolt.visibleFraction(tick);
            if (a <= 0.01f || vf <= 0.01f) continue;

            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.10f, GREEN_FAR,  a * 0.20f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.06f, GREEN_GLOW, a * 0.45f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.03f, GREEN_CORE, a * 0.80f, cam);
        }

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();
    }

    /**
     * 渲染锥形闪电束：沿方向发射粗闪电 + 分支 + 绿色外围闪电粒子
     * 与 ThunderExplosionSkill 的粒子锥形路径一致
     */
    private static void renderBeamLightning(PoseStack poseStack, BufferBuilder buf,
                                            Vec3 camPos, Entity entity, LightningState state,
                                            float partialTick) {
        double px = entity.xo + (entity.getX() - entity.xo) * partialTick;
        double py = entity.yo + (entity.getY() - entity.yo) * partialTick;
        double pz = entity.zo + (entity.getZ() - entity.zo) * partialTick;

        // 发射原点（局部坐标）
        Vec3 origin = new Vec3(0, entity.getBbHeight() * 0.4, 0);
        Vec3 direction = new Vec3(state.dirX, state.dirY, state.dirZ).normalize();

        double coneRad = Math.toRadians(CONE_ANGLE_DEG);
        int tick = state.tick;
        state.bolts.removeIf(b -> tick - b.spawnTick > b.totalLife);
        state.sparks.removeIf(sp -> tick - sp.spawnTick > sp.lifetime);

        RAND.setSeed(tick * 37L + state.playerId.hashCode());

        // 每 tick 都生成新闪电（密集持续效果）
        if (tick >= state.nextBurstTick) {
            int gap = 1 + RAND.nextInt(3);
            state.nextBurstTick = tick + gap;

            // ===== 1. 主闪电（粗·高曲折）沿方向 =====
            int mainSegs = 14 + RAND.nextInt(12); // 更多分段，更曲折
            Vec3 end = origin.add(direction.scale(BEAM_LENGTH));
            List<Vec3> mainPath = generateBeamBoltPath(origin, end, mainSegs,
                    0.8f + RAND.nextFloat() * 0.8f); // 更大的偏移量

            float fp = RAND.nextFloat() * (float) Math.PI * 2;
            int emerge = 1 + RAND.nextInt(2);
            int hold = 4 + RAND.nextInt(5);
            int retract = 2 + RAND.nextInt(2);
            int fade = 2 + RAND.nextInt(2);
            state.bolts.add(new BoltInstance(mainPath, tick, emerge, hold, retract, fade, fp,
                    BoltInstance.TYPE_MAIN));
            state.sparks.add(new SparkInstance(end, tick, 2 + RAND.nextInt(3), 0.10f));

            // 主闪电的回击
            if (RAND.nextFloat() < 0.6f) {
                int rsTick = tick + emerge + hold + RAND.nextInt(2);
                List<Vec3> rsPath = mutatePath(mainPath, 0.35f);
                state.bolts.add(new BoltInstance(rsPath, rsTick, 1, 3, 1, 1, fp + 1.7f,
                        BoltInstance.TYPE_MAIN));
            }

            // ===== 2. 粗分支（1-2条，从主路径中段分支出去） =====
            int thickBranchCount = 1 + RAND.nextInt(2);
            for (int i = 0; i < thickBranchCount; i++) {
                float t = 0.2f + RAND.nextFloat() * 0.5f;
                double dist = BEAM_LENGTH * t;
                Vec3 branchStart = origin.add(direction.scale(dist));

                // 粗分支方向：在锥体内偏离，但偏离角较小
                double brAngle = RAND.nextDouble() * 2 * Math.PI;
                double brSpread = Math.tan(coneRad * (0.2 + RAND.nextDouble() * 0.5)) * (BEAM_LENGTH - dist) * 0.6;
                double brUpOffset = (RAND.nextDouble() - 0.5) * brSpread * 0.3;
                Vec3 branchEnd = branchStart.add(
                        Math.cos(brAngle) * brSpread,
                        brUpOffset,
                        Math.sin(brAngle) * brSpread);

                int brSegs = 5 + RAND.nextInt(6);
                List<Vec3> brPath = generateBeamBoltPath(branchStart, branchEnd, brSegs,
                        0.4f + RAND.nextFloat() * 0.5f);
                float brFp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(brPath, tick + RAND.nextInt(2),
                        1, 2 + RAND.nextInt(3), 1, 1 + RAND.nextInt(2), brFp,
                        BoltInstance.TYPE_THICK_BRANCH));
                state.sparks.add(new SparkInstance(branchEnd, tick, 1 + RAND.nextInt(2), 0.06f));
            }

            // ===== 3. 细分支（1-2条，从主路径末端散射） =====
            int thinBranchCount = 1 + RAND.nextInt(2);
            for (int i = 0; i < thinBranchCount; i++) {
                float t = 0.4f + RAND.nextFloat() * 0.5f;
                double dist = BEAM_LENGTH * t;
                Vec3 branchStart = origin.add(direction.scale(dist));

                double brAngle = RAND.nextDouble() * 2 * Math.PI;
                double brSpread = Math.tan(Math.toRadians(CONE_ANGLE_DEG) * (0.5 + RAND.nextDouble() * 1.0)) * dist;
                double brUpOffset = (RAND.nextDouble() - 0.5) * brSpread * 0.4;
                Vec3 branchEnd = branchStart.add(
                        Math.cos(brAngle) * brSpread,
                        brUpOffset,
                        Math.sin(brAngle) * brSpread);

                int brSegs = 3 + RAND.nextInt(4);
                List<Vec3> brPath = generateBeamBoltPath(branchStart, branchEnd, brSegs,
                        0.15f + RAND.nextFloat() * 0.2f);
                float brFp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(brPath, tick + RAND.nextInt(1),
                        1, 2 + RAND.nextInt(2), 1, 1, brFp,
                        BoltInstance.TYPE_BRANCH));
                state.sparks.add(new SparkInstance(branchEnd, tick, 1 + RAND.nextInt(2), 0.03f));
            }

            // ===== 4. 绿色细密闪电粒子（外围锥体表面） =====
            int greenCount = 6 + RAND.nextInt(6);
            for (int i = 0; i < greenCount; i++) {
                float t = 0.1f + RAND.nextFloat() * 0.85f;
                double dist = BEAM_LENGTH * t;

                // 锥体表面位置（外围）
                double gAngle = RAND.nextDouble() * 2 * Math.PI;
                double gRadius = Math.tan(Math.toRadians(CONE_ANGLE_DEG) * (0.6 + RAND.nextDouble() * 0.4)) * dist;
                double gx = Math.cos(gAngle) * gRadius;
                double gz = Math.sin(gAngle) * gRadius;
                double gy = (RAND.nextDouble() - 0.5) * gRadius * 0.3;
                Vec3 gp = origin.add(direction.scale(dist)).add(gx, gy, gz);

                // 绿色闪电小路径（从中心到外围的短促分支）
                Vec3 centerPt = origin.add(direction.scale(dist * 0.9));
                int gSegs = 2 + RAND.nextInt(3);
                List<Vec3> gPath = generateBeamBoltPath(centerPt, gp, gSegs,
                        0.08f + RAND.nextFloat() * 0.12f);
                float gFp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(gPath, tick + RAND.nextInt(1),
                        1, 1 + RAND.nextInt(2), 1, 1, gFp,
                        BoltInstance.TYPE_GREEN));

                // 绿色闪烁粒子
                state.sparks.add(new SparkInstance(gp, tick, 1 + RAND.nextInt(2),
                        0.03f + RAND.nextFloat() * 0.04f));
            }

            // ===== 5. 绿色环绕闪电（螺旋环绕主光束） =====
            int greenWrapCount = 4 + RAND.nextInt(3);
            for (int i = 0; i < greenWrapCount; i++) {
                double t = 0.2 + RAND.nextDouble() * 0.6;
                double dist = BEAM_LENGTH * t;
                Vec3 center = origin.add(direction.scale(dist));

                double wrapRadius = Math.tan(coneRad * (0.25 + RAND.nextDouble() * 0.35)) * Math.max(dist, 2.0);
                wrapRadius = Math.max(wrapRadius, 0.5);

                double a1 = RAND.nextDouble() * 2.0 * Math.PI;
                double a2 = a1 + (Math.PI / 2) + RAND.nextDouble() * Math.PI; // 90~270度环绕

                // 构建光束垂直向量（用于环绕定位）
                Vec3 upLocal = Math.abs(direction.y) < 0.99
                        ? new Vec3(0, 1, 0).cross(direction).normalize().cross(direction).normalize()
                        : new Vec3(1, 0, 0);
                Vec3 rightLocal = direction.cross(upLocal).normalize();
                upLocal = rightLocal.cross(direction).normalize();

                double rad1 = wrapRadius * (0.85 + RAND.nextDouble() * 0.3);
                double rad2 = wrapRadius * (0.85 + RAND.nextDouble() * 0.3);
                Vec3 gs = center.add(rightLocal.scale(Math.cos(a1) * rad1)).add(upLocal.scale(Math.sin(a1) * rad1));
                Vec3 ge = center.add(rightLocal.scale(Math.cos(a2) * rad2)).add(upLocal.scale(Math.sin(a2) * rad2));

                // 沿光束方向偏移，产生螺旋效果
                double alongOff = BEAM_LENGTH * 0.08 * (RAND.nextDouble() - 0.5);
                gs = gs.add(direction.scale(-alongOff));
                ge = ge.add(direction.scale(alongOff));

                int gwSegs = 3 + RAND.nextInt(4);
                float gwDisplace = 0.10f + RAND.nextFloat() * 0.15f;
                List<Vec3> gwPath = generateBeamBoltPath(gs, ge, gwSegs, gwDisplace);
                float gwFp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(gwPath, tick + RAND.nextInt(1),
                        1, 2 + RAND.nextInt(2), 1, 1, gwFp,
                        BoltInstance.TYPE_GREEN));

                // 绿色火花
                state.sparks.add(new SparkInstance(ge, tick, 1 + RAND.nextInt(2),
                        0.03f + RAND.nextFloat() * 0.04f));
            }
        }

        // 限制数量
        while (state.bolts.size() > 120) state.bolts.remove(0);
        while (state.sparks.size() > 100) state.sparks.remove(0);

        // ===== 渲染 =====
        poseStack.pushPose();
        poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
        Matrix4f mat = poseStack.last().pose();
        Vec3 cam = new Vec3(camPos.x - px, camPos.y - py, camPos.z - pz);

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // 渲染白色/金色火花
        for (SparkInstance sp : state.sparks) {
            float a = sp.alpha(tick);
            if (a <= 0.01f) continue;
            float sz = sp.size * (0.5f + 0.5f * a);
            renderSpark(buf, mat, sp.pos, sz, WHITE_HOT, a * 0.9f, cam);
            renderSpark(buf, mat, sp.pos, sz * 2f, INNER_GLOW, a * 0.35f, cam);
        }

        // 渲染所有闪电路径
        for (BoltInstance bolt : state.bolts) {
            float a = bolt.alpha(tick);
            float vf = bolt.visibleFraction(tick);
            if (a <= 0.01f || vf <= 0.01f) continue;

            switch (bolt.type) {
                case BoltInstance.TYPE_MAIN:
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.38f, FAR_GLOW,   a * 0.12f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.26f, OUTER_GLOW, a * 0.30f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.15f, INNER_GLOW, a * 0.55f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.08f, CORE_COLOR, a * 0.85f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.04f, WHITE_HOT,   a, cam);
                    break;
                case BoltInstance.TYPE_THICK_BRANCH:
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.24f, FAR_GLOW,   a * 0.12f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.16f, OUTER_GLOW, a * 0.30f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.10f, INNER_GLOW, a * 0.55f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.05f, CORE_COLOR, a * 0.85f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.025f, WHITE_HOT,  a, cam);
                    break;
                case BoltInstance.TYPE_BRANCH:
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.10f, FAR_GLOW,   a * 0.12f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.06f, OUTER_GLOW, a * 0.30f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.03f, INNER_GLOW, a * 0.55f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.015f, CORE_COLOR, a * 0.85f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.008f, WHITE_HOT,  a, cam);
                    break;
                case BoltInstance.TYPE_GREEN:
                case BoltInstance.TYPE_RING:
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.08f, GREEN_FAR,  a * 0.20f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.05f, GREEN_GLOW, a * 0.45f, cam);
                    renderBoltPathGrow(buf, mat, bolt.path, vf, 0.025f, GREEN_CORE, a * 0.80f, cam);
                    break;
            }
        }

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();
    }

    /**
     * 沿方向生成闪电路径，支持任意起止位置
     */
    private static List<Vec3> generateBeamBoltPath(Vec3 base, Vec3 top, int segs, float displace) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(base);
        Vec3 dir = top.subtract(base);
        double len = dir.length();
        if (len < 0.001) { pts.add(top); return pts; }
        Vec3 dn = dir.scale(1.0 / len);
        Vec3 perp1 = Math.abs(dn.x) < 0.9 ? new Vec3(1, 0, 0).cross(dn).normalize()
                : new Vec3(0, 1, 0).cross(dn).normalize();
        Vec3 perp2 = dn.cross(perp1).normalize();

        for (int i = 1; i < segs; i++) {
            double t = (double) i / segs;
            Vec3 basePt = base.add(dir.scale(t));
            double env = Math.sin(t * Math.PI);
            double om = displace * env * (0.3 + RAND.nextDouble() * 0.7);
            double ang = RAND.nextDouble() * 2.0 * Math.PI;
            pts.add(basePt.add(perp1.scale(Math.cos(ang) * om))
                    .add(perp2.scale(Math.sin(ang) * om)));
        }
        pts.add(top);
        return pts;
    }

    private static List<Vec3> mutatePath(List<Vec3> original, float maxOffset) {
        List<Vec3> mutated = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            Vec3 pt = original.get(i);
            if (i == 0 || i == original.size() - 1) {
                mutated.add(pt);
            } else {
                double ox = (RAND.nextDouble() - 0.5) * 2.0 * maxOffset;
                double oy = (RAND.nextDouble() - 0.5) * 2.0 * maxOffset;
                double oz = (RAND.nextDouble() - 0.5) * 2.0 * maxOffset;
                mutated.add(new Vec3(pt.x + ox, pt.y + oy, pt.z + oz));
            }
        }
        return mutated;
    }

    private static List<Vec3> generateBoltPath(Vec3 base, Vec3 top, int segs, float displace) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(base);
        Vec3 dir = top.subtract(base);
        double len = dir.length();
        if (len < 0.001) { pts.add(top); return pts; }
        Vec3 dn = dir.scale(1.0 / len);
        Vec3 perp1 = Math.abs(dn.x) < 0.9 ? new Vec3(1, 0, 0).cross(dn).normalize()
                : new Vec3(0, 1, 0).cross(dn).normalize();
        Vec3 perp2 = dn.cross(perp1).normalize();

        for (int i = 1; i < segs; i++) {
            double t = (double) i / segs;
            Vec3 basePt = base.add(dir.scale(t));
            double env = Math.sin(t * Math.PI);
            double om = displace * env * (0.3 + RAND.nextDouble() * 0.7);
            double ang = RAND.nextDouble() * 2.0 * Math.PI;
            pts.add(basePt.add(perp1.scale(Math.cos(ang) * om))
                    .add(perp2.scale(Math.sin(ang) * om)));
        }
        pts.add(top);
        return pts;
    }

    private static void renderGroundArcs(PoseStack poseStack, BufferBuilder buf,
                                         Vec3 camPos, LightningState state,
                                         Level level, float partialTick) {
        double px = state.landX;
        double py = state.landY;
        double pz = state.landZ;

        float progress = Math.min(1.0f, state.tick / (float) state.duration);
        float fadeAlpha = progress < 0.2f ? 1.0f : 1.0f - (progress - 0.2f) / 0.8f;
        if (fadeAlpha <= 0.01f) return;

        float maxRadius = 2.0f + progress * 18.0f;

        BlockPos cg = findGround(level, px, py, pz);
        double groundY = cg != null ? cg.getY() - py + 0.05 : 0.05;

        state.bolts.removeIf(b -> state.tick - b.spawnTick > b.totalLife);
        state.sparks.removeIf(sp -> state.tick - sp.spawnTick > sp.lifetime);

        RAND.setSeed(state.tick * 53L + state.playerId.hashCode());
        int newBolts = 3 + RAND.nextInt(5);
        for (int n = 0; n < newBolts; n++) {
            double angle = RAND.nextDouble() * 2.0 * Math.PI;
            float radius = maxRadius * (0.4f + RAND.nextFloat() * 0.6f);
            double endX = Math.cos(angle) * radius;
            double endZ = Math.sin(angle) * radius;

            BlockPos eg = findGround(level, px + endX, py, pz + endZ);
            double egY = eg != null ? eg.getY() - py + 0.05 : groundY;

            Vec3 start = new Vec3(0, groundY + 0.3, 0);
            Vec3 end = new Vec3(endX, egY + 0.3 + RAND.nextDouble() * 0.4, endZ);

            int segs = 10 + RAND.nextInt(8);
            List<Vec3> path = generateBoltPathFlat(start, end, segs, 1.8f);
            float fp = RAND.nextFloat() * (float) Math.PI * 2;
            state.bolts.add(new BoltInstance(path, state.tick, 3, 2, 0, 3, fp));

            state.sparks.add(new SparkInstance(end, state.tick, 3 + RAND.nextInt(4), 0.06f + RAND.nextFloat() * 0.08f));

            int branches = 2 + RAND.nextInt(3);
            for (int br = 0; br < branches; br++) {
                float t = 0.2f + RAND.nextFloat() * 0.6f;
                Vec3 brStart = start.add(end.subtract(start).scale(t));
                double brAngle = angle + (RAND.nextDouble() - 0.5) * 1.0;
                float brR = radius * (0.15f + RAND.nextFloat() * 0.4f);
                double brEndX = Math.cos(brAngle) * brR;
                double brEndZ = Math.sin(brAngle) * brR;
                BlockPos brg = findGround(level, px + brEndX, py, pz + brEndZ);
                double brgY = brg != null ? brg.getY() - py + 0.05 : egY;
                Vec3 brEnd = new Vec3(brEndX, brgY + 0.3 + RAND.nextDouble() * 0.3, brEndZ);

                int brSegs = 5 + RAND.nextInt(5);
                List<Vec3> brPath = generateBoltPathFlat(brStart, brEnd, brSegs, 1.0f);
                float brFp = RAND.nextFloat() * (float) Math.PI * 2;
                state.bolts.add(new BoltInstance(brPath, state.tick, 2, 1, 0, 3, brFp));
                state.sparks.add(new SparkInstance(brEnd, state.tick, 2 + RAND.nextInt(3), 0.04f));
            }
        }

        while (state.bolts.size() > 70) state.bolts.remove(0);
        while (state.sparks.size() > 100) state.sparks.remove(0);

        poseStack.pushPose();
        poseStack.translate(px - camPos.x, py - camPos.y, pz - camPos.z);
        Matrix4f mat = poseStack.last().pose();
        Vec3 cam = new Vec3(camPos.x - px, camPos.y - py, camPos.z - pz);

        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float flashAlpha = fadeAlpha * (1.0f - progress) * 0.5f;
        renderCenterFlash(buf, mat, groundY + 0.3, 0.4f + progress * 2.5f, flashAlpha, cam);

        for (SparkInstance sp : state.sparks) {
            float a = sp.alpha(state.tick) * fadeAlpha;
            if (a <= 0.01f) continue;
            float sz = sp.size * (0.5f + 0.5f * a);
            renderSpark(buf, mat, sp.pos, sz, WHITE_HOT, a * 0.9f, cam);
            renderSpark(buf, mat, sp.pos, sz * 2.5f, INNER_GLOW, a * 0.35f, cam);
        }

        for (BoltInstance bolt : state.bolts) {
            float a = bolt.alpha(state.tick) * fadeAlpha;
            float vf = bolt.visibleFraction(state.tick);
            if (a <= 0.01f || vf <= 0.01f) continue;

            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.40f, FAR_GLOW,   a * 0.12f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.25f, OUTER_GLOW, a * 0.30f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.14f, INNER_GLOW, a * 0.60f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.06f, CORE_COLOR, a * 0.85f, cam);
            renderBoltPathGrow(buf, mat, bolt.path, vf, 0.025f, WHITE_HOT,  a, cam);
        }

        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();
    }

    private static List<Vec3> generateBoltPathFlat(Vec3 start, Vec3 end, int segs, float displace) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(start);
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.001) { pts.add(end); return pts; }
        Vec3 dn = dir.scale(1.0 / len);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 p1 = dn.cross(up).normalize();
        if (p1.lengthSqr() < 0.0001) p1 = dn.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 p2 = dn.cross(p1).normalize();

        for (int i = 1; i < segs; i++) {
            double t = (double) i / segs;
            Vec3 base = start.add(dir.scale(t));
            double om = displace * Math.sin(t * Math.PI) * (0.3 + RAND.nextDouble() * 0.7);
            double ang = RAND.nextDouble() * 2.0 * Math.PI;
            pts.add(base.add(p1.scale(Math.cos(ang) * om))
                    .add(p2.scale(Math.sin(ang) * om)));
        }
        pts.add(end);
        return pts;
    }

    private static void renderSpark(BufferBuilder buf, Matrix4f mat, Vec3 pos,
                                    float size, float[] color, float alpha, Vec3 cam) {
        if (alpha < 0.01f) return;
        float r = color[0], g = color[1], b = color[2];

        Vec3 toCam = cam.subtract(pos).normalize();
        Vec3 right = new Vec3(0, 1, 0).cross(toCam).normalize();
        if (right.lengthSqr() < 0.0001) right = new Vec3(1, 0, 0);
        Vec3 up = toCam.cross(right).normalize();

        float h = size * 0.5f;
        Vec3 v1 = pos.add(right.scale(-h)).add(up.scale(-h));
        Vec3 v2 = pos.add(right.scale( h)).add(up.scale(-h));
        Vec3 v3 = pos.add(right.scale( h)).add(up.scale( h));
        Vec3 v4 = pos.add(right.scale(-h)).add(up.scale( h));

        buf.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, alpha).endVertex();

        buf.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, alpha).endVertex();
        buf.vertex(mat, (float) v4.x, (float) v4.y, (float) v4.z).color(r, g, b, alpha).endVertex();
    }

    private static void renderBoltPathGrow(BufferBuilder buf, Matrix4f mat,
                                           List<Vec3> pts, float growProgress,
                                           float width, float[] color, float alpha, Vec3 cam) {
        if (pts.size() < 2 || alpha < 0.005f || growProgress <= 0.01f) return;
        float r = color[0], g = color[1], b = color[2];

        int maxIdx = (int) (growProgress * (pts.size() - 1));
        if (maxIdx < 1) maxIdx = 1;
        if (maxIdx > pts.size() - 1) maxIdx = pts.size() - 1;

        for (int i = 0; i < maxIdx; i++) {
            Vec3 p1 = pts.get(i);
            Vec3 p2 = pts.get(i + 1);
            Vec3 mid = p1.add(p2).scale(0.5);
            Vec3 toCam = cam.subtract(mid).normalize();
            Vec3 segDir = p2.subtract(p1);
            if (segDir.lengthSqr() < 0.0001) continue;
            segDir = segDir.normalize();
            Vec3 perp = toCam.cross(segDir).normalize();
            if (perp.lengthSqr() < 0.0001) {
                perp = new Vec3(0, 1, 0).cross(segDir).normalize();
            }
            if (perp.lengthSqr() < 0.0001) continue;

            float hw = width * 0.5f;
            Vec3 off = perp.scale(hw);
            Vec3 v1 = p1.add(off), v2 = p1.subtract(off);
            Vec3 v3 = p2.add(off), v4 = p2.subtract(off);
            float a1 = alpha * (0.3f + 0.7f * (float) i / maxIdx);
            float a2 = alpha * (0.3f + 0.7f * (float) (i + 1) / maxIdx);

            buf.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, a1).endVertex();
            buf.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a1).endVertex();
            buf.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, a2).endVertex();

            buf.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a1).endVertex();
            buf.vertex(mat, (float) v4.x, (float) v4.y, (float) v4.z).color(r, g, b, a2).endVertex();
            buf.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, a2).endVertex();
        }
    }

    private static void renderCenterFlash(BufferBuilder buf, Matrix4f mat,
                                          double y, float radius, float alpha, Vec3 cam) {
        if (alpha < 0.01f) return;
        int segs = 20;
        Vec3 center = new Vec3(0, y, 0);
        Vec3 to = cam.normalize();
        Vec3 r = new Vec3(0, 1, 0).cross(to).normalize();
        if (r.lengthSqr() < 0.0001) r = new Vec3(1, 0, 0);
        Vec3 f = to.cross(r).normalize();
        for (int i = 0; i < segs; i++) {
            double a1 = 2.0 * Math.PI * i / segs;
            double a2 = 2.0 * Math.PI * (i + 1) / segs;
            Vec3 p1 = center.add(r.scale(Math.cos(a1) * radius)).add(f.scale(Math.sin(a1) * radius));
            Vec3 p2 = center.add(r.scale(Math.cos(a2) * radius)).add(f.scale(Math.sin(a2) * radius));
            buf.vertex(mat, (float) center.x, (float) center.y, (float) center.z)
                    .color(1f, 0.95f, 0.3f, alpha * 0.8f).endVertex();
            buf.vertex(mat, (float) p1.x, (float) p1.y, (float) p1.z)
                    .color(1f, 0.95f, 0.3f, alpha * 0.05f).endVertex();
            buf.vertex(mat, (float) p2.x, (float) p2.y, (float) p2.z)
                    .color(1f, 0.95f, 0.3f, alpha * 0.05f).endVertex();
        }
    }

    private static BlockPos findGround(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) x, (int) y, (int) z);
        for (int dy = 0; dy < 15; dy++) {
            pos.set((int) x, (int) y - dy, (int) z);
            if (pos.getY() < level.getMinBuildHeight()) break;
            if (!level.getBlockState(pos).isAir()) return pos.immutable();
        }
        return new BlockPos((int) x, (int) y, (int) z);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}