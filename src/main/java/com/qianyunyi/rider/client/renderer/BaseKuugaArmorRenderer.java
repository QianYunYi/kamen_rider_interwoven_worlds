package com.qianyunyi.rider.client.renderer;

import com.qianyunyi.rider.client.renderer.layer.BrightGlowingGeoLayer;
import com.qianyunyi.rider.item.base.BaseAnimatedArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 空我系铠甲的通用渲染器基类。
 * 所有空我形态的骨骼结构完全一致，因此将骨骼初始化、发光层添加、渲染类型统一集中在此处。
 * 子类只需传入对应的 GeoModel 即可。
 *
 * @param <T> 铠甲物品类型
 */
public class BaseKuugaArmorRenderer<T extends BaseAnimatedArmorItem> extends GeoArmorRenderer<T> {

    public BaseKuugaArmorRenderer(GeoModel<T> model) {
        super(model);
        initBones();
        addRenderLayer(new BrightGlowingGeoLayer<>(this));
    }

    private void initBones() {
        this.head = new GeoBone(null, "armorHead", false, 0.0, false, false);
        this.body = new GeoBone(null, "armorBody", false, 0.0, false, false);
        this.rightArm = new GeoBone(null, "armorRightArm", false, 0.0, false, false);
        this.leftArm = new GeoBone(null, "armorLeftArm", false, 0.0, false, false);
        this.rightLeg = new GeoBone(null, "armorRightLeg", false, 0.0, false, false);
        this.leftLeg = new GeoBone(null, "armorLeftLeg", false, 0.0, false, false);
        this.rightBoot = new GeoBone(null, "armorRightBoot", false, 0.0, false, false);
        this.leftBoot = new GeoBone(null, "armorLeftBoot", false, 0.0, false, false);
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}