package com.qianyunyi.rider.client.renderer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

import com.qianyunyi.rider.item.model.ArcleModel;
import com.qianyunyi.rider.item.ArcleItem;

public class ArcleArmorRenderer extends BaseKuugaArmorRenderer<ArcleItem> {

    public static final ResourceLocation NORMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/armor/arcle.png");
    public static final ResourceLocation PETRIFY_TEXTURE = ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/armor/petrify_arcle.png");

    public ArcleArmorRenderer() {
        super(new ArcleModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ArcleItem animatable) {
        ItemStack stack = getCurrentStack();
        if (stack != null && !stack.isEmpty()) {
            CompoundTag tag = stack.getOrCreateTag();
            if ("petrifyRune".equals(tag.getString("ActiveRune"))) {
                return PETRIFY_TEXTURE;
            }
        }
        return NORMAL_TEXTURE;
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        this.setAllVisible(false);
        this.setBoneVisible(this.body, true);
        applyNbtMask();
    }

    private void applyNbtMask() {
        ItemStack stack = getCurrentStack();
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();

        String[] amadam = {"redAmadam", "orangeAmadam", "blueAmadam", "greenAmadam", "purpleAmadam", "goldAmadam", "darkAmadam"};
        for (String boneName : amadam) {
            setBoneVisibleByName(boneName, boneName.equals(tag.getString("ActiveAmadam")));
        }

        String[] panel = {"basePanel", "risingPanel"};
        for (String boneName : panel) {
            setBoneVisibleByName(boneName, boneName.equals(tag.getString("ActivePanel")));
        }

        String[] rune = {"baseRune", "risingRune", "petrifyRune"};
        for (String boneName : rune) {
            setBoneVisibleByName(boneName, boneName.equals(tag.getString("ActiveRune")));
        }

        int level = tag.getInt("CrackLevel");
        setBoneVisibleByName("crack1", level >= 1);
        setBoneVisibleByName("crack2", level >= 2);
        setBoneVisibleByName("crack3", level >= 3);
    }

    private void setBoneVisibleByName(String boneName, boolean visible) {
        GeoBone bone = this.model.getBone(boneName).orElseThrow();
        this.setBoneVisible(bone, visible);
    }
}