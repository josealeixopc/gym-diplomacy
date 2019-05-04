package cruz.agents;

import ddejonge.bandana.tools.Logger;
import ddejonge.bandana.tournament.GameResult;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * The class that makes the connection between the Open AI environment and the BANDANA player.
 */
public abstract class OpenAIAdapter {

    /** A map containing an integer ID of each power, in order to be able to map a power to an integer and vice-versa. */
    protected Map<String, Integer> powerNameToInt;

    /** Number of supply centers controlled in the previous negotiation stage */
    protected int previousNumSc;

    /** Whether an action that the OpenAI env returned is valid or not.*/
    protected boolean validAction;

    /** The value of the reward achieved because of the previous actions. */
    private float previousActionReward;

    /** A boolean determining whether the current Diplomacy game has ended or not. */
    private boolean done;

    /** An arbitrary string that may contain information for debug, that can be sent to the OpenAI environment. */
    protected String info;

    /** An Observer instance that allows us to know the current game state. It is used to know when the games has ended. */
    public OpenAIObserver openAIObserver;

    OpenAIAdapter() {
        this.init();
    }

    private void init(){
        this.done = false;
        this.info = null;
        this.validAction = true;
    }

    /**
     * Creates the OpenAIObserver instance which will connect to the Parlance server.
     *
     * The path for the logging is given because the Observer class needs one, but it is not essential.
     *
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
     * Executes on the beginning of a game.
     */
    void beginningOfGame() {
        this.done = false;

        // The observer needs to be created and destroyed every game, because it does not know when the tournament ends
        // and will be left hanging.
        this.createObserver();
        this.previousNumSc = this.getPower().getOwnedSCs().size();
    }

    /**
     * Executes on the end of a game. Takes the game result as an argument to allow post-processing.
     * @param gameResult The GameResult object containing information about the score at the end of the game.
     */
    void endOfGame(GameResult gameResult) {

        try {
            this.done = true;
            this.sendEndOfGameNotification();

            // Terminate observer so it does not hang and cause exceptions.
            this.openAIObserver.exit();
        }
        catch (Exception e) {
            // do nothing
        }
    }

    protected void generatePowerNameToIntMap() {
        this.powerNameToInt = new HashMap<>();
        this.powerNameToInt.put("NONE", 0);
        this.powerNameToInt.put(this.getPower().getName(), 1); // make sure WE are always power number 1

        int id = 2;
        String agent_name = this.getPower().getName();
        this.powerNameToInt.put(agent_name, 1);

        List<Power> powers = this.getGame().getPowers();
        for(Power pow : powers) {
            if(!pow.getName().equals(this.getPower().getName())) {
                powerNameToInt.put(pow.getName(), id);
                id++;
            }
        }
    }

    protected ProtoMessage.ObservationData generateObservationData() {

        ProtoMessage.ObservationData.Builder observationDataBuilder = ProtoMessage.ObservationData.newBuilder();

        Map<String, ProtoMessage.ProvinceData.Builder> nameToProvinceDataBuilder = new HashMap<>();

        // FIRST PROCESS ALL PROVINCES
        Vector<Province> provinces = this.getGame().getProvinces();

        int id = 1;

        for (Province p : provinces) {
            ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder();
            int isSc = p.isSC() ? 1 : 0;

            provinceDataBuilder.setId(id);
            provinceDataBuilder.setSc(isSc);

            nameToProvinceDataBuilder.put(p.getName(), provinceDataBuilder);

            id++;
        }

        // THEN ADD THE OWNERS & UNITS OF EACH PROVINCE
        List<Power> powers = this.getGame().getPowers();
        for (Power pow : powers) {
            for (Province p : pow.getOwnedSCs()) {
                // Get the correspondent province builder and add the current owner of the province
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = nameToProvinceDataBuilder.get(p.getName());
                provinceDataBuilder.setOwner(powerNameToInt.get(pow.getName()));
            }

            for (Region r : pow.getControlledRegions()) {
                Province p = r.getProvince();

                // Get the correspondent province builder and add the current owner of the province
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = nameToProvinceDataBuilder.get(p.getName());
                provinceDataBuilder.setOwner(powerNameToInt.get(pow.getName()));
                provinceDataBuilder.setUnit(powerNameToInt.get(pow.getName()));
            }
        }

        // ADD CREATED PROVINCES TO OBSERVATION
        for (Map.Entry<String, ProtoMessage.ProvinceData.Builder> entry : nameToProvinceDataBuilder.entrySet()) {
            observationDataBuilder.addProvinces(entry.getValue().build());
        }

        // CALCULATE REWARD
        this.previousActionReward = this.calculateReward();

        observationDataBuilder.setPreviousActionReward(this.previousActionReward);
        observationDataBuilder.setDone(this.done);

        if (this.info != null) {
            observationDataBuilder.setInfo(this.info);
        }

        String agent_name = this.getPower().getName();
        observationDataBuilder.setPlayer(powerNameToInt.get(agent_name));

        return observationDataBuilder.build();
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

            SocketClient socketClient = new SocketClient(5000);
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

    protected abstract float calculateReward();

    public void setInfo(String s) {
        this.info = s;
    }

    /**
     * This method should be implemented in order to access the power of the player that is using the adapter.
     * @return The Power object, representing the Power with which the player is playing.
     */
    protected abstract Power getPower();

    /**
     * This method should be implemented in order to access the information of the current game.
     * @return The Game object, representing the current game.
     */
    protected abstract Game getGame();
}
