package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30
//import java.lang.constant.ConstantDescs.NULL

/**
 * Creates a Mesh object from vertexdata, intexdata and a given set of vertex attributes
 *
 * @param vertexdata plain float array of vertex data
 * @param indexdata  index data
 * @param attributes vertex attributes contained in vertex data
 * @throws Exception If the creation of the required OpenGL objects fails, an exception is thrown
 *
 * Created by Fabian on 16.09.2017.
 */
class Mesh(vertexdata: FloatArray, indexdata: IntArray, attributes: Array<VertexAttribute>, var material: Material? = null) {
    //private data
    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var indexcount = indexdata.size

    init {
        vao = glGenVertexArrays()
        glBindVertexArray(vao)

        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferData(GL_ARRAY_BUFFER,vertexdata,GL_STATIC_DRAW)

        attributes.forEach {
            glEnableVertexAttribArray(it.index); GLError.checkThrow()
            glVertexAttribPointer(it.index, it.n, it.type, it.normalized, it.stride, it.offset); GLError.checkThrow()
        }

        ibo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ibo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexdata,GL_STATIC_DRAW)
    }

    /**
     * renders the mesh
     */
    fun render() {
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES,indexcount,GL_UNSIGNED_INT,0)
        //glDrawElements(GL11.GL_LINES,indexcount,GL_UNSIGNED_INT,0)
        glBindVertexArray(0)
    }

    fun render(shader: ShaderProgram) {
        material?.bind(shader)
        render()
    }

    /**
     * Deletes the previously allocated OpenGL objects for this mesh
     */
    fun cleanup() {
        if (ibo != 0) GL15.glDeleteBuffers(ibo)
        if (vbo != 0) GL15.glDeleteBuffers(vbo)
        if (vao != 0) GL30.glDeleteVertexArrays(vao)
    }
}