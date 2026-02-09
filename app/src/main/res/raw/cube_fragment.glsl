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

    // Brighter ambient for vivid colors
    float diff = max(dot(N, L), 0.0);
    float ambient = 0.50;
    float lighting = ambient + diff * 0.55;

    // Blinn-Phong specular
    float spec = pow(max(dot(N, H), 0.0), uSpecularPower) * uSpecularStrength;

    vec3 litColor = uBaseColor * lighting;

    // Texture
    vec3 texColor = texture2D(uTexture, vTexCoord).rgb;
    vec3 finalColor = mix(litColor, litColor * texColor, uTextureStrength);

    // Specular highlight
    finalColor += vec3(spec);

    // === BEVEL EFFECT ===
    // Dark edge border (like the old Canvas renderer had)
    float edgeX = min(vTexCoord.x, 1.0 - vTexCoord.x);
    float edgeY = min(vTexCoord.y, 1.0 - vTexCoord.y);
    float edgeDist = min(edgeX, edgeY);

    // Dark border: narrow dark line at the very edge
    float borderFactor = smoothstep(0.0, 0.04, edgeDist);
    finalColor *= mix(0.25, 1.0, borderFactor);

    // Bright inner bevel highlight near (but not at) the edge
    float bevelHighlight = smoothstep(0.04, 0.12, edgeDist) * (1.0 - smoothstep(0.12, 0.25, edgeDist));
    finalColor += vec3(bevelHighlight * 0.15);

    // Center highlight: subtle bright spot at face center
    float cx = vTexCoord.x - 0.5;
    float cy = vTexCoord.y - 0.5;
    float centerDist = cx * cx + cy * cy;
    float centerHighlight = 1.0 - smoothstep(0.0, 0.15, centerDist);
    finalColor += vec3(centerHighlight * 0.08);

    // Layer clearing flash
    if (uClearFlash > 0.0) {
        if (uClearFlash < 0.3) {
            finalColor = mix(finalColor, vec3(1.0), 0.7);
        } else {
            float fadeAlpha = 1.0 - (uClearFlash - 0.3) / 0.7;
            finalColor *= fadeAlpha;
        }
    }

    // FULLY OPAQUE output — alpha=1.0 for solid cubes
    gl_FragColor = vec4(finalColor, uAlpha);
}
