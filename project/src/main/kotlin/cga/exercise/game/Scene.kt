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
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*
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
    private val normalShader: ShaderProgram
    private val greyShader: ShaderProgram
    private val toonShader: ShaderProgram
    private val one_bit_monochrom_Shader: ShaderProgram
    private val nightShader: ShaderProgram
    private var usedShader: ShaderProgram

    var setShader: Int = 1
    var setBitShaderColour = 1

    //Meshes
    private var groundmesh: Mesh
    private var gatemesh: Mesh
    private var wallmesh: Mesh
    private var wallmeshright: Mesh
    private var lanternmesh: Mesh
    private var lanternmeshleft: Mesh
//    private var carmesh: Mesh

    //Objekte
    private var gate1 = Renderable()
    private var gate2 = Renderable()
    private var gate3 = Renderable()
    private var ground1 = Renderable()
    private var ground2 = Renderable()
    private var ground3 = Renderable()
    private var car1 = Renderable()
    private var car2 = Renderable()
    private var car3 = Renderable()
    private var wall1 = Renderable()
    private var wall2 = Renderable()
    private var wall3 = Renderable()
    private var wall4 = Renderable()
    private var wall1right = Renderable()
    private var wall2right = Renderable()
    private var wall3right = Renderable()
    private var wall4right = Renderable()
    private var lantern1 = Renderable()
    private var lantern2 = Renderable()
    private var lantern3 = Renderable()
    private var lantern1left = Renderable()
    private var lantern2left = Renderable()
    private var lantern3left = Renderable()

    //Listen für Worldobjects
    private val groundworld = arrayOf(ground1, ground2, ground3)
    private val gateworld = arrayOf(gate1, gate2, gate3)
    private val wallworld = arrayOf(wall1, wall2, wall3, wall4)
    private val wallworldright = arrayOf(wall1right, wall2right, wall3right, wall4right)
    private val lanternworld = arrayOf(lantern1,lantern2,lantern3)
    private val lanternworldleft = arrayOf(lantern1left,lantern2left,lantern3left)
    private var worldID = 0
    private var wallworldID = 0
    private val cars = arrayOf(car1, car2, car3)


    private var camera = TronCamera()

    private var car = ModelLoader.loadModel("assets/models/car/fixed_car/car.obj", Math.toRadians(0.0f), Math.toRadians(180.0f), Math.toRadians(0.0f))
            ?: throw IllegalArgumentException("loading failed")

    private var pointLight = Pointlight(Vector3f(), Vector3f())
    private var spotLightleft = Spotlight(Vector3f(), Vector3f())
    private var spotLightright = Spotlight(Vector3f(), Vector3f())

    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var bool: Boolean = false

    /**
     * Some values for lane-changing & vehicle controls
     *
     */
    private var gamestart = false
    private var is_in_lane_change = false
    private var remaining_lane_change = 0.0
    private var break_overheat = 0
    private var speed = -20.0f

    //scene loading

    private var alreadyloaded = false
    private var wallalreadyloaded = false

    //scene setup
    init {
        staticShader = ShaderProgram("assets/shaders/simple_vert.glsl", "assets/shaders/simple_frag.glsl")
        normalShader = ShaderProgram("assets/shaders/normal_vert.glsl", "assets/shaders/normal_frag.glsl")
        greyShader = ShaderProgram("assets/shaders/grey_vert.glsl", "assets/shaders/grey_frag.glsl")
        toonShader = ShaderProgram("assets/shaders/toon_vert.glsl", "assets/shaders/toon_frag.glsl")
        nightShader = ShaderProgram("assets/shaders/night_vert.glsl", "assets/shaders/night_frag.glsl")
        one_bit_monochrom_Shader = ShaderProgram("assets/shaders/1bit_monochrom_vert.glsl", "assets/shaders/1bit_monochrom_frag.glsl")

        usedShader = normalShader

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

        //Gegenverkehr
        var car1 = ModelLoader.loadModel("assets/models/car2.obj", Math.toRadians(0.0f), Math.toRadians(180.0f), Math.toRadians(0.0f))
                ?: throw IllegalArgumentException("loading failed")

//        val res1: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/car2.obj")
//        val objMesh1: OBJLoader.OBJMesh = res1.objects[0].meshes[0]

        //Ground
        val res2: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/ground.obj")
        val objMesh2: OBJLoader.OBJMesh = res2.objects[0].meshes[0]

        //Gate
        val res3: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/gate2.obj")
        val objMesh3: OBJLoader.OBJMesh = res3.objects[0].meshes[0]

        //Wall
        val res4: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/bigwall.obj")
        val objMesh4: OBJLoader.OBJMesh = res4.objects[0].meshes[0]
        val res5: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/bigwall.obj")
        val objMesh5: OBJLoader.OBJMesh = res5.objects[0].meshes[0]

        //Lanterns
        val res6: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/lantern.obj")
        val objMesh6: OBJLoader.OBJMesh = res6.objects[0].meshes[0]
        val res7: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/lantern.obj")
        val objMesh7: OBJLoader.OBJMesh = res7.objects[0].meshes[0]

        //Material
        val ground_emit = Texture2D("assets/textures/ground2_emit.jpg", true)
        val ground_diff = Texture2D("assets/textures/ground2_diff.jpg", true)
        val ground_spec = Texture2D("assets/textures/ground2_spec.jpg", true)

        val gate_emit = Texture2D("assets/textures/gate_emit.jpg", true)
        val gate_diff = Texture2D("assets/textures/gate_diff.jpg", true)
        val gate_spec = Texture2D("assets/textures/gate_spec.jpg", true)

        ground_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ground_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        ground_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        gate_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)                //Textur trotz Repeat erscheint mir Linear
        gate_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        gate_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val groundMaterial = Material(ground_diff, ground_emit, ground_spec, 60.0f, Vector2f(8.0f, 8.0f))
        val gateMaterial = Material(gate_diff, gate_emit, gate_spec, 60.0f, Vector2f(8.0f, 8.0f))
//        val carMaterial = Material(texture_diff, texture_emit, texture_spec, 100.0f, Vector2f(8.0f, 8.0f))

        //Carmesh
//        carmesh = Mesh(objMesh1.vertexData, objMesh3.indexData, vertexAttributes, carMaterial)
//        car1.list.add(carmesh)
//        car2.list.add(carmesh)
//        car3.list.add(carmesh)

        //Groundmesh
        groundmesh = Mesh(objMesh2.vertexData, objMesh2.indexData, vertexAttributes, groundMaterial)
        ground1.list.add(groundmesh)
        ground2.list.add(groundmesh)
        ground3.list.add(groundmesh)

        //Gatemesh
        gatemesh = Mesh(objMesh3.vertexData, objMesh3.indexData, vertexAttributes, gateMaterial)
        gate1.list.add(gatemesh)
        gate2.list.add(gatemesh)
        gate3.list.add(gatemesh)

        //Wall
        wallmesh = Mesh(objMesh4.vertexData, objMesh4.indexData, vertexAttributes, groundMaterial)
        wall1.list.add(wallmesh)
        wall2.list.add(wallmesh)
        wall3.list.add(wallmesh)
        wall4.list.add(wallmesh)

        wallmeshright = Mesh(objMesh5.vertexData, objMesh5.indexData, vertexAttributes, groundMaterial)
        wall1right.list.add(wallmeshright)
        wall2right.list.add(wallmeshright)
        wall3right.list.add(wallmeshright)
        wall4right.list.add(wallmeshright)

        //Lantern
        lanternmesh = Mesh(objMesh6.vertexData, objMesh6.indexData, vertexAttributes, groundMaterial)
        lantern1.list.add(lanternmesh)
        lantern2.list.add(lanternmesh)
        lantern3.list.add(lanternmesh)

        lanternmeshleft = Mesh(objMesh7.vertexData, objMesh7.indexData, vertexAttributes, groundMaterial)
        lantern1left.list.add(lanternmeshleft)
        lantern2left.list.add(lanternmeshleft)
        lantern3left.list.add(lanternmeshleft)


        //Lighting
        pointLight = Pointlight(camera.getWorldPosition(), Vector3f(1f, 1f, 0f))
        spotLightleft = Spotlight(Vector3f(-0.8f, 1.0f, -1.0f), Vector3f(1.0f))
        spotLightright = Spotlight(Vector3f(0.8f, 1.0f, -1.0f), Vector3f(1.0f))

        //Transformations
        car.scaleLocal(Vector3f(0.8f))

        camera.rotateLocal(Math.toRadians(-25.0f), 0.0f, 0.0f)
        camera.translateLocal(Vector3f(0.0f, 0.5f, 4.0f))
        pointLight.translateLocal(Vector3f(0.0f, 4.0f, 0.0f))
        spotLightleft.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)
        spotLightright.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(), 0.0f)

        //Car 2 Translation
//        car1.translateLocal(Vector3f(0.0f,1.0f, -4.0f))
//        car1.rotateLocal(0.0f,0.0f,0.0f)

        //Ground Translationen
        ground2.translateLocal(Vector3f(0.0f, 0.0f, -45.0f))
        ground3.translateLocal(Vector3f(0.0f, 0.0f, -90.0f))

        //Gate Translatationen
        gate1.translateLocal(Vector3f(0.0f, -1.5f, -45.0f))
        gate1.scaleLocal(Vector3f(0.3f))
        gate2.translateLocal(Vector3f(0.0f, -1.5f, -90.0f))
        gate2.scaleLocal(Vector3f(0.3f))
        gate3.translateLocal(Vector3f(0.0f, -1.5f, -135.0f))
        gate3.scaleLocal(Vector3f(0.3f))

        //Wall Translatationen
        wall1.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall1.translateGlobal(Vector3f(-22.0f, 0.0f, 33.5f))
        wall1.scaleLocal(Vector3f(0.2f))
        wall2.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall2.translateGlobal(Vector3f(-22.0f, 0.0f, -60.0f))
        wall2.scaleLocal(Vector3f(0.2f))
        wall3.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall3.translateGlobal(Vector3f(-22.0f, 0.0f, -153.5f))
        wall3.scaleLocal(Vector3f(0.2f))
        wall4.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall4.translateGlobal(Vector3f(-22.0f, 0.0f, -247.0f))
        wall4.scaleLocal(Vector3f(0.2f))

        wall1right.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall1right.translateGlobal(Vector3f(22.0f, 0.0f, 33.5f))
        wall1right.scaleLocal(Vector3f(0.2f))
        wall2right.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall2right.translateGlobal(Vector3f(22.0f, 0.0f, -60.0f))
        wall2right.scaleLocal(Vector3f(0.2f))
        wall3right.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall3right.translateGlobal(Vector3f(22.0f, 0.0f, -153.5f))
        wall3right.scaleLocal(Vector3f(0.2f))
        wall4right.rotateAroundPoint(0.0f, Math.toRadians(90.0f), 0.0f, Vector3f(0.0f))
        wall4right.translateGlobal(Vector3f(22.0f, 0.0f, -247.0f))
        wall4right.scaleLocal(Vector3f(0.2f))

        //Lantern Translationen
        lantern1.translateLocal(Vector3f(21.0f, -1.5f, -40.0f))
        lantern1.scaleLocal(Vector3f(0.1f))
        lantern1.rotateLocal(0.0f,Math.toRadians(-90.0f),0.0f)
        lantern2.translateLocal(Vector3f(21.0f, -1.5f, -85.0f))
        lantern2.scaleLocal(Vector3f(0.1f))
        lantern2.rotateLocal(0.0f,Math.toRadians(-90.0f),0.0f)
        lantern3.translateLocal(Vector3f(21.0f, -1.5f, -130.0f))
        lantern3.scaleLocal(Vector3f(0.1f))
        lantern3.rotateLocal(0.0f,Math.toRadians(-90.0f),0.0f)

        lantern1left.translateLocal(Vector3f(-21.0f, -1.5f, -40.0f))
        lantern1left.scaleLocal(Vector3f(0.1f))
        lantern1left.rotateLocal(0.0f,Math.toRadians(90.0f),0.0f)
        lantern2left.translateLocal(Vector3f(-21.0f, -1.5f, -85.0f))
        lantern2left.scaleLocal(Vector3f(0.1f))
        lantern2left.rotateLocal(0.0f,Math.toRadians(90.0f),0.0f)
        lantern3left.translateLocal(Vector3f(-21.0f, -1.5f, -130.0f))
        lantern3left.scaleLocal(Vector3f(0.1f))
        lantern3left.rotateLocal(0.0f,Math.toRadians(90.0f),0.0f)


        //Parents
        pointLight.parent = car
        spotLightleft.parent = car
        spotLightright.parent = car
        camera.parent = car

    }

    fun render(dt: Float, t: Float) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        when (setShader) {
            1 -> usedShader = normalShader
            2 -> usedShader = greyShader
            3 -> usedShader = toonShader
            4 -> usedShader = one_bit_monochrom_Shader
            5 -> usedShader = nightShader
            else -> {
            }
        }


        usedShader.use()
        usedShader.setUniform("sceneColour", Vector3f(1.0f, 1.0f, 1.0f))

        camera.bind(usedShader)
        ground1.render(usedShader)
        ground2.render(usedShader)
        ground3.render(usedShader)


        //usedShader.setUniform("sceneColour", Vector3f(abs(sin(t / 1)), abs(sin(t / 3)), abs(sin(t / 2))))

        car.render(usedShader)
        pointLight.bind(usedShader, "cyclePoint")

        spotLightleft.bind(usedShader, "cycleSpot", camera.getCalculateViewMatrix())
        spotLightright.bind(usedShader, "rightSpot", camera.getCalculateViewMatrix())

        car1.render(usedShader)
        //car2.render(usedShader)
        //car3.render(usedShader)

        gate1.render(usedShader)
        gate2.render(usedShader)
        gate3.render(usedShader)

        wall1.render(usedShader)
        wall2.render(usedShader)
        wall3.render(usedShader)
        wall4.render(usedShader)

        wall1right.render(usedShader)
        wall2right.render(usedShader)
        wall3right.render(usedShader)
        wall4right.render(usedShader)

        lantern1.render(usedShader)
        lantern2.render(usedShader)
        lantern3.render(usedShader)

        lantern1left.render(usedShader)
        lantern2left.render(usedShader)
        lantern3left.render(usedShader)

    }

    fun update(dt: Float, t: Float) {
        pointLight.lightCol = Vector3f(abs(sin(t / 1)), abs(sin(t / 3)), abs(sin(t / 2)))


        //Unendlicher Ground und Gates
        if (car.getWorldPosition().z.toInt() % 45 == 0 && alreadyloaded == false && car.getWorldPosition().z.toInt() != 0) {
            alreadyloaded = true
            groundworld[worldID].translateLocal(Vector3f(0.0f, 0.0f, -135.0f))

            //Platzierung der Tore mit leichten Random X Faktor
            var checkgate = car.getWorldPosition().x - gateworld[worldID].getWorldPosition().x - 5.0f
            println("vorher - Car ${car.getWorldPosition().x}-Gate${gateworld[worldID].getWorldPosition().x} = $checkgate")


            //Check Gate Mechanic
            if (checkgate > -11.0f && checkgate < -4.6f)  {                                                             //linkes tor
                speed -= 0.5f
                setBitShaderColour = 1
                usedShader.setUniform("bitcolor", Vector3f(255.0f, 0.0f, 0.0f), "red")
            }
            if (checkgate > -4.5f && checkgate < 2.0f)  {                                                               //mittleres tor
                speed -= 0.5f
                setBitShaderColour = 2
                usedShader.setUniform("bitcolor", Vector3f(0.0f, 255.0f, 0.0f), "green")
            }
                if (checkgate > 2.1f && checkgate < 8.6f)  {                                                            //rechtes tor
                speed -= 0.5f
                setBitShaderColour = 3
                usedShader.setUniform("bitcolor", Vector3f(0.0f, 0.0f, 255.0f), "blue")
            }
            if (checkgate > 8.7f || checkgate < -11.0f)  {                                                              //neben den toren
                if (speed < -10.0f) speed += 1.0f                                                                       //Speed bis auf Minimal 10 verringern können
                setBitShaderColour = 3
                usedShader.setUniform("bitcolor", Vector3f(255.0f, 255.0f, 255.0f), "white")
            }

            gateworld[worldID].translateLocal(Vector3f((Math.random() * 15 - Math.random() * 15).toFloat() * 1.5f, 0.0f, -135.0f / 0.3f))   //Skalierung bei Translation wieder rausrechnen (/0.3f)

            //Korrektur der Tore Falls sie die Spielwelt zu sehr verlassen würden.
            if (gateworld[worldID].getWorldPosition().x > 8) {
                gateworld[worldID].translateLocal(Vector3f(-8.0f / 0.3f, 0.0f, 0.0f / 0.3f))   //Skalierung bei Translation wieder rausrechnen (/0.3f)
            }
            if (gateworld[worldID].getWorldPosition().x < -8) {
                gateworld[worldID].translateLocal(Vector3f(8.0f / 0.3f, 0.0f, 0.0f / 0.3f))   //Skalierung bei Translation wieder rausrechnen (/0.3f)
            }

            //Laternen Replatzierung
            lanternworld[worldID].rotateLocal(0.0f,Math.toRadians(90.0f),0.0f)
            lanternworld[worldID].translateLocal(Vector3f(0.0f, 0.0f, -135.0f / 0.1f))
            lanternworld[worldID].rotateLocal(0.0f,Math.toRadians(-90.0f),0.0f)

            lanternworldleft[worldID].rotateLocal(0.0f,Math.toRadians(-90.0f),0.0f)
            lanternworldleft[worldID].translateLocal(Vector3f(0.0f, 0.0f, -135.0f / 0.1f))
            lanternworldleft[worldID].rotateLocal(0.0f,Math.toRadians(90.0f),0.0f)

//            if (car.getWorldPosition().x > gateworld[worldID].getWorldPosition().x && car.getWorldPosition().x < gateworld[worldID].getWorldPosition().x) {
//                speed -= 1.0f
//                println("gate ${gateworld[worldID]}")
//            }



            worldID += 1
            //println("$worldID")
            if (worldID == groundworld.size) worldID = 0

        }

        if (car.getWorldPosition().z.toInt() % 65 == 0 && wallalreadyloaded == false && car.getWorldPosition().z.toInt() != 0) {
            wallalreadyloaded = true
            wallworld[wallworldID].translateGlobal(Vector3f(0.0f, 0.0f, -280.5f))
            wallworldright[wallworldID].translateGlobal(Vector3f(0.0f, 0.0f, -280.5f))
            //println("${camera.getDirection()}")

            wallworldID += 1

            //println("${car.getWorldPosition().z.toInt()}")
            if (wallworldID == wallworld.size) wallworldID = 0

//            println("Wand ${wallworldID-1} wird bewegt nach ${wallworld[wallworldID].getWorldPosition().x}/${wallworld[wallworldID].getWorldPosition().y}/${wallworld[wallworldID].getWorldPosition().z}")
        }



        if (car.getWorldPosition().z.toInt() % 45 != 0) alreadyloaded = false
        if (car.getWorldPosition().z.toInt() % 65 != 0) wallalreadyloaded = false

        if (window.getKeyState(GLFW_KEY_P)) {
            //println("${camera.getWorldPosition().sub(getWorldZAxis())}")
        }

        if (window.getKeyState(GLFW_KEY_W)) gamestart = true
        if (window.getKeyState(GLFW_KEY_W)) {
            //if (gamestart == true) {


            car.distance += 0.1
            var deg = car.distance % 360
            var degInRad = deg * kotlin.math.PI / 180

            /**
             * Some stuff for lane chaning
             * When their is an active lane_change we need to reduce the remaining lane_change by adding it to our movement vector
             */

            var path_to_side = 0.0
            if (remaining_lane_change > 0 && car.getWorldPosition().x < 21.0f) {                      //
                remaining_lane_change -= 0.05
                path_to_side += 0.05
            }
            if (remaining_lane_change < 0 && car.getWorldPosition().x > -21.0f) {
                remaining_lane_change += 0.05
                path_to_side -= 0.05
            }

            if (break_overheat >= 0) {
                break_overheat -= 1
            }

            car.translateLocal(Vector3f(path_to_side.toFloat(), 0.0f, speed * dt))
            /**
             * When lane_change is over re-enable chaning the lane
             */
            if (remaining_lane_change <= 0.001 || remaining_lane_change >= -0.001) {
                is_in_lane_change = false
            }


        }

        if (window.getKeyState(GLFW_KEY_A)) {
            /**
             * Initiate lane-change
             */
            if (!is_in_lane_change) {
                is_in_lane_change = true
                remaining_lane_change = -3.0
            }
        }

        if (window.getKeyState(GLFW_KEY_D)) {
            /**
             * Initiate lane-change
             */
            if (!is_in_lane_change) {
                is_in_lane_change = true
                remaining_lane_change = 3.0
            }
        }

        if (window.getKeyState(GLFW_KEY_S)) {

            if (break_overheat <= 500) {
                car.translateLocal(Vector3f(0.0f, 0.0f, 15.0f * dt))
                break_overheat += 2
            }
            if (break_overheat >= 498 && window.getKeyState(GLFW_KEY_S) && break_overheat <= 500) {
                break_overheat += 100
            }
        }
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {

        //Shader wechsel
        if (window.getKeyState(GLFW_KEY_1)) {
            when (setShader) {
                1 -> setShader = 2
                2 -> setShader = 3
                3 -> setShader = 4
                4 -> setShader = 5
                5 -> setShader = 1
                else -> {
                    setShader = 1
                }
            }
        }

        //Interaktiver Shader
        if (window.getKeyState(GLFW_KEY_2)) {
            when (setBitShaderColour) {
                1 -> {
                    setBitShaderColour = 2
                    usedShader.setUniform("bitcolor", Vector3f(255.0f, 0.0f, 0.0f), "red")
                }
                2 -> {
                    setBitShaderColour = 3
                    usedShader.setUniform("bitcolor", Vector3f(0.0f, 255.0f, 0.0f), "green")
                }
                3 -> {
                    setBitShaderColour = 4
                    usedShader.setUniform("bitcolor", Vector3f(0.0f, 0.0f, 255.0f), "blue")
                }
                4 -> {
                    setBitShaderColour = 1
                    usedShader.setUniform("bitcolor", Vector3f(255.0f, 255.0f, 255.0f), "white")
                }
                else -> {
                    setShader = 1
                }
            }
        }

    }

    fun onMouseMove(xpos: Double, ypos: Double) {
        val deltaX: Double = xpos - oldMousePosX
        val deltaY: Double = ypos - oldMousePosY

        oldMousePosX = xpos
        oldMousePosY = ypos


        if (bool) {
            println("${camera.getWorldXAxis()}")
            if (camera.getWorldXAxis().z <= 0.8f && camera.getWorldXAxis().z >= -0.8f) {
                camera.rotateAroundPoint(0.0f, Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, Vector3f(0.0f))
            }
            else{
                camera.rotateAroundPoint(0.0f, Math.toRadians(camera.getWorldXAxis().z * 1.05f), 0.0f, Vector3f(0.0f))      //Wenn überschritten wird Camera zurück rotiert
            }
        }

        bool = true

    }

    fun cleanup() {}
}
