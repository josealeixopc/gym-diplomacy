package cruz.anacUtils;

import java.util.ArrayList;
import java.util.List;

public class MyWeightsMatrixAll {

    public static List<double[][]> weightMatrices = new ArrayList<double[][]>() {{
        // add(MyWeightsMatrix0.matrix);
        // add(MyWeightsMatrix1.matrix);
        // add(MyWeightsMatrix20.matrix);
        // add(MyWeightsMatrix30.matrix);
        // add(MyWeightsMatrix40.matrix);
        // add(MyWeightsMatrix50.matrix);
        // add(MyWeightsMatrix60.matrix);
    }};

    public static double[][] append(double[][] a, double[][] b) {
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
