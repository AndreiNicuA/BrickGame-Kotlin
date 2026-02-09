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
    // Fresnel-like edge glow — edges facing away from camera are brighter
    float edge = 1.0 - abs(dot(N, V));
    float alpha = uAlpha + edge * 0.3;
    vec3 col = uBaseColor * (0.5 + edge * 0.5);
    gl_FragColor = vec4(col, alpha);
}
