package com.qianyunyi.rider.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qianyunyi.rider.entity.vehicle.gouram.HorseGouramTracker;
import com.qianyunyi.rider.entity.model.GouramModel;
import com.qianyunyi.rider.entity.vehicle.gouram.GouramEntity;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.model.GeoModel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HorseGouramLayer extends RenderLayer<Horse, HorseModel<Horse>> {
    private static final ResourceLocation GOURAM_TEXTURE = 
       ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "textures/entities/gouram.png");
    private static final ResourceLocation GOURAM_MODEL = 
       ResourceLocation.fromNamespaceAndPath("kamen_rider_interwoven_worlds", "geo/gouram.geo.json");
    
    private static final Field BODY_FIELD;
    private static final Field HEAD_FIELD;
    
    static {
        Field body = null;
        Field head = null;
        try {
            body = HorseModel.class.getDeclaredField("body");
            body.setAccessible(true);
            head = HorseModel.class.getDeclaredField("headParts");
            head.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        BODY_FIELD = body;
        HEAD_FIELD = head;
    }
    
    private static final Map<String, Consumer<GeoBone>> IDLE_ANIMATION = new HashMap<>();
    
    static {
        IDLE_ANIMATION.put("rightFrontGouram", bone -> {
            bone.setRotX((float) Math.toRadians(-0.7509));
            bone.setPosY((float) Math.toRadians(-0.0218));
            bone.setRotZ((float) Math.toRadians(-85));
            bone.setPosX(-9);
            bone.setPosY(-18.4f);
            bone.setPosZ(3);
        });
        IDLE_ANIMATION.put("leftFrontGouram", bone -> {
            bone.setRotX((float) Math.toRadians(-0.7509));
            bone.setPosY((float) Math.toRadians(-0.0218));
            bone.setRotZ((float) Math.toRadians(85));
            bone.setPosX(9);
            bone.setPosY(-18.4f);
            bone.setPosZ(3);
        });
        IDLE_ANIMATION.put("afterGouram", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(-14);
            bone.setPosZ(11);
        });
        IDLE_ANIMATION.put("helmet", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(1.55f);
            bone.setPosZ(-0.075f);
        });
        IDLE_ANIMATION.put("fender2", bone -> {
            bone.setRotX((float) Math.toRadians(-28.25));
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(-2.175f);
            bone.setPosZ(-0.225f);
            bone.setScaleX(1.0475f);
            bone.setScaleY(1);
            bone.setScaleZ(1);
        });
        IDLE_ANIMATION.put("left_wing", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ((float) Math.toRadians(90));
            bone.setPosX(0);
            bone.setPosY(-8);
            bone.setPosZ(-4);
        });
        IDLE_ANIMATION.put("right_wing", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ((float) Math.toRadians(-90));
            bone.setPosX(0);
            bone.setPosY(-8);
            bone.setPosZ(-4);
        });
        IDLE_ANIMATION.put("leftAfterGouram", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ((float) Math.toRadians(-90));
            bone.setPosX(-1);
            bone.setPosY(-3);
            bone.setPosZ(-3);
        });
        IDLE_ANIMATION.put("rightAfterGouram", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ((float) Math.toRadians(90));
            bone.setPosX(1);
            bone.setPosY(-3);
            bone.setPosZ(-3);
        });
        IDLE_ANIMATION.put("bone9", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(-0.3f);
            bone.setPosZ(-2);
        });
        IDLE_ANIMATION.put("leftFoot", bone -> {
            bone.setPosX(0);
            bone.setPosY(0);
            bone.setPosZ(-2);
        });
        IDLE_ANIMATION.put("rightFoot", bone -> {
            bone.setPosX(0);
            bone.setPosY(0);
            bone.setPosZ(-2);
        });
        IDLE_ANIMATION.put("right_arm", bone -> {
            bone.setRotX(0);
            bone.setPosY((float) Math.toRadians(-35));
            bone.setRotZ(0);
            bone.setPosX(0.15f);
            bone.setPosY(0);
            bone.setPosZ(5.08f);
        });
        IDLE_ANIMATION.put("right_arm9", bone -> {
            bone.setRotX((float) Math.toRadians(-60));
            bone.setPosY((float) Math.toRadians(-30));
            bone.setRotZ((float) Math.toRadians(-50));
            bone.setPosX(0);
            bone.setPosY(0);
            bone.setPosZ(0);
        });
        IDLE_ANIMATION.put("right_horn", bone -> {
            bone.setRotX((float) Math.toRadians(-12.5843));
            bone.setPosY((float) Math.toRadians(2.1006));
            bone.setRotZ((float) Math.toRadians(10.1571));
            bone.setPosX(0);
            bone.setPosY(0.95f);
            bone.setPosZ(0);
        });
        IDLE_ANIMATION.put("left_arm", bone -> {
            bone.setRotX(0);
            bone.setPosY((float) Math.toRadians(35));
            bone.setRotZ(0);
            bone.setPosX(0.15f);
            bone.setPosY(0);
            bone.setPosZ(5.08f);
        });
        IDLE_ANIMATION.put("left_arm6", bone -> {
            bone.setRotX((float) Math.toRadians(-60));
            bone.setPosY((float) Math.toRadians(30));
            bone.setRotZ((float) Math.toRadians(50));
            bone.setPosX(0);
            bone.setPosY(0);
            bone.setPosZ(0);
        });
        IDLE_ANIMATION.put("left_horn", bone -> {
            bone.setRotX((float) Math.toRadians(-12.5843));
            bone.setPosY((float) Math.toRadians(-2.1006));
            bone.setRotZ((float) Math.toRadians(-10.1571));
            bone.setPosX(0);
            bone.setPosY(0.95f);
            bone.setPosZ(0);
        });
        IDLE_ANIMATION.put("rightHead", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(-2);
            bone.setPosZ(0);
        });
        IDLE_ANIMATION.put("leftHead", bone -> {
            bone.setRotX(0);
            bone.setPosY(0);
            bone.setRotZ(0);
            bone.setPosX(0);
            bone.setPosY(-2);
            bone.setPosZ(0);
        });
    }
    
    private final GouramModel gouramModel;
    private final GeoRenderer<GouramEntity> geoRenderer;
    
    public HorseGouramLayer(RenderLayerParent<Horse, HorseModel<Horse>> renderer) {
        super(renderer);
        this.gouramModel = new GouramModel();
        this.geoRenderer = new GeoRenderer<>() {
            @Override
            public GeoModel<GouramEntity> getGeoModel() {
                return gouramModel;
            }
            
            @Override
            public GouramEntity getAnimatable() {
                return null;
            }
            
            @Override
            public ResourceLocation getTextureLocation(GouramEntity animatable) {
                return GOURAM_TEXTURE;
            }

            @Override
            public void fireCompileRenderLayersEvent() {

            }

            @Override
            public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                return false;
            }

            @Override
            public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {

            }

            @Override
            public void updateAnimatedTextureFrame(GouramEntity animatable) {

            }
        };
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       Horse horse, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!HorseGouramTracker.hasGouram(horse) || !horse.isAlive()) return;
        
        HorseModel<Horse> horseModel = this.getParentModel();
        BakedGeoModel bakedModel = gouramModel.getBakedModel(GOURAM_MODEL);
        RenderType renderType = RenderType.entityTranslucent(GOURAM_TEXTURE);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        
        applyIdleAnimation(bakedModel);
        
        ModelPart body = getModelPart(horseModel, BODY_FIELD);
        ModelPart head = getModelPart(horseModel, HEAD_FIELD);
        
        if (body != null && head != null) {
            poseStack.pushPose();
            body.translateAndRotate(poseStack);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(0, -0.6, -0.3);
            renderBone(bakedModel, "rightFrontGouram", poseStack, bufferSource, vertexConsumer, renderType, packedLight);
            renderBone(bakedModel, "leftFrontGouram", poseStack, bufferSource, vertexConsumer, renderType, packedLight);
            renderBone(bakedModel, "afterGouram", poseStack, bufferSource, vertexConsumer, renderType, packedLight);
            poseStack.popPose();

            poseStack.pushPose();
            head.translateAndRotate(poseStack);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(0, -0.85, -1.55);
            poseStack.mulPose(Axis.XP.rotationDegrees(60));
            renderBone(bakedModel, "helmet", poseStack, bufferSource, vertexConsumer, renderType, packedLight);
            poseStack.popPose();
        }
    }
    
    private void applyIdleAnimation(BakedGeoModel model) {
        for (Map.Entry<String, Consumer<GeoBone>> entry : IDLE_ANIMATION.entrySet()) {
            model.getBone(entry.getKey()).ifPresent(entry.getValue());
        }
    }
    
    private static ModelPart getModelPart(HorseModel<Horse> model, Field field) {
        if (field == null) return null;
        try {
            return (ModelPart) field.get(model);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
    
    private void renderBone(BakedGeoModel model, String boneName, 
                            PoseStack poseStack, MultiBufferSource bufferSource,
                            VertexConsumer vertexConsumer, RenderType renderType, 
                            int packedLight) {
        model.getBone(boneName).ifPresent(bone -> {
            geoRenderer.renderRecursively(poseStack, null, bone, renderType, 
                                         bufferSource, vertexConsumer, false, 0, 
                                         packedLight, OverlayTexture.NO_OVERLAY, 
                                         1, 1, 1, 1);
        });
    }
}