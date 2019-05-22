package cruz.agents;

import com.google.gson.Gson;
import cruz.anacUtils.MyWeightsMatrixAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("Duplicates")


public class MyNeuralNetwork {

    /**
     * Code related to operations comes from here: https://gist.github.com/Jeraldy/7d4262db0536d27906b1e397662512bc
     */

    public static double[][] predict(double[][] input) {
        MyWeightsMatrixAll.setWeightMatrices();

        return predict(input, MyWeightsMatrixAll.weightMatrices);
    }

    public static double[][] predict(double[][] input, List<double[][]> weightMatrices) {
        double[][] currMatrix = input;
        double[][] resultMatrix = null;

        for (double[][] weightMatrix : weightMatrices) {
            // System.out.println("Multiplying: m1[" + currMatrix.length + "][" + currMatrix[0].length + "]" +
            //         " and m2[" + weightMatrix.length + "][" + weightMatrix[0].length + "]");
            resultMatrix = multiply(currMatrix, weightMatrix);
            resultMatrix = tanh(resultMatrix);
            currMatrix = resultMatrix;
        }


        return resultMatrix;
    }

    public static double[][] observationToInput(ProtoMessage.ObservationData observationData) {
        // The size of the first layer is 757, presumably because it has an input for each possible value
        // For instance, the first value may be 0...7, therefore there are 8 inputs. The input with value 1
        // corresponds to the observed value. Or so I believe.

        int NUMBER_OF_PLAYERS = 7;

        int sizeOfInput = 75*(NUMBER_OF_PLAYERS + 1) + 75*2 + NUMBER_OF_PLAYERS;
        // NUMBER_OF_PLAYERS + 1, where 1 is the None (no one owns the province)

        double [][] input = new double[1][sizeOfInput];

        for(ProtoMessage.ProvinceData provinceData: observationData.getProvincesList()) {
            // If everything is ordered, each province is characterized by 10 inputs
            // 8 of those inputs are related to the owner
            // 2 are related to the SC attribute
            int indexOfOwnerObs = (provinceData.getId() - 1) * (NUMBER_OF_PLAYERS + 1 + 2) + provinceData.getOwner();
            input[0][indexOfOwnerObs] = 1;

            int indexOfSCObs = (provinceData.getId() - 1) * (NUMBER_OF_PLAYERS + 1 + 2) + (NUMBER_OF_PLAYERS + 1) + provinceData.getSc();
            input[0][indexOfSCObs] = 1;
        }

        int indexOfPlayerObs = sizeOfInput - 1 - NUMBER_OF_PLAYERS + observationData.getPlayer();
        input[0][indexOfPlayerObs] = 1;

        return input;
    }

    public static ProtoMessage.DealData outputToDealData(double[][] output) {
        // Output should follow the same logic as input

        int MAXIMUM_NUMBER_OF_SC = 18;
        int NUMBER_OF_PHASES_AHEAD = 20;
        int NUMBER_OF_OPPONENTS = 6;

        // I'll assume that the node with higher value shall be the one to be executed.

        int currentIndex = 0;

        boolean defendUnitExecute = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, 2) == 1;
        currentIndex += 2;
        int defendUnitRegion = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, MAXIMUM_NUMBER_OF_SC);
        currentIndex += MAXIMUM_NUMBER_OF_SC;

        boolean defendSCExecute = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, 2) == 1;
        currentIndex +=2;
        int defendSCPower = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, NUMBER_OF_OPPONENTS);
        currentIndex +=NUMBER_OF_OPPONENTS;

        boolean attackRegionExecute = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex,2) == 1;
        currentIndex +=2;
        int attackRegionRegion = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, MAXIMUM_NUMBER_OF_SC);
        currentIndex += MAXIMUM_NUMBER_OF_SC;

        boolean supportAttackRegionExecute = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, 2) == 1;
        currentIndex += 2;
        int supportAttackRegionRegion = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, MAXIMUM_NUMBER_OF_SC);
        currentIndex += MAXIMUM_NUMBER_OF_SC;

        int numOfPhasesAhead = getRelativeIndexOfHigherValueBetweenBounds(output[0], currentIndex, NUMBER_OF_PHASES_AHEAD);
        currentIndex += NUMBER_OF_PHASES_AHEAD;

        ProtoMessage.DealData.Builder dealDataBuilder = ProtoMessage.DealData.newBuilder();

        ProtoMessage.DealData.DefendUnitData.Builder defendUnitDataBuilder = ProtoMessage.DealData.DefendUnitData.newBuilder();
        defendUnitDataBuilder.setExecute(defendUnitExecute);
        defendUnitDataBuilder.setRegion(defendUnitRegion);

        ProtoMessage.DealData.DefendSCData.Builder defendSCDataBuilder = ProtoMessage.DealData.DefendSCData.newBuilder();
        defendSCDataBuilder.setExecute(defendSCExecute);
        defendSCDataBuilder.setAllyPower(defendSCPower);

        ProtoMessage.DealData.AttackRegionData.Builder attackRegionDataBuilder = ProtoMessage.DealData.AttackRegionData.newBuilder();
        attackRegionDataBuilder.setExecute(attackRegionExecute);
        attackRegionDataBuilder.setRegion(attackRegionRegion);

        ProtoMessage.DealData.SupportAttackRegionData.Builder supportAttackRegionDataBuilder = ProtoMessage.DealData.SupportAttackRegionData.newBuilder();
        supportAttackRegionDataBuilder.setExecute(supportAttackRegionExecute);
        supportAttackRegionDataBuilder.setRegion(supportAttackRegionRegion);

        dealDataBuilder.setDefendUnit(defendUnitDataBuilder.build());
        dealDataBuilder.setDefendSC(defendSCDataBuilder.build());
        dealDataBuilder.setAttackRegion(attackRegionDataBuilder.build());
        dealDataBuilder.setSupportAttackRegion(supportAttackRegionDataBuilder.build());
        dealDataBuilder.setPhasesFromNow(numOfPhasesAhead);

        return dealDataBuilder.build();
    }

    public static int getRelativeIndexOfHigherValueBetweenBounds(double[] arr, int minIndex, int delta) {
        int maxRelativeIndex = -1;
        double maxValue = -1000000;

        for(int i = 0; i < delta; i++) {
            if(arr[minIndex + i] > maxValue) {
                maxRelativeIndex = i;
                maxValue = arr[minIndex + i];
            }
        }

        return maxRelativeIndex;
    }

    /**
     * Element wise multiplication
     *
     * @param m1 matrix
     * @param m2 matrix
     * @return y = a * x
     */


    public static double[][] multiply(double[][] m1, double[][] m2) {
        int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if(m1ColLength != m2RowLength) {
            throw new RuntimeException("Illegal matrix dimensions.\n" +
                    "m1[" + m1.length + "][" + m1[0].length + "]\n" +
                    "m2[" + m2.length + "][" + m2[0].length + "]\n");
        }
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        double[][] mResult = new double[mRRowLength][mRColLength];
        for(int i = 0; i < mRRowLength; i++) {         // rows from m1
            for(int j = 0; j < mRColLength; j++) {     // columns from m2
                for(int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
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

    public static double[][] tanh(double[][] m) {
        double[][] mout = new double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                mout[i][j] = Math.tanh(m[i][j]);
            }
        }

        return mout;
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
