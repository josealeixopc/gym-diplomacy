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

    /** Ordered list of regions controlled. The default list of controlled regions may not be ordered.
     * It's important for this list to be ordered, so that an action taken
     * in the same state twice leads to the same outcome.*/
    List<Region> orderedControlledRegions;

    /** Ordered list of powers. The default list of powers may not be ordered.
     * It's important for this list to be ordered, so that an action taken
     * in the same state twice leads to the same outcome.*/
    List<Power> orderedNegotiatingPowers;

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
     * @return A BasicDeal created with data from the Open AI module.
     */
    public List<BasicDeal> getDealsFromDipBrain() {
        ProtoMessage.BandanaRequest message = generateRequestMessage();

        ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = this.serviceClient.getAction(message);

        // If something went wrong with getting the response from Python module
        if (diplomacyGymResponse == null) {
            return null;
        }

        return this.generateDeals(diplomacyGymResponse.getDeal());
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
     * According to the data received from DipBrain DRL module, decide what deals should be proposed.
     * @param dealData
     * @return
     */
    private List<BasicDeal> generateDeals(ProtoMessage.DealData dealData) {
        List<BasicDeal> deals = new ArrayList<>();

        // Get current controlled regions and then create an ordered list with them to be use in deal generation
        this.orderedControlledRegions = this.sortRegionList(this.agent.me.getControlledRegions());

        // Get the current negotiating powers and then create an ordered list for deal generation without us in the list
        this.orderedNegotiatingPowers = this.sortPowerList(this.agent.getNegotiatingPowers());
        this.orderedNegotiatingPowers.remove(this.agent.me);

        // Derive year and phase of deal from number of phases ahead
        Map.Entry<Integer, Phase> phaseAndYear = Utilities.calculatePhaseAndYear(this.agent.game.getYear(), this.agent.game.getPhase(), dealData.getPhasesFromNow());

        int year = phaseAndYear.getKey();
        Phase phase = phaseAndYear.getValue();

        // Only if execute is true
        if(dealData.getDefendUnit().getExecute()) {
            int clippedRegionIndex = this.clipRegionIndex(dealData.getDefendUnit().getRegion());
            BasicDeal generatedDeal = generateDefendUnitsMutual(clippedRegionIndex, year, phase);

            if (generatedDeal != null) {
                // if we could find a deal
                deals.add(generatedDeal);
            }
        }

        // Only if execute is true
        if(dealData.getDefendSC().getExecute()) {
            int clippedPowerIndex = this.clipPowerIndex(dealData.getDefendSC().getAllyPower());
            deals.add(generateDefendSupplyCentersMutual(clippedPowerIndex, year, phase));
        }

        // Only if execute is true
        if(dealData.getAttackRegion().getExecute()) {
            int clippedRegionIndex = this.clipRegionIndex(dealData.getAttackRegion().getRegion());
            BasicDeal generatedDeal = generateAttack(clippedRegionIndex, year, phase);

            if (generatedDeal != null) {
                // if we could find a deal
                deals.add(generatedDeal);
            }
        }

        // Only if execute is true
        if(dealData.getSupportAttackRegion().getExecute()) {
            int clippedRegionIndex = this.clipRegionIndex(dealData.getSupportAttackRegion().getRegion());
            BasicDeal generatedDeal = generateSupportAttack(clippedRegionIndex, year, phase);

            if (generatedDeal != null) {
                // if we could find a deal
                deals.add(generatedDeal);
            }
        }

        return deals;
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
     * This method clips the given integer, in case it is greater than the size of the list of controlled regions.
     *
     * Because the DRL agent will provide an integer n where 0 <= n <= maximum_number_of_units, n may be larger
     * than the number of CURRENT units. Therefore, we clip it, so that any n greater than our number of units maps to
     * the maximum index possible.
     * @param n
     * @return
     */
    private int clipRegionIndex(int n) {
        if(n >= this.agent.me.getControlledRegions().size()) {
            return this.agent.me.getControlledRegions().size() - 1;
        }
        else {
            return n;
        }
    }

    /**
     * This method clips the given integer, in case it is greater than the size of the list of controlled SCs/provinces.
     *
     * Because the DRL agent will provide an integer n where 0 <= n <= maximum_number_of_provinces, n may be larger
     * than the number of CURRENT units. Therefore, we clip it, so that any n greater than our number of SCs maps to
     * the maximum index possible.
     * @param n
     * @return
     */
    private int clipProvinceIndex(int n) {
        if(n >= this.agent.me.getOwnedSCs().size()) {
            return this.agent.me.getOwnedSCs().size() - 1;
        }
        else {
            return n;
        }
    }

    /**
     * This method clips the given integer, in case it is greater than the size of the list of negotiating powers.
     *
     * Because the DRL agent will provide an integer n where 0 <= n <= number_of_players, n may be larger
     * than the number of CURRENT negotiating powers.
     * Therefore, we clip it, so that any n greater than the number of negotiating powers
     * to the maximum index possible.
     * @param n
     * @return
     */
    private int clipPowerIndex(int n) {
        if(n >= this.orderedNegotiatingPowers.size()) {
            return this.orderedNegotiatingPowers.size() - 1;
        }
        else {
            return n;
        }
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

    /**
     * This methods returns ONE deal, to support a hold order in ONE random region. The deal is made randomly
     * to a valid neighbour power.
     *
     * @param year
     * @param phase
     * @return
     */
    private BasicDeal generateDefendUnitsMutual(int regionIndex, int year, Phase phase) {
        List<OrderCommitment> ocs = new ArrayList<>();
        List<DMZ> dmzs = new ArrayList<>();

        // Choose random unit

        // An unit is addressed by the Region it occupies.
        Region ourRegion = this.orderedControlledRegions.get(regionIndex);

        List<Region> adjacentRegions = ourRegion.getAdjacentRegions();

        // Shuffle in order to randomize the process
        Collections.shuffle(adjacentRegions);

        // Try to get a deal with a random surrounding region.
        for(Region adjacentRegion : adjacentRegions) {
            Power controller = this.agent.game.getController(adjacentRegion);
            if(controller == null){
                // if no one controls region, don't consider it
                continue;
            }
            if (!this.orderedNegotiatingPowers.contains(controller)) {
                // if it's a non-negotiating or if it's us, don't consider
                continue;
            }

            HLDOrder hldOrderTheirs = new HLDOrder(controller, adjacentRegion);
            HLDOrder hldOrderOurs = new HLDOrder(this.agent.me, ourRegion);

            SUPOrder supOrderTheirs = new SUPOrder(controller, ourRegion, hldOrderOurs);
            SUPOrder supOrderOurs = new SUPOrder(this.agent.me, adjacentRegion, hldOrderTheirs);

            ocs.add(new OrderCommitment(year, phase, supOrderOurs));
            ocs.add(new OrderCommitment(year, phase, supOrderTheirs));

            BasicDeal deal = new BasicDeal(ocs, dmzs);
            return deal;
        }

        return null;
    }

    /**
     * This method returns ONE deal, where we choose a random Power and propose not invading (DMZ) each other's
     * supply centers.
     *
     * @param year
     * @param phase
     * @return
     */
    private BasicDeal generateDefendSupplyCentersMutual(int powerIndex, int year, Phase phase) {
        List<OrderCommitment> ocs = new ArrayList<>();
        List<DMZ> dmzs = new ArrayList<>();

        List<Province> ourProvinces = this.agent.me.getOwnedSCs();

        // Try to get a deal with a random Power.
        Power opponent = this.orderedNegotiatingPowers.get(powerIndex);

        List<Province> provincesDMZ = new ArrayList<>();
        provincesDMZ.addAll(opponent.getOwnedSCs());
        provincesDMZ.addAll(ourProvinces);

        List<Power> involvedPowers = new ArrayList<>();
        involvedPowers.add(this.agent.me);
        involvedPowers.add(opponent);

        dmzs.add(new DMZ(year, phase, involvedPowers, provincesDMZ));
        return new BasicDeal(ocs, dmzs);
    }

    /**
     * This methods returns ONE deal, to coordinate an attack on ONE random region,
     * with the support of another random region.
     *
     * In this deal, WE are the main attackers (therefore we win the control).
     *
     * @param year
     * @param phase
     * @return
     */
    private BasicDeal generateAttack(int regionIndex, int year, Phase phase) {
        List<OrderCommitment> ocs = new ArrayList<>();
        List<DMZ> dmzs = new ArrayList<>();

        // An unit is addressed by the Region it occupies.
        Region ourRegion = this.orderedControlledRegions.get(regionIndex);

        List<Region> adjacentRegions = ourRegion.getAdjacentRegions();

        // Shuffle in order to randomize the process
        Collections.shuffle(adjacentRegions);

        // Try to get a deal to attack a random surrounding region.
        for(Region targetRegion : adjacentRegions) {
            Power targetPower = this.agent.game.getController(targetRegion);

            List<Region> possibleSupports = new ArrayList<>(adjacentRegions);
            possibleSupports.remove(targetRegion);

            MTOOrder mtoOrder = new MTOOrder(this.agent.me, ourRegion, targetRegion);

            for(Region supportingRegion: possibleSupports) {
                Power supportingPower = this.agent.game.getController(supportingRegion);

                if (supportingPower == null) {
                    // if the region is not controlled by anyone, find a new one that is
                    continue;
                }

                if (supportingPower.equals(targetPower)) {
                    // if the supporting power is the target, don't ask for help, obviously
                    continue;
                }

                if (!this.orderedNegotiatingPowers.contains(supportingPower)) {
                    // if the supporting power does not negotiate
                    continue;
                }

                SUPMTOOrder supmtoOrder = new SUPMTOOrder(supportingPower, supportingRegion, mtoOrder);

                ocs.add(new OrderCommitment(year, phase, mtoOrder));
                ocs.add(new OrderCommitment(year, phase, supmtoOrder));

                return new BasicDeal(ocs, dmzs);
            }
        }

        return null;
    }

    /**
     * This methods returns ONE deal, to coordinate an attack on ONE random region,
     * with the support of another random region.
     *
     * In this deal, THE OTHER OPPONENT is the main attacker (therefore they win the control).
     *
     * @param year
     * @param phase
     * @return
     */
    private BasicDeal generateSupportAttack(int regionIndex, int year, Phase phase) {
        List<OrderCommitment> ocs = new ArrayList<>();
        List<DMZ> dmzs = new ArrayList<>();

        // An unit is addressed by the Region it occupies.
        Region ourRegion = this.orderedControlledRegions.get(regionIndex);

        List<Region> adjacentRegions = ourRegion.getAdjacentRegions();

        // Shuffle in order to randomize the process
        Collections.shuffle(adjacentRegions);

        // Try to get a deal to attack a random surrounding region.
        for(Region targetRegion : adjacentRegions) {
            Power targetPower = this.agent.game.getController(targetRegion);

            List<Region> possibleSupports = new ArrayList<>(adjacentRegions);
            possibleSupports.remove(targetRegion);

            for(Region regionToSupport: possibleSupports) {
                Power powerToSupport = this.agent.game.getController(regionToSupport);

                if (powerToSupport == null) {
                    // if the region is not controlled by anyone, find a new one that is
                    continue;
                }

                if (powerToSupport.equals(targetPower)) {
                    // if the supporting power is the target, don't ask for help, obviously
                    continue;
                }

                if (!this.orderedNegotiatingPowers.contains(powerToSupport)) {
                    // if the supporting power does not negotiate
                    continue;
                }

                MTOOrder mtoOrder = new MTOOrder(powerToSupport, ourRegion, targetRegion);
                SUPMTOOrder supmtoOrder = new SUPMTOOrder(this.agent.me, regionToSupport, mtoOrder);

                ocs.add(new OrderCommitment(year, phase, mtoOrder));
                ocs.add(new OrderCommitment(year, phase, supmtoOrder));

                return new BasicDeal(ocs, dmzs);
            }
        }

        return null;
    }

    /**
     * Returns an alphabetically ordered region list, according to its name.
     * @param controlledRegions
     */
    private List<Region> sortRegionList(List<Region> controlledRegions) {
        List<Region> orderedControlledRegions = new ArrayList<>(controlledRegions);
        orderedControlledRegions.sort(new Comparator<Region>() {
            @Override
            public int compare(Region region, Region t1) {
                return region.getName().compareToIgnoreCase(t1.getName());
            }
        });

        return orderedControlledRegions;
    }

    /**
     * Returns an alphabetically ordered region list, according to its name.
     * @param powers
     */
    private List<Power> sortPowerList(List<Power> powers) {
        List<Power> orderedPowers = new ArrayList<>(powers);
        orderedPowers.sort(new Comparator<Power>() {
            @Override
            public int compare(Power power, Power t1) {
                return power.getName().compareToIgnoreCase(t1.getName());
            }
        });

        return orderedPowers;
    }
}
