package com.qianyunyi.rider.entity.kaijin.gurongi;

import com.qianyunyi.rider.entity.kaijin.gurongi.base.Gurongi;
import com.qianyunyi.rider.entity.kaijin.gurongi.base.GurongiGroup;
import com.qianyunyi.rider.entity.projectile.GoGadoruBaWindProjectile;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.EnumSet;

/**
 * 葛·伽多鲁·巴（Go Gadoru Ba）
 * <p>
 * 拥有五种形态，形态之间随机切换（默认每种形态持续 20 秒）：
 * <ul>
 *   <li>格斗体（grapple）：隐藏 sword/crossbow/wand，攻击动画 attack1 / attack2</li>
 *   <li>刚力体（herculean）：显示 sword，攻击动画 sword_attack1 / sword_attack2，
 *       弱化攻击与速度，防御力大幅提升（额外 +50% 减伤）</li>
 *   <li>射击体（shooting）：显示 crossbow，攻击动画 crossbow_attack，
 *       在 0.75 秒发射与天马弩子弹相同的风弹（支持非玩家射手），弱化防御、提升攻击</li>
 *   <li>俊敏体（quick）：显示 wand，攻击动画 wand_attack1 / wand_attack2，
 *       弱化防御与攻击，提升速度 / 攻击速度 / 跳跃高度（类似 Zu-Baduu-Ba 高跳，不播放跳跃动画）</li>
 *   <li>电击体（shocker）：默认隐藏全部武器，属性大幅提升，可使用所有武器，
 *       一次只切换一种武器，切武器不改变纹理，动画跟随所选武器；仅在被闪电击中后解锁</li>
 * </ul>
 * <p>
 * 参考狼王（NGamioZedaEntity）的动画版本同步模式与 ZuBaduuBaEntity 的近战轮询 / 跳跃逻辑。
 */
public class GoGadoruBaEntity extends Gurongi {

    // ========== 武器类型 ==========
    public enum WeaponType {
        NONE("walk", "attack1", "attack2", false),
        SWORD("sword_walk", "sword_attack1", "sword_attack2", false),
        CROSSBOW("crossbow_walk", "crossbow_attack", "crossbow_attack", true),
        WAND("wand_walk", "wand_attack1", "wand_attack2", false);

        public final String walkAnim;
        public final String attackAnim1;
        public final String attackAnim2;
        public final boolean isRanged;

        WeaponType(String walkAnim, String attackAnim1, String attackAnim2, boolean isRanged) {
            this.walkAnim = walkAnim;
            this.attackAnim1 = attackAnim1;
            this.attackAnim2 = attackAnim2;
            this.isRanged = isRanged;
        }
    }

    // ========== 形态 ==========
    public enum FormType {
        GRAPPLE("go/go_gadoru_ba_grapple_form", WeaponType.NONE, 1.0, 1.0, 1.0, 1.0, 1.0f, "格斗体"),
        HERCULEAN("go/go_gadoru_ba_herculean_form", WeaponType.SWORD, 1.0, 1.5, 0.5, 2, 0.5f, "刚力体"),
        SHOOTING("go/go_gadoru_ba_shooting_form", WeaponType.CROSSBOW, 1.0, 1.5, 1.0, 0.5, 1.2f, "射击体"),
        QUICK("go/go_gadoru_ba_quick_form", WeaponType.WAND, 1.0, 0.75, 2.0, 0.7, 1.2f, "俊敏体"),
        SHOCKER("go/go_gadoru_ba_shocker_form", WeaponType.NONE, 2.0, 2.0, 1.5, 2, 0.5f, "电击体");

        public final String texture;
        public final WeaponType defaultWeapon;
        public final double healthMult;
        public final double attackMult;
        public final double speedMult;
        public final double armorMult;
        /** 额外减伤系数：叠乘基类 50% 基础减伤，射击体/俊敏体 >1.0 表示额外受伤 */
        public final float damageReductionMult;
        public final String displayName;

        FormType(String texture, WeaponType defaultWeapon,
                 double healthMult, double attackMult, double speedMult,
                 double armorMult, float damageReductionMult, String displayName) {
            this.texture = texture;
            this.defaultWeapon = defaultWeapon;
            this.healthMult = healthMult;
            this.attackMult = attackMult;
            this.speedMult = speedMult;
            this.armorMult = armorMult;
            this.damageReductionMult = damageReductionMult;
            this.displayName = displayName;
        }
    }

    // ========== 基础属性 ==========
    private static final double BASE_HEALTH = 400.0;
    private static final double BASE_ATTACK = 100.0;
    private static final double BASE_SPEED = 0.2;
    private static final double BASE_ARMOR = 50.0;
    private static final double BASE_ARMOR_TOUGHNESS = 5.0;
    private static final double FOLLOW_RANGE = 40.0; // 索敌范围（目标选择与跟踪保持范围，格）

    // ========== 形态切换 ==========
    private static final int FORM_DURATION_TICKS = 400; // 20 秒
    private static final int SHOCKER_WEAPON_SWITCH_TICKS = 200; // 电击体武器切换间隔 10 秒
    private static final int FORM_SWITCH_COOLDOWN_TICKS = 20;   // 形态切换后 1 秒内不再切换，避免抖动
    private static final double CHASE_SWITCH_DISTANCE = 5.0;    // 目标超过 5 格时切换追击/远程形态

    // ========== 近战轮询 ==========
    private int meleeCooldownTicks;
    private int meleeTicksRemaining;
    private int meleeHitTick;
    private boolean lastMeleeWasAttack1 = true;
    private static final int MELEE_COOLDOWN_TICKS = 10;

    // 格斗体 attack1/2 与刚力体 sword_attack1/2：动画 0.75s，0.5s 造成伤害
    private static final int MELEE_DURATION_TICKS = 15;
    private static final int MELEE_HIT_TICK = 10;
    // 俊敏体 wand_attack1/2：动画 0.5s，0.25s 造成伤害
    private static final int WAND_MELEE_DURATION_TICKS = 10;
    private static final int WAND_MELEE_HIT_TICK = 5;

    // 无武器（格斗体/电击体无武器）踢击 attack_kick：动画 1s，0.25s 造成伤害，期间不可移动
    private int kickTicksRemaining;
    private int kickHitTick;
    private static final int KICK_DURATION_TICKS = 20;
    private static final int KICK_HIT_TICK = 5;

    // ========== 形态切换距离规则 ==========
    private static final double SHOOT_RANGE_MIN = 5.0;   // 射击体最小射程
    private static final double SHOOT_RANGE_MAX = 15.0;  // 超过该距离（或目标过高）切换俊敏体
    private static final double QUICK_PURSUE_RANGE = 4.0; // 俊敏体追击锁定距离：贴近到该值以内才退出追击
    private static final double HIGH_SWITCH_DISTANCE = 5.0; // 目标 Y 高度差过大切换俊敏体的阈值

    // ========== 射击（crossbow） ==========
    private int crossbowCooldown;
    private int crossbowTicksRemaining;
    private static final int CROSSBOW_COOLDOWN_TICKS = 30;
    private static final int CROSSBOW_ANIMATION_TICKS = 30; // 动画 1.5s
    private static final int CROSSBOW_FIRE_TICK = 10;       // 剩余 10 刻时发射（1s）
    private static final float PROJECTILE_SPEED = 6.0F;

    // ========== 电击体变身特效 ==========
    private int shockerFxTicks;
    private static final int SHOCKER_FX_TICKS = 20; // 变身动画 + 周身闪电粒子持续 1s

    // ========== 电击体闪电免疫被动 ==========
    private int shockerImmuneCount;        // 当前 1s 窗口内已免疫次数
    private int shockerImmuneWindowStart;  // 免疫窗口起始 tickCount
    private int shockerTotalImmune;        // 累计免疫总次数
    private int shockerBuffLevel;          // 已生效的属性提升等级
    private static final int SHOCKER_IMMUNE_WINDOW_TICKS = 20;  // 免疫窗口 1s
    private static final int SHOCKER_MAX_IMMUNE_PER_WINDOW = 3; // 1s 内最多免疫次数
    private static final float SHOCKER_IMMUNE_HEAL_RATIO = 0.05F; // 每次免疫恢复 5% 生命
    private static final float SHOCKER_MAX_HP_THRESHOLD = 0.5F;   // 仅免疫 <50% 最大生命的闪电伤害

    // ========== 跳跃（俊敏体） ==========
    private int jumpCooldown = 0;
    private static final int JUMP_COOLDOWN_TICKS = 60;

    // ========== 动画版本同步（参考狼王） ==========
    public static final EntityDataAccessor<String> ANIMATION =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> ANIM_VERSION =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.INT);
    // 当前形态
    public static final EntityDataAccessor<String> FORM =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.STRING);
    // 电击体当前所选武器
    public static final EntityDataAccessor<String> WEAPON =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.STRING);
    // 电击体是否已解锁（被闪电击中）
    public static final EntityDataAccessor<Boolean> SHOCKER_UNLOCKED =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.BOOLEAN);
    // 骨骼组可见性（供模型层读取）
    public static final EntityDataAccessor<Boolean> SWORD_VISIBLE =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CROSSBOW_VISIBLE =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> WAND_VISIBLE =
            SynchedEntityData.defineId(GoGadoruBaEntity.class, EntityDataSerializers.BOOLEAN);

    public static final String ANIM_NONE = "empty";

    private String animationprocedure = ANIM_NONE;
    private int lastAnimVersion = -1;

    // ========== 形态状态 ==========
    private int formTimer = FORM_DURATION_TICKS;
    private int formSwitchCooldown = 0;
    private int shockerWeaponTimer = SHOCKER_WEAPON_SWITCH_TICKS;

    private boolean initFormApplied = false;

    private static final ResourceKey<DamageType> VOID_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("minecraft", "out_of_world")
    );

    public GoGadoruBaEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(KamenRiderInterwovenWorldsModEntities.GO_GADORU_BA.get(), world);
    }

    public GoGadoruBaEntity(EntityType<GoGadoruBaEntity> type, Level world) {
        super(type, world);
        xpReward = 50;
        setNoAi(false);
        this.group = GurongiGroup.GO;
        // 保留基类全局 50% 基础减伤，本类仅叠加形态额外减伤系数
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION, ANIM_NONE);
        this.entityData.define(ANIM_VERSION, 0);
        this.entityData.define(FORM, FormType.GRAPPLE.name());
        this.entityData.define(WEAPON, WeaponType.NONE.name());
        this.entityData.define(SHOCKER_UNLOCKED, false);
        this.entityData.define(SWORD_VISIBLE, false);
        this.entityData.define(CROSSBOW_VISIBLE, false);
        this.entityData.define(WAND_VISIBLE, false);
        // 默认纹理为格斗体，避免出生瞬间发生紫黑乱码（缺失纹理）
        setTexture(FormType.GRAPPLE.texture);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANIMATION.equals(key)) {
            this.animationprocedure = this.entityData.get(ANIMATION);
        }
    }

    // ========================================================================
    //  形态系统
    // ========================================================================

    public FormType getForm() {
        try {
            return FormType.valueOf(this.entityData.get(FORM));
        } catch (IllegalArgumentException e) {
            return FormType.GRAPPLE;
        }
    }

    public void setForm(FormType form) {
        if (form == FormType.SHOCKER && !isShockerUnlocked()) {
            return; // 未解锁电击体
        }
        FormType prev = getForm();
        if (form != prev) {
            this.entityData.set(FORM, form.name());
            // 切换形态改变对应纹理（纹理形态同名）
            this.setTexture(form.texture);
            // 切换形态后武器归位到该形态默认武器（电击体则回到默认格斗体武器 NONE）
            setWeapon(form.defaultWeapon);
            applyFormAttributes(form);
            // 进入电击体：播放变身动画 + 周身闪电粒子
            if (form == FormType.SHOCKER && !level().isClientSide) {
                startShockerTransform();
            }
        }
    }

    /**
     * 进入电击体时的变身表现：播放 shocker_form（1s），并持续在周身随机生成闪电粒子。
     */
    private void startShockerTransform() {
        shockerFxTicks = SHOCKER_FX_TICKS;
        // 变身 1s 期间熄灭身上火焰、清除所有 Buff/Debuff
        this.clearFire();
        this.removeAllEffects();
        setAnimation("shocker_form");
    }

    /**
     * 在实体周身随机分布闪电粒子（由 tick 持续调用，共 1s）。
     */
    private void spawnShockerParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        double x = this.getX() + (this.random.nextDouble() - 0.5) * 2.2;
        double y = this.getY() + this.random.nextDouble() * this.getBbHeight();
        double z = this.getZ() + (this.random.nextDouble() - 0.5) * 2.2;
        serverLevel.sendParticles(
                (net.minecraft.core.particles.ParticleOptions) com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModParticleTypes.LIGHTNING.get(),
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0
        );
    }

    public WeaponType getCurrentWeapon() {
        try {
            return WeaponType.valueOf(this.entityData.get(WEAPON));
        } catch (IllegalArgumentException e) {
            return WeaponType.NONE;
        }
    }

    public void setWeapon(WeaponType weapon) {
        this.entityData.set(WEAPON, weapon.name());
        updateWeaponVisibility();
    }

    /**
     * 更新 sword / crossbow / wand 三个骨骼组的可见性。
     * <ul>
     *   <li>非电击体：由形态决定（格斗体全隐藏，刚力体显示剑，射击体显示弩，俊敏体显示杖）</li>
     *   <li>电击体：根据当前所选武器，一次只显示一种武器（未选武器则全部隐藏，等同格斗体）</li>
     * </ul>
     */
    private void updateWeaponVisibility() {
        FormType form = getForm();
        WeaponType weapon = getCurrentWeapon();
        boolean sword, crossbow, wand;
        if (form == FormType.SHOCKER) {
            sword = weapon == WeaponType.SWORD;
            crossbow = weapon == WeaponType.CROSSBOW;
            wand = weapon == WeaponType.WAND;
        } else {
            sword = weapon == WeaponType.SWORD;
            crossbow = weapon == WeaponType.CROSSBOW;
            wand = weapon == WeaponType.WAND;
        }
        this.entityData.set(SWORD_VISIBLE, sword);
        this.entityData.set(CROSSBOW_VISIBLE, crossbow);
        this.entityData.set(WAND_VISIBLE, wand);
    }

    public boolean isSwordVisible() {
        return this.entityData.get(SWORD_VISIBLE);
    }

    public boolean isCrossbowVisible() {
        return this.entityData.get(CROSSBOW_VISIBLE);
    }

    public boolean isWandVisible() {
        return this.entityData.get(WAND_VISIBLE);
    }

    // ========== 电击体解锁 ==========

    public boolean isShockerUnlocked() {
        return this.entityData.get(SHOCKER_UNLOCKED);
    }

    public void unlockShocker() {
        this.entityData.set(SHOCKER_UNLOCKED, true);
    }

    // ========== 形态属性应用 ==========

    private void applyFormAttributes(FormType form) {
        if (level().isClientSide) return;

        // 记录切换前的血量比例，避免形态切换时生命上限变化导致血量突变
        float healthRatio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());

        setAttributeBase(Attributes.MAX_HEALTH, BASE_HEALTH * form.healthMult);
        setAttributeBase(Attributes.ATTACK_DAMAGE, BASE_ATTACK * form.attackMult);
        setAttributeBase(Attributes.MOVEMENT_SPEED, BASE_SPEED * form.speedMult);
        setAttributeBase(Attributes.ARMOR, BASE_ARMOR * form.armorMult);
        setAttributeBase(Attributes.ARMOR_TOUGHNESS, BASE_ARMOR_TOUGHNESS * form.armorMult);

        // 按切换前的血量比例恢复血量，夹在 [1, 新上限] 之间
        float newMax = this.getMaxHealth();
        this.setHealth(Math.max(1.0F, Math.min(newMax, healthRatio * newMax)));

        // 电击体重新应用属性提升
        if (form == FormType.SHOCKER && shockerBuffLevel > 0) {
            applyShockerBuff(shockerBuffLevel);
        }
    }

    private void setAttributeBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ========================================================================
    //  动画
    // ========================================================================

    public String getAnimation() {
        return this.entityData.get(ANIMATION);
    }

    /**
     * 设置动画：每次调用都递增版本号，确保客户端即使同名动画也能重复播放。
     */
    public void setAnimation(String animation) {
        this.entityData.set(ANIMATION, animation);
        this.entityData.set(ANIM_VERSION, this.entityData.get(ANIM_VERSION) + 1);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "move_controller", 4, this::movementAnimation));
        data.add(new AnimationController<>(this, "action_controller", 4, this::actionAnimation));
    }

    /**
     * 移动层：根据当前武器的行走动画播放。多体层与动作层互不互斥，GeckoLib 自动混合。
     */
    private PlayState movementAnimation(AnimationState<GoGadoruBaEntity> state) {
        if (this.isDeadOrDying()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("death"));
        }
        String walkAnim = getCurrentWeapon().walkAnim;
        if (state.isMoving() || this.walkAnimation.speed() > 0.01F) {
            return state.setAndContinue(RawAnimation.begin().thenLoop(walkAnim));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    /**
     * 动作层：客户端版本跟踪，播放单次攻击 / 动作动画。
     */
    private PlayState actionAnimation(AnimationState<GoGadoruBaEntity> state) {
        String animation = this.entityData.get(ANIMATION);
        int version = this.entityData.get(ANIM_VERSION);

        if (ANIM_NONE.equals(animation)) {
            lastAnimVersion = -1;
            return PlayState.STOP;
        }

        if (version != lastAnimVersion) {
            lastAnimVersion = version;
            state.getController().forceAnimationReset();
            return state.setAndContinue(RawAnimation.begin().thenPlay(animation));
        }
        return PlayState.CONTINUE;
    }

    // ========================================================================
    //  AI 目标
    // ========================================================================

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // 俊敏体高跳（仅俊敏体生效）
        this.goalSelector.addGoal(1, new QuickJumpGoal());
        // 射击体远程攻击（仅射击体生效）
        this.goalSelector.addGoal(1, new RangedAttackGoal());
        // 浮空
        this.goalSelector.addGoal(2, new FloatGoal(this));
        // 近战（射击体不进行近战）
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            public boolean canUse() {
                if (shockerFxTicks > 0) return false; // 变身期间禁止普攻/近战
                if (getCurrentWeapon().isRanged) return false;
                return super.canUse();
            }

            @Override
            protected void checkAndPerformAttack(@NotNull LivingEntity enemy, double dist) {
                if (meleeTicksRemaining > 0 || kickTicksRemaining > 0) return;
                double reachSqr = getAttackReachSqr(enemy);
                if (dist <= reachSqr && this.getTicksUntilNextAttack() <= 0) {
                    this.resetAttackCooldown();
                    this.mob.doHurtTarget(enemy);
                }
            }

            @Override
            protected double getAttackReachSqr(@NotNull LivingEntity attackTarget) {
                return 9.0;
            }
        });
        // 游荡 / 环顾
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // 目标选择
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    // ========================================================================
    //  tick 更新（服务端权威）
    // ========================================================================

    @Override
    public void tick() {
        super.tick();

        // 首次出生应用格斗体（默认形态）
        if (!initFormApplied) {
            initFormApplied = true;
            if (!level().isClientSide) {
                this.entityData.set(FORM, FormType.GRAPPLE.name());
                this.setTexture(FormType.GRAPPLE.texture);
                applyFormAttributes(FormType.GRAPPLE);
                updateWeaponVisibility();
            }
        }

        // 行走动画重置：空闲时若单次动画已播完则复位
        if (level().isClientSide) {
            return;
        }

        // 仅在服务端复位动画，避免客户端把服务端同步来的攻击动画立刻清掉
        if (getAnimation() != null && !ANIM_NONE.equals(getAnimation())
                && meleeTicksRemaining <= 0 && crossbowTicksRemaining <= 0
                && shockerFxTicks <= 0 && kickTicksRemaining <= 0) {
            setAnimation(ANIM_NONE);
        }

        // 电击体变身 1s 期间：不可移动、不可普攻
        if (shockerFxTicks > 0) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
        }

        // ===== 近战冷却递减 =====
        if (meleeCooldownTicks > 0) meleeCooldownTicks--;

        // ===== 受控近战序列 =====
        if (meleeTicksRemaining > 0) {
            performMeleeTick();
        }

        // ===== 受控踢击序列 =====
        if (kickTicksRemaining > 0) {
            performKickTick();
        }

        // ===== 受控弩击序列 =====
        if (crossbowTicksRemaining > 0) {
            performCrossbowTick();
        }

        // ===== 电击体变身特效：周身随机分布闪电粒子，持续 1s =====
        if (shockerFxTicks > 0) {
            shockerFxTicks--;
            spawnShockerParticles();
        }

        // ===== 形态自动切换（规则驱动） =====
        if (formSwitchCooldown > 0) formSwitchCooldown--;
        LivingEntity target = getTarget();
        boolean hasTarget = target != null && target.isAlive();
        double dist = hasTarget ? distanceTo(target) : 0.0;

        if (!hasTarget) {
            // 无目标时回到格斗体
            if (getForm() != FormType.GRAPPLE) {
                setForm(FormType.GRAPPLE);
            }
        } else if (getForm() == FormType.SHOCKER) {
            // 电击体：保持形态直至目标死亡/丢失（武器切换见下方）
        } else {
            double dy = Math.abs(target.getY() - getY());
            boolean tooFarOrHigh = dist > SHOOT_RANGE_MAX || dy > HIGH_SWITCH_DISTANCE;
            FormType desired;
            // 追击滞回：一旦处于俊敏体就在持续追击，保持俊敏体直到真正贴近目标，
            // 避免追到一半落入中距离(5~15)就被切射击体截胡
            boolean inPursuit = getForm() == FormType.QUICK
                    && (tooFarOrHigh || dist > QUICK_PURSUE_RANGE);
            if (inPursuit) {
                desired = FormType.QUICK;
            } else if (tooFarOrHigh) {
                // 目标过远或过高 → 俊敏体（高机动追击）
                desired = FormType.QUICK;
            } else if (dist > SHOOT_RANGE_MIN && dist < SHOOT_RANGE_MAX) {
                // 5 < 距离 < 15 → 射击体远程输出
                desired = FormType.SHOOTING;
            } else {
                // 近距离 → 格斗体（若已经处于刚力体则保持；受击转刚力在 hurt 中处理）
                desired = (getForm() == FormType.HERCULEAN) ? FormType.HERCULEAN : FormType.GRAPPLE;
            }
            if (desired != getForm() && formSwitchCooldown <= 0) {
                setForm(desired);
                formSwitchCooldown = FORM_SWITCH_COOLDOWN_TICKS;
            }
        }

        // ===== 电击体武器自动切换（遵循基础形态的武器切换规则） =====
        if (getForm() == FormType.SHOCKER && shockerFxTicks <= 0) {
            shockerWeaponTimer--;
            if (shockerWeaponTimer <= 0) {
                shockerWeaponTimer = SHOCKER_WEAPON_SWITCH_TICKS;
                WeaponType next;
                if (hasTarget) {
                    double dy = Math.abs(target.getY() - getY());
                    if (dist > SHOOT_RANGE_MAX || dy > HIGH_SWITCH_DISTANCE) {
                        // 过远/过高 → 杖（俊敏体武器）
                        next = WeaponType.WAND;
                    } else if (dist > SHOOT_RANGE_MIN && dist < SHOOT_RANGE_MAX) {
                        // 中距离 → 弩（射击体武器）
                        next = WeaponType.CROSSBOW;
                    } else {
                        // 近距离 → 近战武器（格斗体 NONE / 刚力体 SWORD）
                        next = this.random.nextBoolean() ? WeaponType.NONE : WeaponType.SWORD;
                    }
                } else {
                    next = WeaponType.NONE;
                }
                setWeapon(next);
            }
        }
    }

    /**
     * 根据情况临时改变的形态持续时间。
     * 基础 20 秒；射击体/俊敏体在有目标对峙时可适当调整。
     */
    private int getCurrentFormDuration() {
        int duration = FORM_DURATION_TICKS;
        if (getTarget() != null) {
            LivingEntity target = getTarget();
            double dist = distanceTo(target);
            // 目标较远且处于射击体时，稍作延长以便远程输出
            if (getForm() == FormType.SHOOTING && dist > 8.0) {
                duration = (int) (FORM_DURATION_TICKS * 1.5);
            } else if (getForm() == FormType.QUICK && dist < 8.0) {
                duration = (int) (FORM_DURATION_TICKS * 0.8);
            }
        }
        return duration;
    }

    /**
     * 根据目标距离随机切换到下一个形态（电击体解锁后才可能加入随机池）。
     * <ul>
     *   <li>距离 > 5 格：在俊敏体（追击）与射击体（远程）之间选择</li>
     *   <li>距离 ≤ 5 格：在近战形态（格斗/刚力/俊敏）之间选择，射击体过近不再选用</li>
     * </ul>
     */
    private void switchToRandomForm(double dist) {
        java.util.List<FormType> pool;
        if (dist > CHASE_SWITCH_DISTANCE) {
            pool = new java.util.ArrayList<>(java.util.Arrays.asList(
                    FormType.QUICK, FormType.SHOOTING
            ));
        } else {
            pool = new java.util.ArrayList<>(java.util.Arrays.asList(
                    FormType.GRAPPLE, FormType.HERCULEAN, FormType.QUICK
            ));
        }
        if (isShockerUnlocked()) {
            pool.add(FormType.SHOCKER);
        }
        if (pool.isEmpty()) return;

        FormType next = pool.get(this.random.nextInt(pool.size()));
        // 避免连续出现相同形态
        if (next == getForm() && pool.size() > 1) {
            FormType prev = next;
            do {
                next = pool.get(this.random.nextInt(pool.size()));
            } while (next == prev);
        }
        setForm(next);
    }

    // ========================================================================
    //  攻击
    // ========================================================================

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!(target instanceof LivingEntity)) return false;

        WeaponType weapon = getCurrentWeapon();
        if (weapon.isRanged) {
            if (crossbowCooldown <= 0 && crossbowTicksRemaining <= 0) {
                startCrossbowAttack();
                return true;
            }
        } else if (meleeCooldownTicks <= 0 && meleeTicksRemaining <= 0
                && kickTicksRemaining <= 0 && shockerFxTicks <= 0) {
            // 无武器（格斗体/电击体无武器）按一定几率触发踢击
            if (weapon == WeaponType.NONE && this.random.nextInt(3) == 0) {
                startKickAttack();
            } else {
                startMeleeAttack();
            }
            return true;
        }
        return false;
    }

    // ===== 踢击（无武器：attack_kick） =====

    private void startKickAttack() {
        kickTicksRemaining = KICK_DURATION_TICKS;
        kickHitTick = KICK_HIT_TICK;
        meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        setAnimation("attack_kick");

        LivingEntity target = getTarget();
        if (target != null) {
            faceTarget(target);
        }
    }

    private void performKickTick() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            faceTarget(target);
            // 播放 0.25s 处造成伤害
            if (kickTicksRemaining == kickHitTick && distanceTo(target) <= 3.5D) {
                target.invulnerableTime = 0;
                // 踢击击退目标，LivingEntity.knockback 会按目标击退抗性自动折减强度
                target.knockback(1.5, target.getX() - this.getX(), target.getZ() - this.getZ());
                target.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }

        kickTicksRemaining--;
        if (kickTicksRemaining <= 0) {
            kickTicksRemaining = 0;
            setAnimation(ANIM_NONE);
        }
    }

    // ===== 近战（attack1/2、sword_attack1/2、wand_attack1/2） =====

    private void startMeleeAttack() {
        WeaponType weapon = getCurrentWeapon();
        // 格斗体/刚力体动画 0.75s、0.5s 出伤；俊敏体动画 0.5s、0.25s 出伤
        if (weapon == WeaponType.WAND) {
            meleeTicksRemaining = WAND_MELEE_DURATION_TICKS;
            meleeHitTick = WAND_MELEE_HIT_TICK;
        } else {
            meleeTicksRemaining = MELEE_DURATION_TICKS;
            meleeHitTick = MELEE_HIT_TICK;
        }
        meleeCooldownTicks = MELEE_COOLDOWN_TICKS;
        lastMeleeWasAttack1 = !lastMeleeWasAttack1;

        setAnimation(lastMeleeWasAttack1 ? weapon.attackAnim1 : weapon.attackAnim2);

        LivingEntity target = getTarget();
        if (target != null) {
            faceTarget(target);
        }
    }

    private void performMeleeTick() {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            faceTarget(target);
            if (meleeTicksRemaining == meleeHitTick && distanceTo(target) <= 3.5D) {
                target.invulnerableTime = 0;
                // 刚力体普攻附带击退（受目标击退抗性影响）
                if (getForm() == FormType.HERCULEAN) {
                    target.knockback(1.5, target.getX() - this.getX(), target.getZ() - this.getZ());
                }
                target.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }

        meleeTicksRemaining--;
        if (meleeTicksRemaining <= 0) {
            meleeTicksRemaining = 0;
            setAnimation(ANIM_NONE);
        }
    }

    // ===== 射击（crossbow_attack，1s 发射、1.5s 动画） =====

    private void startCrossbowAttack() {
        crossbowCooldown = CROSSBOW_COOLDOWN_TICKS;
        crossbowTicksRemaining = CROSSBOW_ANIMATION_TICKS;
        setAnimation(WeaponType.CROSSBOW.attackAnim1);

        LivingEntity target = getTarget();
        if (target != null) {
            faceTarget(target);
        }
    }

    private void performCrossbowTick() {
        getNavigation().stop();

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            faceTarget(target);
        }

        crossbowTicksRemaining--;
        // 剩余 CROSSBOW_FIRE_TICK 刻（即已播放 1s）时发射风弹，随后动画继续播放至结束
        if (crossbowTicksRemaining == CROSSBOW_FIRE_TICK) {
            fireWindBolt();
        }
        if (crossbowTicksRemaining <= 0) {
            crossbowTicksRemaining = 0;
            setAnimation(ANIM_NONE);
        }
    }

    /**
     * 发射与天马弩子弹相同的风弹。射手为本实体，风弹命中逻辑已扩展支持非玩家射手。
     */
    private void fireWindBolt() {
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // 直接朝目标位置发射（而非依赖实体偏航），确保飞行方向实时指向目标，避免世界固定方向
        Vec3 start = this.getEyePosition();
        Vec3 aim;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.35D, 0);
            aim = targetPos.subtract(start);
        } else {
            aim = this.getViewVector(1.0F);
        }

        GoGadoruBaWindProjectile projectile = new GoGadoruBaWindProjectile(sl, this, 0.0F);
        // shoot 会按方向矢量同时设定速度与偏航/俯仰（使弹体朝目标飞行）
        projectile.shoot(aim.x, aim.y, aim.z, PROJECTILE_SPEED, 0.1F);
        sl.addFreshEntity(projectile);

        sl.playSound(null, blockPosition(),
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) return;
        Vec3 toTarget = target.position().subtract(this.position());
        if (toTarget.horizontalDistanceSqr() <= 0.001D) return;
        float yaw = (float) (Math.atan2(toTarget.z, toTarget.x) * (180D / Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    // ========================================================================
    //  伤害减免（50% 基础 + 形态额外）
    // ========================================================================

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // 电击体：闪电磁场免疫（1s 窗口内最多 3 次，仅限 <50% 最大生命的闪电伤害）
        if (getForm() == FormType.SHOCKER
                && source.is(DamageTypeTags.IS_LIGHTNING)
                && !level().isClientSide
                && amount > 0.0F
                && amount < this.getMaxHealth() * SHOCKER_MAX_HP_THRESHOLD) {
            // 1s 滑动窗口重置
            if (tickCount - shockerImmuneWindowStart >= SHOCKER_IMMUNE_WINDOW_TICKS) {
                shockerImmuneWindowStart = tickCount;
                shockerImmuneCount = 0;
            }
            if (shockerImmuneCount < SHOCKER_MAX_IMMUNE_PER_WINDOW) {
                shockerImmuneCount++;
                shockerTotalImmune++;
                // 每次免疫恢复 5% 最大生命
                this.heal(this.getMaxHealth() * SHOCKER_IMMUNE_HEAL_RATIO);
                // 每累计免疫 10 次，提升 1% 属性
                int buffLevel = shockerTotalImmune / 10;
                if (buffLevel > shockerBuffLevel) {
                    shockerBuffLevel = buffLevel;
                    applyShockerBuff(buffLevel);
                }
                return false; // 完全免疫
            }
        }

        float applied = amount;
        if (!source.is(VOID_DAMAGE)) {
            // 基类已应用 50% 基础减伤，这里再叠加形态额外减伤系数
            applied = amount * getForm().damageReductionMult;
        }
        boolean result = super.hurt(source, applied);

        // 单次受到伤害 > 5% 最大生命 且 目标与自身距离 < 5 格 → 切换为刚力体防御
        if (result && !level().isClientSide
                && getForm() != FormType.SHOCKER && shockerFxTicks <= 0
                && applied > this.getMaxHealth() * 0.05F) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && distanceTo(target) < 5.0D) {
                setForm(FormType.HERCULEAN);
            }
        }
        return result;
    }

    /**
     * 电击体属性提升：每级提升 1%（基于当前形态属性再乘倍率）。
     */
    private void applyShockerBuff(int level) {
        if (level().isClientSide) return;
        double mult = 1.0 + 0.01 * level;
        setAttributeBase(Attributes.MAX_HEALTH, BASE_HEALTH * getForm().healthMult * mult);
        setAttributeBase(Attributes.ATTACK_DAMAGE, BASE_ATTACK * getForm().attackMult * mult);
        setAttributeBase(Attributes.MOVEMENT_SPEED, BASE_SPEED * getForm().speedMult * mult);
        setAttributeBase(Attributes.ARMOR, BASE_ARMOR * getForm().armorMult * mult);
        setAttributeBase(Attributes.ARMOR_TOUGHNESS, BASE_ARMOR_TOUGHNESS * getForm().armorMult * mult);
        // 确保当前血量不超出新的生命上限
        float newMax = this.getMaxHealth();
        this.setHealth(Math.min(this.getHealth(), newMax));
    }

    // ========================================================================
    //  俊敏体高跳（类似 Zu-Baduu-Ba，不播放跳跃动画）+ 免疫坠落
    // ========================================================================

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        if (getForm() == FormType.QUICK) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    private class QuickJumpGoal extends Goal {
        public QuickJumpGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getForm() != FormType.QUICK) return false;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (shockerFxTicks > 0) return false; // 变身期间禁止移动
            if (jumpCooldown > 0) return false;
            if (!onGround()) return false;
            if (meleeTicksRemaining > 0 || kickTicksRemaining > 0) return false;

            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double heightDiff = target.getY() - getY();

            // 水平距离在追击范围内跳跃；若目标位于高处（水平贴近但垂直差大）也应触发大跳
            boolean inHorizontalRange = horizontalDist >= 2.0 && horizontalDist <= 24.0;
            boolean highUpTarget = horizontalDist < 2.0 && heightDiff > 1.0 && heightDiff <= 24.0;
            return inHorizontalRange || highUpTarget;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) return;

            getLookControl().setLookAt(target, 360.0f, 360.0f);
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double heightDiff = target.getY() - getY();

            double normalizedDist = Math.min(Math.max((horizontalDist - 3.0) / 21.0, 0.0), 1.0);
            double vyFromDist = 0.6 + normalizedDist * 1.2;
            double minVyForHeight = heightDiff > 0 ? Math.sqrt(0.16 * heightDiff) + 0.3 : 0.4;
            double vy = Math.min(Math.max(vyFromDist, minVyForHeight), 2.2);

            double airTime = vy * 25.0;
            double dragFactor = 1.0 - Math.pow(0.91, airTime);
            double targetLanding = horizontalDist * 0.9;
            double vh = Math.min(Math.max(targetLanding * 0.09 / Math.max(dragFactor, 0.01), 0.2), 1.5);

            if (horizontalDist < 0.01) {
                setDeltaMovement(0, vy, 0);
            } else {
                setDeltaMovement(dx / horizontalDist * vh, vy, dz / horizontalDist * vh);
            }
            hasImpulse = true;
            jumpCooldown = JUMP_COOLDOWN_TICKS;
        }
    }

    // ===== 射击体远程攻击目标 =====

    private class RangedAttackGoal extends Goal {
        private static final double RANGE_MAX = 16.0;
        private static final double RANGE_MIN = 5.0;

        public RangedAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (shockerFxTicks > 0) return false; // 变身期间禁止攻击/移动
            if (getCurrentWeapon() != WeaponType.CROSSBOW) return false;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) return false;
            if (crossbowTicksRemaining > 0) return false;
            double dist = distanceTo(target);
            return dist >= RANGE_MIN && dist <= RANGE_MAX;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            startCrossbowAttack();
        }
    }

    // ========================================================================
    //  数据持久化与死亡
    // ========================================================================

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("CurrentForm", getForm().name());
        compound.putString("CurrentWeapon", getCurrentWeapon().name());
        compound.putBoolean("ShockerUnlocked", isShockerUnlocked());
        compound.putInt("MeleeCooldown", meleeCooldownTicks);
        compound.putInt("MeleeTicks", meleeTicksRemaining);
        compound.putBoolean("LastMeleeWasAttack1", lastMeleeWasAttack1);
        compound.putInt("CrossbowCooldown", crossbowCooldown);
        compound.putInt("JumpCooldown", jumpCooldown);
        compound.putInt("FormTimer", formTimer);
        compound.putInt("ShockerBuffLevel", shockerBuffLevel);
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CurrentForm")) {
            try {
                setForm(FormType.valueOf(compound.getString("CurrentForm")));
            } catch (IllegalArgumentException e) {
                setForm(FormType.GRAPPLE);
            }
        } else {
            setForm(FormType.GRAPPLE);
        }
        // 从 NBT 恢复的形态不再由首个 tick 的默认逻辑覆盖
        initFormApplied = true;
        if (compound.contains("CurrentWeapon")) {
            try {
                setWeapon(WeaponType.valueOf(compound.getString("CurrentWeapon")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (compound.contains("ShockerUnlocked")) {
            this.entityData.set(SHOCKER_UNLOCKED, compound.getBoolean("ShockerUnlocked"));
        }
        if (compound.contains("MeleeCooldown")) meleeCooldownTicks = compound.getInt("MeleeCooldown");
        if (compound.contains("MeleeTicks")) meleeTicksRemaining = compound.getInt("MeleeTicks");
        if (compound.contains("LastMeleeWasAttack1")) lastMeleeWasAttack1 = compound.getBoolean("LastMeleeWasAttack1");
        if (compound.contains("CrossbowCooldown")) crossbowCooldown = compound.getInt("CrossbowCooldown");
        if (compound.contains("JumpCooldown")) jumpCooldown = compound.getInt("JumpCooldown");
        if (compound.contains("FormTimer")) formTimer = compound.getInt("FormTimer");
        if (compound.contains("ShockerBuffLevel")) {
            shockerBuffLevel = compound.getInt("ShockerBuffLevel");
            // 载入完成后重新套用电击体属性提升
            if (getForm() == FormType.SHOCKER && shockerBuffLevel > 0) {
                applyShockerBuff(shockerBuffLevel);
            }
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    public static void init() {
    }

    // ========================================================================
    //  属性
    // ========================================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ARMOR, BASE_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, BASE_ARMOR_TOUGHNESS)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }
}