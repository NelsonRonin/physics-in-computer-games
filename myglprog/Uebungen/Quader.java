package Uebungen;

import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.GL3;

public class Quader {
    private MyGLBase1 mygl;

    private Vec3 rotationAxis;

    // Side lengths
    private double a;
    private double b;
    private double c;

    // Quader floor edges
    private Vec3 A;
    private Vec3 B;
    private Vec3 C;
    private Vec3 D;

    // Quader roof edges
    private Vec3 E;
    private Vec3 F;
    private Vec3 G;
    private Vec3 H;

    public Quader(MyGLBase1 mygl, double a, double b, double c, Vec3 rotationAxis) {
        this.mygl = mygl;
        this.a = a;
        this.b = b;
        this.c = c;

        this.rotationAxis = rotationAxis;

        // Prepare half lengths for edges
        double a2 = a * 0.5;
        double b2 = b * 0.5;
        double c2 = c * 0.5;

        setEdges(a, b, c, a2, b2, c2);
    }

    public Quader(MyGLBase1 mygl, double a, double b, double c) {
        this.mygl = mygl;
        this.a = a;
        this.b = b;
        this.c = c;

        // Prepare half lengths for edges
        double a2 = a * 0.5;
        double b2 = b * 0.5;
        double c2 = c * 0.5;

        setEdges(a, b, c, a2, b2, c2);
    }

    public void drawQuader(GL3 gl) {
        mygl.rewindBuffer(gl);

        // Draw floor
        putArea(A, B, C, D, new Vec3(0, -1, 0));
        // Draw roof
        putArea(E, F, G, H, new Vec3(0, 1, 0));
        // Draw front
        putArea(A, B, F, E, new Vec3(1, 0, 0));
        // Draw back
        putArea(D, C, G, H, new Vec3(-1, 0, 0));
        // Draw left
        putArea(A, D, H, E, new Vec3(0, 0, 1));
        // Draw right
        putArea(B, C, G, F, new Vec3(0, 0, -1));

        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_TRIANGLES);
    }

    private void putArea(Vec3 A, Vec3 B, Vec3 C, Vec3 D, Vec3 n) {
        mygl.setNormal(n.x, n.y, n.z);

        mygl.putVertex(A.x, A.y, A.z);
        mygl.putVertex(B.x, B.y, B.z);
        mygl.putVertex(C.x, C.y, C.z);

        mygl.putVertex(C.x, C.y, C.z);
        mygl.putVertex(D.x, D.y, D.z);
        mygl.putVertex(A.x, A.y, A.z);
    }

    private void setEdges(double a, double b, double c, double a2, double b2, double c2) {
        // Floor area edges
        A = new Vec3(a2, -b2, c2);
        B = new Vec3(a2, -b2, -c2);
        C = new Vec3(-a2, -b2, -c2);
        D = new Vec3(-a2, -b2, c2);

        // Roof area edges
        E = new Vec3(a2, b2, c2);
        F = new Vec3(a2, b2, -c2);
        G = new Vec3(-a2, b2, -c2);
        H = new Vec3(-a2, b2, c2);
    }

    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }
    public Vec3 getRotationAxis() { return rotationAxis; }
}
