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
uniform float uTransparency;
uniform float uClearFlash;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uLightDir);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 H = normalize(L + V);

    // Lambertian diffuse
    float diff = max(dot(N, L), 0.0);
    float ambient = 0.35;
    float lighting = ambient + diff * 0.65;

    // Blinn-Phong specular
    float spec = pow(max(dot(N, H), 0.0), uSpecularPower) * uSpecularStrength;

    // Base color with lighting
    vec3 litColor = uBaseColor * lighting;

    // Texture blend
    vec3 texColor = texture2D(uTexture, vTexCoord).rgb;
    vec3 finalColor = mix(litColor, litColor * texColor, uTextureStrength);

    // Specular on top
    finalColor += vec3(spec);

    // Layer clearing flash
    if (uClearFlash > 0.0) {
        if (uClearFlash < 0.3) {
            finalColor = mix(finalColor, vec3(1.0), 0.6);
        } else {
            float fadeAlpha = 1.0 - (uClearFlash - 0.3) / 0.7;
            finalColor *= fadeAlpha;
        }
    }

    float alpha = uAlpha * uTransparency;
    gl_FragColor = vec4(finalColor, alpha);
}
