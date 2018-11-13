import ch.fhnw.util.math.Mat4;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class Car implements WindowListener, GLEventListener, KeyListener {
    //  ---------  globale Daten  ---------------------------

    float xleft = -30;
    float xright = 30;
    float ybottom = -26;
    float ytop = 26;
    float znear = -100;
    float zfar = 1000;

    final float dt = 0.1f;  // Zeitschritt

    String windowTitle = "JOGL-Application";
    int windowWidth = 800;
    int windowHeight = 600;
    String vShader = MyShaders.vShader0;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader

    String vShader1 = MyShaders.vShader1;                 // Vertex-Shader
    String fShader1 = MyShaders.fShader0;                 // Fragment-Shader

    Frame frame;
    GLCanvas canvas;                                     // OpenGL Window
    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    Vehicle veh = new Vehicle();

    class Vehicle
    {
        float b = 1.5f;
        float d = 4;

        float alpha = 10f;          // Lenkradeinschlag
        float v = 2;               // Schnelligkeit
        float w = 0.5f, h=0.3f;     // Wheel sizes
        float toRad = (float)(Math.PI / 180);   // Transform angle from degree to rad
        float toDeg = (float)(180 / Math.PI);   // Transform angle from rad tp degree

        void draw(GL3 gl){
            mygl.rewindBuffer(gl);

            mygl.putVertex(0, -b,0);
            mygl.putVertex(d, -b,0);
            mygl.putVertex(d, b,0);
            mygl.putVertex(0, b,0);

            mygl.copyBuffer(gl);
            mygl.drawArrays(gl, GL3.GL_LINE_LOOP);

            // Car wheels
            mygl.pushM();
            // Wheel 1  (left, back)
            mygl.multM(gl, Mat4.translate(0, 1.5f * b, 0));
            veh.drawWheel(gl);
            // Wheel 2  (right back)
            mygl.multM(gl, Mat4.translate(0, -3f * b, 0));
            veh.drawWheel(gl);
            // Wheel 3  (right, front)
            mygl.multM(gl, Mat4.translate(d , 0, 0));
            mygl.pushM();
            mygl.multM(gl, Mat4.rotate(alpha, 0, 0, 1));
            veh.drawWheel(gl);
            // Wheel 4  (left, front)
            mygl.popM(gl);
            mygl.multM(gl, Mat4.translate(0, 3f * b, 0));
            mygl.multM(gl, Mat4.rotate(alpha, 0, 0, 1));
            veh.drawWheel(gl);
            mygl.popM(gl);
        }

        void drawWheel(GL3 gl)
        {
            mygl.rewindBuffer(gl);

            mygl.putVertex(-w, -h,0);
            mygl.putVertex(w, -h,0);
            mygl.putVertex(w, h,0);
            mygl.putVertex(-w, h,0);

            mygl.copyBuffer(gl);
            mygl.drawArrays(gl, GL3.GL_LINE_LOOP);
        }

        void move(GL3 gl, float dt)
        {
            //gar nichts machen --> über Objektsystem --> Matrix :) M (ModelView Matrix)

            if( Math.abs(alpha) < 1.0e-6) {
                mygl.multM(gl, Mat4.translate(v * dt, 0, 0));
            } else {
                double ym = b + (d / Math.tan(alpha * toRad));

                mygl.multM(gl, Mat4.translate(0, (float)ym , 0));
                mygl.multM(gl, Mat4.rotate((float) ((v * dt) / ym) * toDeg, 0, 0, 1));
                mygl.multM(gl, Mat4.translate(0, (float)-ym , 0));
            }
        }
    }

    //  ---------  Keyboard Events----------------------------
    @Override
    public void keyTyped(KeyEvent e) {
        char key = e.getKeyChar();

        switch (key) {
            // Stop the vehicle
            case 's':
                veh.v = 0;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            // Left arrow: turn left
            case KeyEvent.VK_LEFT:
                veh.alpha++;
                break;

            // Right arrow: turn right
            case KeyEvent.VK_RIGHT:
                veh.alpha--;
                break;

            // Go faster (increase v)
            case KeyEvent.VK_UP:
                veh.v += 0.2f;
                break;

            // Go slower (decrease v)
            case KeyEvent.VK_DOWN:
                veh.v -= 0.2f;
                break;

            default:
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    //  ---------  Methoden  --------------------------------

    public Car()                                   // Konstruktor
    {
        createFrame();
    }


    void createFrame()                             // Fenster erzeugen
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


    public void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                               float x2, float y2, float z2)
    {
        mygl.rewindBuffer(gl);
        mygl.putVertex(x1,y1,z1);
        mygl.putVertex(x2,y2,z2);
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
        programId = MyShaders.initShaders(gl,vShader1,fShader);
        mygl = new MyGLBase1(gl, programId, maxVerts);
        gl.glClearColor(0,0,0,1);                          // Hintergrundfarbe

        FPSAnimator anim = new FPSAnimator(canvas, 24, true);
        anim.start();
    }


    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL3 gl = drawable.getGL().getGL3();
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);      // Bildschirm loeschen

        mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar)); //setze Projektionsmatrix

        mygl.pushM();
        mygl.setM(gl, Mat4.ID);

        mygl.setColor(0.7f, 0.7f, 0.7f);
        zeichneStrecke(gl,-20,0,0, 20,0,0);               // x-Achse
        zeichneStrecke(gl,0,-20,0, 0,20,0);               // y-Achse

        mygl.setColor(1,1,0);
        mygl.popM(gl);
        veh.draw(gl);
        veh.move(gl, dt);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height)
    {
        GL3 gl = drawable.getGL().getGL3();
        // Set the viewport to be the entire window
        gl.glViewport(0, 0, width, height);
    }


    @Override
    public void dispose(GLAutoDrawable drawable)  { }                  // not needed

    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args)
    {
        new Car();
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
}
