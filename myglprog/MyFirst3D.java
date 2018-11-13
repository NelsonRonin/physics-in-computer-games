//  -------------   JOGL 3D Beispiel-Programm (Lichtstrahl durch Dreieck) -------------------

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

public class MyFirst3D implements WindowListener, GLEventListener, KeyListener {

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

    private RotKoerper rotk;
    private float elevation = 20;
    private float azimut = 40;

    //  ---------  Methoden  --------------------------------

    public MyFirst3D()                                   // Konstruktor
    {
        createFrame();
    }

    void createFrame()                                    // Fenster erzeugen
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

    public void zeichneDreieck(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.putVertex(x3, y3, z3);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_TRIANGLES);
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
        mygl = new MyGLBase1(gl, programId, maxVerts);     // OpenGL Hilfsfunktiontn
        gl.glClearColor(0.8f, 0.8f, 0.8f, 1);        // Hintergrundfarbe

        rotk = new RotKoerper(mygl);

        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1,1);

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

        // ------ Projektionsmatrix
        float xleft = -4, xright = 4;
        float ybottom = -3, ytop = 3;
        float znear = -100, zfar = 1000;
        mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar));

        // -----  Kamera-System
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);

        Vec3 A = new Vec3(0, 0, 3);                            // Kamera-Pos. (Auge)
        Vec3 B = new Vec3(0, 0, 0);                            // Zielpunkt
        Vec3 up = new Vec3(0, 1, 0);                           // up-Richtung

        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));

        mygl.setShadingLevel(gl,0);

        // -----  Koordinatenachsen
        mygl.setColor(0, 0, 0);
        zeichneStrecke(gl, 0, 0, 0, 5, 0, 0);         // x-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 5, 0);         // y-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 0, 5);         // z-Achse

        // -----  Figuren zeichnen
        // Dreieck im 3D Raum mit Strecke durch Dreieck
//        mygl.setColor(1, 1, 1);
//        zeichneStrecke(gl, -1, 3, 5, 2, -0.5f, -4);
//        mygl.setColor(1, 0, 0);
//        zeichneDreieck(gl, 0, 0.3f, 0.3f, 2.5f, 0.8f, 1, 0.5f, 1.5f, -1);

        // Kugel mit Licht
        mygl.setShadingParam(gl, 0.2f, 0.6f);
        mygl.setShadingParam2(gl, 0.4f, 20);
        mygl.setLightPosition(gl, 0,0,10);
        mygl.setShadingLevel(gl,1);

        mygl.setColor(1, 0, 0);
        rotk.zeichneKugel(gl, 0.5f, 20,20, true );
//        mygl.setColor(1,1,0);
//        rotk.zeichneKugel(gl, 0.5f, 20,20, false );
    }


    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            // Pfeil key up: Abschusswinkel vergrössern
            case KeyEvent.VK_UP:
                elevation++;
                break;

            // Pfeil key down: Abschusswinkel verkleinern
            case KeyEvent.VK_DOWN:
                elevation--;
                break;

            // Space key: Starte den Wurf
            case KeyEvent.VK_LEFT:
                azimut--;
                break;

            // Space key: Starte den Wurf
            case KeyEvent.VK_RIGHT:
                azimut++;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL3 gl = drawable.getGL().getGL3();
        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed

    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args) {
        new MyFirst3D();
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