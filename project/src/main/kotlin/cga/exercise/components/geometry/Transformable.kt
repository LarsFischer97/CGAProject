package cga.exercise.components.geometry

import org.joml.Matrix4f
import org.joml.Vector3f
import javax.swing.text.Position

open class Transformable(var parent: Transformable? = null): ITransformable {
    var matrix = Matrix4f()
    var directiongrad = 0.0
    var distance = 0.0

    override fun rotateLocal(pitch: Float, yaw: Float, roll: Float) {
        matrix.rotateXYZ(pitch,yaw,roll)
    }

    override fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f) {
        // eigene Matrix erstellen und multiplizieren
        // 1.Translate midpoint to origin 2.Rotate by α 3.Translate back
        val tempMat = Matrix4f()

        tempMat.translate(altMidpoint)
        tempMat.rotateXYZ(pitch,yaw,roll)
        tempMat.translate(Vector3f(altMidpoint).negate())

        matrix = tempMat.mul(matrix)
    }

    override fun translateLocal(deltaPos: Vector3f) {
        matrix.translate(deltaPos)
    }

    override fun translateGlobal(deltaPos: Vector3f) {
        // eigene Matrix erstellen und multiplizieren
        val translationMatrix = Matrix4f().translate(deltaPos)
        translationMatrix.mul(matrix,matrix)
    }

    override fun scaleLocal(scale: Vector3f) {
        matrix.scale(scale)
    }

    override fun getPosition(): Vector3f {
        return Vector3f(matrix.m30(),matrix.m31(),matrix.m32())
    }


    open fun getDirection(): Double{
        return directiongrad
    }

    fun setDirection(i : Double){
        directiongrad = i
    }

    override fun getWorldPosition(): Vector3f {
        val tempMat = getWorldModelMatrix();
        return Vector3f(tempMat.m30(),tempMat.m31(),tempMat.m32())
    }

    override fun getXAxis(): Vector3f {
        return Vector3f(matrix.m00(),matrix.m01(),matrix.m02()).normalize()
    }

    override fun getYAxis(): Vector3f {
        return Vector3f(matrix.m10(),matrix.m11(),matrix.m12()).normalize()
    }

    override fun getZAxis(): Vector3f {
        return Vector3f(matrix.m20(),matrix.m21(),matrix.m22()).normalize()
    }

    override fun getWorldXAxis(): Vector3f {
        val tempMat = getWorldModelMatrix()
        return Vector3f(tempMat.m00(),tempMat.m01(),tempMat.m02()).normalize()
    }

    override fun getWorldYAxis(): Vector3f {
        val tempMat = getWorldModelMatrix()
        return Vector3f(tempMat.m10(),tempMat.m11(),tempMat.m12()).normalize()
    }

    override fun getWorldZAxis(): Vector3f {
        val tempMat = getWorldModelMatrix()
        return Vector3f(tempMat.m20(),tempMat.m21(),tempMat.m22()).normalize()
    }

    override fun getWorldModelMatrix(): Matrix4f {
        val mul_matrix = getLocalModelMatrix()
        parent?.getWorldModelMatrix()?.mul(matrix, mul_matrix)
        return mul_matrix
    }

    override fun getLocalModelMatrix(): Matrix4f = Matrix4f(matrix)
}