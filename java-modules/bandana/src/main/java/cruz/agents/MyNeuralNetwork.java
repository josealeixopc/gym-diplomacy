package cruz.agents;

import com.google.gson.Gson;
import cruz.anacUtils.MyWeightsMatrixAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MyNeuralNetwork {

    /**
     * Code related to operations comes from here: https://gist.github.com/Jeraldy/7d4262db0536d27906b1e397662512bc
     */

    public static double[][] predict(double[][] input) {
        return predict(input, MyWeightsMatrixAll.weightMatrices);
    }

    public static double[][] predict(double[][] input, List<double[][]> weightMatrices) {
        double[][] currMatrix = input;
        double[][] resultMatrix = null;

        for (double[][] weightMatrix : weightMatrices) {
            resultMatrix = multiply(currMatrix, weightMatrix);
            resultMatrix = softmax(resultMatrix);
            currMatrix = resultMatrix;
        }

        return resultMatrix;
    }

    /**
     * Element wise multiplication
     *
     * @param a matrix
     * @param x matrix
     * @return y = a * x
     */
    public static double[][] multiply(double[][] x, double[][] a) {
        int m = a.length;
        int n = a[0].length;

        if (x.length != m || x[0].length != n) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }
        double[][] y = new double[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                y[j][i] = a[j][i] * x[j][i];
            }
        }
        return y;
    }

    public static double[][] softmax(double[][] z) {
        double[][] zout = new double[z.length][z[0].length];
        double sum = 0.;
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                sum += Math.exp(z[i][j]);
            }
        }
        for (int i = 0; i < z.length; i++) {
            for (int j = 0; j < z[0].length; j++) {
                zout[i][j] = Math.exp(z[i][j]) / sum;
            }
        }
        return zout;
    }

    /**
     * @param a matrix
     * @return sigmoid of matrix a
     */
    public static double[][] sigmoid(double[][] a) {
        int m = a.length;
        int n = a[0].length;
        double[][] z = new double[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                z[i][j] = (1.0 / (1 + Math.exp(-a[i][j])));
            }
        }
        return z;
    }

    public static void main(String[] args) {
        double[][] input = {
                {0, 1, 2},
                {0, 2, 3}
        };
        predict(input);
    }

}
