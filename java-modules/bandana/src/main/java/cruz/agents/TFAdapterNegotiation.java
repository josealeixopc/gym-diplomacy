package cruz.agents;

import org.tensorflow.*;
import org.tensorflow.op.core.TensorForestTreeDeserialize;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TFAdapterNegotiation {

    public static void main(String[] args) {
        // good idea to print the version number, 1.2.0 as of this writing
        System.out.println(TensorFlow.version());
        String modelDir = "/home/jazz/Projects/FEUP/dip-q/agents/dip-q-brain/dip_q_brain/checkpoint";
        SavedModelBundle b = SavedModelBundle.load(modelDir, "serve");

        try(Graph g = b.graph()) {

            try (Session sess = b.session()) {

                int[][] inputData = new int[1][151];

                for (int i = 0; i < inputData.length; i++) {
                    for (int j = 0; j < inputData[i].length; j++) {
                        inputData[i][j] = 1;
                    }
                }

                Tensor input = Tensor.create(inputData, Integer.class);

                int[][] output = predict(sess, input);
                int[] action = output[0];

                System.out.println("Action to take: " + Arrays.toString(action));

            }
        }

        b.close();
    }

    public static int[][] predict(Session sess, Tensor inputTensor) {
        Tensor result = sess.runner()
                .feed("input/Ob", inputTensor)
                .fetch("output/Cast_1:0").run().get(0);
        int[][] outputBuffer = new int[1][9];
        result.copyTo(outputBuffer);
        return outputBuffer;
    }

    public static float[][] generateInputData(ProtoMessage.ObservationData observationData) {
        // int inputSize = observationData.getProvincesCount() ...;
        return null;
    }
}
