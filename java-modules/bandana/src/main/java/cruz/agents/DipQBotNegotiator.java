package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.anac.ANACNegotiator;
import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.*;
import ddejonge.bandana.tools.Utilities;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("Duplicates")

public class DipQBotNegotiator extends ANACNegotiator {

    /**
     * Main method to start the agent.
     * <p>
     * This player can be started with the following arguments:
     * -name  	[the name of your agent]
     * -log		[the path to the folder where you want the log files to be stored]
     * -fy 		[the year after which your agent will propose a draw]
     * -gamePort  [the port of the game server]
     * -negoPort  [the port of the negotiation server]
     * <p>
     * e.g. java -jar ANACExampleNegotiator.jar -name alice -log C:\\documents\log -fy 1920 -gamePort 16713 -negoPort 16714
     * <p>
     * All of these arguments are optional.
     * <p>
     * Note however that during the competition the values of these arguments will be chosen by the organizers
     * of the competition, so you can only control them during the development of your negotiator.
     *
     * @param args
     */
    public static void main(String[] args) {
        // THE MAIN METHOD MUST CONSIST ONLY OF THE FOLLOWING TWO LINES ONCE EVERYTHING IS DONE
        // THIS IS EXPRESSED IN THE ANAC 2019 MANUAL
        DipQBotNegotiator myPlayer = new DipQBotNegotiator(args);
        myPlayer.run();

//        System.out.println("Hello");
//        DipQBotNegotiator myPlayer = new DipQBotNegotiator(args);
//        myPlayer.getDealFromDipQ();
//        System.out.println("Hello again");
    }

    public Random random = new Random();
    DBraneTactics dBraneTactics;

    //Constructor

    /**
     * You must implement a Constructor with exactly this signature.
     * The body of the Constructor must start with the line <code>super(args)</code>
     * but below that line you can put whatever you like.
     *
     * @param args
     */
    public DipQBotNegotiator(String[] args) {
        super(args);

        dBraneTactics = this.getTacticalModule();
    }


    /**
     * This method is automatically called at the start of the game, after the 'game' field is set.
     * <p>
     * It is called when the first NOW message is received from the game server.
     * The NOW message contains the current phase and the positions of all the units.
     * <p>
     * You are allowed, but not required, to implement this method
     */
    @Override
    public void start() {

        //You can use the logger to write stuff to the log file.
        //The location of the log file can be set through the command line option -log.
        // it is not necessary to call getLogger().enable() because this is already automatically done by the ANACNegotiator class.

        boolean printToConsole = true; //if set to true the text will be written to file, as well as printed to the standard output stream. If set to false it will only be written to file.
        this.getLogger().logln("game is starting!", printToConsole);

    }

    @Override
    public void negotiate(long negotiationDeadline) {

        // this.getLogger().logln(me.getName() + ".negotiate() Negotiation deadline: " + negotiationDeadline, true);

        getDealFromDipQ();

        BasicDeal newDealToPropose = null;


        //This loop repeats 2 steps. The first step is to handle any incoming messages,
        // while the second step tries to find deals to propose to the other negotiators.
        while (System.currentTimeMillis() < negotiationDeadline) {

            // I (the player) is responsible for sending/accepting new deals during this time period


            //STEP 1: Handle incoming messages.


            //See if we have received any message from any of the other negotiators.
            // e.g. a new proposal or an acceptance of a proposal made earlier.
            while (hasMessage()) {

                //Warning: you may want to add some extra code to break out of this loop,
                // just in case the other agents send so many proposals that your agent can't get
                // the chance to make any proposals itself.

                //if yes, remove it from the message queue.
                Message receivedMessage = removeMessageFromQueue();

                if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.ACCEPT)) {

                    DiplomacyProposal acceptedProposal = (DiplomacyProposal) receivedMessage.getContent();

                    this.getLogger().logln(me.getName() + ".negotiate() Received acceptance from " + receivedMessage.getSender() + ": " + acceptedProposal, true);

                    // Here we can handle any incoming acceptances.
                    // This random negotiator doesn't do anything with such messages however.

                    // Note: if a certain proposal has been accepted by all players it is still not considered
                    // officially binding until the protocol manager has sent a CONFIRM message.

                    // Note: if all agents involved in a proposal have accepted the proposal, then you will not receive an ACCEPT
                    // message from the last agent that accepted it. Instead, you will directly receive a CONFIRM message from the
                    // Protocol Manager.

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.PROPOSE)) {

                    DiplomacyProposal receivedProposal = (DiplomacyProposal) receivedMessage.getContent();

                    this.getLogger().logln(me.getName() + ".negotiate() Received proposal: " + receivedProposal, true);

                    BasicDeal deal = (BasicDeal) receivedProposal.getProposedDeal();

                    boolean outDated = false;

                    for (DMZ dmz : deal.getDemilitarizedZones()) {

                        // Sometimes we may receive messages too late, so we check if the proposal does not
                        // refer to some round of the game that has already passed.
                        if (isHistory(dmz.getPhase(), dmz.getYear())) {
                            outDated = true;
                            break;
                        }

                        //TODO: decide whether this DMZ is acceptable or not (in combination with the rest of the proposed deal).
						/*
						List<Power> powers = dmz.getPowers();
						List<Province> provinces = dmz.getProvinces();
						*/

                    }
                    for (OrderCommitment orderCommitment : deal.getOrderCommitments()) {


                        // Sometimes we may receive messages too late, so we check if the proposal does not
                        // refer to some round of the game that has already passed.
                        if (isHistory(orderCommitment.getPhase(), orderCommitment.getYear())) {
                            outDated = true;
                            break;
                        }

                        //TODO: decide whether this order commitment is acceptable or not (in combination with the rest of the proposed deal).
                        /*Order order = orderCommitment.getOrder();*/
                    }

                    //If the deal is not outdated, then check that it is consistent with the deals we are already committed to.
                    String consistencyReport = null;
                    if (!outDated) {

                        List<BasicDeal> commitments = new ArrayList<BasicDeal>();
                        commitments.addAll(this.getConfirmedDeals());
                        commitments.add(deal);
                        consistencyReport = Utilities.testConsistency(game, commitments);


                    }

                    if (!outDated && consistencyReport == null) {
                        // DECIDE WHETHER OR NOT TO ACCEPT THE DEAL

                        // JC: In order to study simpler scenarios first, reject all incoming negotiations
                        this.rejectProposal(receivedProposal.getId());
                        this.getLogger().logln(me.getName() + ".negotiate()  Rejecting: " + receivedProposal, true);

                        // This agent simply flips a coin to determine whether to accept the proposal or not.
                        // if (random.nextInt(2) == 0) { // accept with 50% probability.
                        //     this.acceptProposal(receivedProposal.getId());
                        //     this.getLogger().logln(me.getName() + ".negotiate()  Accepting: " + receivedProposal, true);
                        // }
                    }

                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.CONFIRM)) {

                    // The protocol manager confirms that a certain proposal has been accepted by all players involved in it.
                    // From now on we consider the deal as a binding agreement.

                    DiplomacyProposal confirmedProposal = (DiplomacyProposal) receivedMessage.getContent();

                    this.getLogger().logln(me.getName() + ".negotiate() RECEIVED CONFIRMATION OF: " + confirmedProposal, true);

                    BasicDeal confirmedDeal = (BasicDeal) confirmedProposal.getProposedDeal();


                    //Reject any proposal that has not yet been confirmed and that is inconsistent with the confirmed deal.
                    // NOTE that normally this is not really necessary because the Notary will already check that
                    // any deal is consistent with earlier confirmed deals before it becomes confirmed.
                    List<BasicDeal> deals = new ArrayList<BasicDeal>(2);
                    deals.add(confirmedDeal);
                    for (DiplomacyProposal standingProposal : this.getUnconfirmedProposals()) {

                        //add this proposal to the list of deals.
                        deals.add((BasicDeal) standingProposal.getProposedDeal());

                        if (Utilities.testConsistency(game, deals) != null) {
                            this.rejectProposal(standingProposal.getId());
                        }

                        //remove the deal again from the list, so that we can add the next standing deal to the list in the next iteration.
                        deals.remove(1);
                    }


                } else if (receivedMessage.getPerformative().equals(DiplomacyNegoClient.REJECT)) {

                    DiplomacyProposal rejectedProposal = (DiplomacyProposal) receivedMessage.getContent();

                    // Some player has rejected a certain proposal.
                    // This example agent doesn't do anything with such messages however.

                    //If a player first accepts a proposal and then rejects the same proposal the reject message cancels
                    // his earlier accept proposal.
                    // However, this is not true if the reject message is sent after the Notary has already sent a confirm
                    // message for that proposal. Once a proposal is confirmed it cannot be undone anymore.
                } else {

                    //We have received any other kind of message.

                    this.getLogger().logln("Received a message of unhandled type: " + receivedMessage.getPerformative() + ". Message content: " + receivedMessage.getContent().toString(), true);

                }

            }


            //STEP 2:  try to find a proposal to make, and if we do find one, propose it.

            if (newDealToPropose == null) { //we only make one proposal per round, so we skip this if we have already proposed something.

                newDealToPropose = getDealFromDipQ();
                // newDealToPropose = searchForNewDealToPropose();

                if (newDealToPropose != null) {

                    this.getLogger().logln(me.getName() + ".negotiate() Proposing: " + newDealToPropose, true);
                    this.proposeDeal(newDealToPropose);

                }
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }


        }


        //whenever you like, you can also propose a draw to all other surviving players:
        //this.proposeDraw();
    }


    BasicDeal searchForNewDealToPropose() {

        BasicDeal bestDeal = null;
        Plan bestPlan = null;

        //Get a copy of our list of current commitments.
        List<BasicDeal> commitments = this.getConfirmedDeals();

        //First, let's see what happens if we do not make any new commitments.
        bestPlan = this.dBraneTactics.determineBestPlan(game, me, commitments);

        //If our current commitments are already inconsistent then we certainly
        // shouldn't make any more commitments.
        if (bestPlan == null) {
            return null;
        }

        //let's generate 10 random deals and pick the best one.
        for (int i = 0; i < 10; i++) {

            //generate a random deal.
            BasicDeal randomDeal = generateRandomDeal();

            if (randomDeal == null) {
                continue;
            }


            //add it to the list containing our existing commitments so that dBraneTactics can determine a plan.
            commitments.add(randomDeal);


            //Ask the D-Brane Tactical Module what it would do under these commitments.
            Plan plan = this.dBraneTactics.determineBestPlan(game, me, commitments);

            //Check if the returned plan is better than the best plan found so far.
            if (plan != null && plan.getValue() > bestPlan.getValue()) {
                bestPlan = plan;
                bestDeal = randomDeal;
            }


            //Remove the randomDeal from the list, for the next iteration.
            commitments.remove(commitments.size() - 1);

            //NOTE: the value returned by plan.getValue() represents the number of Supply Centers that the D-Brane Tactical Module
            // expects to conquer in the current round under the given commitments.
            //
            // Of course, this is only a rough indication of which plan is truly the "best". After all, sometimes it is better
            // not to try to conquer as many Supply Centers as you can directly, but rather organize your armies and only attack in a later
            // stage.
            // Therefore, you may want to implement your own algorithm to determine which plan is the best.
            // You can call plan.getMyOrders() to retrieve the complete list of orders that D-Brane has chosen for you under the given commitments.

        }


        return bestDeal;


    }


    public BasicDeal generateRandomDeal() {


        //Get the names of all the powers that are connected to the negotiation server and which have not been eliminated.
        List<Power> aliveNegotiatingPowers = this.getNegotiatingPowers();

        //if there are less than 2 negotiating powers left alive (only me), then it makes no sense to negotiate.
        int numAliveNegoPowers = aliveNegotiatingPowers.size();
        if (numAliveNegoPowers < 2) {
            return null;
        }

        //Let's generate 3 random demilitarized zones.
        List<DMZ> demilitarizedZones = new ArrayList<DMZ>(3);
        for (int i = 0; i < 3; i++) {

            //1. Create a list of powers
            ArrayList<Power> powers = new ArrayList<Power>(2);

            //1a. add myself to the list
            powers.add(me);

            //1b. add a random other power to the list.
            Power randomPower = me;
            while (randomPower.equals(me)) {

                int numNegoPowers = aliveNegotiatingPowers.size();
                randomPower = aliveNegotiatingPowers.get(random.nextInt(numNegoPowers));
            }
            powers.add(randomPower);

            //2. Create a list containing 3 random provinces.
            ArrayList<Province> provinces = new ArrayList<Province>();
            for (int j = 0; j < 3; j++) {
                int numProvinces = this.game.getProvinces().size();
                Province randomProvince = this.game.getProvinces().get(random.nextInt(numProvinces));
                provinces.add(randomProvince);
            }


            //This agent only generates deals for the current year and phase.
            // However, you can pick any year and phase here, as long as they do not lie in the past.
            // (actually, you can also propose deals for rounds in the past, but it doesn't make any sense
            //  since you obviously cannot obey such deals).
            demilitarizedZones.add(new DMZ(game.getYear(), game.getPhase(), powers, provinces));

        }


        //let's generate 3 random OrderCommitments
        List<OrderCommitment> randomOrderCommitments = new ArrayList<OrderCommitment>();


        //get all units of the negotiating powers.
        List<Region> units = new ArrayList<Region>();
        for (Power power : aliveNegotiatingPowers) {
            units.addAll(power.getControlledRegions());
        }


        for (int i = 0; i < 3; i++) {

            //Pick a random unit and remove it from the list
            if (units.size() == 0) {
                break;
            }
            Region randomUnit = units.remove(random.nextInt(units.size()));

            //Get the corresponding power
            Power power = game.getController(randomUnit);

            //Determine a list of potential destinations for the unit.
            // a Region is a potential destination for a unit if it is adjacent to that unit (or it is the current location of the unit)
            //  and the Province is not demilitarized for the Power controlling that unit.
            List<Region> potentialDestinations = new ArrayList<Region>();

            //Create a list of adjacent regions, including the current location of the unit.
            List<Region> adjacentRegions = new ArrayList<>(randomUnit.getAdjacentRegions());
            adjacentRegions.add(randomUnit);

            for (Region adjacentRegion : adjacentRegions) {

                Province adjacentProvince = adjacentRegion.getProvince();

                //Check that the adjacent Region is not demilitarized for the power controlling the unit.
                boolean isDemilitarized = false;
                for (DMZ dmz : demilitarizedZones) {
                    if (dmz.getPowers().contains(power) && dmz.getProvinces().contains(adjacentProvince)) {
                        isDemilitarized = true;
                        break;
                    }

                }

                //If it is not demilitarized, then we can add the region to the list of potential destinations.
                if (!isDemilitarized) {
                    potentialDestinations.add(adjacentRegion);
                }
            }


            int numPotentialDestinations = potentialDestinations.size();
            if (numPotentialDestinations > 0) {

                Region randomDestination = potentialDestinations.get(random.nextInt(numPotentialDestinations));

                Order randomOrder;
                if (randomDestination.equals(randomUnit)) {
                    randomOrder = new HLDOrder(power, randomUnit);
                } else {
                    randomOrder = new MTOOrder(power, randomUnit, randomDestination);
                }
                // Of course we could also propose random support orders, but we don't do that here.


                //We only generate deals for the current year and phase.
                // However, you can pick any year and phase here, as long as they do not lie in the past.
                // (actually, you can also propose deals for rounds in the past, but it doesn't make any sense
                //  since you obviously cannot obey such deals).
                randomOrderCommitments.add(new OrderCommitment(game.getYear(), game.getPhase(), randomOrder));
            }

        }

        BasicDeal deal = new BasicDeal(randomOrderCommitments, demilitarizedZones);


        return deal;

    }

    public ProtoMessage.GameData generateGameData() {
        ProtoMessage.GameData.Builder gameDataBuilder = ProtoMessage.GameData.newBuilder();

        // FIRST CREATE REGIONS
        // For every region, create RegionData and ProvinceData
        for (Region r : this.game.getRegions()) {
            ProtoMessage.RegionData.Builder regionDataBuilder = ProtoMessage.RegionData.newBuilder();
            regionDataBuilder.setName(r.getName());

            // Add adjacent regions names
            for (Region adjRegion : r.getAdjacentRegions()) {
                regionDataBuilder.addAdjacentRegionsName(adjRegion.getName());
            }

            ProtoMessage.RegionData regionData = regionDataBuilder.build();

            Province pro = r.getProvince();

            // Check if province has been previously added to the game data
            ProtoMessage.ProvinceData provinceData = gameDataBuilder.getNameToProvincesMap().get(pro.getName());

            // If there isn't any province with the given name yet, add a new one
            if (provinceData == null) {
                // Building the Protobuf information
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder();

                // For now, create province with no owner
                ProtoMessage.PowerData.Builder powerDataBuilder = ProtoMessage.PowerData.newBuilder();
                powerDataBuilder.setName(ProtoMessage.PowerData.PowerName.NONE);

                ProtoMessage.PowerData owner = powerDataBuilder.build();

                provinceDataBuilder.setName(pro.getName());
                provinceDataBuilder.setOwner(owner);
                provinceDataBuilder.setSc(pro.isSC());
                provinceDataBuilder.putNameToRegions(regionData.getName(), regionData);

                provinceData = provinceDataBuilder.build();
            }
            // If there is a province, build a new one from the one that exists and update it with the new region
            else {
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder(provinceData);
                provinceDataBuilder.putNameToRegions(regionData.getName(), regionData);
                provinceData = provinceDataBuilder.build();
            }

            gameDataBuilder.putNameToProvinces(provinceData.getName(), provinceData);
        }

        // THEN ADD THE OWNERS OF EACH REGION
        for (Power pow : this.game.getPowers()) {
            List<Region> controlledRegions = pow.getControlledRegions();

            // Set the owner of the province
            for (Region r : controlledRegions) {
                Province pro = r.getProvince();

                // In this case, the province is the one that has an owner
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder(gameDataBuilder.getNameToProvincesMap().get(pro.getName()));

                ProtoMessage.PowerData.Builder powerDataBuilder = ProtoMessage.PowerData.newBuilder();
                powerDataBuilder.setName(ProtoMessage.PowerData.PowerName.valueOf(pow.getName()));

                ProtoMessage.PowerData owner = powerDataBuilder.build();

                provinceDataBuilder.setOwner(owner);

                ProtoMessage.ProvinceData provinceData = provinceDataBuilder.build();
                gameDataBuilder.putNameToProvinces(provinceData.getName(), provinceData);
            }

        }

        // SAVE WHAT POWER WE ARE

        ProtoMessage.PowerData.Builder powerDataBuilder = ProtoMessage.PowerData.newBuilder();
        powerDataBuilder.setName(ProtoMessage.PowerData.PowerName.valueOf(me.getName()));

        ProtoMessage.PowerData ownPower = powerDataBuilder.build();

        gameDataBuilder.setOwnPower(ownPower);

        return gameDataBuilder.build();
    }

    public BasicDeal dealDataToDeal(ProtoMessage.DealData dealData) {

        List<DMZ> dmzs = new ArrayList<>();
        List<OrderCommitment> ocs = new ArrayList<>();

        ProtoMessage.OrderCommitment ocData = dealData.getOc(0);
        ProtoMessage.MoveOrder moData = ocData.getMove(0);

        Order o = new MTOOrder(me, this.game.getRegion(moData.getStartProvince().getName() + "AMY"), this.game.getRegion(moData.getDestinationProvince().getName() + "AMY"));
        OrderCommitment oc = new OrderCommitment(this.game.getYear(), this.game.getPhase(), o);

        ocs.add(oc);

        return new BasicDeal(ocs, dmzs);
    }

    public BasicDeal getDealFromDipQ() {
        try {

            ProtoMessage.GameData gameData = generateGameData();

            byte[] gameByteArray = gameData.toByteArray();

            File gameDataFile = new File("log/game_data.txt");
            try {
                gameDataFile.createNewFile(); // if file already exists will do nothing
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileOutputStream fos = new FileOutputStream(gameDataFile, false)) {
                fos.write(gameByteArray);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SocketClient socketClient = new SocketClient("127.0.1.1", 5000);
            String message = socketClient.sendMessageAndReceiveResponse(gameByteArray);
            System.out.println("Message received from the server : '" + message + "'.");

            // TODO: CHECK IF THIS WORKS WITH PYTHON
            ProtoMessage.DealData dealData = ProtoMessage.DealData.parseFrom(message.getBytes());

            return dealDataToDeal(dealData);

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Each round, after each power has submitted its orders, this method is called several times:
     * once for each order submitted by any other power.
     *
     * @param arg0 An order submitted by any of the other powers.
     */
    @Override
    public void receivedOrder(Order arg0) {
        // TODO Auto-generated method stub

    }

}
