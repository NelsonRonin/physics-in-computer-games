public abstract class Dynamics {

    abstract double[] f(double[] x);      // Vektorfelder

    public double[] euler (double[] x, double dt) {
        double[] y = f(x);
        int n = x.length;
        double[] xx = new double[n];

        for(int i=0; i < n; i++) {
            xx[i] = x[i] + y[i] * dt;
        }

        return xx;
    }
}
