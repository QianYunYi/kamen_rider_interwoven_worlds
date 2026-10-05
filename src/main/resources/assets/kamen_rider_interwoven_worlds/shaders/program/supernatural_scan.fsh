#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;

uniform float ScanCenterX;
uniform float ScanCenterY;
uniform float ScanRadius;
uniform float ScanIntensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec3 grey = vec3(dot(color.rgb, vec3(0.299, 0.587, 0.114)));

    float aspect = OutSize.x / OutSize.y;
    float dx = (texCoord.x - ScanCenterX) * aspect;
    float dy = texCoord.y - ScanCenterY;
    float dist = sqrt(dx * dx + dy * dy);

    float ringWidth = 0.03;
    float t = smoothstep(ScanRadius - ringWidth, ScanRadius + ringWidth, dist);

    vec3 desaturated = mix(grey, color.rgb, t);

    float ring = max(0.0, 1.0 - abs(dist - ScanRadius) / ringWidth);
    vec3 greenRing = vec3(0.05, 0.95, 0.35) * ring * 0.5;

    vec3 result = mix(color.rgb, desaturated + greenRing, ScanIntensity);

    fragColor = vec4(result, 1.0);
}