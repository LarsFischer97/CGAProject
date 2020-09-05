package cga.exercise.components.light

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f

open class Pointlight(var lightPos: Vector3f = Vector3f(), var lightCol: Vector3f = Vector3f(), var attParam: Vector3f = Vector3f(1.0f,0.5f,0.1f)): IPointLight, Transformable() {

    init {
        translateGlobal(lightPos)
    }

    override fun bind(shaderProgram: ShaderProgram, name: String) {
        shaderProgram.setUniform(name + "LightPos", getWorldPosition()) //Vector3f enthält Position nach Gesamtsumme an Translationen inkl. Parents (nicht lightPos übergeben damit sich der Wert noch verändern kann?)
        shaderProgram.setUniform(name + "LightCol", lightCol)
        shaderProgram.setUniform(name + "LightAttParam", attParam) //Attenuation Parameters (zur Lichtabschwächung)
    }
}