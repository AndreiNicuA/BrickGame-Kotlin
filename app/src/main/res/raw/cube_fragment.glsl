precision mediump float;

varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

uniform vec3 uBaseColor;
uniform float uAlpha;
uniform vec3 uLightDir;
uniform vec3 uCameraPos;
uniform sampler2D uTexture;
uniform float uTextureStrength;
uniform float uSpecularPower;
uniform float uSpecularStrength;
uniform float uClearFlash;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uLightDir);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 H = normalize(L + V);

    // Lighting
    float diff = max(dot(N, L), 0.0);
    float ambient = 0.50;
    float lighting = ambient + diff * 0.55;

    // Blinn-Phong specular
    float spec = pow(max(dot(N, H), 0.0), uSpecularPower) * uSpecularStrength;

    // Material color = base piece color tinted by texture
    // textureStrength=0 → pure base color, textureStrength=1 → fully texture-driven
    vec3 texColor = texture2D(uTexture, vTexCoord).rgb;
    // Texture modulates the base color: bright texels preserve color, dark texels darken it
    vec3 materialColor = mix(uBaseColor, uBaseColor * texColor / vec3(0.7), uTextureStrength);

    vec3 finalColor = materialColor * lighting + vec3(spec);

    // Bevel: dark border + highlight ring
    float edgeX = min(vTexCoord.x, 1.0 - vTexCoord.x);
    float edgeY = min(vTexCoord.y, 1.0 - vTexCoord.y);
    float edgeDist = min(edgeX, edgeY);
    float borderFactor = smoothstep(0.0, 0.035, edgeDist);
    finalColor *= mix(0.2, 1.0, borderFactor);
    // Inner highlight bevel
    float bevel = smoothstep(0.035, 0.10, edgeDist) * (1.0 - smoothstep(0.10, 0.22, edgeDist));
    finalColor += vec3(bevel * 0.12);

    // Layer clearing flash
    if (uClearFlash > 0.0) {
        if (uClearFlash < 0.3) {
            finalColor = mix(finalColor, vec3(1.0), 0.7);
        } else {
            finalColor *= 1.0 - (uClearFlash - 0.3) / 0.7;
        }
    }

    gl_FragColor = vec4(finalColor, uAlpha);
}
