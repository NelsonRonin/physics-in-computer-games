//  -------------   JOGL 2D Beispiel-Programm (Dreieck) -------------------

import java.awt.*;
import java.awt.event.*;

import ch.fhnw.util.math.Mat4;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.util.*;

public class MyFirst2D
    implements WindowListener, GLEventListener {

    //  ---------  global Data  ---------------------------

    String windowTitle = "JOGL-Application";
    int windowWidth = 800;
    int windowHeight = 600;
    String vShader = MyShaders.vShader1;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader
    Frame frame;
    GLCanvas canvas;                                     // OpenGL Window
    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    // Transformation variables
    float xLeft = -4;
    float xRight = 4;
    float yBottom = -3;
    float yTop = 3;
    float zNear = -100;
    float zFar = 100;
    float rotationAngle = 0;


    //  ---------  Methods  --------------------------------

    public MyFirst2D()                                   // Konstruktor
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
    }

    private void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINES);
    }


    private void zeichneDreieck(GL3 gl, float x1, float y1, float z1,
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
        // Get OpenGL class
        GL3 gl = drawable.getGL().getGL3();

        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();

        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);

        // Background color
        gl.glClearColor(0, 0, 1, 1);

        // Initialize animation with 40 fps and start (Display method will be called 40 times per frame)
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        // Get OpenGL class
        GL3 gl = drawable.getGL().getGL3();
        // Initialize (empty) screen
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
        // Initialize view (matrix): Set identity matrix (E: diagonal 1, rest 0)
        mygl.setM(gl, Mat4.ID);

        // Make view bigger (Camera goes back (projection matrix)
        mygl.setP(gl, Mat4.ortho(xLeft, xRight, yBottom, yTop, zNear, zFar));

        // Draw x-axe and y-axe
        mygl.setColor(0.7f, 0.7f, 0.7f);
        // Draw axes of view
        zeichneStrecke(gl, -4, 0, 0, 4, 0, 0);               // x-Achse
        zeichneStrecke(gl, 0, -4, 0, 0, 4, 0);               // y-Achse

        // Move Dreieck 1 on x-axe and 0.5 on y-axe
        mygl.setM(gl, Mat4.translate(1, 0.5f, 0));

        // Rotate Dreieck with 30° (on z-axe of Dreieck)
        mygl.multM(gl, Mat4.rotate(rotationAngle, 0, 0, 1));

        // Draw axes of Dreieck
        zeichneStrecke(gl, -1, 0, 0, 1, 0, 0);               // x-Achse
        zeichneStrecke(gl, 0, -1, 0, 0, 1, 0);               // y-Achse

        // Draw Dreieck
        mygl.setColor(1, 0, 0);
        zeichneDreieck(gl, -0.5f, 0, 0, 0.5f, 0, 0, 0, 0.8f, 0);

        rotationAngle++;
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) {
        GL3 gl = drawable.getGL().getGL3();
        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
    }


    @Override
    public void dispose(GLAutoDrawable drawable) {
    }                  // not needed


    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args) {
        new MyFirst2D();
    }

    //  ---------  Window-Events  --------------------

    public void windowClosing(WindowEvent e) {
        System.out.println("closing window");
        System.exit(0);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

}