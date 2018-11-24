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
import java.util.Random;

public class PhysicsExercise3 implements WindowListener, GLEventListener, KeyListener {

    //  ---------  global data  ---------------------------
    private String windowTitle = "Physics Exercise 3 - curtain with wind";
    private int windowWidth = 1000;
    private int windowHeight = 1000;
    private String vShader = MyShaders.vShader1;                // Vertex-Shader
    private String fShader = MyShaders.fShader0;                // Fragment-Shader

    private GLCanvas canvas;                                    // OpenGL Window
    private int programId;                                      // OpenGL-Id
    private MyGLBase1 mygl;                                     // Help functions
    private int maxVerts = 2048;                                // max. Amount Vertices in Vertex-Array

    //  ---------  Projection matrix variables  ---------------------------
    private float xLeft = -2, xRight = 2;
    private float yBottom = -2, yTop = 2;
    private float zNear = -100, zFar = 1000;

    private float elevation = 10;
    private float azimut = 30;
    private float distance = 3;                                 // Distance to O
    private Vec3 A = new Vec3(0,1, distance);             // Camera-Pos. (Eye)
    private Vec3 B = new Vec3(0,0.6,0);             // LookAt (Point of interest)
    private Vec3 up = new Vec3(0,1,0);                 // Up-Direction

    //  ---------  Data for curtain/wind animation  ---------------------------
    private float curtainHeight = 1.2f;
    private float curtainWidth = 2;
    private int cols = 50;                                      // Column count of curtain
    private int rows = 30;                                      // Row count of curtain
    private Vec3[][] curtainNodes = new Vec3[rows][cols];       // All nodes of curtain
    private Vec3[][] nodesV = new Vec3[rows][cols];             // Velocity of each node

    // Wind random min and max
    private float windXmin = -0.6f, windXmax = 0.6f;
    private float windYmin = -0.1f, windYmax = 0.1f;
    private float windZmin = -1.1f, windZmax = 1.2f;

    //  ---------  Methods  --------------------------------

    public PhysicsExercise3()
    {
        createFrame();
    }

    void createFrame()
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

        // Add keyListener to canvas and frame
        f.addKeyListener(this);
        canvas.addKeyListener(this);
    }

    /**
     * @param gl gl object
     * @param x1 start x position
     * @param y1 start y position
     * @param z1 start z position
     * @param x2 end x position
     * @param y2 end y position
     * @param z2 end z position
     */
    private void drawLine(GL3 gl, float x1, float y1, float z1,
                          float x2, float y2, float z2
    ) {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1, y1, z1);
        mygl.putVertex(x2, y2, z2);
        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_LINE_STRIP);
    }

    /**
     * @param gl gl object
     */
    private void drawFloor(GL3 gl) {
        int floorCount = 24;
        int floorPositionY = 0;
        float floorLineDistance = 0.5f;
        int floorLineLength = 10;

        // Draw line along x-Axe
        for (float i = -floorCount; i < floorCount; i += floorLineDistance) {
            drawLine(gl, i, floorPositionY, 0, i, floorPositionY, floorLineLength);
        }

        // Draw line along z-Axe
        for (float j = 0; j < (2 * floorCount); j += floorLineDistance) {
            drawLine(gl, -floorLineLength, floorPositionY, 0, floorLineLength, floorPositionY, j);
        }
    }

    /**
     * @param gl gl object
     */
    private void drawCurtainBar(GL3 gl) {
        int lastColIndex = cols - 1;

        drawLine(gl,
            curtainNodes[0][0].x - 0.5f, curtainNodes[0][0].y, curtainNodes[0][0].z,
            curtainNodes[0][lastColIndex].x + 0.5f, curtainNodes[0][lastColIndex].y, curtainNodes[0][lastColIndex].z
        );
        drawLine(gl,
            curtainNodes[0][0].x - 0.5f, curtainNodes[0][0].y + 0.2f, curtainNodes[0][0].z,
            curtainNodes[0][lastColIndex].x + 0.5f, curtainNodes[0][lastColIndex].y + 0.2f, curtainNodes[0][lastColIndex].z
        );
        drawLine(gl,
            curtainNodes[0][0].x - 0.5f, curtainNodes[0][0].y, curtainNodes[0][0].z,
            curtainNodes[0][0].x - 0.5f, curtainNodes[0][0].y + 0.2f, curtainNodes[0][0].z
        );
        drawLine(gl,
            curtainNodes[0][lastColIndex].x + 0.5f, curtainNodes[0][lastColIndex].y, curtainNodes[0][lastColIndex].z,
            curtainNodes[0][lastColIndex].x + 0.5f, curtainNodes[0][lastColIndex].y + 0.2f, curtainNodes[0][lastColIndex].z
        );
    }

    /**
     * @param gl gl object
     */
    private void drawCurtain(GL3 gl) {
        // Fetch all rows
        for (int i = 0; i < rows; i++) {
            // Fetch all columns
            for (int j = 0; j < cols; j++) {
                // Draw vertical lines
                if (i < rows - 1) {
                    Vec3 currentNodeRowStart = curtainNodes[i][j];
                    Vec3 currentNodeRowEnd = curtainNodes[i + 1][j];

                    drawLine(gl,
                        currentNodeRowStart.x, currentNodeRowStart.y, currentNodeRowStart.z,
                        currentNodeRowEnd.x, currentNodeRowEnd.y, currentNodeRowEnd.z
                    );
                }

                // Draw horizontal lines
                if (j < cols - 1) {
                    Vec3 currentNodeColStart = curtainNodes[i][j];
                    Vec3 currentNodeColEnd = curtainNodes[i][j + 1];

                    drawLine(gl,
                        currentNodeColStart.x, currentNodeColStart.y, currentNodeColStart.z,
                        currentNodeColEnd.x, currentNodeColEnd.y, currentNodeColEnd.z
                    );
                }
            }
        }
    }

    /**
     * Initialize positions and velocities for all nodes of curtain
     */
    private void initCurtainNodes() {
        // Sizes of curtain
        float startPosTop = 1.4f;
        float startPosLeft = -1f;

        // Initial positions of a node
        int zPos = 0;
        double stepSizeH = (double) curtainWidth / cols;
        double stepSizeV = (double) curtainHeight / rows;

        // Fetch all rows
        float rowPos = startPosTop;
        for (int i = 0; i < rows; i++) {

            float colPos = startPosLeft;
            // Fetch all columns
            for (int j = 0; j < cols; j++) {
                // Initialize position of each node
                curtainNodes[i][j] = new Vec3(colPos, rowPos, zPos);

                // Initialize velocity for each node with Zero
                nodesV[i][j] = new Vec3(0, 0, 0);
                colPos += stepSizeH;
            }
            rowPos -= stepSizeV;
        }
    }

    /**
     * Calculate new positions for every node in the curtain
     */
    private void calculateNewNodesPositions() {
        // Constants for calculation of each node
        float dt = 0.01f;
        float g = 0.981f;
        float m = 0.1f;
        float velocityAbsorption = 0.95f;

        // Random wind forces for each column
        Vec3[] windForces = getWindForcesForColumns();

        // Calculate all spring forces of current node positions
        Vec3[][] springForces = getAllSpringForces();

        // Fetch all rows (except first row, these node stick to the curtain bar)
        for (int i = 1; i < rows; i++) {
            // Fetch all columns
            for (int j = 0; j < cols; j++) {
                // Gravitational force
                Vec3 mainForce = new Vec3(0, -m * g, 0);

                // Calculate spring forces of all neighbours and sum them up with the gravitational force
                mainForce = mainForce.add(springForces[i][j]);

                // Add random wind force (calculated before for each column)
                mainForce = mainForce.add(windForces[j]);

                // a = F / m
                Vec3 a = mainForce.scale(1/m);

                // v = v + a * dt
                nodesV[i][j] = nodesV[i][j].add( a.scale(dt) );

                // v = 0.95 * v
                nodesV[i][j] = nodesV[i][j].scale(velocityAbsorption);

                // x = x + v * dt
                curtainNodes[i][j] = curtainNodes[i][j].add(nodesV[i][j].scale(dt));
            }
        }
    }

    /**
     * Generates random wind forces for all columns
     *
     * @return Vec3[]: all random wind forces for each column
     */
    private Vec3[] getWindForcesForColumns() {
        Vec3[] windForces = new Vec3[cols];
        for (int j = 0; j < cols; j++) {
            windForces[j] = new Vec3(
                getRandomNumberInRange(windXmin, windXmax),
                getRandomNumberInRange(windYmin, windYmax),
                getRandomNumberInRange(windZmin, windZmax)
            );
        }

        return windForces;
    }

    private Vec3[][] getAllSpringForces() {
        Vec3[][] springForces = new Vec3[rows][cols];
        float k = 500;

        for (int i = 1; i < rows; i++) {
            // Fetch all columns
            for (int j = 0; j < cols; j++) {
                springForces[i][j] = calculateSpringForce(i, j, k);
            }
        }

        return springForces;
    }

    /**
     * Calculate vector sum of all spring forces of nodes neighbours
     *
     * @param rowIndex  row index of current node
     * @param colIndex  col index of current node
     * @param k         constant for spring force
     * @return          spring force sum
     */
    private Vec3 calculateSpringForce(int rowIndex, int colIndex, float k) {
        Vec3 currentNode = curtainNodes[rowIndex][colIndex];
        Vec3 springForce = new Vec3(0,0,0);

        // d0 is initial distance between nodes (different between columns and rows)
        float d0UpDown = curtainHeight / rows;
        float d0LeftRight = curtainWidth / cols;

        // Node LEFT of current node (exists)
        if (colIndex > 0) {
            springForce = springForce.add(getSpringForce(currentNode, curtainNodes[rowIndex][colIndex - 1], k, d0LeftRight));
        }

        // Node RIGHT of current node (exists)
        if (colIndex < curtainNodes[rowIndex].length - 1) {
            springForce = springForce.add(getSpringForce(currentNode, curtainNodes[rowIndex][colIndex + 1], k, d0LeftRight));
        }

        // Node UP of current node (exists)
        if (rowIndex > 0) {
            springForce = springForce.add(getSpringForce(currentNode, curtainNodes[rowIndex - 1][colIndex], k, d0UpDown));
        }

        // Node DOWN of current node (exists)
        if (rowIndex < curtainNodes.length - 1) {
            springForce = springForce.add(getSpringForce(currentNode, curtainNodes[rowIndex + 1][colIndex], k, d0UpDown));
        }

        return springForce;
    }

    /**
     * Calculate spring force between current node and given neighbour node
     *
     * @param currentNode   currently calculated node
     * @param nextNode      neighbour of current node
     * @param k             spring force constant
     * @param d0            given initial distance between given nodes
     * @return              calculated spring force
     */
    private Vec3 getSpringForce(Vec3 currentNode, Vec3 nextNode, float k, float d0) {
        // d = AB = B - A
        Vec3 dVector = nextNode.subtract(currentNode);
        // |d| = sqrt( pow(d.x) + pow(d.y) + pow(d.z) )
        float normDvector = currentNode.distance(nextNode);
        // (|d| - d0)
        float springDeflection = normDvector - d0;
        // (d / |d|)
        Vec3 springVectorDivide = dVector.scale(1/normDvector);
        // F = k  *  (|d| - d0)  *  (d / |d|)
        return springVectorDivide.scale(k * springDeflection);
    }

    /**
     * Get random number in given range
     *
     * @param min lower range limit
     * @param max upper range limit
     * @return    random number
     */
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
        gl.glClearColor(0.9f, 0.9f, 0.9f, 1);    // Background color

        // Initialize animation with 40 fps and start (Display method will be called 40 times per frame)
        FPSAnimator anim = new FPSAnimator(canvas, 40, true);
        anim.start();

        // Initialize variables
        initCurtainNodes();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL3 gl = drawable.getGL().getGL3();
        // -----  Visibility test
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        // ------ Projection matrix
        mygl.setP(gl, Mat4.ortho(xLeft, xRight, yBottom, yTop, zNear, zFar));

        // -----  Camera-System
        Mat4 R1 = Mat4.rotate(-elevation, 1, 0, 0);
        Mat4 R2 = Mat4.rotate(azimut, 0, 1, 0);
        Mat4 R = R2.postMultiply(R1);
        mygl.setM(gl, Mat4.lookAt(R.transform(A), R.transform(B), R.transform(up)));
        mygl.setShadingLevel(gl, 0);

        // -----  Coordinate axes
        mygl.setColor(1, 0, 0);
        drawLine(gl, -5, 0, 0, 5, 0, 0); // x-Axe
        mygl.setColor(0, 1, 0);
        drawLine(gl, curtainNodes[0][0].x - 0.51f, -5, 0, -1.5f, 5, 0); // y-Axe
        drawLine(gl, curtainNodes[0][curtainNodes[0].length-1].x + 0.51f, -5, 0, 1.5f, 5, 0); // y-Axe
        mygl.setColor(0, 0, 1);
        drawLine(gl, 0, 0, -10, 0, 0, 10); // z-Axe

        // DRAW SCENE
        mygl.setColor(0, 0, 0);
        drawFloor(gl);

        mygl.setColor(1, 0, 0);
        drawCurtainBar(gl);

        mygl.setColor(0, 0, 1);
        drawCurtain(gl);

        // Calculate new curtain (nodes) positions
        calculateNewNodesPositions();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) {
        GL3 gl = drawable.getGL().getGL3();

        // Prevent deformation of Image
        float aspect = (float)height / width;
        yBottom = aspect * xLeft;
        yTop = aspect * xRight;

        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {} // not needed


    //  -----------  main-Method  ---------------------------

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

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code)
        {
            case KeyEvent.VK_LEFT: azimut--;
                break;
            case KeyEvent.VK_RIGHT: azimut++;
                break;
            case KeyEvent.VK_UP: elevation++;
                break;
            case KeyEvent.VK_DOWN: elevation--;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {
        char key = e.getKeyChar();

        switch (key) {
            // Capital V: Abschussgeschwindigkeit vergrössern
            case 'W':
                windXmin -= 0.01f;
                windXmax += 0.01f;
                windYmin -= 0.01f;
                windYmax += 0.01f;
                windZmin -= 0.01f;
                windZmax += 0.01f;
                break;
            // Lower v: Abschussgeschwindigkeit verkleinern
            case 'w':
                windXmin += 0.01f;
                windXmax -= 0.01f;
                windYmin += 0.01f;
                windYmax -= 0.01f;
                windZmin += 0.01f;
                windZmax -= 0.01f;
                break;

            default:
                break;
        }
    }
}