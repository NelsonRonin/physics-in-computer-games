//  -------------   JOGL Programm Rotator -------------------
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

public class GyroTest implements WindowListener, GLEventListener, KeyListener {

    //  ---------  globale Daten  ---------------------------

    private String windowTitle = "JOGL-Application";
    private int windowWidth = 800;
    private int windowHeight = 600;
    private String vShader = MyShaders.vShader2;                 // Vertex-Shader
    private String fShader = MyShaders.fShader0;                 // Fragment-Shader

    private GLCanvas canvas;                                     // OpenGL Window
    private int programId;                                       // OpenGL-Id
    private MyGLBase1 mygl;                                      // Hilfsfunktionen
    private int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array
    private RotKoerper rotk;                                     // Rotationskoerper

    private float xleft = -3, xright = 3;                        // ViewingVolume
    private float ybottom, ytop;
    private float znear = -100, zfar = 1000;

    private float elevation = 10;                                 // Kamera-System
    private float azimut = 20;
    private float distance = 3;                                  // Abstand von O
    private Vec3 A = new Vec3(0, 0, distance);             // Kamera-Pos. (Auge)
    private Vec3 B = new Vec3(0, 0, 0);                 // Zielpunkt
    private Vec3 up = new Vec3(0, 1, 0);                // up-Richtung

    // Gyro data
    private double I1 = 1, I2 = 2, I3 = 1;
    private double dt = 0.005;

    private GyroDynamics gyro = new GyroDynamics(I1, I2, I3);

    //  ---------  Methoden  --------------------------------

    public GyroTest()                                            // Konstruktor
    {
        createFrame();
    }

    void createFrame()                                    // Fenster erzeugen
    {
        Frame f = new Frame(windowTitle);
        f.setSize(windowWidth, windowHeight);
        f.addWindowListener(this);
        GLProfile glp = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCaps = new GLCapabilities(glp);
        canvas = new GLCanvas(glCaps);
        canvas.addGLEventListener(this);
        f.add(canvas);
        f.setVisible(true);
        f.addKeyListener(this);
        canvas.addKeyListener(this);
    }

    public void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINES);
    }

    //  ----------  OpenGL-Events   ---------------------------

    @Override
    public void init(GLAutoDrawable drawable)             //  Initialisierung
    {
        GL3 gl = drawable.getGL().getGL3();
        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();
        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);     // OpenGL Helper functions
        gl.glClearColor(0, 0, 1, 1); // Background color

        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1, 1);
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();

        rotk = new RotKoerper(mygl);
        gyro.setState(2, 10, 0, 1, 0, 0, 0);
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        // -----  Sichtbarkeitstest
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        mygl.setM(gl, Mat4.ID);
        mygl.setLightPosition(gl, 0, 0, 0);     //  Lichtquelle im Kamera-System

        // -----  Kamera-System
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);
        mygl.setM(gl, Mat4.lookAt(R.transform(A), R.transform(B), R.transform(up)));
        mygl.setShadingLevel(gl, 0);

        // -----  Koordinatenachsen
        mygl.setColor(0.7f, 0.7f, 0.7f);
        zeichneStrecke(gl, 0, 0, 0, 5, 0, 0);         // x-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 5, 0);         // y-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 0, 5);         // z-Achse

        // -----  Figuren zeichnen
        mygl.setColor(1, 0, 0);
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setShadingLevel(gl, 1);
        mygl.setColor(1, 1, 1);

        // Draw gyro
        float r = 1, s = 0.5f;
        Mat4 Rgyro = gyro.getRotation();
        mygl.multM(gl, Mat4.translate(0, 0.5f * s, 0));
        mygl.multM(gl, Rgyro);
        rotk.zeichneZylinder(gl, r, s, 20, 20, true);

        gyro.move(dt);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) {
        GL3 gl = drawable.getGL().getGL3();
        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
        // ------ Projektionsmatrix
        float aspect = (float) height / width;
        ybottom = aspect * xleft;
        ytop = aspect * xright;
        mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed


    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args) {
        new GyroTest();
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

    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT:
                azimut--;
                break;
            case KeyEvent.VK_RIGHT:
                azimut++;
                break;
            case KeyEvent.VK_UP:
                elevation++;
                break;
            case KeyEvent.VK_DOWN:
                elevation--;
                break;
        }

    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}