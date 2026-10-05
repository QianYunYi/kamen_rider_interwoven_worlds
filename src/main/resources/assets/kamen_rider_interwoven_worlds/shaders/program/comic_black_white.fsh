#version 150

uniform sampler2D DiffuseSampler;
uniform float Threshold;
uniform float Invert; // 新增：反转参数 0=正常 1=反转
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float binary = luminance > Threshold ? 1.0 : 0.0;

    // 反转逻辑
    if (Invert > 0.5) {
        binary = 1.0 - binary;
    }

    fragColor = vec4(binary, binary, binary, 1.0);
}