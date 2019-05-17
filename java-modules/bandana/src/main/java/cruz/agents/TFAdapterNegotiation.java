package cruz.agents;

import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TFAdapterNegotiation {

    private static float[][] predict(Session sess, Tensor inputTensor) {
        Tensor result = sess.runner()
                .feed("input", inputTensor)
                .fetch("not_activated_output").run().get(0);
        float[][] outputBuffer = new float[1][3];
        result.copyTo(outputBuffer);
        return outputBuffer;
    }
}
