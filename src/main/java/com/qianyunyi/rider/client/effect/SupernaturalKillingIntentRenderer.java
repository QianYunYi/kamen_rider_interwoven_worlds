package com.qianyunyi.rider.client.effect;

import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.List;

public class SupernaturalKillingIntentRenderer {

    private static final ResourceLocation SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(
            KamenRiderInterwovenWorldsMod.MODID, "shaders/post/supernatural_killing_intent.json");
    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 60;
    private static final float BREATH_FREQ_START = 0.18f;
    private static final float BREATH_FREQ_END = 0.5f;
    private static final float BREATH_AMPLITUDE_START = 0.25f;
    private static final float BREATH_AMPLITUDE_END = 0.5f;

    private static PostChain postChain;
    private static float targetIntensity;
    private static int totalDuration;
    private static int elapsedTicks;
    private static boolean fadingOut;
    private static Field passesField;

    static {
        for (Field field : PostChain.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(field.getType())) {
                passesField = field;
                passesField.setAccessible(true);
                break;
            }
        }
        if (passesField == null) {
            KamenRiderInterwovenWorldsMod.LOGGER.warn("SupernaturalKillingIntentRenderer: Failed to find passes field in PostChain");
        }
    }

    public static void start(int duration, float intensity) {
        targetIntensity = intensity;
        totalDuration = duration;
        elapsedTicks = 0;
        fadingOut = false;
        loadShader();
    }

    public static void stop() {
        totalDuration = 0;
        elapsedTicks = 0;
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
            postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                    mc.getMainRenderTarget(), SHADER_LOCATION);
            postChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } catch (Exception e) {
            e.printStackTrace();
            postChain = null;
        }
    }

    public static void render(float partialTicks) {
        if (postChain == null || totalDuration <= 0) return;

        elapsedTicks++;

        float intensity = calculateIntensity();
        updateShaderUniforms(partialTicks, intensity);
        postChain.process(partialTicks);

        if (elapsedTicks >= totalDuration + FADE_OUT_TICKS) {
            stop();
        }
    }

    private static float calculateIntensity() {
        if (fadingOut) {
            float fadeProgress = (float) (elapsedTicks - totalDuration) / FADE_OUT_TICKS;
            float base = targetIntensity * Math.max(0.0f, 1.0f - fadeProgress);
            return applyBreathing(base);
        }

        if (elapsedTicks >= totalDuration) {
            fadingOut = true;
            elapsedTicks = totalDuration;
            return applyBreathing(targetIntensity);
        }

        float base;
        if (elapsedTicks < FADE_IN_TICKS) {
            float fadeProgress = (float) elapsedTicks / FADE_IN_TICKS;
            base = targetIntensity * (fadeProgress * fadeProgress * (3.0f - 2.0f * fadeProgress));
        } else {
            base = targetIntensity;
        }

        return applyBreathing(base);
    }

    private static float applyBreathing(float baseIntensity) {
        int effectiveTicks = Math.min(elapsedTicks, totalDuration);
        float progress = (float) effectiveTicks / Math.max(1, totalDuration);

        float freq = BREATH_FREQ_START + (BREATH_FREQ_END - BREATH_FREQ_START) * progress;
        float amplitude = BREATH_AMPLITUDE_START + (BREATH_AMPLITUDE_END - BREATH_AMPLITUDE_START) * progress;

        float breathPhase = effectiveTicks * freq * 0.314159265f;
        float breathWave = (float) Math.sin(breathPhase);

        float exhaleBias = breathWave < 0 ? 1.15f : 0.9f;
        float breath = breathWave * amplitude * exhaleBias;

        return baseIntensity * (0.7f + breath);
    }

    @SuppressWarnings("unchecked")
    private static void updateShaderUniforms(float partialTicks, float intensity) {
        if (postChain == null || passesField == null) return;
        try {
            List<PostPass> passes = (List<PostPass>) passesField.get(postChain);
            float time = (System.currentTimeMillis() % 100000) / 1000.0f;
            for (PostPass pass : passes) {
                pass.getEffect().safeGetUniform("Intensity").set(intensity);
                pass.getEffect().safeGetUniform("Time").set(time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isActive() {
        return totalDuration > 0 && postChain != null;
    }
}