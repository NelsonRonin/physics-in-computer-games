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

public class PhysicsExercise4 implements WindowListener, GLEventListener, KeyListener {

    //  ---------  Global data  ---------------------------

    // ------ General data
    private String windowTitle = "JOGL-Application";
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
    private float znear = -100, zfar = 1000;

    // ------ Projection matrix
    private float elevation = 10;
    private float azimut = 30;
    private float dist = 3;                                 // Distance Camera-System von 0

    // ------ Earth data
    private RotKoerper rotK;
    private final static double g = 9.81e-6;                // Gravity acceleration
    private final static double eG = 6.6726e-29;            // Gravitation constant
    private final static double eR = 6.371;                 // Earth radius
    private final static double eM = 5.976e24;              // Earth mass
    private final static double dt = 60;                    // time step

    // ------ Satellite data
    private double satX = 0, satY = 0, satZ = 42;
    private double satVX = 0.002, satVY = 0.001, satVZ = 0;
    private double sR = 42.050;

    // ---------  Satellite Class  --------------------------
    private class Satellite extends Dynamics {
        double[] X = {satX, satY, satZ, satVX, satVY, satVZ};

        void draw(GL3 gl) {
            mygl.pushM();
            mygl.multM(gl, Mat4.translate((float)sat.X[0], (float)sat.X[1], (float)sat.X[2]));
            rotK.zeichneKugel(gl, 1, 20, 20, true);
            mygl.popM(gl);
        }

        void move() {
            X = runge(X, dt);
        }

        @Override
        public double[] f(double[] x) {
            double r3 = Math.pow(sR, 3);
            double GM = -((eG * eM) / r3);

            return new double[]{
                x[3],   //vx
                x[4],   //vy
                x[5],   //vz
                GM * x[0],
                GM * x[1],
                GM * x[2]
            };
        }

        void berechneBahn(GL3 gl){
            double[] X = {satX, satY, sR, satVX, satVY, satVZ };
            mygl.rewindBuffer(gl);

            for(int i = 0; i<1500; i++){
                X = runge(X, dt);
                mygl.putVertex((float) X[0], (float) X[1], (float) X[2]);
            }

            mygl.copyBuffer(gl);
            mygl.drawArrays(gl, GL3.GL_LINE_LOOP);
        }
    }

    private Satellite sat = new Satellite();

    //  ---------  Methoden  --------------------------------

    // Constructor
    public PhysicsExercise4() { createFrame(); }

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
        mygl = new MyGLBase1(gl, programId, maxVerts);     // OpenGL helper functions
        gl.glClearColor(0, 0, 0, 0);        // Background color

        rotK = new RotKoerper(mygl);

        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1, 1);

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
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);

        Vec3 A = new Vec3(0, 0, dist);                            // Kamera-Pos. (Auge)
        Vec3 B = new Vec3(0, 0, 0);                            // Zielpunkt
        Vec3 up = new Vec3(0, 1, 0);                           // up-Richtung

        mygl.setM(gl, Mat4.lookAt(R.transform(A), R.transform(B), R.transform(up)));
        mygl.setShadingLevel(gl, 0);

        mygl.setM(gl, Mat4.ID);
        mygl.setLightPosition(gl, 0,0,10);
        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));
        mygl.setShadingLevel(gl,0);

        // -----  Set shading and lightning
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setShadingLevel(gl, 1);

        // -----  Draw earth
        mygl.setColor(0, 0, 1);
        rotK.zeichneKugel(gl, (float) eR, 20,20, true);

        // -----  Draw satellite trail
        mygl.setColor(1, 0, 0);
        sat.berechneBahn(gl);

        // -----  Draw satellite
        mygl.setColor(1, 0, 0);
        sat.draw(gl);
        sat.move();
    }


    @Override
    public void keyTyped(KeyEvent e) {
        char key = e.getKeyChar();

        switch (key) {
            case '+':
                dist++;
                break;
            case '-':
                dist--;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
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

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed

    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args) {
        new PhysicsExercise4();
    }

    //  ---------  Window-Events  --------------------

    public void windowClosing(WindowEvent e) {
        System.out.println("closing window");
        System.exit(0);
    }

    public void windowActivated(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
}