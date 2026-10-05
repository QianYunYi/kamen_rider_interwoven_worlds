package com.qianyunyi.rider.internal.henshin.ability.strategy;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.module.KuugaDataModule;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramConfig;
import com.qianyunyi.rider.entity.GouramEntity;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModEntities;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModMobEffects;
import com.qianyunyi.rider.init.KamenRiderInterwovenWorldsModSounds;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.GrowingFormKuugaHenshinBehavior;
import com.qianyunyi.rider.internal.henshin.behavior.impl.kuuga.ability.KuugaFormUnlockManager;
import com.qianyunyi.rider.potion.action.SuppressEnergyExplodeAction;
import com.qianyunyi.rider.internal.combat.skill.kuuga.TitanSwordComboHandler;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.util.DelayedTask;
import com.qianyunyi.rider.util.kuuga.KuugaDamageProcess;
import com.qianyunyi.rider.util.kuuga.MorphingPowerUtil;
import com.qianyunyi.rider.util.HenshinUtil;
import com.qianyunyi.rider.util.SpecialEffectsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public abstract class AbstractKuugaFormAttackStrategy implements IRiderFormAttackStrategy {

    private static final long SNEAK_EFFECT_COOLDOWN = 1000L;
    private static final long CLICK_COOLDOWN = 100L;

    // 仅主线程访问，使用 HashMap 消除不必要的同步开销
    private static final Map<UUID, KuugaFormUnlockManager> UNLOCK_MANAGERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SNEAK_EFFECT_TIMES = new HashMap<>();
    private static final Map<UUID, Long> LAST_RIGHT_CLICK_TIMES = new HashMap<>();
    private static final Map<UUID, Long> EMERGENCY_RESCUE_COOLDOWNS = new HashMap<>();

    @Override
    public void onFormEnter(ServerPlayer player) {
        UUID uuid = player.getUUID();
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        if (dataManager != null) {
            KuugaDataModule kuugaData = dataManager.getKuugaData();
            if (kuugaData != null) {
                if (!kuugaData.isDragonForm() || !kuugaData.isPegasusForm() || !kuugaData.isTitanForm()) {
                    UNLOCK_MANAGERS.put(uuid, new KuugaFormUnlockManager(player));
                }
            }
        }
    }

    @Override
    public void onFormLeave(ServerPlayer player) {
        UUID uuid = player.getUUID();
        MorphingPowerUtil.doRestoreAll(player);
        UNLOCK_MANAGERS.remove(uuid);
        LAST_SNEAK_EFFECT_TIMES.remove(uuid);
        LAST_RIGHT_CLICK_TIMES.remove(uuid);
        EMERGENCY_RESCUE_COOLDOWNS.remove(uuid);
    }

    @Override
    public void onLivingAttack(ServerPlayer player, LivingAttackEvent event) {
        if (!event.getEntity().getUUID().equals(player.getUUID())) return;
        absorbIfElemental(player, event);
        KuugaFormUnlockManager mgr = UNLOCK_MANAGERS.get(player.getUUID());
        if (mgr != null) {
            mgr.onHurt();
        }
    }

    @Override
    public void onLivingHurt(ServerPlayer player, LivingHurtEvent event) {
        specialDamageHandling(player, event);
        playPunchSound(player, event);
        if (!event.getEntity().getUUID().equals(player.getUUID())) return;
        checkEmergencyRescue(player);
        reduceIfUltimate(player, event);
        handleHalfHealthDamageUnlock(player);
        float finalDamage = KuugaDamageProcess.processDamage(event, player, event.getSource(), event.getAmount());
        event.setAmount(finalDamage);
    }

    private void handleHalfHealthDamageUnlock(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth() * 0.5f) return;
        KuugaFormUnlockManager mgr = UNLOCK_MANAGERS.get(player.getUUID());
        if (mgr != null) mgr.handleHalfHealthDamage();
    }

    @Override
    public void onLivingDeath(ServerPlayer player, LivingDeathEvent event) {
        handleDeath(player, event);
    }

    @Override
    public void onPlayerTick(ServerPlayer player, TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        handleSneakEffects(player);
        KuugaFormUnlockManager mgr = UNLOCK_MANAGERS.get(player.getUUID());
        if (mgr == null) return;
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);
        if (dataManager != null) {
            KuugaDataModule kuugaData = dataManager.getKuugaData();
            if (kuugaData != null && kuugaData.isPegasusForm()) return;
        }
        mgr.handleSneak(player.isCrouching());
    }

    private void checkEmergencyRescue(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) player.level();

        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null) return;
        KuugaDataModule kuugaData = data.getKuugaData();
        if (kuugaData == null) return;
        if (!kuugaData.isGouramUnlocked()) return;

        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent > GouramConfig.rescueHealthThreshold) return;

        // 血量低于阈值时，仅以一定概率触发救援
        if (level.random.nextDouble() > GouramConfig.rescueTriggerChance) return;

        UUID uuid = player.getUUID();
        long currentTick = level.getGameTime();
        Long lastRescue = EMERGENCY_RESCUE_COOLDOWNS.get(uuid);
        if (lastRescue != null && currentTick - lastRescue < GouramConfig.rescueCooldownSeconds * 20L) {
            return;
        }

        AABB searchArea = new AABB(
                player.getX() - 64, player.getY() - 64, player.getZ() - 64,
                player.getX() + 64, player.getY() + 64, player.getZ() + 64
        );
        List<GouramEntity> existing = level.getEntitiesOfClass(GouramEntity.class, searchArea);
        GouramEntity existingGouram = null;
        for (GouramEntity g : existing) {
            if (g.getOwnerUUID() != null && g.getOwnerUUID().equals(uuid) && g.isAlive()) {
                existingGouram = g;
                break;
            }
        }
        if (existingGouram != null) {
            EMERGENCY_RESCUE_COOLDOWNS.put(uuid, currentTick);
            existingGouram.setRescueClear(true);
            return;
        }

        EMERGENCY_RESCUE_COOLDOWNS.put(uuid, currentTick);

        int spawnRadius = GouramConfig.rescueSearchRadius;
        double spawnX = player.getX();
        double spawnY = player.getY() + 4;
        double spawnZ = player.getZ();
        boolean foundValid = false;

        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * spawnRadius;
            double testX = player.getX() + Math.cos(angle) * dist;
            double testY = player.getY() + 2 + level.random.nextDouble() * (spawnRadius - 2);
            double testZ = player.getZ() + Math.sin(angle) * dist;

            BlockPos testPos = new BlockPos((int) testX, (int) testY, (int) testZ);
            if (!level.getBlockState(testPos).isSolid() && !level.getBlockState(testPos.above()).isSolid()) {
                int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, testPos).getY();
                if (testY > surfaceY + 1) {
                    spawnX = testX;
                    spawnY = testY;
                    spawnZ = testZ;
                    foundValid = true;
                    break;
                }
            }
        }

        if (!foundValid) {
            spawnX = player.getX();
            spawnY = player.getY() + 5;
            spawnZ = player.getZ();
        }

        GouramEntity gouram = new GouramEntity(KamenRiderInterwovenWorldsModEntities.GOURAM.get(), level);
        gouram.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0);
        gouram.setOwnerUUID(player.getUUID());
        gouram.setRescueClear(true);

        level.addFreshEntity(gouram);
    }

    @Override
    public void onLivingJump(ServerPlayer player, LivingEvent.LivingJumpEvent event) {
        KuugaFormUnlockManager mgr = UNLOCK_MANAGERS.get(player.getUUID());
        if (mgr != null) mgr.handleJump();
        if (!player.isCrouching()) return;
        String currentForm = getCurrentForm(player);
        if (currentForm != null) {
            switch (currentForm) {
                case "dragon_form", "rising_dragon_form",
                     "ultimate_form", "ultimate_form_dark_eye",
                     "rising_ultimate_form", "rising_ultimate_form_dark_eye",
                     "super_rising_ultimate_form" ->
                        player.level().playSound(null, player.blockPosition(),
                                KamenRiderInterwovenWorldsModSounds.JUMP_DRAGON.get(),
                                SoundSource.PLAYERS, 1, 1);
                default -> {}
            }
        }
    }


    @Override
    public void onRightClick(ServerPlayer player, PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() == LogicalSide.CLIENT) return;
        if (!event.getEntity().getUUID().equals(player.getUUID())) return;

        var cfgOpt = RiderRegistryHelper.getCurrentFormConfig(player);
        if (cfgOpt.isEmpty()) return;
        FormConfig cfg = cfgOpt.get();

        UUID uuid = player.getUUID();
        long currentTime = System.currentTimeMillis();
        long lastClick = LAST_RIGHT_CLICK_TIMES.getOrDefault(uuid, 0L);
        if (currentTime - lastClick < CLICK_COOLDOWN) {
            event.setCanceled(true);
            return;
        }
        LAST_RIGHT_CLICK_TIMES.put(uuid, currentTime);

        String formName = cfg.getFormName();
        ItemStack heldItem = player.getMainHandItem();
        boolean isMorphWeapon = MorphingPowerUtil.isMorphWeapon(heldItem);

        if (isMorphWeapon) {
            if (player.isCrouching()) {
                ItemStack restored = MorphingPowerUtil.restore(heldItem);
                if (!restored.isEmpty()) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, restored);
                    HenshinUtil.sendAnimationMessage(player, "weapon_changes");
                }
            }
            return;
        }

        switch (formName) {
            case "dragon_form":
            case "rising_dragon_form":
                if (MorphingPowerUtil.eligibleStick(heldItem)) {
                    MorphingPowerUtil.tryMorphStick(cfg, player);
                }
                break;
            case "pegasus_form":
            case "rising_pegasus_form":
                if (heldItem.getItem() instanceof CrossbowItem) {
                    MorphingPowerUtil.tryMorphCrossbow(cfg, player);
                }
                break;
            case "titan_form":
            case "rising_titan_form":
                if (heldItem.getItem() instanceof SwordItem) {
                    MorphingPowerUtil.tryMorphSwordDual(cfg, player);
                }
                break;
            case "ultimate_form":
            case "ultimate_form_dark_eye":
            case "rising_ultimate_form":
            case "rising_ultimate_form_dark_eye":
            case "super_rising_ultimate_form":
                String weaponType = MorphingPowerUtil.getWeaponType(heldItem);
                if (weaponType != null) {
                    switch (weaponType) {
                        case "sword" -> MorphingPowerUtil.tryMorphSwordDual(cfg, player);
                        case "stick" -> MorphingPowerUtil.tryMorphStick(cfg, player);
                        case "crossbow" -> MorphingPowerUtil.tryMorphCrossbow(cfg, player);
                    }
                }
                break;
        }
    }

    protected void specialDamageHandling(ServerPlayer player, LivingHurtEvent event) {}

    private void absorbIfElemental(ServerPlayer player, LivingAttackEvent event) {
        DamageSource src = event.getSource();
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        KuugaDataModule d = data.getKuugaData();

        if (isFallDamage(src)) {
            event.setCanceled(true);
        } else if (isLightningDamage(src)) {
            event.setCanceled(true);
            float damage = event.getAmount();
            float currentSuperMax = d.getSuperEnergyMax();
            final float MAX_SATURATION = 1000.0F;
            if (d.getSuperChargeSaturation() >= MAX_SATURATION) return;
            float chargeAmount = getChargeAmount(currentSuperMax, damage);
            float newSaturation = d.getSuperChargeSaturation() + chargeAmount;
            if (newSaturation > MAX_SATURATION) {
                chargeAmount = MAX_SATURATION - d.getSuperChargeSaturation();
            }
            if (chargeAmount > 0) {
                d.addSuperChargeSaturation(chargeAmount);
                d.setSuperCharging(true);
            }
            if (!d.isHasGoldPower() && d.getAmadamFusion() >= 30) {
                d.setHasGoldPower(true);
            }
            if (d.isHasGoldPower()) {
                if (currentSuperMax >= 3000 && !d.isAmazinglyMightyForm()) {
                    d.setAmazinglyMightyForm(true);
                }
                if (currentSuperMax >= 6000 && !d.isRisingUltimateForm() && d.isUltimateForm()) {
                    d.setRisingUltimateForm(true);
                }
                if (currentSuperMax >= 10000 && !d.isSuperRisingUltimateForm() && d.isUltimateForm()) {
                    d.setSuperRisingUltimateForm(true);
                }
            }
        } else if (isFireDamage(src)) {
            event.setCanceled(true);
            player.clearFire();
            d.addSuperEnergy(event.getAmount());
        }
    }

    private static float getChargeAmount(float currentSuperMax, float damage) {
        if (currentSuperMax <= 1000) return damage;
        if (currentSuperMax < 3000) return damage * 0.7f;
        if (currentSuperMax < 6000) return damage * 0.4f;
        if (currentSuperMax < 10000) return damage * 0.2f;
        return damage * 0.1f;
    }

    private void reduceIfUltimate(ServerPlayer player, LivingHurtEvent event) {
        if (player.hasEffect(KamenRiderInterwovenWorldsModMobEffects.ULTIMATE_POWER.get())) {
            event.setAmount(event.getAmount() * 0.5f);
            event.getEntity().setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * 播放拳击音效
     * @param player 玩家
     * @param event 受伤事件
     */
    private void playPunchSound(ServerPlayer player, LivingHurtEvent event) {
        // 验证攻击者是否为当前玩家，不是则跳过
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || !attacker.getUUID().equals(player.getUUID())) return;
        // 手持物品时不播放空手拳击音效
        if (!player.getMainHandItem().isEmpty()) return;
        // 技能激活时不播放
        if (RiderPlayerVariablesProvider.get(player).isSkillActive()) return;
        // 随机音调变化，范围 0.9~1.1，增加打击感
        float p = 1 + (player.level().getRandom().nextFloat() - 0.5f) * 0.2f;
        player.level().playSound(null, player.blockPosition(),
                KamenRiderInterwovenWorldsModSounds.PUNCH.get(), SoundSource.PLAYERS, 1, p);
    }

    private void handleDeath(ServerPlayer player, LivingDeathEvent event) {
        LivingEntity v = event.getEntity();
        DamageSource source = event.getSource();
        Entity killer = source.getEntity();
        RiderPlayerData dataManager = RiderPlayerVariablesProvider.get(player);

        if (v.getType() == EntityType.VILLAGER) {
            if (killer != null && killer == player) {
                dataManager.getKuugaData().addHolySpring(-10);
                KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}手动击杀村民{}，扣10点神圣之泉", player.getName(), v.getName());
            }
            if (killer != null && killer != player) {
                if (player.hasLineOfSight(v) && player.distanceTo(v) <= 10) {
                    dataManager.getKuugaData().addHolySpring(-5);
                    KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}看见村民{}，扣5点神圣之泉", player.getName(), v.getName());
                }
            }
        }

        if (killer != null && killer == player) {
            boolean isHostile = (v instanceof Monster) || (v instanceof ZombieVillager);
            if (isHostile) {
                boolean hasVillagerNearPlayer = !player.level().getNearbyEntities(
                        Villager.class,
                        TargetingConditions.forNonCombat().range(10),
                        player,
                        player.getBoundingBox().inflate(10)
                ).isEmpty();
                if (hasVillagerNearPlayer) {
                    dataManager.getKuugaData().addHolySpring(3);
                    KamenRiderInterwovenWorldsMod.LOGGER.info("玩家{}击杀怪物{}，玩家10格内有村民，+3点神圣之泉", player.getName(), v.getName());
                }
            }
        }

        if (v.getPersistentData().getBoolean("is_explosive")) {
            event.setCanceled(true);
            if (v.getPersistentData().getBoolean("is_calamity_titan_explosion")) {
                // 灾厄泰坦标记的爆炸，额外延迟1秒再播放音效、粒子和出伤
                final LivingEntity calEntity = v;
                final LivingEntity calPlayer = player;
                DelayedTask.run(v.level(), 20, () -> {
                    calPlayer.level().playSound(null, calPlayer.blockPosition(),
                            KamenRiderInterwovenWorldsModSounds.NORMAL_EXPLOSION.get(), SoundSource.PLAYERS, 1, 1);
                    SuppressEnergyExplodeAction.triggerExplosion(calEntity.level(), calEntity);
                });
            } else {
                player.level().playSound(null, player.blockPosition(),
                        KamenRiderInterwovenWorldsModSounds.NORMAL_EXPLOSION.get(), SoundSource.PLAYERS, 1, 1);
                SuppressEnergyExplodeAction.triggerExplosion(v.level(), v);
            }
            return;
        }

        // 爆炸进行中的二次死亡拦截（is_explosive 已移除，但爆炸流程尚未结束）
        if (v.getPersistentData().getBoolean("is_exploding")) {
            event.setCanceled(true);
            return;
        }

        if (!v.getUUID().equals(player.getUUID())) return;

        KuugaFormUnlockManager mgr = UNLOCK_MANAGERS.get(player.getUUID());
        if (mgr != null) mgr.resetAll();

        event.setCanceled(true);
        player.setHealth(player.getMaxHealth() / 2f);
        GrowingFormKuugaHenshinBehavior.switchToBaseForm(player);
        player.invulnerableTime = 100;
        player.addEffect(new MobEffectInstance(
                KamenRiderInterwovenWorldsModMobEffects.HENSHIN_CANNOT.get(),
                12000, 0, false, false
        ));
    }

    private void handleSneakEffects(ServerPlayer player) {
        if (!player.isCrouching()) return;
        UUID uuid = player.getUUID();
        long currentTime = System.currentTimeMillis();
        long lastTime = LAST_SNEAK_EFFECT_TIMES.getOrDefault(uuid, 0L);
        if (currentTime - lastTime < SNEAK_EFFECT_COOLDOWN) return;
        LAST_SNEAK_EFFECT_TIMES.put(uuid, currentTime);

        String currentForm = getCurrentForm(player);
        if (currentForm == null) return;

        switch (currentForm) {
            case "dragon_form" ->
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 19, false, false));
            case "mighty_form" -> {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 4, false, false));
                highlightNearbyEnemies(player, 5);
            }
            case "pegasus_form" -> {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 4, false, false));
                highlightNearbyEnemies(player, 30);
            }
            case "rising_dragon_form" ->
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 29, false, false));
            case "rising_mighty_form" -> {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 9, false, false));
                highlightNearbyEnemies(player, 10);
            }
            case "rising_pegasus_form" -> {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 9, false, false));
                highlightNearbyEnemies(player, 45);
            }
            case "ultimate_form", "ultimate_form_dark_eye",
                 "rising_ultimate_form", "rising_ultimate_form_dark_eye",
                 "super_rising_ultimate_form" ->
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 29, false, false));
            default -> {}
        }
    }

    protected void highlightNearbyEnemies(ServerPlayer player, float scope) {
        SpecialEffectsUtil.highlightNearbyEnemies(scope, player, 20);
    }

    protected String getCurrentForm(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        return data == null ? null : data.getActiveFormName();
    }

    protected static boolean isFireDamage(DamageSource s) {
        return s.is(DamageTypes.IN_FIRE) || s.is(DamageTypes.ON_FIRE)
                || s.is(DamageTypes.FIREBALL) || s.is(DamageTypes.UNATTRIBUTED_FIREBALL);
    }

    protected static boolean isLightningDamage(DamageSource s) {
        return s.is(DamageTypes.LIGHTNING_BOLT);
    }

    protected static boolean isFallDamage(DamageSource s) {
        return s.is(DamageTypes.FALL) || s.is(DamageTypes.FLY_INTO_WALL);
    }

    // ========== 左键泰坦剑连击 ==========

    @Override
    public void onLeftClickEmpty(ServerPlayer player, PlayerInteractEvent.LeftClickEmpty event) {
        // event 为 null 时表示从 handleTitanSwordComboPacket 直接调用（服务端），放行
        if (event != null && event.getSide() == LogicalSide.CLIENT) return;
        handleTitanSwordCombo(player);
    }

    @Override
    public void onLeftClickBlock(ServerPlayer player, PlayerInteractEvent.LeftClickBlock event) {
        if (event != null && event.getSide() == LogicalSide.CLIENT) return;
        handleTitanSwordCombo(player);
    }

    private void handleTitanSwordCombo(ServerPlayer player) {
        TitanSwordComboHandler.executeCombo(player);
    }
}