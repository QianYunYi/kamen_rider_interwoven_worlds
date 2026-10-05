package com.qianyunyi.rider.client.renderer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.qianyunyi.rider.item.model.GrowingFormKuugaItemModel;
import com.qianyunyi.rider.item.GrowingFormKuugaItemItem;

public class GrowingFormKuugaItemArmorRenderer extends BaseKuugaArmorRenderer<GrowingFormKuugaItemItem> {
    public GrowingFormKuugaItemArmorRenderer() {
        super(new GrowingFormKuugaItemModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        this.setAllVisible(false);
        applyNbtMask();
    }

    private void applyNbtMask() {
        ItemStack stack = getCurrentStack();
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();

        int henshinStage = 0;
        for (int i = 1; i <= 5; i++) {
            if (tag.contains("henshin" + i) && tag.getString("henshin" + i).equals("true")) {
                henshinStage = i;
            }
        }
        if (henshinStage == 0) return;

        if (henshinStage >= 1) {
            this.setBoneVisible(this.head, true);
            this.setBoneVisible(this.body, true);
        }
        if (henshinStage >= 2) {
            this.setBoneVisible(this.rightArm, true);
            this.setBoneVisible(this.leftArm, true);
        }
        if (henshinStage >= 3) {
            this.setBoneVisible(this.rightLeg, true);
            this.setBoneVisible(this.leftLeg, true);
        }
        if (henshinStage >= 4) {
            this.setBoneVisible(this.rightBoot, true);
            this.setBoneVisible(this.leftBoot, true);
        }
    }
}