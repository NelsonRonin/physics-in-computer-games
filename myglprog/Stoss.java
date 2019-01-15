//  -------------   Elastischer Stoss in der Ebene -------------------

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Stoss
       implements GLEventListener, KeyListener, ActionListener
{

    //  ---------  globale Daten  ---------------------------

    float xleft=-4, xright=4;                           // ViewingVolume
    float aspect, ybottom, ytop;
    float znear=-10, zfar=100;

    String windowTitle = "JOGL-Application";
    int windowWidth = 800;
    int windowHeight = 600;
    int canvasWidth = windowWidth;
    int canvasHeight = 8*windowHeight/10;
    String vShader = MyShaders.vShader1;                 // Vertex-Shader
    String fShader = MyShaders.fShader0;                 // Fragment-Shader

    JFrame f;
    GLCanvas canvas;                                     // OpenGL Window
    JTextField tfvx, tfvy;                               // Kontrollelemente
    JButton bt;
    Container c;

    int programId;                                       // OpenGL-Id
    MyGLBase1 mygl;                                      // Hilfsfunktionen
    int maxVerts = 2048;                                 // max. Anzahl Vertices im Vertex-Array

    // ----  Kugeldaten
    double x1start=-4, y1start=2;                        // Startposition Kugel 1
    double x2start=0, y2start=0;                         // Startposition Kugel 2
    double vx1start=0.8, vy1start=-0.6;                  // Geschwindigkeit Kugel 1  [m/s]
    double vx2start=0, vy2start=0     ;                  // Geschwindigkeit Kugel 2
    double m1=0.5, m2=0.5;                               // Massen
    double r1=0.2, r2=0.2;                               // Radien
    double dt = 0.02;                                    // Zeitschritt  [s]


    class Kugel
    {  double x,y;
       double vx, vy;
       double r;
       double m;

       public Kugel(double x, double y,
           double vx, double vy,
           double r, double m)
       { this.x = x;
       this.y = y;
       this.vx = vx;
       this.vy = vy;
       this.r = r;
       this.m = m;
       }

       public void draw(GL3 gl)
       {  zeichneKreis(gl,(float)x,(float)y,
           (float)r,20);
       }

       public void move(double dt)
       {  x += vx*dt;
          y += vy*dt;
       }

    }

     Kugel k1 = new Kugel(x1start,y1start,vx1start,vy1start,r1,m1);
     Kugel k2 = new Kugel(x2start,y2start,vx2start,vy2start,r2,m2);


    //  ---------  Methoden  --------------------------------

    public Stoss()                                   // Konstruktor
    { createFrame();
    }


    void createFrame()                                    // Fenster erzeugen
    { f = new JFrame(windowTitle);
      f.setSize(windowWidth, windowHeight);
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.setResizable(false);
      f.addKeyListener(this);
      c = f.getContentPane();
      c.setLayout(null);                                // LayoutManager ausschalten

       GLProfile glp = GLProfile.get(GLProfile.GL3);
       GLCapabilities glCaps = new GLCapabilities(glp);
       canvas = new GLCanvas(glCaps);
       canvas.addGLEventListener(this);
       canvas.setSize(canvasWidth, canvasHeight);
       canvas.addKeyListener(this);
       c.add(canvas);                                   // OpenGL-Window zum Frame hinzufuegen

      // ------  Kontrollelemente  --------
      int baseLine = windowHeight-80;
      JLabel labelvx = new JLabel("vx [m/s]:", SwingConstants.CENTER);
      JLabel labelvy = new JLabel("vy [m/s]:", SwingConstants.CENTER);
      labelvx.setBounds(10,baseLine,100,30);            // (x,y,width,height)
      labelvy.setBounds(240,baseLine,120,30);
      c.add(labelvx);
      c.add(labelvy);
      tfvx = new JTextField(10);
      tfvy = new JTextField(10);
      tfvx.setBounds(104, baseLine, 100, 30);
      tfvy.setBounds(370, baseLine, 100, 30);
      c.add(tfvx);
      c.add(tfvy);
      bt = new JButton("start");
      bt.setBounds(700, baseLine, 70,30);
      bt.addActionListener(this);
      c.add(bt);
      f.setVisible(true);
    }


    public void zeichneStrecke(GL3 gl, float x1, float y1, float z1,
                                 float x2, float y2, float z2)
    {  mygl.rewindBuffer(gl);
       mygl.putVertex(x1,y1,z1);
       mygl.putVertex(x2,y2,z2);
       mygl.copyBuffer(gl);
       mygl.drawArrays(gl, GL3.GL_LINES);
    }


    public void zeichneDreieck(GL3 gl, float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3)
    {  mygl.rewindBuffer(gl);
       mygl.putVertex(x1,y1,z1);
       mygl.putVertex(x2,y2,z2);
       mygl.putVertex(x3,y3,z3);
       mygl.copyBuffer(gl);
       mygl.drawArrays(gl, GL3.GL_TRIANGLES);
    }


    public void zeichneKreis(GL3 gl,
                             float xm, float ym,
                             float r, int nPkte)
    {  double phi = 2*Math.PI / nPkte;
       mygl.rewindBuffer(gl);
       mygl.putVertex(xm, ym, 0);
       for (int i=0; i<=nPkte; i++)
         mygl.putVertex((float)(xm+r*Math.cos(i*phi)),
                    (float)(ym+r*Math.sin(i*phi)),0);
       mygl.copyBuffer(gl);
       mygl.drawArrays(gl, GL3.GL_TRIANGLE_FAN);
    }

    void stoss(Kugel k1, Kugel k2)
    {  Vec3 n = new Vec3(k2.x-k1.x, k2.y-k1.y,0);
        double d = n.length();
        if ( d > k1.r+k2.r)
            return;
        double m1 = k1.m;
        double m2 = k2.m;
        double m = m1+m2;
        n = n.normalize();
        Vec3 v1 = new Vec3(k1.vx, k1.vy, 0);
        Vec3 v2 = new Vec3(k2.vx, k2.vy, 0);
        double v1n = v1.dot(n);
        double v2n = v2.dot(n);
        Vec3 v1nVec = n.scale((float)v1n);
        Vec3 v2nVec = n.scale((float)v2n);
        Vec3 v1p = v1.subtract(v1nVec);
        Vec3 v2p = v2.subtract(v2nVec);
        double vv1n = (m1*v1n+m2*v2n-(v1n-v2n)*m2)/m;
        double vv2n = (m2*v2n+m1*v1n-(v2n-v1n)*m1)/m;
        Vec3 vv1 = v1p.add(n.scale((float)vv1n));
        Vec3 vv2 = v2p.add(n.scale((float)vv2n));
        k1.vx = vv1.x;
        k1.vy = vv1.y;
        k2.vx = vv2.x;
        k2.vy = vv2.y;
    }

    //  ----------  OpenGL-Events   ---------------------------

    @Override
    public void init(GLAutoDrawable drawable)             //  Initialisierung
    {  GL3 gl = drawable.getGL().getGL3();
       System.out.println("OpenGl Version: " + gl.glGetString(gl.GL_VERSION));
       System.out.println("Shading Language: " + gl.glGetString(gl.GL_SHADING_LANGUAGE_VERSION));
       System.out.println();
       programId = MyShaders.initShaders(gl,vShader,fShader);
       mygl = new MyGLBase1(gl, programId, maxVerts);
       gl.glClearColor(0,0,1,1);                          // Hintergrundfarbe
       FPSAnimator anim = new FPSAnimator(canvas,80,true);
       anim.start();
       tfvx.setText(String.format("%1.2f", vx1start));
       tfvy.setText(String.format("%1.2f", vy1start));
    }


    @Override
    public void display(GLAutoDrawable drawable)
     { GL3 gl = drawable.getGL().getGL3();
       gl.glClear(GL3.GL_COLOR_BUFFER_BIT);      // Bildschirm loeschen
       mygl.setColor(0.7f, 0.7f, 0.7f);
       mygl.setM(gl, Mat4.ID);
       zeichneStrecke(gl,xleft,0,0, xright,0,0);               // x-Achse
       zeichneStrecke(gl,0,ybottom,0, 0,ytop,0);               // y-Achse
       mygl.setColor(1,1,0);
       this.stoss(k1, k2);
       k1.draw(gl);
       k2.draw(gl);
       k1.move(dt);
       k2.move(dt);
     }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height)
    {  GL3 gl = drawable.getGL().getGL3();
       aspect = (float)height/width;
       ybottom = aspect*xleft;
       ytop = aspect*xright;
       mygl.setP(gl, Mat4.ortho(xleft, xright, ybottom, ytop, znear, zfar));
       // Set the viewport to be the entire window
       gl.glViewport(0, 0, width, height);
    }


    @Override
    public void dispose(GLAutoDrawable drawable)  { }                  // not needed


    //  -----------  main-Methode  ---------------------------

    public static void main(String[] args)
    { new Stoss();
    }


    //  ------  Ereignisverarbeitungen  --------

    public void keyPressed(KeyEvent e)
    { int key = e.getKeyCode();
    }
    public void keyTyped(KeyEvent e)
    { char key = e.getKeyChar();
    }

    public void keyReleased(KeyEvent e) { }

    String trim(String s)                // fuehrende Blanks entfernen
    {  int n = s.length();
       for (int i=0; i<n; i++)
           if ( s.charAt(i) != ' ' )
              return s.substring(i);
        return "";
    }


    public void actionPerformed(ActionEvent e)
      {  JButton b = (JButton)e.getSource();
         if ( b == bt )
         {  String txtvx = tfvx.getText();
            String txtvy = tfvy.getText();
            vx1start = Double.parseDouble(trim(txtvx));
            vy1start = Double.parseDouble(trim(txtvy));
            k1 = new Kugel(x1start,y1start,vx1start,vy1start,r1,m1);
            k2 = new Kugel(x2start,y2start,vx2start,vy2start,r2,m2);
            System.out.println("start");
         }
      }

}