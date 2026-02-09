precision mediump float;

attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;

varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    vWorldPos = (uModelMatrix * vec4(aPosition, 1.0)).xyz;
    vNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
    vTexCoord = aTexCoord;
}
