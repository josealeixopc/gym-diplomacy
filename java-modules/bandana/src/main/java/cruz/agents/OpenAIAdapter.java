package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Logger;
import ddejonge.bandana.tournament.GameResult;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.*;

import java.io.File;
import java.util.*;

/**
 * The class that makes the connection between the Open AI environment and the BANDANA player.
 */
public class OpenAIAdapter {

    /**
     * Reward given for each deal rejected by other players
     */
    public static final int REJECTED_DEAL_REWARD = -5;

    /**
     * Reward given for each deal accepted by other players.
     */
    public static final int ACCEPTED_DEAL_REWARD = +5;

    /**
     * Reward given for winning the game
     */

    public static final int WON_GAME_REWARD = +100;

    /**
     * Reward given for losing the game
     */

    public static final int LOST_GAME_REWARD = -100;

    /**
     * Reward given for generating an invalid deal
     */
    public static final int INVALID_DEAL_REWARD = -10;


    /**
     * The OpenAINegotiator instance to which this adapter is connected.
     */
    public OpenAINegotiator agent;

    /**
     * A map containing an integer ID of each power, in order to be able to map a power to an integer and vice-versa.
     */
    private Map<String, Integer> powerNameToInt;

    /**
     * The value of the reward achieved because of the previous actions.
     */
    private float previousActionReward;

    /**
     * A boolean determining whether the current Diplomacy game has ended or not.
     */
    public boolean done;

    /**
     * An arbitrary string that may contain information for debug, that can be sent to the OpenAI environment.
     */
    private String info;

    /**
     * An Observer instance that allows us to know the current game state. It is used to know when the games has ended.
     */
    public OpenAIObserver openAIObserver;

    /**
     * @param agent The OpenAINegotiator instance that will receive actions from the OpenAI environment.
     */
    OpenAIAdapter(OpenAINegotiator agent) {
        this.agent = agent;

        this.resetReward();
        this.done = false;
        this.info = null;
    }

    /**
     * Creates the OpenAIObserver instance which will connect to the Parlance server.
     * <p>
     * The path for the logging is given because the Observer class needs one, but it is not essential.
     * <p>
     * TODO (low-prio): Figure out how to create an Observer without needing a logging path.
     */
    private void createObserver() {
        String openAIObserverPath = "log" + File.separator + "OpenAIObserver" + Logger.getDateString();
        File logFile = new File(openAIObserverPath);
        logFile.mkdirs();

        this.openAIObserver = new OpenAIObserver(openAIObserverPath, this);
        this.openAIObserver.connectToServer();
    }

    /**
     * Retrieves a deal from the Open AI environment that is connected to the localhost on port 5000.
     *
     * @return A BasicDeal created with data from the Open AI module.
     */
    public BasicDeal getDealFromDipQ() {
        try {
            // Make sure the power to int map is updated with the current Powers in the game
            this.generatePowerNameToIntMap();

            ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

            ProtoMessage.ObservationData observationData = this.generateObservationData();

            bandanaRequestBuilder.setObservation(observationData);

            bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.GET_DEAL_REQUEST);

            byte[] message = bandanaRequestBuilder.build().toByteArray();

            SocketClient socketClient = new SocketClient("127.0.1.1", 5000, this.agent.getLogger());
            byte[] response = socketClient.sendMessageAndReceiveResponse(message);

            // If something went wrong with getting the response from Python module
            if (response == null) {
                return null;
            }

            ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymResponse.parseFrom(response);
            BasicDeal generatedDeal = this.generateDeal(diplomacyGymResponse.getDeal());

            // If deal is invalid, give negative reward. If an invalid deal is returned, the game will deal with it, so
            // we can still return it.
            if(!(this.isDealValid(generatedDeal))) {
                this.addReward(INVALID_DEAL_REWARD);
            }

            return generatedDeal;


        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Sends a message to the Open AI environment notifying the end of the game. The "done" boolean will be set to true,
     * and a response with "CONFIRM" is expected.
     */
    public void sendEndOfGameNotification() {
        try {
            ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();
            bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.SEND_GAME_END);

            ProtoMessage.ObservationData observationData = this.generateObservationData();
            bandanaRequestBuilder.setObservation(observationData);

            byte[] message = bandanaRequestBuilder.build().toByteArray();

            SocketClient socketClient = new SocketClient("127.0.1.1", 5000, this.agent.getLogger());
            byte[] response = socketClient.sendMessageAndReceiveResponse(message);

            if (response == null) {
                return;
            }

            ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymResponse.parseFrom(response);

            if (diplomacyGymResponse.getType() != ProtoMessage.DiplomacyGymResponse.Type.CONFIRM) {
                throw new Exception("The response from DiplomacyGym to the end of game notification is not 'CONFIRM'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes on the beginning of a game.
     */
    void beginningOfGame() {
        this.done = false;

        // The observer needs to be created and destroyed every game, because it does not know when the tournament ends
        // and will be left hanging.
        this.createObserver();
    }

    /**
     * Executes on the end of a game.
     */
    void endOfGame(GameResult gameResult) {

        // Yes, weird work around, but for some reason it works
        String nameOfPlayer = "'OpenAINegotiator'";

        String nameOfWinner = gameResult.getSoloWinner();

        if(nameOfWinner == null) {
            System.out.println("GAME RESULT: No one won with a solo victory.");
        }
        else {
            System.out.printf("GAME RESULT: Player " + nameOfWinner + " win with a solo victory.");
        }

        if (nameOfPlayer.equals(nameOfWinner)) // winner
        {
            this.wonGame();
        } else {
            this.lostGame();
        }

        this.done = true;
        this.sendEndOfGameNotification();

        // Terminate observer so it does not hang and cause exceptions.
        this.openAIObserver.exit();
    }

    /**
     * Executes when a deal is accepted.
     */
    void acceptedDeal() {
        this.addReward(ACCEPTED_DEAL_REWARD);
    }

    /**
     * Executes when a deal is rejected.
     */
    void rejectedDeal() {
        this.addReward(REJECTED_DEAL_REWARD);
    }

    void wonGame() {
        this.addReward(WON_GAME_REWARD);
    }

    void lostGame() {
        this.addReward(LOST_GAME_REWARD);
    }

    private void generatePowerNameToIntMap() {
        this.powerNameToInt = new HashMap<>();
        this.powerNameToInt.put("NONE", 0);

        int id = 1;

        for (Power pow : this.agent.game.getPowers()) {
            powerNameToInt.put(pow.getName(), id);
            id++;
        }
    }

    private ProtoMessage.ObservationData generateObservationData() {

        ProtoMessage.ObservationData.Builder observationDataBuilder = ProtoMessage.ObservationData.newBuilder();

        Map<String, ProtoMessage.ProvinceData.Builder> nameToProvinceDataBuilder = new HashMap<>();

        int id = 1;

        // FIRST PROCESS ALL PROVINCES
        for (Province p : this.agent.game.getProvinces()) {
            ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder();
            int isSc = p.isSC() ? 1 : 0;

            provinceDataBuilder.setId(id);
            provinceDataBuilder.setSc(isSc);

            nameToProvinceDataBuilder.put(p.getName(), provinceDataBuilder);

            id++;
        }

        // THEN ADD THE OWNERS OF EACH PROVINCE
        for (Power pow : this.agent.game.getPowers()) {
            for (Region r : pow.getControlledRegions()) {
                Province p = r.getProvince();

                // Get the correspondent province builder and add the current owner of the province
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = nameToProvinceDataBuilder.get(p.getName());
                provinceDataBuilder.setOwner(powerNameToInt.get(pow.getName()));
            }
        }

        // ADD CREATED PROVINCES TO OBSERVATION
        for (Map.Entry<String, ProtoMessage.ProvinceData.Builder> entry : nameToProvinceDataBuilder.entrySet()) {
            observationDataBuilder.addProvinces(entry.getValue().build());
        }

        observationDataBuilder.setPreviousActionReward(this.previousActionReward);
        observationDataBuilder.setDone(this.done);

        if (this.info != null) {
            observationDataBuilder.setInfo(this.info);
        }

        // Reset previous reward
        this.resetReward();

        return observationDataBuilder.build();
    }

    private BasicDeal generateDeal(ProtoMessage.DealData dealData) {
        List<DMZ> dmzs = new ArrayList<>();
        List<OrderCommitment> ocs = new ArrayList<>();


        // Add MY order commitment
        Province ourStartProvince = this.agent.game.getProvinces().get(dealData.getOurMove().getStartProvince());
        Province ourDestinationProvince = this.agent.game.getProvinces().get(dealData.getOurMove().getDestinationProvince());

        Order ourOrder = new MTOOrder(
                this.agent.me,
                ourStartProvince.getRegions().get(0),
                ourDestinationProvince.getRegions().get(0));

        OrderCommitment ourOC = new OrderCommitment(this.agent.game.getYear(), this.agent.game.getPhase(), ourOrder);

        ocs.add(ourOC);

        // Add THEIR order commitment
        Province theirStartProvince = this.agent.game.getProvinces().get(dealData.getTheirMove().getStartProvince());
        Province theirDestionationProvince = this.agent.game.getProvinces().get(dealData.getTheirMove().getDestinationProvince());

        String nameOfPowerToProposeTo = null;

        for (Map.Entry<String, Integer> entry : powerNameToInt.entrySet()) {
            if (entry.getValue() == dealData.getPowerToPropose()) {
                nameOfPowerToProposeTo = entry.getKey();
            }
        }

        assert nameOfPowerToProposeTo != null;

        Order theirOrder = new MTOOrder(
                this.agent.game.getPower(nameOfPowerToProposeTo),
                theirStartProvince.getRegions().get(0),
                theirDestionationProvince.getRegions().get(0)
        );

        OrderCommitment theirOC = new OrderCommitment(this.agent.game.getYear(), this.agent.game.getPhase(), theirOrder);

        ocs.add(theirOC);

        return new BasicDeal(ocs, dmzs);
    }

    /**
     * Checks if a deal is valid. It checks if it is consistent with the current deals in place and if it is well
     * structured.
     *
     * @param deal The deal to analyze.
     * @return True if the deal is valid. False otherwise.
     */
    private boolean isDealValid(BasicDeal deal) {
        boolean isDealConsistent = true;
        boolean isDealWellStructured;

        if (ddejonge.bandana.tools.Utilities.testValidity(this.agent.game, deal) == null) {
            isDealConsistent = false;
        }

        isDealWellStructured = isDealWellStructured(deal);

        boolean valid = isDealConsistent && isDealWellStructured;

        return valid;
    }

    /**
     * This function checks whether a deal is well structured or not. This verification is made by the 'proposeDeal' method,
     * however we cannot access it so we rewrite it and use it.
     *
     * @param deal The deal to analyze.
     * @return True if the deal is well structured. False otherwise.
     */
    private boolean isDealWellStructured(BasicDeal deal) {

        boolean wellStructured = true;

        boolean containsOtherPower = false;
        Iterator it = deal.getDemilitarizedZones().iterator();

        while (it.hasNext()) {
            DMZ commitment = (DMZ) it.next();
            if (commitment.getPowers().size() > 1) {
                containsOtherPower = true;
                break;
            }

            if (!((Power) commitment.getPowers().get(0)).getName().equals(this.agent.me.getName())) {
                containsOtherPower = true;
                break;
            }
        }

        it = deal.getOrderCommitments().iterator();

        while (it.hasNext()) {
            OrderCommitment commitment = (OrderCommitment) it.next();
            if (!commitment.getOrder().getPower().getName().equals(this.agent.me.getName())) {
                containsOtherPower = true;
            }

            if (!(commitment.getOrder() instanceof HLDOrder) && !(commitment.getOrder() instanceof MTOOrder) && !(commitment.getOrder() instanceof SUPOrder) && !(commitment.getOrder() instanceof SUPMTOOrder)) {
                wellStructured = false;
            }
        }

        if (!containsOtherPower) {
            wellStructured = false;
        }

        return wellStructured;
    }

    private void addReward(int reward) {
        this.previousActionReward += reward;
    }

    private void resetReward() {
        this.previousActionReward = 0;
    }

    public void setInfo(String s) {
        this.info = s;
    }
}
