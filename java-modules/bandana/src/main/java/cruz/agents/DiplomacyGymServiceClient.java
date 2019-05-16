package cruz.agents;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiplomacyGymServiceClient {

    private static final Logger logger = Logger.getLogger(DiplomacyGymServiceClient.class.getName());


    private final ManagedChannel channel;
    private final DiplomacyGymServiceGrpc.DiplomacyGymServiceBlockingStub blockingStub;
    private final DiplomacyGymServiceGrpc.DiplomacyGymServiceStub asyncStub;

    private Random random = new Random();
    private TestHelper testHelper;

    /** Construct client for accessing DiplomacyGymService server at {@code host:port}. */
    public DiplomacyGymServiceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public DiplomacyGymServiceClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = DiplomacyGymServiceGrpc.newBlockingStub(channel);
        asyncStub = DiplomacyGymServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Blocking unary call example. Calls getAction and prints the response.
     */

    public ProtoMessage.DiplomacyGymResponse getAction(ProtoMessage.BandanaRequest request) {
        ProtoMessage.DiplomacyGymResponse response = null;

        try {
            response = blockingStub.getAction(request);
            if(testHelper != null) {
                testHelper.onMessage(response);
            }
        } catch (StatusRuntimeException e) {
            this.logWarning("RPC failed: {0}", e.getStatus());
            if (testHelper != null) {
                testHelper.onRpcError(e);
            }
        }

        return response;
    }

    public ProtoMessage.DiplomacyGymOrdersResponse getTacticAction(ProtoMessage.BandanaRequest request) {
        throw new RuntimeException("This method has not been implemented.");
    }

    /**
     * Only used for unit test, as we do not want to introduce randomness in unit test.
     */
    @VisibleForTesting
    void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Only used for helping unit test.
     */
    @VisibleForTesting
    interface TestHelper {
        /**
         * Used for verify/inspect message received from server.
         */
        void onMessage(Message message);

        /**
         * Used for verify/inspect error received from server.
         */
        void onRpcError(Throwable exception);
    }

    @VisibleForTesting
    void setTestHelper(TestHelper testHelper) {
        this.testHelper = testHelper;
    }

    private void logWarning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }

}
