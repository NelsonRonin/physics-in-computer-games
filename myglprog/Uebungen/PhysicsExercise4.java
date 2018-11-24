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

    //  ---------  global Data  ---------------------------

    private String windowTitle = "JOGL-Application";
    private int windowWidth = 800;
    private int windowHeight = 600;
    private String vShader = MyShaders.vShader2;        // Vertex-Shader
    private String fShader = MyShaders.fShader0;        // Fragment-Shader
    private GLCanvas canvas;                            // OpenGL Window
    private int programId;                              // OpenGL-Id
    private MyGLBase1 mygl;                             // Helper functions
    private int maxVerts = 2048;                        // max. amount Vertices in Vertex-Array

    // Sphere values
    private RotKoerper earth;
    private float sphereRadius = 2;

    // ------ Projection matrix
    private float elevation = 20;
    private float azimut = 40;

    private float xleft = -1, xright = 1;
    private float ybottom, ytop;
    private float znear = 0.5f, zfar = 100;

    private float dist = 8;                             // Distance Camera-System von 0

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

    public void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINES);
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
        gl.glClearColor(1, 1, 1, 1);        // Background color

        earth = new RotKoerper(mygl);

        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1,1);

        // Initialize animation with 40 fps and start (Display method will be called 40 times per frame)
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();

        // -----  Visibility test
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        // -----  Camera-System
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);
        Vec3 A = new Vec3(0, 0, dist);                            // Camera-Pos. (Eye)
        Vec3 B = new Vec3(0, 0, 0);                            // Look at point
        Vec3 up = new Vec3(0, 1, 0);                           // up-Direction

        mygl.setM(gl, Mat4.ID);
        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));
        mygl.setLightPosition(gl, 0, 0, 10);
        mygl.setShadingLevel(gl,0);

        // -----  Coordinate axes
        mygl.setColor(0, 0, 0);
        zeichneStrecke(gl, 0, 0, 0, 5, 0, 0);         // x-axe
        zeichneStrecke(gl, 0, 0, 0, 0, 5, 0);         // y-axe
        zeichneStrecke(gl, 0, 0, 0, 0, 0, 5);         // z-axe

        // Draw earth with light
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setShadingLevel(gl, 1);
        mygl.setColor(0, 0, 1);
        earth.zeichneKugel(gl, sphereRadius, 20,30, true );
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
        mygl.setP(gl, Mat4.perspective(xleft, xright, ybottom, ytop, znear, zfar));
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