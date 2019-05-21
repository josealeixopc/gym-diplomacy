package cruz.anacUtils;

import java.util.ArrayList;
import java.util.List;

public class MyWeightsMatrixAll {

    public static List<double[][]> weightMatrices;

    public static double[][] append(double[][] a, double[][] b) {
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static void setWeightMatrices() {
        weightMatrices = new ArrayList<>();

        double[][] m0 = append(MyWeightsMatrix00.matrix, MyWeightsMatrix01.matrix);
        m0 = append(m0, MyWeightsMatrix02.matrix);
        m0 = append(m0, MyWeightsMatrix03.matrix);
        m0 = append(m0, MyWeightsMatrix04.matrix);
        m0 = append(m0, MyWeightsMatrix05.matrix);
        m0 = append(m0, MyWeightsMatrix06.matrix);
        m0 = append(m0, MyWeightsMatrix07.matrix);

        weightMatrices.add(m0);

        weightMatrices.add(MyWeightsMatrix10.matrix);
        weightMatrices.add(MyWeightsMatrix20.matrix);
    }
}
