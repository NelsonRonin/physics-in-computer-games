package Uebungen;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class PhysicsExercise1
    implements WindowListener, GLEventListener {

    //  ---------  globale Daten  ---------------------------

    private String windowTitle = "Physik Übung 1 - Tragflügel";
    int windowWidth = 1000;
    int windowHeight = 1000;
    String vShader = MyShaders.vShader0;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader

    GLCanvas canvas;                                     // OpenGL Window
    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    //  ---------  Methoden  --------------------------------

    public PhysicsExercise1()                                   // Konstruktor
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

    private void zeichneKreis(GL3 gl, float xm, float ym, float r, int nPoints)
    {
        double phi = 2 * Math.PI / nPoints;
        mygl.rewindBuffer(gl);

        mygl.putVertex(xm, ym, 0);

        for (int i = 0; i <= nPoints; i++) {
            mygl.putVertex(
                (float) (xm + r * Math.cos(i * phi)),
                (float) (ym + r * Math.sin(i * phi)),
                0);
        }

        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_TRIANGLE_FAN);
    }

    private void zeichneTragfluegel(GL3 gl, float xm, float ym, float r, float a, int nPoints)
    {
        double phi = 2 * Math.PI / nPoints;
        mygl.rewindBuffer(gl);

        mygl.putVertex(xm, ym, 0);

        for (int i = 0; i <= nPoints; i++) {
            double x = (xm + r * Math.cos(i * phi));
            double y = (ym + r * Math.sin(i * phi));

            mygl.putVertex(
                (float)( (0.5f * (1 + (Math.pow(a, 2) / (Math.pow(x, 2) + Math.pow(y, 2))))) * x ),
                (float)( (0.5f * (1 - (Math.pow(a, 2) / (Math.pow(x, 2) + Math.pow(y, 2))))) * y ),
                0);
        }

        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_TRIANGLE_FAN);
    }

    //  ----------  OpenGL-Events   ---------------------------

    @Override
    public void init(GLAutoDrawable drawable) // Initialisierung
    {
        GL3 gl = drawable.getGL().getGL3();
        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();
        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);
        gl.glClearColor(1, 1, 1, 1); // Hintergrundfarbe
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT); // Bildschirm loeschen
        mygl.setColor(0, 0, 0);
        zeichneStrecke(gl, -1, 0, 0, 1, 0, 0); // x-Achse
        zeichneStrecke(gl, 0, -1, 0, 0, 1, 0); // y-Achse

        float a = 0.2f;
        float xm = -0.1f * a;
        float ym = 0.25f * (a - xm);
        double rad = Math.sqrt(Math.pow(a - xm, 2) + Math.pow(ym, 2));

        // Draw white normal circle
        mygl.setColor(0.5f, 0.5f, 0.5f);
        zeichneKreis(gl, xm, ym, (float)rad, 36);

        // Draw Kutta-Joukowski-Transformation
        mygl.setColor(1, 0, 0);
        zeichneTragfluegel(gl, xm, ym, (float)rad, a, 36);
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
        new PhysicsExercise1();
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