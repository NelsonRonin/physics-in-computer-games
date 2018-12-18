package Uebungen;

import ch.fhnw.util.math.Mat4;
import ch.fhnw.util.math.Quaternion;
import ch.fhnw.util.math.Vec3;

public class GyroDynamics extends Dynamics {
    // Zustandsvektor:   [w1, w2, w3, q0, q1, q2, q3]
    private double[] x;

    // Trägheitsmomente
    double I1, I2, I3;

    public GyroDynamics(double I1, double I2, double I3) {
        this.I1 = I1;
        this.I2 = I2;
        this.I3 = I3;
    }

    public void move(double dt) {
        x = runge(x, dt);
        double q0 = x[3], q1 = x[4], q2 = x[5], q3 = x[6];
        Quaternion q = new Quaternion(q1, q2, q3, q0);
        q = q.normalize();
        x[3] = q.w;
        x[4] = q.x;
        x[5] = q.y;
        x[6] = q.z;
    }

    public void setState(double w1, double w2, double w3,
                         double q0, double q1, double q2, double q3) {
        x = new double[] {
            w1, w2, w3,
            q0, q1, q2, q3
        };
    }

    public Mat4 getRotation() {
        double q0 = x[3], q1 = x[4], q2 = x[5], q3 = x[6];
        Quaternion quaternion = new Quaternion(q1, q2, q3, q0);
        return new Mat4(quaternion);
    }

    public double[] f(double[] x) {
        double w1 = x[0], w2 = x[1], w3 = x[2];
        double q0 = x[3], q1 = x[4], q2 = x[5], q3 = x[6];
        Vec3 D = torque(x);

        return new double[] {
            ((I2 - I3) * w2 * w3 + D.x) / I1,
            ((I3 - I1) * w3 * w1 + D.y) / I2,
            ((I1 - I2) * w1 * w2 + D.z) / I3,
            -0.5 * (q1*w1 + q2*w2 + q3*w3),
            0.5 * (q0*w1 + q2*w3 - q3*w2),
            0.5 * (q0*w2 + q3*w1 - q1*w3),
            0.5 * (q0*w3 + q1*w2 - q2*w1),
        };
    }

    public Vec3 torque(double [] x) {
        return Vec3.ZERO;
    }
}
