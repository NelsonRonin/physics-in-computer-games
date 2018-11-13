package Uebungen;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

public class PhysicsExercise3 implements WindowListener, GLEventListener {

    //  ---------  globale Daten  ---------------------------

    private String windowTitle = "Physik Übung 3 - Vorhang mit Wind";
    private int windowWidth = 1000;
    private int windowHeight = 1000;
    private String vShader = MyShaders.vShader1;                // Vertex-Shader
    private String fShader = MyShaders.fShader0;                // Fragment-Shader

    private GLCanvas canvas;                                    // OpenGL Window
    private int programId;                                      // OpenGL-Id
    private MyGLBase1 mygl;                                     // Hilfsfunktionen
    private int maxVerts = 2048;                                // max. Anzahl Vertices im Vertex-Array

    //  ---------  Projektionsmatrix Variabeln  ---------------------------
    private float xLeft = -2, xRight = 2;
    private float yBottom = -2, yTop = 2;
    private float zNear = -100, zFar = 1000;

    //  ---------  Daten für die Vorhang/Wind Animation  ---------------------------
    float vorhangHeight = 1.2f;
    float vorhangWidth = 2;
    private int cols = 50;                                      // Anzahl Spalten des Vorhangs
    private int rows = 30;                                      // Anzahl Zeilen des Vorhangs
    private Vec3[][] vorhangNodes = new Vec3[rows][cols];       // Alle Nodes des Vorhangs

    //  ---------  Methoden  --------------------------------

    public PhysicsExercise3()                                   // Konstruktor
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
                                        float x2, float y2, float z2
    ) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINE_LOOP);
    }

    private void zeichneBoden(GL3 gl) {
        int floorCount = 12;
        int floorPosition = 0;
        float floorLineDistance = 0.5f;
        int floorLineLength = 5;

        // Zeichne Strecken entlang der x-Achse
        for (float i = -floorCount; i < floorCount; i += floorLineDistance) {
            zeichneStrecke(gl, i, floorPosition, 0, i, floorPosition, floorLineLength);
        }

        // Zeichne Strecken entlang der z-Achse
        for (float j = 0; j < (2 * floorCount); j += floorLineDistance) {
            zeichneStrecke(gl, -floorLineLength, floorPosition, 0, floorLineLength, floorPosition, j);
        }
    }

    private void zeichneVorhangStange(GL3 gl) {
        int lastColIndex = cols - 1;

        zeichneStrecke(gl,
            vorhangNodes[0][0].x - 0.5f, vorhangNodes[0][0].y, vorhangNodes[0][0].z,
            vorhangNodes[0][lastColIndex].x + 0.5f, vorhangNodes[0][lastColIndex].y, vorhangNodes[0][lastColIndex].z
        );
        zeichneStrecke(gl,
            vorhangNodes[0][0].x - 0.5f, vorhangNodes[0][0].y + 0.2f, vorhangNodes[0][0].z,
            vorhangNodes[0][lastColIndex].x + 0.5f, vorhangNodes[0][lastColIndex].y + 0.2f, vorhangNodes[0][lastColIndex].z
        );
        zeichneStrecke(gl,
            vorhangNodes[0][0].x - 0.5f, vorhangNodes[0][0].y, vorhangNodes[0][0].z,
            vorhangNodes[0][0].x - 0.5f, vorhangNodes[0][0].y + 0.2f, vorhangNodes[0][0].z
        );
        zeichneStrecke(gl,
            vorhangNodes[0][lastColIndex].x + 0.5f, vorhangNodes[0][lastColIndex].y, vorhangNodes[0][lastColIndex].z,
            vorhangNodes[0][lastColIndex].x + 0.5f, vorhangNodes[0][lastColIndex].y + 0.2f, vorhangNodes[0][lastColIndex].z
        );
    }

    private void zeichneVorhang(GL3 gl) {
        // Alle Zeilen durchgehen
        for (int i = 0; i < rows; i++) {
            // Alle Spalten durchgehen
            for (int j = 0; j < cols; j++) {
                // Zeichne Vertikale Linie
                if (i < rows - 1) {
                    Vec3 currentRowStart = vorhangNodes[i][j];
                    Vec3 currentRowEnd = vorhangNodes[i + 1][j];

                    zeichneStrecke(gl,
                        currentRowStart.x, currentRowStart.y, currentRowStart.z,
                        currentRowEnd.x, currentRowEnd.y, currentRowEnd.z
                    );
                }

                // Zeichne Horizontale Linie
                if (j < cols - 1) {
                    Vec3 currentColStart = vorhangNodes[i][j];
                    Vec3 currentColEnd = vorhangNodes[i][j + 1];

                    zeichneStrecke(gl,
                        currentColStart.x, currentColStart.y, currentColStart.z,
                        currentColEnd.x, currentColEnd.y, currentColEnd.z
                    );
                }
            }
        }
    }

    private void initVorhangNodes() {
        // Grössen des Vorhangs: (Breite: -10 bis 10, Höhe: 2 bis 18
        float startPosTop = 1.4f;
        float startPosLeft = -1;

        // Initiale Positionen eines nodes
        int zPos = 0;
        double stepSizeH = (double)vorhangWidth / cols;
        double stepSizeV = (double)vorhangHeight / rows;

        // Alle Zeilen durchgehen
        float rowPos = startPosTop;
        for (int i = 0; i < rows; i++) {

            float colPos = startPosLeft;
            // Alle Spalten durchgehen
            for (int j = 0; j < cols; j++) {
                vorhangNodes[i][j] = new Vec3(colPos, rowPos, zPos);
                colPos += stepSizeH;
            }
            rowPos -= stepSizeV;
        }
    }

    private void berechneNeueNodesPositionen() {
        // Konstanten für die Berechnung eines neuen nodes
        float dt = 0.01f;
//        float k = 0.005f;
        float k = 500;
        float g = 0.981f;
        float m = 0.1f;
        float geschwindikeitsDaempfung = 0.95f;

        // Geschwindigkeit auf einen Node
        Vec3 v = new Vec3( 0, 0, 0);

        // Windkraft für jede Spalte definieren
        Vec3[] windkraefte = new Vec3[cols];
        for (int j = 0; j < cols; j++) {
            windkraefte[j] = new Vec3(
                getRandomNumberInRange(-0.6f, 0.6f),
                getRandomNumberInRange(-0.1f, 0.1f),
                getRandomNumberInRange(-1.1f, 1.2f)
            );
        }

        // Alle Zeilen durchgehen (ausser der ersten Zeile, wo die Stange montiert ist)
        for (int i = 1; i < rows; i++) {
            // Alle Spalten durchgehen
            for (int j = 0; j < cols; j++) {
                System.out.println("vorhangNodes[i][j]: " + vorhangNodes[i][j]);

                // Get wind force for each column
                Vec3 wind = windkraefte[j];

                // Berechne Federkräfte der Nachbarn
                Vec3 federkraft = berechneFederkraft(i, j, k);

                // Gravitationskraft hinzufügen
                federkraft = federkraft.scale(m * -g);

                // Windkraft hinzufügen
                federkraft = federkraft.add(wind);

                // a = F / m
                Vec3 a = federkraft.scale(1/m);

                // v = v + a * dt
                v = v.add( a.scale(dt) );

                // v = 0.95 * v
                v = v.scale(geschwindikeitsDaempfung);

                // x = x + v * dt
                vorhangNodes[i][j] = vorhangNodes[i][j].add(v.scale(dt));
                System.out.println("vorhangNodes[i][j]: " + vorhangNodes[i][j]);
                System.out.println("-------------------------------------------------");
            }
        }
    }

    private Vec3 berechneFederkraft(int rowIndex, int colIndex, float k) {
        Vec3 currentNode = vorhangNodes[rowIndex][colIndex];
        Vec3 federkraft = new Vec3(0,0,0);
        float d0UpDown = vorhangHeight / rows;
        float d0LeftRight = vorhangWidth / cols;

        // Node LINKS vom aktuellen Node (existiert)
        if (colIndex > 0) {
            federkraft = federkraft.add(getFederkraft(currentNode, vorhangNodes[rowIndex][colIndex - 1], k, d0LeftRight));
        }

        // Node RECHTS vom aktuellen Node (existiert)
        if (colIndex < vorhangNodes[rowIndex].length - 1) {
            federkraft = federkraft.add(getFederkraft(currentNode, vorhangNodes[rowIndex][colIndex + 1], k, d0LeftRight));
        }

        // Node OBEN vom aktuellen Node (existiert)
        if (rowIndex > 0) {
            federkraft = federkraft.add(getFederkraft(currentNode, vorhangNodes[rowIndex - 1][colIndex], k, d0UpDown));
        }

        // Node UNTEN vom aktuellen Node (existiert)
        if (rowIndex < vorhangNodes.length - 1) {
            federkraft = federkraft.add(getFederkraft(currentNode, vorhangNodes[rowIndex + 1][colIndex], k, d0UpDown));
        }

        return federkraft;
    }

    private Vec3 getFederkraft(Vec3 currentNode, Vec3 nextNode, float k, float d0) {
        // d = AB = B - A
        Vec3 dVector = nextNode.subtract(currentNode);
        // |d| = sqrt( pow(d.x) + pow(d.y) + pow(d.z) )
        float betragDvector = (float)Math.sqrt( Math.pow(dVector.x, 2) + Math.pow(dVector.y, 2) + Math.pow(dVector.z, 2) );
        // (|d| - d0)
        float federAuslenkung = betragDvector - d0;
        // (d / |d|)
        Vec3 federVectorDurch = dVector.scale(1/betragDvector);

        // F = k  *  (|d| - d0)  *  (d / |d|)
        return federVectorDurch.scale(k * federAuslenkung);
    }

    private static float getRandomNumberInRange(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return min + new Random().nextFloat() * (max - min);
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

        // Initialize variables
        initVorhangNodes();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        // -----  Sichtbarkeitstest
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        // ------ Projektionsmatrix
        mygl.setP(gl, Mat4.ortho(xLeft, xRight, yBottom, yTop, zNear, zFar));

        // -----  Kamera-System
        float elevation = 20;
        float azimut = 40;
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);
        Vec3 A = new Vec3(0, 0, 2);                            // Kamera-Pos. (Auge)
        Vec3 B = new Vec3(0, 0, 0);                            // Zielpunkt
        Vec3 up = new Vec3(0, 1, 0);                           // up-Richtung
        mygl.setM(gl, Mat4.lookAt(R.transform(A), B, R.transform(up)));

        // -----  Koordinatenachsen
        mygl.setColor(1, 0, 0);
        zeichneStrecke(gl, -5, 0, 0, 5, 0, 0); // x-Achse
        mygl.setColor(0, 1, 0);
        zeichneStrecke(gl, 0, -5, 0, 0, 5, 0); // y-Achse
        mygl.setColor(0, 0, 1);
        zeichneStrecke(gl, 0, 0, -10, 0, 0, 10); // z-Achse

        // Zeichne den Boden
        mygl.setColor(0, 0, 0);
        zeichneBoden(gl);
//
        // Zeichne die Vorhang-Stange
        mygl.setColor(1, 0, 0);
        zeichneVorhangStange(gl);

        // Zeichne Vorhang
        mygl.setColor(0, 0, 1);
        zeichneVorhang(gl);

        // Berechne neue Vorhang (nodes) Positionen
        berechneNeueNodesPositionen();
    }

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
        new PhysicsExercise3();
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