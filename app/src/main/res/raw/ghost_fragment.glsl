precision mediump float;

varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

uniform vec3 uBaseColor;
uniform float uAlpha;
uniform vec3 uCameraPos;

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCameraPos - vWorldPos);

    // Fresnel: edges facing away from camera are brighter
    float fresnel = 1.0 - abs(dot(N, V));
    fresnel = fresnel * fresnel; // Sharper falloff

    // Edge outline on UV boundaries
    float edgeX = min(vTexCoord.x, 1.0 - vTexCoord.x);
    float edgeY = min(vTexCoord.y, 1.0 - vTexCoord.y);
    float edge = 1.0 - smoothstep(0.0, 0.06, min(edgeX, edgeY));

    float alpha = uAlpha + fresnel * 0.2 + edge * 0.3;
    vec3 col = uBaseColor * (0.6 + fresnel * 0.4);

    gl_FragColor = vec4(col, alpha);
}
