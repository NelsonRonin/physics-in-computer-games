package Uebungen;

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

    public double[] runge(double[] x, double dt){
        double[] y1 = f(x);
        int n = x.length;
        double[] xtmp = new double[n];
        double dt2 = dt * 0.5;

        for (int i =0; i<n; i++){
            xtmp[i] = x[i] + y1[i] *dt2;
        }

        double[] y2 = f(xtmp);
        for (int i =0; i<n; i++){
            xtmp[i] = x[i] + y2[i] *dt2;
        }

        double[] y3 = f(xtmp);
        for (int i =0; i<n; i++){
            xtmp[i] = x[i] + y3[i] *dt;
        }

        double[] y4 = f(xtmp);
        double[] y = new double[n];

        for (int i =0; i<n; i++){
            y[i] = (y1[i] + 2*y2[i]+ 2*y3[i]+ y4[i])/6;
        }

        double[] xx = new double[n];

        for(int i = 0; i < n ; i++){
            xx[i] = x[i] + y[i] * dt;
        }
        return xx;
    }
}
