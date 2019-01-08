package Uebungen;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

public class PhysicsExercise5 implements WindowListener, GLEventListener, KeyListener {

    //  ---------  Global data  ---------------------------

    // ------ General data
    private String windowTitle = "Physics Exercise 5 - Explosion";
    private int windowWidth = 800;
    private int windowHeight = 600;
    private String vShader = MyShaders.vShader2;        // Vertex-Shader
    private String fShader = MyShaders.fShader0;        // Fragment-Shader

    private GLCanvas canvas;                            // OpenGL Window
    private int programId;                              // OpenGL-Id
    private MyGLBase1 mygl;                             // Helper functions
    private int maxVerts = 2048;                        // max. amount Vertices in Vertex-Array

    // ------ Display settings
    private float xleft = -50, xright = 50;
    private float ybottom, ytop;
    private float znear = -200, zfar = 1000;

    // ------ Projection matrix
    private float elevation = 20;
    private float azimut = 40;
    private float dist = 6;                                 // Distance Camera-System von 0

    // ------ Wall data
    private boolean isDiceWall = true;
    private boolean hasExplosionStarted = false;
    private double dt = 0.1;

    // ------ Explosion data
    private ExplosionCircle explosionCircle;
    private float explotionCenterX = 20;
    private float explotionCenterY = 15;
    private float explotionRadius = 10;
    // Velocity ranges
    private float blockVXmin, blockVXmax;
    private float blockVYmin, blockVYmax;
    private float blockVZmin, blockVZmax;

    // ------ Dice
    private Wall diceWall;
    private Quader dice;
    // Rotation data
    private double[] IdF = new double[]{ 1, 2, 1 };

    // ------ Quader
    private Wall quaderWall;
    private Quader quader;
    // Gyro data
    private double[] IqF = new double[]{ 1, 2, 1 };

    // ------ Gravity
    private boolean isGravityOn;
    private final float g = 9.81f;

    /**
     * Gyro Dynamics
     */
    public class Gyro extends GyroDynamics {

        public Gyro (double I1, double I2, double I3) {
            super(I1, I2, I3);
        }

        public Vec3 torque(double[] x) {
            Mat4 rGyro = getRotation();
            Vec3 F = new Vec3(1, 0, 0);
            Mat4 rInv = rGyro.transpose();
            Vec3 FF = rInv.transform(F);
            Vec3 r = new Vec3(
                quaderWall.block.getA(),
                quaderWall.block.getB(),
                quaderWall.block.getC()
            );

            return r.cross(FF);
        }
    }

    /**
     * Wall class
     * ----------------------------------------------------------------
     * - draws a wall with given blocks and saves it's positions
     * - is used to make blocks move on explosion
     * - resets initial wall (blocks) position
     */
    private class Wall {
        double[] x;
        int width;
        int height;
        Vec3[][] wallBlocksPos;
        Vec3[][] wallBlocksV;
        boolean[][] wallBlocksMoving;
        float[][] rotationAngle;
        float[][] rotationV;
        Quader[][] blocks;
        Quader block;
        Gyro[][] gyros;

        public Wall(double[] x, int w, int h, Quader block) {
            this.x = x;
            this.width = w;
            this.height = h;
            this.block = block;
        }

        void initializeBlocks() {
            double rowPos = x[1];
            int rows = (int)(height/x[1]);
            int cols = (int)(width/x[0]);
            wallBlocksPos = new Vec3[rows][cols];
            wallBlocksV = new Vec3[rows][cols];
            wallBlocksMoving = new boolean[rows][cols];
            rotationAngle = new float[rows][cols];
            rotationV = new float[rows][cols];
            blocks = new Quader[rows][cols];
            gyros = new Gyro[rows][cols];

            for (int i = 0; i < rows; i++) {
                double colPos = x[0];

                for (int j = 0; j < cols; j++) {
                    wallBlocksPos[i][j] = new Vec3((float)colPos, (float)rowPos, (float)x[2]);
                    wallBlocksV[i][j] = getRandomThrowVelocity();
                    wallBlocksMoving[i][j] = false;

                    rotationAngle[i][j] = 0;
                    rotationV[i][j] = (float)Math.random() * 4 + 1;
                    gyros[i][j] = new Gyro(1, 1, 0.6);

                    blocks[i][j] = new Quader(mygl, block.getA(), block.getB(), block.getC(),
                        new Vec3(Math.random() + 0.5, Math.random() + 0.5, Math.random() + 0.5));

                    double  w1 = getRandomNumberInRange(10,40);
                    double  w2 = getRandomNumberInRange(10,40);
                    double  w3 = getRandomNumberInRange(10,40);
                    gyros[i][j].setState(w1, w2, w3, 1, 0, 0, 0);

                    colPos += block.getA();
                }

                rowPos += block.getC();
            }
        }

        void drawWall(GL3 gl) {
            boolean isNext = false;

            for (int i = 0; i < wallBlocksPos.length; i++) {
                for (int j = 0; j < wallBlocksPos[i].length; j++) {
                    // Change color for each Square
                    if (isNext) {
                        mygl.setColor(1, 0, 0);
                        isNext = false;
                    } else {
                        mygl.setColor(0, 0, 1);
                        isNext = true;
                    }

                    // Draw quader
                    mygl.pushM();
                    mygl.multM(gl, Mat4.translate(wallBlocksPos[i][j].x, wallBlocksPos[i][j].y, wallBlocksPos[i][j].z));

                    // rotation
                    if (hasExplosionStarted && wallBlocksMoving[i][j] && !hasReachedFloor(wallBlocksPos[i][j])) {
                        if (isDiceWall) {
                            Vec3 rotAxis = blocks[i][j].getRotationAxis();
                            mygl.multM(gl, Mat4.rotate(rotationAngle[i][j], rotAxis.x, rotAxis.y, rotAxis.z));
                        } else {
                            Mat4 Rgyro = gyros[i][j].getRotation();
                            mygl.multM(gl, Rgyro);
                            gyros[i][j].move(0.001);
                        }
                    }

                    block.drawQuader(gl);
                    mygl.popM(gl);
                }
            }

            if (hasExplosionStarted && isDiceWall) {
                for (int i = 0; i < wallBlocksPos.length; i++) {
                    for (int j = 0; j < wallBlocksPos[i].length; j++) {
                        rotationAngle[i][j] += rotationV[i][j];
                    }
                }
            }
        }

        void move() {
            for (int i = 0; i < wallBlocksPos.length; i++) {
                for (int j = 0; j < wallBlocksPos[i].length; j++) {
                    if (
                        (wallBlocksMoving[i][j] || explosionCircle.isBlockInsideExplosion(wallBlocksPos[i][j])) &&
                        !hasReachedFloor(wallBlocksPos[i][j])
                    ) {
                        // Throw block (MOVEMENT)
                        wallBlocksPos[i][j] = wallBlocksPos[i][j].add(wallBlocksV[i][j].scale((float)dt));
                        wallBlocksMoving[i][j] = true;

                        if (isGravityOn) {
                            wallBlocksV[i][j] = wallBlocksV[i][j].add(new Vec3(0, -g, 0).scale((float)dt));
                        }

                        // Turn block (ROTATION)
                        // todo: add rotation (gyro dynamics)

                    }
                }
            }
        }

        void reset() {
            this.initializeBlocks();
        }
    }

    /**
     * Explosion circle class
     * ----------------------------------------------------------------
     * Defines the range of the explosion on the wall
     * and checks if a given block is inside the explosion range
     */
    private class ExplosionCircle {
        public Vec3 centerPosition;
        public float radius;

        public ExplosionCircle(Vec3 pos, float r) {
            centerPosition = pos;
            radius = r;
        }

        public boolean isBlockInsideExplosion(Vec3 blockPos) {
            // todo: also check edges of block (not only middle point

            // is center point of block inside circle
            return centerPosition.distance(blockPos) <= radius;
        }
    }

    //  ---------  Methoden  --------------------------------

    /**
     * Get a random start velocity for a block in wall
     *
     * @return random vector
     */
    private Vec3 getRandomThrowVelocity() {
        if (isGravityOn) {
            blockVXmin = 8; blockVXmax = 16;
            blockVYmin = 8; blockVYmax = 16;
            blockVZmin = 12; blockVZmax = 25;
        } else {
            blockVXmin = 0; blockVXmax = 2;
            blockVYmin = 0; blockVYmax = 2;
            blockVZmin = 2; blockVZmax = 5;
        }

        return new Vec3(
            getRandomNumberInRange(blockVXmin, blockVXmax),
            getRandomNumberInRange(blockVYmin, blockVYmax),
            getRandomNumberInRange(blockVZmin, blockVZmax)
        );
    }

    /**
     * Get random number in given range
     *
     * @param min lower range limit
     * @param max upper range limit
     * @return    random number
     */
    private static float getRandomNumberInRange(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return min + new Random().nextFloat() * (max - min);
    }

    /**
     * Find out if current block has reached floor (then stop it's animation/movement
     *
     * @param blockPos position of current block in wall
     * @return         if block has reached floor
     */
    private boolean hasReachedFloor(Vec3 blockPos) {
        return this.isGravityOn && blockPos.y <= 0;
    }

    /**
     * @param gl gl object
     * @param x1 start x position
     * @param y1 start y position
     * @param z1 start z position
     * @param x2 end x position
     * @param y2 end y position
     * @param z2 end z position
     */
    private void drawLine(GL3 gl, float x1, float y1, float z1,
                          float x2, float y2, float z2
    ) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINE_STRIP);
    }

    /**
     * Draw a blue line to visualize, that gravity is set on
     *
     * @param gl gl object
     */
    private void drawGravityArrow(GL3 gl) {
        mygl.setColor(0, 0, 1);
        drawLine(gl, -10, 20, 2, -10, 10, 2);
        drawLine(gl, -10, 10, 2, -11, 11, 2);
        drawLine(gl, -10, 10, 2, -9, 11, 2);
    }

    // Constructor
    public PhysicsExercise5() { createFrame(); }

    // Create window
    void createFrame()
    {
        Frame f = new Frame(windowTitle);
        f.setSize(windowWidth, windowHeight);
        f.addWindowListener(this);
        f.addKeyListener(this);

        GLProfile glp = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCaps = new GLCapabilities(glp);

        canvas = new GLCanvas(glCaps);
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);

        f.add(canvas);
        f.setVisible(true);
    }

    //  ----------  OpenGL-Events   ---------------------------

    //  Initialisation
    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL3 gl = drawable.getGL().getGL3();
        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();
        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);              // OpenGL helper functions
        gl.glClearColor(0.9f, 0.9f, 0.9f, 1);        // Background color

        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1, 1);

        double wallZ = 1;
        double[] wallStartPos = new double[] {1, 1, wallZ};

        dice = new Quader(mygl, 2, 2, 2, new Vec3(Math.random() + 0.5, Math.random() + 0.5, Math.random() + 0.5));
        diceWall = new Wall(wallStartPos, 25, 15, dice);
        diceWall.initializeBlocks();

        quader = new Quader(mygl, 2, 1, 1);
        quaderWall = new Wall(wallStartPos, 25, 21, quader);
        quaderWall.initializeBlocks();

        Vec3 explotionCenterPos = new Vec3(explotionCenterX, explotionCenterY, wallZ);
        explosionCircle = new ExplosionCircle(explotionCenterPos, explotionRadius);

        isGravityOn = false;

        // Initialize animation with 40 fps and start (Display method will be called 40 times per frame)
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();

        // -----  Sichtbarkeitstest
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        // -----  Kamera-System
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0); // Rotation on x-axe
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);     // Rotation on y-axe
        Mat4 R = R2.postMultiply(R1);                        // R = R2 * R1
        Vec3 eye = new Vec3(0, 0, dist);               // Camera-Pos. (Eye)
        Vec3 target = new Vec3(0, 0, 0);            // Target point (LookAt)
        Vec3 up = new Vec3(0, 1, 0);                // UP-Direction

        mygl.setM(gl, Mat4.ID);
        mygl.setLightPosition(gl, 0,0,10);
        mygl.setM(gl, Mat4.lookAt(R.transform(eye), target, R.transform(up)));
        mygl.setShadingLevel(gl, 0);

        // ----- Draw axes
        mygl.setColor(0, 0, 0);
        drawLine(gl, 0, 0, 0, 50, 0, 0);     // x-axe
        drawLine(gl, 0, 0, 0, 0, 50, 0);     // y-axe
        drawLine(gl, 0, 0, 0, 0, 0, 50);     // z-axe

        // ------ Draw gravity arrow if set
        if (isGravityOn) {
            drawGravityArrow(gl);
        }

        // -----  Set shading and lightning
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setShadingLevel(gl, 1);

        // ----- Draw wall  (switch between block types)
        if (isDiceWall) {
            diceWall.drawWall(gl);

            if (hasExplosionStarted) {
                diceWall.move();
            } else {
                diceWall.reset();
            }
        } else {
            quaderWall.drawWall(gl);

            if (hasExplosionStarted) {
                quaderWall.move();
            } else {
                quaderWall.reset();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char key = e.getKeyChar();

        switch (key) {
            case 'x':
                isDiceWall = !isDiceWall;
                break;
            case 'g':
                isGravityOn = !isGravityOn;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            // Space key: start explosion
            case KeyEvent.VK_SPACE:
                hasExplosionStarted = !hasExplosionStarted;
                break;
            // Arrow key up: turn camera upwards
            case KeyEvent.VK_UP:
                elevation++;
                break;
            // Arrow key down: turn camera downwards
            case KeyEvent.VK_DOWN:
                elevation--;
                break;
            // Arrow key left: turn camera to left
            case KeyEvent.VK_LEFT:
                azimut--;
                break;
            // Arrow key right: turn camera to right
            case KeyEvent.VK_RIGHT:
                azimut++;
                break;
            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL3 gl = drawable.getGL().getGL3();

        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);

        // ---- Projections matrix
        float aspect = (float)height/width;
        ybottom = aspect * xleft;
        ytop = aspect * xright;
        mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar));
    }

    //  -----------  main-Methode  ---------------------------
    public static void main(String[] args) {
        new PhysicsExercise5();
    }

    //  ---------  Window-Events  --------------------
    public void windowClosing(WindowEvent e) {
        System.out.println("closing window");
        System.exit(0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed
    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
}