package cruz.agents;

import org.tensorflow.*;

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
        final int OBS_SIZE = 4;
        final int ACT_SIZE = 1;

        String modelDir = "/home/jazz/Projects/FEUP/dip-q/agents/dip-q-brain/dip_q_brain/models/model.ckpt";
        String modelName = "saved_model.pb";
        String modelFullPath = modelDir + File.separator + modelName;

        Path modelPath;
        byte[] graphDef = null;

        try {
            modelPath = Paths.get(modelFullPath);
            graphDef = Files.readAllBytes(modelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SavedModelBundle b = SavedModelBundle.load(modelDir, "serve");

        try(Graph graph = b.graph()) {
            // assert graphDef != null;
            // graph.importGraphDef(graphDef);

            //Just print needed operations for debug
            System.out.println(graph.operation("input/Ob").output(0));
            // System.out.println(graph.operation("output/strided_slice\"").output(0));

            try (Session sess = b.session()) {

                float thetaThresholdRadians = 12f * 2f * 3.14f / 360f;
                float xThreshold = 2.4f;

                float[][] inputData = {{1.8095541f,  2.2624320f,  3.5591793f, 1.3286867f}};
                Tensor input = Tensor.create(inputData, Float.class);


                float[] output = predict(sess, input);
                System.out.println("Output: " + Arrays.toString(output));
            }
        }

        b.close();
    }

    private void printGraphOperations(Graph graph) {
        Iterator iter = graph.operations();

        while(iter.hasNext()) {
            Object op = iter.next();
            System.out.println("Operation: " + op);
        }
    }

    private static float[] predict(Session sess, Tensor inputTensor) {
        Tensor result = sess.runner()
                .feed("input/Ob", inputTensor)
                .fetch("output/strided_slice").run().get(0);
        float[] outputBuffer = new float[1];
        result.copyTo(outputBuffer);
        return outputBuffer;
    }
}
