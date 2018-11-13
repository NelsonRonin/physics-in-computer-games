//  -------------   JOGL 3D Beispiel-Programm (Lichtstrahl durch Dreieck) -------------------

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class Wirbel implements WindowListener, GLEventListener {

    //  ---------  globale Daten  ---------------------------

    String windowTitle = "JOGL-Application";
    int windowWidth = 800;
    int windowHeight = 600;
    String vShader = MyShaders.vShader1;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader
    GLCanvas canvas;                                     // OpenGL Window
    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    //  ---------  Daten für Wirbel  ---------------------------

    int nPunkte = 800;
    double[] x = new double[nPunkte];   // Bahn
    double[] y = new double[nPunkte];
    double[] z = new double[nPunkte];
    double dt = 0.2;

    class WirbelDynamics extends Dynamics {
        double a = -0.4;
        double b = 2;

        public double[] f (double[] x) {
            double len2 = x[0] * x[0] + x[1] * x[1];
            double[] y = {
                (a * x[0] + b * x[1]) / len2,
                (a * x[1] - b * x[0]) / len2,
                0
            };

            return y;
        }
    }
    public WirbelDynamics wirbelDynamics = new WirbelDynamics();

    //  ---------  Methoden  --------------------------------

    public Wirbel()                                   // Konstruktor
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

    public void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINES);
    }


    public void zeichneBahn(GL3 gl, double[] x, double[] y, double[] z) {
        mygl.rewindBuffer(gl);

        for (int i=0; i < x.length; i++)
            mygl.putVertex((float)x[i], (float)y[i], (float)z[i]);

        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINE_STRIP);
    }

    public void berechneBahn(double[] x, double[] y, double[] z, double dt) {
        x[0] = 18;
        y[0] = 10;

        for (int i=0; i < x.length; i++) {
            // todo: fix this shit maaaaan ;)
            double[] xyz = { x[i], y[i], z[i] };

            xyz = wirbelDynamics.euler(xyz, dt);
            x[i] = xyz[0];
            y[i] = xyz[1];
            z[i] = xyz[2];
        }
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
        berechneBahn(x, y, z, dt);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();

        // -----  Sichtbarkeitstest
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        // ------ Projektionsmatrix
        float xleft = -12, xright = 12;
        float ybottom = -8, ytop = 8;
        float znear = -100, zfar = 1000;
        mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar));

        // -----  Kamera-System
        float elevation = 20;
        float azimut = 40;
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);
        Vec3 A = new Vec3(0, 0, 3);                            // Kamera-Pos. (Auge)
        Vec3 B = new Vec3(0, 0, 0);                            // Zielpunkt
        Vec3 up = new Vec3(0, 1, 0);                           // up-Richtung
        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));

        // -----  Koordinatenachsen
        mygl.setColor(0, 0, 0);
        zeichneStrecke(gl, 0, 0, 0, 5, 0, 0);         // x-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 5, 0);         // y-Achse
        zeichneStrecke(gl, 0, 0, 0, 0, 0, 5);         // z-Achse

        // -----  Figuren zeichnen
        mygl.setColor(1, 0, 0);
        zeichneBahn(gl, x, y, z);
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
        new Wirbel();
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