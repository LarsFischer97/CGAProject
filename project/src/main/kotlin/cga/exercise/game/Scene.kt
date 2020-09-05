package cga.exercise.game

import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.Pointlight
import cga.exercise.components.light.Spotlight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Math
import org.joml.Vector2f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import java.lang.IllegalArgumentException
import java.lang.Math.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.cos
import kotlin.math.sin


/**
 * Created by Fabian on 16.09.2017.
 */
class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val staticShader2: ShaderProgram

    private var mesh4: Mesh

    private var ground = Renderable()
    private var ground2 = Renderable ()
    private var ground3 = Renderable ()
    private val world = arrayOf(ground, ground2, ground3)
    private var worldID = 0

    private var camera = TronCamera()
    private var cycle = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj", Math.toRadians(-90.0f), Math.toRadians(90.0f), 0.0f)
            ?: throw IllegalArgumentException("loading failed")

    private var pointLight = Pointlight(Vector3f(), Vector3f())
    private var spotLight = Spotlight(Vector3f(), Vector3f())

    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var bool: Boolean = false

    /**
     * Some values for lane-changing & vehicle controls
     *
     */
    private var is_in_lane_change = false
    private var remaining_lane_change = 0.0
    private var break_overheat = 0

    //scene loading

    private var alreadyloaded = false

    //scene setup
    init {
        staticShader = ShaderProgram("assets/shaders/simple_vert.glsl", "assets/shaders/simple_frag.glsl")
        staticShader2 = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")

        //initial opengl state
        glClearColor(0f, 0f, 0f, 1.0f); GLError.checkThrow()
        glEnable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //Attributes
        val stride: Int = 8 * 4
        val attrPos = VertexAttribute(0, 3, GL_FLOAT, false, stride, 0)
        val attrTC = VertexAttribute(1, 2, GL_FLOAT, false, stride, 3 * 4)
        val attrNorm = VertexAttribute(2, 3, GL_FLOAT, false, stride, 5 * 4)
        val vertexAttributes = arrayOf(attrPos, attrTC, attrNorm)

        //Ground
        val res2: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/ground.obj")
        val objMesh2: OBJLoader.OBJMesh = res2.objects[0].meshes[0]

        //Material
        val texture_emit = Texture2D("assets/textures/ground_emit.png", true)
        val texture_diff = Texture2D("assets/textures/ground_diff.png", true)
        val texture_spec = Texture2D("assets/textures/ground_spec.png", true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val groundMaterial = Material(texture_diff, texture_emit, texture_spec, 60.0f, Vector2f(64.0f, 64.0f))

        //Groundmesh
        mesh4 = Mesh(objMesh2.vertexData, objMesh2.indexData, vertexAttributes, groundMaterial)
        ground.list.add(mesh4)
        ground2.list.add(mesh4)
        ground3.list.add(mesh4)



        //Lighting
        pointLight = Pointlight(camera.getWorldPosition(), Vector3f(1f, 1f, 0f))
        spotLight = Spotlight(Vector3f(0.0f, 1.0f, -2.0f), Vector3f(1.0f))

        //Transformations
        cycle.scaleLocal(Vector3f(0.8f))
        camera.rotateLocal(Math.toRadians(-35.0f), 0.0f, 0.0f)
        camera.translateLocal(Vector3f(0.0f, 0.0f, 4.0f))
        pointLight.translateLocal(Vector3f(0.0f, 4.0f, 0.0f))
        spotLight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)

        ground2.translateLocal(Vector3f(0.0f,0.0f, -45.0f))
        ground3.translateLocal(Vector3f(0.0f,0.0f, -90.0f))


        //Parents
        pointLight.parent = cycle
        spotLight.parent = cycle
        camera.parent = cycle

    }

    fun render(dt: Float, t: Float) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        staticShader2.use()
        staticShader2.setUniform("sceneColour", Vector3f(1.0f, 1.0f, 1.0f))
        camera.bind(staticShader2)
        ground.render(staticShader2)
        ground2.render(staticShader2)
        ground3.render(staticShader2)

        staticShader2.setUniform("sceneColour", Vector3f(abs(sin(t / 1)), abs(sin(t / 3)), abs(sin(t / 2))))
        cycle.render(staticShader2)
        pointLight.bind(staticShader2, "cyclePoint")
        spotLight.bind(staticShader2, "cycleSpot", camera.getCalculateViewMatrix())
    }

    fun update(dt: Float, t: Float) {

        pointLight.lightCol = Vector3f(abs(sin(t / 1)), abs(sin(t / 3)), abs(sin(t / 2)))
        //Automatisches losfahren


        if (cycle.getWorldPosition().z.toInt()%45 == 0 && alreadyloaded == false && cycle.getWorldPosition().z.toInt() != 0){
            alreadyloaded = true
            world[worldID].translateLocal(Vector3f(0.0f,0.0f, -135.0f))
            //ground.translateLocal(Vector3f(0.0f,0.0f, -135.0f))
            worldID += 1
            println("$worldID")
            if (worldID == world.size) worldID = 0

        }

        if (cycle.getWorldPosition().z.toInt()%45 != 0) alreadyloaded = false


        if (window.getKeyState(GLFW_KEY_W)) {
            cycle.distance +=0.1
            var deg = cycle.distance%360
            var degInRad = deg * kotlin.math.PI / 180

            /**
             * Some stuff for lane chaning
             * When their is an active lane_change we need to reduce the remaining lane_change by adding it to our movement vector
             */
            var path_to_side = 0.0
            if(remaining_lane_change > 0){                      //
                remaining_lane_change -= 0.05
                path_to_side += 0.05
            }
            if(remaining_lane_change < 0){
                remaining_lane_change += 0.05
                path_to_side -= 0.05
            }

            if (break_overheat >= 0){
                break_overheat -= 1
            }
//            cycle.translateLocal(Vector3f(sin(cycle.getDirection() * kotlin.math.PI / 180).toFloat() * dt, 0.0f, (cos((cycle.getDirection() * kotlin.math.PI / 180).toFloat()) * dt * -1)))
            //cycle.translateLocal(Vector3f(path_to_side.toFloat(), sin(degInRad).toFloat() * 5.0f * dt, cos(degInRad).toFloat() * 5.0f * dt))
            cycle.translateLocal(Vector3f(path_to_side.toFloat(), 0.0f, -20.0f * dt))
            //cycle.rotateLocal(deg.toFloat() * dt, 0.0f,0.0f )

            /**
             * When lane_change is over re-enable chaning the lane
             */
            if(remaining_lane_change <= 0.001 || remaining_lane_change >= -0.001){
                is_in_lane_change = false
            }
            //println("${cycle.getWorldPosition().z}")
        }


        if (window.getKeyState(GLFW_KEY_A)) {
            /**
             * Initiate lane-change
             */
            if(!is_in_lane_change){
                is_in_lane_change = true
                remaining_lane_change = -3.0
            }
//            cycle.translateLocal(Vector3f(-5.0f * dt, 0.0f, 0.0f))
//            camera.rotateLocal(0.0f, Math.toRadians(-10.0f * dt),0.0f)
//            cycle.setDirection(cycle.getDirection() + 0.1)
//            println("Key: A - xyz (${cycle.getPosition().x}/ ${cycle.getPosition().y}/ ${cycle.getPosition().z}) direction (${cycle.getDirection()})")
//            cycle.rotateLocal(0.0f, 1.0f * dt, 0.0f)
        }

        if (window.getKeyState(GLFW_KEY_D)) {
            /**
             * Initiate lane-change
             */
            if(!is_in_lane_change){
                is_in_lane_change = true
                remaining_lane_change = 3.0
            }
//            cycle.translateLocal(Vector3f(5.0f * dt, 0.0f, 0.0f))
//            camera.rotateLocal(0.0f, Math.toRadians(10.0f * dt),0.0f)
//            cycle.setDirection(cycle.getDirection() - 0.1)
//            println("Key D - xyz (${cycle.getPosition().x}/ ${cycle.getPosition().y}/ ${cycle.getPosition().z}) direction (${cycle.getDirection()})")
//            cycle.rotateLocal(0.0f, -1.0f * dt, 0.0f)
        }

        if (window.getKeyState(GLFW_KEY_S)) {

            if (break_overheat <= 500) {
                cycle.translateLocal(Vector3f(0.0f, 0.0f, 3.0f * dt))
                break_overheat +=2
            }
            if (break_overheat >= 498) {
                break_overheat +=100
            }
        }

//        if (window.getKeyState(GLFW_KEY_W)){
//            cycle.translateLocal(Vector3f(0.0f, 0.0f, -2.0f * dt))
//        }

        /*
        if(window.getKeyState(GLFW_KEY_W)){
            //Bewege Kugel vorwärts
            if(window.getKeyState(GLFW_KEY_LEFT_SHIFT)){
                cycle.translateLocal(Vector3f(0.0f, 0.0f, -14.0f * dt))
            }
            else if(window.getKeyState(GLFW_KEY_LEFT_CONTROL)){
                cycle.translateLocal(Vector3f(0.0f, 0.0f, -2.0f * dt))
            }
            else {
                cycle.translateLocal(Vector3f(0.0f, 0.0f, -6.0f * dt))
            }
            if (window.getKeyState(GLFW_KEY_A)){
                //Bewege Kamera nach links
                //cycle.rotateLocal(0.0f, Math.toRadians(50.0f * dt),0.0f)

                cycle.translateLocal(Vector3f(-5.0f * dt, 0.0f, 0.0f))

                //camera.rotateLocal(0.0f, Math.toRadians(-10.0f * dt),0.0f)
            }
            if (window.getKeyState(GLFW_KEY_D)){
                //Bewege Sphere/Kamera nach rechts
                //cycle.rotateLocal(0.0f, Math.toRadians(-50.0f * dt),0.0f)
                cycle.translateLocal(Vector3f(5.0f * dt, 0.0f, 0.0f))
                //camera.rotateLocal(0.0f, Math.toRadians(10.0f * dt),0.0f)
            }
        }
        if (window.getKeyState(GLFW_KEY_S)) {
            //Bewege Kugel rückwärts
            cycle.translateLocal(Vector3f(0.0f, 0.0f, 3 * dt))
            if (window.getKeyState(GLFW_KEY_A)) {
                //Bewege Kamera nach links
                cycle.rotateLocal(0.0f, Math.toRadians(0.5f), 0.0f)
            }
            if (window.getKeyState(GLFW_KEY_D)) {
                //Bewege Sphere/Kamera nach rechts
                cycle.rotateLocal(0.0f, Math.toRadians(-0.5f), 0.0f)

            }
        }
        if (window.getKeyState(GLFW_KEY_T)){

            camera.rotateLocal(Math.toRadians(-35.0f), 0.0f, 0.0f)
            camera.translateLocal(Vector3f(0.0f,0.0f,4.0f))
        }
        var a: Boolean? = null
        var d: Boolean? = null


        if (window.getKeyState(GLFW_KEY_A) && a != true){
            camera.rotateLocal(0.0f, Math.toRadians(-60.0f),0.0f)
            d = false
            a = true
        }

        if (window.getKeyState(GLFW_KEY_A) && d != true){
            camera.rotateLocal(0.0f, Math.toRadians(60.0f),0.0f)
            a = false
            d = true
        }


         */
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {


    }

    fun onMouseMove(xpos: Double, ypos: Double) {
        val deltaX: Double = xpos - oldMousePosX
        val deltaY: Double = ypos - oldMousePosY

        oldMousePosX = xpos
        oldMousePosY = ypos

        if (bool) {
            camera.rotateAroundPoint(0.0f, Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, Vector3f(0.0f))
        }
        bool = true
    }

    fun cleanup() {}
}
