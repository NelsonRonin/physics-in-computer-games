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
    private int maxVerts = 8192;                        // max. amount Vertices in Vertex-Array

    // ------ Display settings
    private float xleft = -50, xright = 50;
    private float ybottom, ytop;
    private float znear = -100, zfar = 1000;

    // ------ Projection matrix
    private float elevation = 20;
    private float azimut = 40;
    private float dist = 6;                                 // Distance Camera-System von 0

    // ------ Wall data
    private boolean isDiceWall = true;
    private boolean hasExplosionStarted = false;
    private double dt = 0.001;

    private Wall diceWall;
    private Wall quaderWall;
    private Quader dice;
    private Quader quader;

    //  ---------  Wall class  ------------------------------
    class Wall extends Dynamics {
        double[] x;
        int width;
        int height;
        Quader block;

        public Wall(double[] x, int w, int h, Quader block) {
            this.x = x;
            this.width = w;
            this.height = h;
            this.block = block;
        }

        void drawWall(GL3 gl) {
            double rowPos = x[1];
            boolean isNext = false;

            while (rowPos <= height) {
                double colPos = x[0];

                while (colPos <= width) {
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
                    mygl.multM(gl, Mat4.translate((float)colPos, (float)rowPos, (float)x[2]));
                    block.drawQuader(gl);
                    mygl.popM(gl);

                    colPos += block.getA();
                }
                rowPos += block.getC();
            }
        }

        void move() {
            x = runge(x, dt);
        }

        void reset() {
            // @todo: reset wall
        }

        @Override
        double[] f(double[] x) {
            return new double[] {};
        }
    }

    //  ---------  Methoden  --------------------------------

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

        double[] wallStartPos = new double[] {1, 1, 1};

        dice = new Quader(mygl, 2, 2, 2);
        diceWall = new Wall(wallStartPos, 25, 15, dice);

        quader = new Quader(mygl, 2, 1, 1);
        quaderWall = new Wall(wallStartPos, 25, 15, quader);

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
            // Capital V: Abschussgeschwindigkeit vergrössern
            case '+':
                dist += 1;
                canvas.repaint();
                break;
            // Lower v: Abschussgeschwindigkeit verkleinern
            case '-':
                dist -= 1;
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