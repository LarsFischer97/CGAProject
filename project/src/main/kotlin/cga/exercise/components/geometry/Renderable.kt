package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11

class Renderable(val list: MutableList <Mesh> = mutableListOf()): IRenderable, Transformable() {

    override fun render(shaderProgram: ShaderProgram) {
        //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        //shaderProgram.use()
        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false)
        list.forEach{
            it.render(shaderProgram)
        }
    }
}