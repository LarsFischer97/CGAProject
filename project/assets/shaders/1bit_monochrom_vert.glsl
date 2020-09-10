#version 330 core

layout(location = 0) in vec3 position;
layout(location =  1) in vec2 tc;
layout(location = 2) in vec3 normale;

//uniforms
// translation object to world
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;

uniform vec2 tcMultiplier;
uniform vec3 cyclePointLightPos;
uniform vec3 cycleSpotLightPos;


out struct VertexData
{
    vec3 position;
    vec2 texture;
    vec3 normale;
    vec3 toPointLight;
    vec3 toSpotLight;
    vec3 bitcolor;

} vertexData;


void main(){
    mat4 modelView = view_matrix * model_matrix;
    vec4 pos =  modelView * vec4(position, 1.0f);
    vec4 nor = inverse(transpose(modelView)) * vec4(normale, 0.0f);

    vec4 lp = view_matrix * vec4(cyclePointLightPos, 1.0); //Position der Lichtquelle im Viewspace
    vertexData.toPointLight = (lp - pos).xyz; //Richtungsvektor der Lichtquelle (im Camera space)

    vec4 lp2 = view_matrix * vec4(cycleSpotLightPos, 1.0);
    vertexData.toSpotLight = (lp2 - pos).xyz;

    gl_Position = projection_matrix * pos;
    vertexData.position = -pos.xyz;
    vertexData.texture = tc * tcMultiplier;
    vertexData.normale = nor.xyz;
}