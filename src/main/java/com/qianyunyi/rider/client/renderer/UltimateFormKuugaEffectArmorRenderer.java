package com.qianyunyi.rider.client.renderer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.qianyunyi.rider.item.model.UltimateFormKuugaEffectModel;
import com.qianyunyi.rider.item.UltimateFormKuugaEffectItem;

public class UltimateFormKuugaEffectArmorRenderer extends BaseKuugaArmorRenderer<UltimateFormKuugaEffectItem> {
    public UltimateFormKuugaEffectArmorRenderer() {
        super(new UltimateFormKuugaEffectModel());
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
        applyNbtMask();
    }

    private void applyNbtMask() {
        ItemStack stack = getCurrentStack();
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();

        String[] panel = {"redEye", "darkEye"};
        for (String bone : panel) setBoneVisibleByName(bone, bone.equals(tag.getString("ActiveEye")));
    }

    private void setBoneVisibleByName(String boneName, boolean visible) {
        this.model.getBone(boneName).ifPresent(bone -> this.setBoneVisible(bone, visible));
    }
}