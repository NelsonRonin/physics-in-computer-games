package Uebungen;

import ch.fhnw.util.math.Mat4;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class PhysicsExercise2 implements WindowListener, GLEventListener, KeyListener {

    //  ---------  globale Daten  ---------------------------

    private String windowTitle = "Physik Übung 2 - Fliegende Hantel";
    private int windowWidth = 1500;
    private int windowHeight = 800;
    private String vShader = MyShaders.vShader1;                 // Vertex-Shader
    private String fShader = MyShaders.fShader0;                 // Fragment-Shader

    private GLCanvas canvas;                                     // OpenGL Window
    private int programId;                                       // OpenGL-Id
    private MyGLBase1 mygl;                                      // Hilfsfunktionen
    private int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    // Variables of viewport coordinates
    private float xLeft = -2;
    private float xRight = 10;
    private float yBottom = -2;
    private float yTop = 10;
    private float toRad = (float)(Math.PI / 180);   // Transform angle from degree to rad

    //  ---------  Daten für Hantel Position und Wurf  ---------------------------
    private boolean startThrow = false;
    private float startPosX = 0;
    private float startPosY = 2;

    // Hantel Positionen und Grössen
    private float hantelX, hantelY;
    private float hantelLength = 0.8f;
    private float kugelSize = 0.1f;

    // Eigenrotation
    private float selfRotation = 0;
    private float selfRotationSpeed = 3;

    // Abschusswinkel in Degree
    private float alpha = 30;

    // Schwerkraft
    private final double g = 9.81;

    // Geschwindigkeiten
    private double vx0 = 5, vy0 = 5;
    private double vx, vy;

    // Beschleunigungen
    private double ax = 0, ay = -g;

    // Zeitschritt
    private final double dt = 0.01;

    class Hantel {
        // Position der Hantel
        float xHantel, yHantel;
        // Länge der Hantel
        float hantelLeng;
        // Radius der Hantelkugeln
        float kugelRadius;
        // Position der ersten Kugel (Hantelanfang)
        float x1, y1;
        // Position der zweiten Kugel (Hantelende)
        float x2, y2;

        Hantel(float xH, float yH, float length, float r) {
            xHantel= xH;
            yHantel = yH;
            hantelLeng = length;
            kugelRadius = r;

            // Positionen der beiden Kugeln setzen
            y1 = -(length / 2);
            y2 = length / 2;
        }

        void draw(GL3 gl){
            mygl.rewindBuffer(gl);

            mygl.setM(gl, Mat4.translate(hantelX, hantelY, 0));

            // Wenn Wurf gestartet wurde: Eigenrotation
            if (startThrow) {
                mygl.multM(gl, Mat4.rotate(selfRotation, 0, 0, 1));
                selfRotation -= selfRotationSpeed;
            }

            zeichneStrecke(gl, x1, y1, 0, x2, y2, 0);
            zeichneKugel(gl, x1, y1, kugelRadius, 30);
            zeichneKugel(gl, x2, y2, kugelRadius, 30);
        }

        private void zeichneKugel(GL3 gl, float xm, float ym, float r, int nPoints)
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
    }

    //  ---------  Methoden  --------------------------------

    public PhysicsExercise2()                                   // Konstruktor
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
        f.add(canvas);
        f.setVisible(true);
    }

    private void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                                        float x2, float y2, float z2
    ) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINES);
    }

    private void zeichneHilfsstrecken(GL3 gl) {
        float alphaRad = alpha * toRad;

        // Zeichne Abschusswinkel (in Grün)
        mygl.setColor(0, 1, 0.5f);
        zeichneStrecke(gl, startPosX, startPosY, 0,
            (float)(Math.cos(alphaRad) + startPosX),
            (float)(Math.sin(alphaRad) + startPosY),
            0
        );

        // Zeichne Geschwindikeit (in Rot)
        mygl.setColor(1, 0, 0);
        float vLength = (float)(0.1 * vx0);
        double xG = ((Math.cos(alphaRad) * vLength) + startPosX);
        double yG = ((Math.sin(alphaRad) * vLength) + startPosY);

        double xS = startPosX - xG;
        double yS = startPosY - yG;

        zeichneStrecke(gl, startPosX, startPosY, 0,
            (float) (xG + 2 * xS),
            (float) (yG + 2 * yS),
            0
        );
    }

    private void zeichneBahn(GL3 gl, double x0, double y0, double vx0, double vy0, int nPunkte)
    {
        double x, y, t;
        mygl.rewindBuffer(gl);

        for (int i=0; i<nPunkte; i++) {
            t = i * dt;
            x = vx0 * t + x0;
            y = -0.5 * g * t * t + vy0 * t + y0;
            mygl.putVertex((float)x, (float)y,0);
        }

        mygl.copyBuffer(gl);
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, nPunkte);
    }

    //  ----------  OpenGL-Events   ---------------------------

    @Override
    public void init(GLAutoDrawable drawable)           //  Initialisierung
    {
        GL3 gl = drawable.getGL().getGL3();
        System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
        System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
        System.out.println();
        programId = MyShaders.initShaders(gl, vShader, fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);
        gl.glClearColor(0.9f, 0.9f, 0.9f, 1);    // Hintergrundfarbe

        // Initialize animation with 40 fps and start (Display method will be called 40 times per frame)
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();

        vx = vx0 * Math.cos(alpha * toRad);
        vy = vy0 * Math.sin(alpha * toRad);
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);      // Bildschirm loeschen

        mygl.setP(gl, Mat4.ortho(xLeft, xRight, yBottom, yTop, -100, 100));
        mygl.pushM();
        mygl.setM(gl, Mat4.ID);

        // Zeichne achsen (Koordinatensystem: X(-10 bis 10), Y(-6 bis 6)
        mygl.setColor(0, 0, 0);
        zeichneStrecke(gl, xLeft, 0, 0, xRight, 0, 0);  // x-Achse
        zeichneStrecke(gl, 0, yBottom, 0, 0, yTop, 0);  // y-Achse

        // Zeichne Hilfsstrecken für Geschwindigkeit und Abschusswinkel
        zeichneHilfsstrecken(gl);

        // Zeichne die zurückgelegte Bahn der Hantel
        mygl.setColor(0, 0, 1);
        zeichneBahn(gl, startPosX, startPosY, vx0 * Math.cos(alpha * toRad), vy0 * Math.sin(alpha * toRad), 1000);

        // Zeichne die Hantel
        mygl.popM(gl);

        Hantel hantel = new Hantel(
            hantelX,
            hantelY,
            hantelLength,
            kugelSize
        );
        hantel.draw(gl);

        // Wenn start key (space) gedrückt wurde: Hantelwurf animieren
        if (startThrow && hantelY > 0) {
            // Wurfparabel
            hantelX += vx * dt;
            hantelY += vy * dt;
            vx += ax * dt;
            vy += ay * dt;
        }
        // Bei Stop des Wurfes: Positionen auf Startpositionen zurücksetzen
        else {
            // Reset Startpositionen
            hantelX = startPosX;
            hantelY = startPosY;
            vx = vx0 * Math.cos(alpha * toRad);
            vy = vy0 * Math.sin(alpha * toRad);
            selfRotation = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char key = e.getKeyChar();

        switch (key) {
            // Capital V: Abschussgeschwindigkeit vergrössern
            case 'V':
                vx0++;
                vy0++;
                break;
            // Lower v: Abschussgeschwindigkeit verkleinern
            case 'v':
                vx0--;
                vy0--;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            // Pfeil key up: Abschusswinkel vergrössern
            case KeyEvent.VK_UP:
                alpha++;
                break;

            // Pfeil key down: Abschusswinkel verkleinern
            case KeyEvent.VK_DOWN:
                alpha--;
                break;
            // Space key: Starte den Wurf
            case KeyEvent.VK_SPACE:
                startThrow = !startThrow;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) {
        GL3 gl = drawable.getGL().getGL3();

        // Verzerrungen des Bildes verhindern
        float aspect = (float)height / width;
        yBottom = aspect * xLeft;
        yTop = aspect * xRight;

        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed


    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args) {
        new PhysicsExercise2();
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