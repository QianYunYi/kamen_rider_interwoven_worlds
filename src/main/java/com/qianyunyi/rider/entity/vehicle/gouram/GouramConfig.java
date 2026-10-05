package com.qianyunyi.rider.entity.vehicle.gouram;

/**
 * 哥莱姆可配置参数表
 * 所有参数集中在此管理，方便手感调试和后续平衡调整
 */
public class GouramConfig {

    // ==================== 模块 C：半血紧急救援 ====================
    public static float rescueHealthThreshold = 0.2f;
    // 触发救援的额外概率（血量低于阈值时，每次受伤有该概率触发）
    public static float rescueTriggerChance = 0.1f;
    public static int rescueCooldownSeconds = 180;
    // 紧急救援的生成半径（哥莱姆在玩家周围多远处生成，越大越惊险）
    public static int rescueSearchRadius = 30;
    public static int rescueMaxClimb = 12;
    public static float rescueClearDamage = 10.0f;
    public static int rescueKnockbackLevel = 5;

    // ==================== 模块 D：空闲巡逻 ====================
    public static float patrolRadiusMin = 15.0f;
    public static float patrolRadiusMax = 25.0f;
    public static float patrolEllipseFactor = 0.6f;
    public static double patrolSpeed = 0.3;

    // ==================== 模块 D：俯冲攻击（战斗支援） ====================
    public static float diveAttackChance = 0.4f;
    public static int diveAttackCooldownTicks = 200;
    public static float diveAttackDamage = 1.0f;
    public static float diveAttackSpeed = 1.5F;

    // ==================== 模块 A：背部骑乘 ====================
    // 骑乘时的前向速度
    public static float pillionForwardSpeed = 1.4f;
    // 骑乘时的加速度
    public static float pillionAcceleration = 0.008f;
    // 骑乘时的最大加速度
    public static float pillionMaxAcceleration = 0.025f;
    // 骑乘时的惯性摩擦系数
    public static float pillionInertiaFriction = 0.94f;
    // 骑乘时的基础滚转速度
    public static float pillionBaseTurnSpeed = 5.0f;
    // 骑乘时的平滑滚转系数
    public static float pillionTurnSmooth = 0.25f;
    // 骑乘时的最大滚转角度
    public static float pillionMaxTurnDelta = 2.5f;
    // 骑乘时的最大驱动时间
    public static int pillionMaxDriveTime = 60;
    // 骑乘时的上升速度
    public static float pillionAscendSpeed = 0.8f;
    // 骑乘时的下降速度
    public static float pillionDescendSpeed = -0.5f;
    // 骑乘时的滚转角度
     public static float pillionRollAmount = 30.0f;
    // 骑乘时的垂直偏移量
    public static double rideBackHeightOffset = 2.5;
    // 骑乘时的前向偏移量（负值=玩家坐在哥莱姆后方）
    public static double rideBackForwardOffset = -0.4;
    // 骑乘时 pitch 旋转轴心高度（相对于哥莱姆实体坐标，越高轴心越靠近玩家座位）
    public static double rideBackPivotY = 2.5;
    // 骑乘时 pitch 旋转幅度缩放（0=不跟pitch, 1=完全跟pitch，默认1.0完全跟随）
    public static double rideBackPitchScale = 1.0;

    // ==================== 模块 A：悬停模式 ====================
    // 悬停模式的垂直速度
    public static float hoverModeVerticalSpeed = 0.02f;
    // 悬停模式的稳定性系数
    public static float hoverModeStabilityFactor = 0.95f;
    // 悬停模式的衰减系数
    public static float hoverModeDecay = 0.92f;

    // ==================== 模块 B：高空救援 ====================
    // 高空救援的阈值高度
    public static int highAltitudeThreshold = 15;
    // 高空救援的生成半径（哥莱姆在玩家周围多远处生成，越大越惊险）
    public static int rescueSpawnRadius = 30;
    // 高空救援的冲刺速度（从远处冲向玩家的速度）
    public static double rescueRushSpeed = 1.5;
    // 高空救援的捕获距离（多近时自动挂载玩家）
    public static double rescueCatchDistance = 1.5;
    // 高空救援的慢落时间
    public static int slowFallingDurationTicks = 200;

    // ==================== 抓握飞行（已有） ====================
    // 抓握飞行的垂直灵敏度
    public static float grabVerticalSensitivity = 0.8f;
    // 抓握飞行的前向速度
    public static float grabForwardSpeed = 0.85f;
    // 抓握飞行的最小高度
	public static float grabMinGroundHeight = 2.5f;
    // 抓握飞行的上升力
	public static float grabLiftForce = 0.3f;
    // 抓握飞行的前向偏移量
    public static double grabForwardOffset = 0.4;
    // 抓握飞行的向下偏移量
    public static double grabDownwardOffset = 0.5;

    // ==================== 跟随主人 ====================
    public static double followStartDistance = 6.0;
    public static double followStopDistance = 4.0;
    public static double followSpeed = 1.2;

    // ==================== 动画键名 ====================
    public static final String ANIM_GOURAM_FLIGHT = "flight";
    public static final String ANIM_GOURAM_FLIGHT_RIDE = "flight_ride";
    public static final String ANIM_GOURAM_RIDE_BACK = "ride_back";
    public static final String ANIM_GOURAM_IDLE = "idle";
    public static final String ANIM_GOURAM_DIVE_ATTACK = "dive_attack";
    public static final String ANIM_GOURAM_EMERGENCY_RESCUE = "emergency_rescue";
    public static final String ANIM_GOURAM_COMBINE = "gouram_combine";
    public static final String ANIM_GOURAM_DISINTEGRATION = "gouram_disintegration";

    public static final String ANIM_PLAYER_GRAB_RIDING = "grab_hand_riding";
    public static final String ANIM_PLAYER_RIDE_BACK = "ride_back_riding";
    public static final String ANIM_PLAYER_FLIGHT_RIDING = "flight_riding";
}