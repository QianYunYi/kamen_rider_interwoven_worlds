package com.qianyunyi.rider.mixin;


import com.qianyunyi.rider.data.RiderPlayerData;
import com.qianyunyi.rider.data.RiderPlayerVariablesProvider;
import com.qianyunyi.rider.data.enums.HenshinState;
import com.qianyunyi.rider.internal.registry.RiderRegistryHelper;
import com.qianyunyi.rider.internal.registry.config.FormConfig;
import com.qianyunyi.rider.internal.registry.config.RiderConfig;
import com.qianyunyi.rider.util.HenshinUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    /* ---------- 拦截所有点击和拖拽 ---------- */
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (player.level().isClientSide()) return;

        FormConfig formCfg = rider_adventure_affiliated$getPlayerFormConfig(player);
        if (formCfg == null) return;

        ItemStack carriedStack = menu.getCarried();
        boolean cancel = false;

        // ============= 1. 手持物品拦截 =============
        if (!carriedStack.isEmpty()) {
            boolean w = rider_adventure_affiliated$isWeapon(carriedStack, formCfg);
            boolean a = rider_adventure_affiliated$isArmor(carriedStack, formCfg);

            if (w) {
                // 武器：取消操作，不清除
                cancel = true;
            }
            if (a) {
                // 盔甲：清除 + 取消
                menu.setCarried(ItemStack.EMPTY);
                cancel = true;
                if (player instanceof ServerPlayer sp) {
                    rider_adventure_affiliated$checkArmorIntegrityAndDeactivateIfNeeded(sp);
                }
            }
        }

        // ============= 2. 点击槽位 =============
        if (slotId >= 0 && slotId < menu.slots.size()) {
            Slot slot = menu.slots.get(slotId);
            if (slot != null && slot.hasItem()) {
                ItemStack slotStack = slot.getItem();
                boolean w = rider_adventure_affiliated$isWeapon(slotStack, formCfg);
                boolean a = rider_adventure_affiliated$isArmor(slotStack, formCfg);

                if (w) {
                    cancel = true;
                }
                if (a) {
                    slot.set(ItemStack.EMPTY);
                    cancel = true;
                    if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.THROW) {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    if (player instanceof ServerPlayer sp) {
                        rider_adventure_affiliated$checkArmorIntegrityAndDeactivateIfNeeded(sp);
                    }
                }
            }
        }

        // ============= 3. 扔出界面外 =============
        if (slotId < 0 && !carriedStack.isEmpty()) {
            boolean w = rider_adventure_affiliated$isWeapon(carriedStack, formCfg);
            boolean a = rider_adventure_affiliated$isArmor(carriedStack, formCfg);

            if (w) {
                cancel = true;
            }
            if (a) {
                menu.setCarried(ItemStack.EMPTY);
                cancel = true;
                if (player instanceof ServerPlayer sp) {
                    rider_adventure_affiliated$checkArmorIntegrityAndDeactivateIfNeeded(sp);
                }
            }
        }

        if (cancel) {
            ci.cancel();
        }
    }

    /* ---------- 获取玩家当前形态配置 ---------- */
    @Unique
    private FormConfig rider_adventure_affiliated$getPlayerFormConfig(Player player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || data.getCurrentState() == HenshinState.INACTIVE) {
            return null;
        }

        String riderName = data.getActiveRiderName();
        String formName = data.getActiveFormName();
        if (riderName == null || formName == null) {
            return null;
        }

        RiderConfig riderCfg = RiderRegistryHelper.getConfig(riderName).orElse(null);
        if (riderCfg == null) {
            return null;
        }

        return riderCfg.getForm(formName);
    }

    /* ---------- 判断是否是武器 ---------- */
    @Unique
    private boolean rider_adventure_affiliated$isWeapon(ItemStack stack, FormConfig formCfg) {
        return formCfg.getWeapons().values().stream().anyMatch(stack::is);
    }

    /* ---------- 判断是否是护甲 ---------- */
    @Unique
    private boolean rider_adventure_affiliated$isArmor(ItemStack stack, FormConfig formCfg) {
        return HenshinUtil.getArmorSlot(stack.getItem(), formCfg.getArmorSet()) != null;
    }

    /* ---------- 检查护甲完整性并在不完整时解除变身 ---------- */
    @Unique
    private void rider_adventure_affiliated$checkArmorIntegrityAndDeactivateIfNeeded(ServerPlayer player) {
        RiderPlayerData data = RiderPlayerVariablesProvider.get(player);
        if (data == null || data.getCurrentState() == HenshinState.INACTIVE) return;

        String riderName = data.getActiveRiderName();
        String formName = data.getActiveFormName();
        if (riderName == null || formName == null) return;

        RiderConfig riderCfg = RiderRegistryHelper.getConfig(riderName).orElse(null);
        if (riderCfg == null) return;

        FormConfig formCfg = riderCfg.getForm(formName);
        if (formCfg == null) return;

        boolean isArmorComplete = HenshinUtil.isFullyEquipped(player, formCfg.getArmorSet());

        if (!isArmorComplete) {
            rider_adventure_affiliated$deactivateTransformation(player, riderCfg, formCfg, data);
        }
    }

    /* ---------- 解除变身并清理护甲 ---------- */
    @Unique
    private void rider_adventure_affiliated$deactivateTransformation(ServerPlayer player, RiderConfig riderCfg, FormConfig formCfg, RiderPlayerData data) {
        data.setCurrentState(HenshinState.INACTIVE);
        data.setActiveRiderName(null);
        data.setActiveFormName(null);

        Item[] armorSet = formCfg.getArmorSet();
        if (armorSet != null) {
            for (int i = 0; i < armorSet.length && i < 4; i++) {
                if (armorSet[i] != null) {
                    HenshinUtil.removeAllItemFromInventory(player, armorSet[i]);
                }
            }
        }

        // 解除变身时才清除武器
        formCfg.getWeapons().values().forEach(weapon -> {
            if (weapon != null) {
                HenshinUtil.removeAllItemFromInventory(player, weapon);
            }
        });

        HenshinUtil.restorePlayerGear(player);
        formCfg.getBehavior().handleDeactivation(player, riderCfg, formCfg);
    }
}