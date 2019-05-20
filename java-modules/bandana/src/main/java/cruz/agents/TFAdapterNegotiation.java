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
        String modelDir = "/home/jazz/Projects/FEUP/dip-q/agents/dip-q-brain/dip_q_brain/checkpoint";
        SavedModelBundle b = SavedModelBundle.load(modelDir, "serve");

        try(Graph g = b.graph()) {

            try (Session sess = b.session()) {

                float[][] inputData = {{1.8095541f,  2.2624320f,  3.5591793f, 1.3286867f}};
                Tensor input = Tensor.create(inputData, Float.class);


                float[] output = predict(sess, input);
                System.out.println("Output: " + Arrays.toString(output));
            }
        }

        b.close();
    }

    public static float[] predict(Session sess, Tensor inputTensor) {
        Tensor result = sess.runner()
                .feed("input/Ob", inputTensor)
                .fetch("output/strided_slice").run().get(0);
        float[] outputBuffer = new float[1];
        result.copyTo(outputBuffer);
        return outputBuffer;
    }
}
