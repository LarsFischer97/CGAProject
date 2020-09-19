package cga.exercise.components.camera

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Math
import org.joml.Vector3f
import org.joml.Vector4f

class TronCamera(var fieldOFView : Float = Math.toRadians(90.0f), var aspectRatio : Float = 16.0f/9.0f, var nearPlane : Float = 0.1f, var farPlane : Float = 100.0f): ICamera, Transformable() {

    override fun getCalculateViewMatrix(): Matrix4f {
        val eye = getWorldPosition()
        //val centerbuffer = getWorldPosition().sub(getWorldZAxis())
        val center = getWorldPosition().sub(getWorldZAxis()) //Abstand Zum Objekt
//        val center = Vector3f(0.0f,0.0f,0.1f)
        val up = getWorldYAxis()
        return Matrix4f().lookAt(eye, center, up)
    }

    override fun getCalculateProjectionMatrix(): Matrix4f {
        return Matrix4f().perspective(fieldOFView,aspectRatio,nearPlane,farPlane)
    }

    override fun bind(shader: ShaderProgram) {
        shader.setUniform("view_matrix", getCalculateViewMatrix(), false)
        shader.setUniform("projection_matrix", getCalculateProjectionMatrix(), false)
    }
}