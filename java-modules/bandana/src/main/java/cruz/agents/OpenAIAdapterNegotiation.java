package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tournament.GameResult;
import es.csic.iiia.fabregues.dip.board.*;
import es.csic.iiia.fabregues.dip.orders.*;

import java.util.*;

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

    int dealsAccepted = 0;

    // REWARD FIELDS END

    /** The agent instance attached to this adapter. */
    private OpenAINegotiator agent;

    OpenAIAdapterNegotiation(OpenAINegotiator agent) {
        this.agent = agent;
    }

    /**
     * Creates the the mapping of a power's name to it's respective ID, which will be used in the OpenAI agent.
     *
     * In this override, the powers are organized alphabetically.
     * This way, the ID of a power will be the SAME throughout every standard game.
     * For instance, 1 will always correspond to "AUS" in a standard game.
     *
     * In this case in particular, OUR power does not correspond to 1. As it does in the parent method. This is because
     * the ID of our power will be provided as part of the observation.
     */
    @Override
    protected void generatePowerNameToIntMap() {
        this.powerNameToInt = new HashMap<>();
        this.powerNameToInt.put("NONE", 0);

        // Order alphabetically in ordered to maintain consistent order in case they are not always ordered the same way
        List<Power> powers = this.getGame().getPowers();
        powers.sort(new Comparator<Power>() {
            @Override
            public int compare(Power p1, Power p2) {
                return p1.getName().compareToIgnoreCase(p2.getName());
            }
        });

        int id = 1;
        for(Power pow : powers) {
            powerNameToInt.put(pow.getName(), id);
            id++;
        }
    }

    @Override
    protected ProtoMessage.ObservationData generateObservationData() {
        return super.generateObservationData();
    }

    /**
     * Retrieves a deal from the Open AI environment that is connected to the localhost on port 5000.
     *
     * @return The Deal data in ProtoMessage format created with data from the Open AI module.
     */
    public ProtoMessage.DealData getDealsDataFromDipBrain() {
        ProtoMessage.BandanaRequest message = generateRequestMessage();

        ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = this.serviceClient.getAction(message);

        // If something went wrong with getting the response from Python module
        if (diplomacyGymResponse == null) {
            return null;
        }

        return diplomacyGymResponse.getDeal();
    }


    private ProtoMessage.BandanaRequest generateRequestMessage() {
        // Make sure the power to int map is updated with the current Powers in the game
        this.generatePowerNameToIntMap();

        ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

        ProtoMessage.ObservationData observationData = this.generateObservationData();

        bandanaRequestBuilder.setObservation(observationData);
        bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.GET_DEAL_REQUEST);

        return bandanaRequestBuilder.build();
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
        reward += dealsAccepted * ACCEPTED_DEAL_REWARD;

        System.out.println("Balance of SCs: " + this.balanceOfScs());
        System.out.println("Deals accepted: " + this.dealsAccepted);

        this.resetRewardValues();

        return reward;
    }

    protected void resetRewardValues() {
        // Reset deal was accepted back to false for the next observation
        this.dealsAccepted = 0;

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
