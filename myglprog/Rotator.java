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

public class Rotator
       implements WindowListener, GLEventListener, KeyListener
{

    //  ---------  globale Daten  ---------------------------

    String windowTitle = "JOGL-Application";
    int windowWidth = 800;
    int windowHeight = 600;
    String vShader = MyShaders.vShader2;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader
    Frame frame;
    GLCanvas canvas;                                     // OpenGL Window
    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array
    RotKoerper rotk;                                     // Rotationskoerper

    float xleft=-5, xright=5;                            // ViewingVolume
    float ybottom, ytop;
    float znear=-100, zfar=1000;

    float elevation = 10;                                // Kamera-System
    float azimut = 30;
    float distance = 3;                                  // Abstand von O
    Vec3 A = new Vec3(0,0,distance);               // Kamera-Pos. (Auge)
    Vec3 B = new Vec3(0,0,0);                   // Zielpunkt
    Vec3 up = new Vec3(0,1,0);                  // up-Richtung

    double g = 9.81;                                      // Erdbeschleunigung
    double dt = 0.01;                                     // Zeitschritt

    // -------  Hantel-Pendel  ------
    class Pendel extends Dynamics {
        float d1 = 1.5f, r1 = 0.2f;                           // Abstand, Radius Kugel1
        float d2 = 0.6f, r2 = 0.1f;                           // Abstand, Radius Kugel2
        float r3 = 0.04f;                                     // Radius Stab

        double m = 1;                                   // Masse
        double I = 2.5;                                 // Trägheitsmoment
        double rs = 0.5 * d1;                           // Abstand Schwerpunkt

        double phi0 = 0;
        double omega0 = 0;
        double[] x = { phi0, omega0 };

        public double[] f(double[] x) {
            double phi = x[0];
            double omega = x[1];

            return new double[]{
                omega,
                -m * g * rs * Math.cos(phi) / I
            };
        }

        public void move(double dt) {
            x = runge(x, dt);
        }

        void zeichne(GL3 gl) {
            mygl.pushM();
            mygl.multM(gl, Mat4.translate(d1, 0, 0));
            rotk.zeichneKugel(gl, r1, 20, 20, true);             // Kugel 1
            mygl.multM(gl, Mat4.translate(-(d1 + d2), 0, 0));
            rotk.zeichneKugel(gl, r2, 20, 20, true);             // Kugel 2
            mygl.pushM();
            mygl.multM(gl, Mat4.rotate(-90, 0, 0, 1));
            rotk.zeichneZylinder(gl, r3, d1 + d2, 20, 20, true);    // Verbindungsstab
            mygl.popM(gl);
            mygl.multM(gl, Mat4.translate(d2, 0, -r3));
            mygl.multM(gl, Mat4.rotate(90, 1, 0, 0));
            rotk.zeichneZylinder(gl, 1.5f * r3, 2 * r3, 20, 20, true);  // Lager
            mygl.popM(gl);
        }
    }

    Pendel pendel = new Pendel();

    //  ---------  Methoden  --------------------------------

    public Rotator()                                   // Konstruktor
    {
        createFrame();
    }

    void createFrame() {                               // Fenster erzeugen
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
    public void init(GLAutoDrawable drawable) {            //  Initialisierung
        GL3 gl = drawable.getGL().getGL3();
        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();
        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);     // OpenGL Hilfsfunktiontn
        gl.glClearColor(0, 0, 1, 1); // Hintergrundfarbe
        rotk = new RotKoerper(mygl);
        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1, 1);
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();
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
        mygl.setColor(1, 0, 0);
        mygl.multM(gl, Mat4.translate(0, 1.8f, 0));

        mygl.multM(gl, Mat4.rotate((float)(pendel.x[0] * 180 / Math.PI), 0, 0, 1));
        pendel.zeichne(gl);

        pendel.move(dt);
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
    public void dispose(GLAutoDrawable drawable)  { }                  // not needed


    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args)
    { new Rotator();
    }

    //  ---------  Window-Events  --------------------

    public void windowClosing(WindowEvent e)
    {   System.out.println("closing window");
        System.exit(0);
    }
    public void windowActivated(WindowEvent e) {  }
    public void windowClosed(WindowEvent e) {  }
    public void windowDeactivated(WindowEvent e) {  }
    public void windowDeiconified(WindowEvent e) {  }
    public void windowIconified(WindowEvent e) {  }
    public void windowOpened(WindowEvent e) {  }


    public void keyPressed(KeyEvent e)
    {  int code = e.getKeyCode();
       switch (code)
       { case KeyEvent.VK_LEFT: azimut--;
                                break;
         case KeyEvent.VK_RIGHT: azimut++;
                                 break;
         case KeyEvent.VK_UP: elevation++;
                                 break;
         case KeyEvent.VK_DOWN: elevation--;
                              break;
       }

    }
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) { }

}