#version 330 core

//input from vertex shader
in struct VertexData
{
    vec3 position;
    vec2 texture;
    vec3 normale;
    vec3 toPointLight;
    vec3 toSpotLight;

} vertexData;


//Material
uniform sampler2D diff;
uniform sampler2D emit;
uniform sampler2D specular;
uniform float shininess;

uniform vec3 cyclePointLightCol;
uniform vec3 cyclePointLightAttParam;

uniform vec3 cycleSpotLightCol;
uniform vec3 cycleSpotLightAttParam;
uniform vec2 cycleSpotLightAngle;
uniform vec3 cycleSpotLightDir;

uniform vec3 cycleSpot2LightCol;
uniform vec3 cycleSpot2LightAttParam;
uniform vec2 cycleSpot2LightAngle;
uniform vec3 cycleSpot2LightDir;


uniform vec3 sceneColour;


//fragment shader output
out vec4 color;


vec3 shade(vec3 n, vec3 l, vec3 v, vec3 diff, vec3 spec, float shine){
    //n = normalize(Normal)
    //l = normalize(LightDir)
    //v = normalize(Position)
    vec3 diffuse =  diff * max(0.0, dot(n,l));
    vec3 reflectDir = reflect(-l, n);
    float cosb = max(dot(v, reflectDir), 0.0);
    vec3 specular = spec * pow(cosb, shine);

    return diffuse + specular;
}

float attenuate(float len, vec3 attParam){
    return 1.0/(attParam.x + attParam.y * len + attParam.z * len * len);
}

vec3 pointLightIntensity(vec3 lightcolour, float len){
    return lightcolour * attenuate(len, cyclePointLightAttParam);
}

vec3 spotLightIntensity(vec3 spotlightcolour, float len, vec3 sp, vec3 spDir){
    float costheta = dot(sp, normalize(spDir));
    float cosphi = cos(cycleSpotLightAngle.x);
    float cosgamma = cos(cycleSpotLightAngle.y);

    float intensity = (costheta-cosgamma)/(cosphi-cosgamma);
    float cintensity = clamp(intensity, 0.0f, 1.0f);

    return spotlightcolour * cintensity * attenuate(len, cycleSpotLightAttParam);
    // return spotlightcolour
}

void main(){
    vec3 n = normalize(vertexData.normale);
    vec3 v = normalize(vertexData.position);
    float lpLength = length(vertexData.toPointLight);
    vec3 lp = vertexData.toPointLight/lpLength;
    float spLength = length(vertexData.toSpotLight);
    vec3 sp = vertexData.toSpotLight/spLength;
    vec3 diffCol = texture(diff, vertexData.texture).rgb;
    vec3 emitCol = texture(emit, vertexData.texture).rgb;
    vec3 specularCol = texture(specular, vertexData.texture).rgb;

    //emissive Term
    vec3 result = emitCol * sceneColour;

    //ambient Term
    result += shade(n, lp, v, diffCol, specularCol, shininess) * pointLightIntensity(cyclePointLightCol, lpLength);
    result += shade(n, sp, v, diffCol, specularCol, shininess) * spotLightIntensity(cycleSpotLightCol, spLength, sp, cycleSpotLightDir);

    color = vec4(result, 1.0);

}