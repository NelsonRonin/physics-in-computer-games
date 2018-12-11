package Uebungen;

import ch.fhnw.util.math.Vec3;
import com.jogamp.opengl.GL3;

public class Quader {
    private MyGLBase1 mygl;
    private double a;
    private double b;
    private double c;

    public Quader(MyGLBase1 mygl, double a, double b, double c) {
        this.mygl = mygl;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public void drawQuader(GL3 gl) {
        double a2 = a * 0.5;
        double b2 = b * 0.5;
        double c2 = c * 0.5;

        // Floor area edges
        Vec3 A = new Vec3(a2, -b2, c2);
        Vec3 B = new Vec3(a2, -b2, -c2);
        Vec3 C = new Vec3(-a2, -b2, -c2);
        Vec3 D = new Vec3(-a2, -b2, c2);

        // Roof area edges
        Vec3 E = new Vec3(a2, b2, c2);
        Vec3 F = new Vec3(a2, b2, -c2);
        Vec3 G = new Vec3(-a2, b2, -c2);
        Vec3 H = new Vec3(-a2, b2, c2);

        // Draw floor
        mygl.setNormal(0, -1, 0);
        drawArea(A, B, C, D);
        // Draw roof
        mygl.setNormal(0, 1, 0);
        drawArea(E, F, G, H);
        // Draw front
        mygl.setNormal(1, 0, 0);
        drawArea(A, B, F, E);
        // Draw back
        mygl.setNormal(-1, 0, 0);
        drawArea(D, C, G, H);
        // Draw left
        mygl.setNormal(0, 0, 1);
        drawArea(A, D, H, E);
        // Draw right
        mygl.setNormal(0, 0, -1);
        drawArea(B, C, G, F);

        mygl.copyBuffer(gl);
        mygl.drawArrays(gl, GL3.GL_TRIANGLES);
    }

    private void drawArea(Vec3 A, Vec3 B, Vec3 C, Vec3 D) {
        mygl.putVertex(A.x, A.y, A.z);
        mygl.putVertex(B.x, B.y, B.z);
        mygl.putVertex(C.x, C.y, C.z);

        mygl.putVertex(C.x, C.y, C.z);
        mygl.putVertex(D.x, D.y, D.z);
        mygl.putVertex(A.x, A.y, A.z);
    }

    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }
}
