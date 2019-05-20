package cruz.anacUtils;

import java.util.ArrayList;
import java.util.List;

public class MyWeightsMatrixAll {

    public static List<double[][]> weightMatrices = new ArrayList<double[][]>() {{
        add(MyWeightsMatrix0.matrix);
        add(MyWeightsMatrix1.matrix);
        add(MyWeightsMatrix2.matrix);
        add(MyWeightsMatrix3.matrix);
        add(MyWeightsMatrix4.matrix);
        add(MyWeightsMatrix5.matrix);
        add(MyWeightsMatrix6.matrix);
    }};
}
