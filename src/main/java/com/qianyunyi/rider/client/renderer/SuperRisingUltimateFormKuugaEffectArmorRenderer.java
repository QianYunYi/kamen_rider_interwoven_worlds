package com.qianyunyi.rider.client.renderer;

import net.minecraft.world.entity.EquipmentSlot;

import com.qianyunyi.rider.item.model.SuperRisingUltimateFormKuugaEffectModel;
import com.qianyunyi.rider.item.SuperRisingUltimateFormKuugaEffectItem;

public class SuperRisingUltimateFormKuugaEffectArmorRenderer extends BaseKuugaArmorRenderer<SuperRisingUltimateFormKuugaEffectItem> {
    public SuperRisingUltimateFormKuugaEffectArmorRenderer() {
        super(new SuperRisingUltimateFormKuugaEffectModel());
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        this.setAllVisible(false);
        this.setBoneVisible(this.head, true);
        this.setBoneVisible(this.body, true);
        this.setBoneVisible(this.rightArm, true);
        this.setBoneVisible(this.leftArm, true);
        this.setBoneVisible(this.rightLeg, true);
        this.setBoneVisible(this.leftLeg, true);
        this.setBoneVisible(this.rightBoot, true);
        this.setBoneVisible(this.leftBoot, true);
    }
}