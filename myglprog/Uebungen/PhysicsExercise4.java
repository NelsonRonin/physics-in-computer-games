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
    private float xleft = -400, xright = 400;
    private float ybottom, ytop;
    private float znear = -500, zfar = 1000;

    // ------ Projection matrix
    private float elevation = 10;
    private float azimut = 30;
    private float dist = 3;                                 // Distance Camera-System von 0

    // ------ Earth data
    private RotKoerper rotK;
    private double g = 9.81e-6;                // Gravity acceleration
    private double eG = 6.6726e-29;            // Gravitation constant
    private double eR = 6.371;                 // Earth radius
    private double eM = 5.976e24;              // Earth mass
    private double dt = 60;                    // time step

    // ------ Stationary StationarySatellite data
    private StationarySatellite stationarySat;
    private double satX = 0, satY = 0, satZ = 42;
    private double satVX = 0.002, satVY = 0.001, satVZ = 0;
    private double sR = 42.050;

    // ------ Moon data
    private Moon moon;
    private double moonX = 307.357885, moonY = 0, moonZ = -230.855996;
    private double moonVX = -0.000612, moonVY = 0, moonVZ = -0.000814;
    private double moonM = 7.35e22;
    private double moonR = 1.738;
    private double moonTrailR = 384.4;

    // ----- Moon StationarySatellite data
    private MoonSatellite moonSat;
    private double moonSatX = 0, moonSatY = 0, moonSatZ = 6.551;
    private double moonSatVX = 0.01095, moonSatVY = 0, moonSatVZ = 0;


    // ---------  Stationary StationarySatellite Class  --------------------------
    private class StationarySatellite extends Dynamics {
        double[] X;
        double R;

        public StationarySatellite(double x, double y, double z, double vx, double vy, double vz, double r) {
            X = new double[6];
            X[0] = x;
            X[1] = y;
            X[2] = z;
            X[3] = vx;
            X[4] = vy;
            X[5] = vz;
            R = r;
        }

        void draw(GL3 gl) {
            mygl.pushM();
            mygl.multM(gl, Mat4.translate((float) X[0], (float) X[1], (float) X[2]));
            rotK.zeichneKugel(gl, (float)R, 20, 20, true);
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

    // ---------  Moon Class  --------------------------
    private class Moon extends Dynamics {
        double[] X;
        double R;

        public Moon(double x, double y, double z, double vx, double vy, double vz, double r) {
            X = new double[6];
            X[0] = x;
            X[1] = y;
            X[2] = z;
            X[3] = vx;
            X[4] = vy;
            X[5] = vz;
            R = r;
        }

        void draw(GL3 gl) {
            mygl.pushM();
            mygl.multM(gl, Mat4.translate((float) X[0], (float) X[1], (float) X[2]));
            rotK.zeichneKugel(gl, (float)R, 20, 20, true);
            mygl.popM(gl);
        }

        void move() {
            X = runge(X, dt);
        }

        @Override
        public double[] f(double[] x) {
            double r3 = Math.pow(moonTrailR, 3);
            double GM = -((eG * eM) / r3);

            return new double[]{
                x[3],   //vx
                0,      //vy
                x[5],   //vz
                GM * x[0],
                0,
                GM * x[2]
            };
        }

        void berechneBahn(GL3 gl){
            double[] X = { moonX, moonY, moonZ, moonVX, moonVY, moonVZ };

            for (int j = 0; j < 20; j++) {
                mygl.rewindBuffer(gl);

                for (int i = 0; i < 2000; i++) {
                    X = runge(X, dt);
                    mygl.putVertex((float) X[0], (float) X[1], (float) X[2]);
                }

                mygl.copyBuffer(gl);
                mygl.drawArrays(gl, GL3.GL_LINES);
            }
        }
    }

    // ---------  Moon Class  --------------------------
    private class MoonSatellite extends Dynamics {
        double[] X;
        double R;

        public MoonSatellite(double x, double y, double z, double vx, double vy, double vz, double r) {
            X = new double[6];
            X[0] = x;
            X[1] = y;
            X[2] = z;
            X[3] = vx;
            X[4] = vy;
            X[5] = vz;
            R = r;
        }

        void draw(GL3 gl) {
            mygl.pushM();
            mygl.multM(gl, Mat4.translate((float) X[0], (float) X[1], (float) X[2]));
            rotK.zeichneKugel(gl, (float)R, 20, 20, true);
            mygl.popM(gl);
        }

        void move() {
            X = runge(X, dt);
        }

        @Override
        public double[] f(double[] x) {
            double r = Math.sqrt(Math.pow(x[0], 2) + Math.pow(x[2], 2));
            double r3 = Math.pow(r, 3);

            double s = Math.sqrt(Math.pow(moon.X[0] - x[0], 2) + Math.pow(moon.X[2] - x[2], 2));
            double s3 = Math.pow(s, 3);

            double GMearth = -((eG * eM) / r3);
            double GMmoon = ((eG * moonM) / s3);

            return new double[]{
                x[3],   //vx
                0,      //vy
                x[5],   //vz
                (GMearth * x[0]) + (GMmoon * (moon.X[0] - x[0])),
                0,
                (GMearth * x[2]) + (GMmoon * (moon.X[2] - x[2]))
            };
        }
    }

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
        stationarySat = new StationarySatellite(satX, satY, satZ, satVX, satVY, satVZ, 1);
        moon = new Moon(moonX, moonY, moonZ, moonVX, moonVY, moonVZ, moonR);
        moonSat = new MoonSatellite(moonSatX, moonSatY, moonSatZ, moonSatVX, moonSatVY, moonSatVZ, 1.5);

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
        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));
        mygl.setLightPosition(gl, 0,0,10);
        mygl.setShadingLevel(gl,0);

        // -----  Set shading and lightning
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setShadingLevel(gl, 1);

        // -----  Draw earth
        mygl.setColor(0, 0, 1);
        rotK.zeichneKugel(gl, (float) eR, 20,20, true);

        // -----  Draw stationary satellite trail
        mygl.setColor(1, 0, 0);
        stationarySat.berechneBahn(gl);

        // -----  Draw satellite
        stationarySat.draw(gl);
        stationarySat.move();

        // -----  Draw moon
        mygl.setColor(0.8f, 0.8f, 0.8f);
        moon.berechneBahn(gl);
        moon.draw(gl);
        moon.move();

        // -----  Draw moon satellite
        mygl.setColor(0, 1, 0);
        moonSat.draw(gl);
        moonSat.move();
    }


    @Override
    public void keyTyped(KeyEvent e) {}

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