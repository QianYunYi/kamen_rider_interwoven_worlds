#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Intensity;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;

    // 重影 —— 缓慢漂移的残像
    float ghostOffset = Intensity * 0.012;
    float ghostAngle = Time * 0.3;
    vec2 ghostDir = vec2(cos(ghostAngle), sin(ghostAngle));
    vec3 ghostColor = texture(DiffuseSampler, uv + ghostDir * ghostOffset).rgb;

    vec3 original = texture(DiffuseSampler, uv).rgb;
    vec3 color = mix(original, ghostColor, Intensity * 0.45);

    // 去饱和度
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(gray), Intensity * 0.65);

    // 冷色调
    vec3 coldTint = vec3(0.55, 0.6, 0.75);
    color = mix(color, color * coldTint, Intensity * 0.45);

    // 隧道视野
    vec2 center = uv - 0.5;
    float dist = length(center);
    float tunnelRadius = 0.85 - Intensity * 0.4;
    float tunnel = 1.0 - smoothstep(tunnelRadius - 0.2, tunnelRadius + 0.1, dist);
    color *= tunnel;

    // 整体变暗
    float darkness = 0.1 + Intensity * 0.3;
    color *= (1.0 - darkness);

    // 边缘暗红
    float edgeDark = smoothstep(0.35, 1.0, dist) * Intensity * 0.2;
    color = mix(color, vec3(0.08, 0.02, 0.02), edgeDark);

    fragColor = vec4(color, 1.0);
}