package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tournament.GameResult;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Phase;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.orders.*;
import jdk.internal.vm.compiler.collections.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The class that makes the connection between the Open AI environment and the BANDANA player.
 */
public class OpenAIAdapterNegotiation extends OpenAIAdapter {

    // REWARD FIELDS BEGIN

    /**  Reward given for each deal accepted by other players. Rejected deals receive negative reward. */
    private static final int ACCEPTED_DEAL_REWARD = +10;

    /**  Reward given for winning the game. Games lost received nehative reward. */
    public static final int WON_GAME_REWARD = +100;

    /** Reward given for capturing a Supply Center (SC). Losing a SC gives a negative reward with the same value. */
    private static final int CAPTURED_SC_REWARD = +100;

    boolean dealWasAccepted = false;

    // REWARD FIELDS END

    /** The agent instance attached to this adapter. */
    private OpenAINegotiator agent;

    OpenAIAdapterNegotiation(OpenAINegotiator agent) {
        this.agent = agent;
    }

    /**
     * Retrieves a deal from the Open AI environment that is connected to the localhost on port 5000.
     *
     * @return A BasicDeal created with data from the Open AI module.
     */
    public BasicDeal getDealFromDipQ() {
        try {
            this.calculateReward();

            byte[] message = generateRequestMessage();

            byte[] response = this.socketClient.sendMessageAndReceiveResponse(message);

            // If something went wrong with getting the response from Python module
            if (response == null) {
                return null;
            }

            ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymResponse.parseFrom(response);
            BasicDeal generatedDeal = this.generateDeal(diplomacyGymResponse.getDeal());

            // If deal is invalid, give negative reward. If an invalid deal is returned, the game will deal with it, so
            // we can still return it.
            if(this.isDealValid(generatedDeal)) {
                this.validAction = true;
            }
            else {
                this.validAction = false;
            }

            return generatedDeal;


        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }


    private byte[] generateRequestMessage() {
        // Make sure the power to int map is updated with the current Powers in the game
        this.generatePowerNameToIntMap();

        ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

        ProtoMessage.ObservationData observationData = this.generateObservationData();

        bandanaRequestBuilder.setObservation(observationData);
        bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.GET_DEAL_REQUEST);

        return bandanaRequestBuilder.build().toByteArray();
    }

    private BasicDeal generateDeal(ProtoMessage.DealData dealData) {
        List<DMZ> dmzs = new ArrayList<>();
        List<OrderCommitment> ocs = new ArrayList<>();


        // Add MY order commitment
        Province ourStartProvince = this.agent.game.getProvinces().get(dealData.getOurMove().getStartProvince());
        Province ourDestinationProvince = this.agent.game.getProvinces().get(dealData.getOurMove().getDestinationProvince());
        Pair<Integer, Phase> yearAndPhaseOfDeal = Utilities.calculatePhaseAndYear(this.agent.game.getYear(), this.agent.game.getPhase(), dealData.getPhasesFromNow());

        Phase phaseOfDeal = yearAndPhaseOfDeal.getRight();
        int yearOfDeal = yearAndPhaseOfDeal.getLeft();

        Order ourOrder = new MTOOrder(
                this.agent.me,
                ourStartProvince.getRegions().get(0),
                ourDestinationProvince.getRegions().get(0));

        OrderCommitment ourOC = new OrderCommitment(yearOfDeal, phaseOfDeal, ourOrder);

        ocs.add(ourOC);

        // Add THEIR order commitment
        Province theirStartProvince = this.agent.game.getProvinces().get(dealData.getTheirMove().getStartProvince());
        Province theirDestinationProvince = this.agent.game.getProvinces().get(dealData.getTheirMove().getDestinationProvince());

        String nameOfPowerToProposeTo = null;

        /* Because we do not want to choose ourselves and we are index number 1 and NONE is 0, just add 2 to the index that comes
         * from Gym. This is why we use NUMBER_OF_OPPONENTS instead of NUMBER_OF_PLAYERS in the environment. */
        int trueOpponentPowerIndex = dealData.getPowerToPropose() + 2;

        for (Map.Entry<String, Integer> entry : powerNameToInt.entrySet()) {
            if (entry.getValue() == trueOpponentPowerIndex) {
                nameOfPowerToProposeTo = entry.getKey();
            }
        }

        assert nameOfPowerToProposeTo != null;

        Order theirOrder = new MTOOrder(
                this.agent.game.getPower(nameOfPowerToProposeTo),
                theirStartProvince.getRegions().get(0),
                theirDestinationProvince.getRegions().get(0)
        );

        OrderCommitment theirOC = new OrderCommitment(yearOfDeal, phaseOfDeal, theirOrder);

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

    /**
     * This function takes the number of supply centers (SCs) controlled in the previous observation (negotiation phase)
     * and returns the balance of SCs. A negative number means SCs were lost. A positive number means SCs were captured.
     * @return
     */
    private int balanceOfScs() {
        int currentNumSc = this.getPower().getOwnedSCs().size();
        int balance = currentNumSc - this.previousNumSc;

        return balance;
    }


    @Override
    protected float calculateReward() {
        float reward = 0;
        reward += this.balanceOfScs() * CAPTURED_SC_REWARD;
        reward += (this.dealWasAccepted ? 1 : -1) * ACCEPTED_DEAL_REWARD;

        System.out.println("Balance of SCs: " + this.balanceOfScs());
        System.out.println("Deal was accepted? " + this.dealWasAccepted);

        this.resetRewardValues();

        return reward;
    }

    protected void resetRewardValues() {
        // Reset deal was accepted back to false for the next observation
        this.dealWasAccepted = false;

        // Set new number of SCs
        this.previousNumSc = this.getPower().getOwnedSCs().size();
    }

    @Override
    protected Power getPower() {
        return this.agent.me;
    }

    @Override
    protected Game getGame() {
        return this.agent.game;
    }

    @Override
    void endOfGame(GameResult gameResult) {
        // Yes, weird work around, but for some reason it works
        String nameOfPlayer = "'OpenAINegotiator'";

        String nameOfWinner = gameResult.getSoloWinner();

        // if(nameOfWinner == null) {
        //     System.out.println("GAME RESULT: No one won with a solo victory.");
        // }
        // else {
        //     System.out.printf("GAME RESULT: Player " + nameOfWinner + " win with a solo victory.");
        // }

        // if (nameOfPlayer.equals(nameOfWinner)) // winner
        // {
        //     this.wonGame();
        // } else {
        //     this.lostGame();
        // }

        super.endOfGame(gameResult);
    }
}
