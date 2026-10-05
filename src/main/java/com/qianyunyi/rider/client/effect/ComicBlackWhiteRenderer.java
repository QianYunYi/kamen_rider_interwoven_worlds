package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.List;

public class ComicBlackWhiteRenderer {

    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(KamenRiderInterwovenWorldsMod.MODID, "shaders/post/comic_black_white.json");
    private static PostChain postChain;
    private static LivingEntity targetEntity;
    private static int remainingTicks;
    private static float threshold = 0.5f;
    private static float invert = 0.0f; // 0=正常 1=反转
    private static Field passesField;

    static {
        try {
            passesField = PostChain.class.getDeclaredField("passes");
        } catch (NoSuchFieldException e) {
            for (Field field : PostChain.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    passesField = field;
                    break;
                }
            }
        }
        if (passesField != null) {
            passesField.setAccessible(true);
        }
    }

    // 启动效果，支持传入反转参数
    public static void start(LivingEntity entity, int duration, float threshold, float invert) {
        targetEntity = entity;
        remainingTicks = duration;
        ComicBlackWhiteRenderer.threshold = threshold;
        ComicBlackWhiteRenderer.invert = invert;
        loadShader();
    }

    // 手动设置反转（随时调用！）
    public static void setInvert(float invertValue) {
        invert = invertValue;
    }

    public static void stop() {
        targetEntity = null;
        remainingTicks = 0;
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    private static void loadShader() {
        Minecraft mc = Minecraft.getInstance();
        if (postChain != null) {
            postChain.close();
        }
        try {
            postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), SHADER_LOCATION);
            postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void render(float partialTicks) {
        if (postChain == null || remainingTicks <= 0) return;
        updateShaderUniforms();
        postChain.process(partialTicks);
        remainingTicks--;
        if (remainingTicks <= 0) stop();
    }

    @SuppressWarnings("unchecked")
    private static void updateShaderUniforms() {
        if (postChain == null || passesField == null) return;
        try {
            List<PostPass> passes = (List<PostPass>) passesField.get(postChain);
            for (PostPass pass : passes) {
                pass.getEffect().safeGetUniform("Threshold").set(threshold);
                pass.getEffect().safeGetUniform("Invert").set(invert); // 更新反转
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static boolean isActive() {
        return remainingTicks > 0 && postChain != null;
    }
}