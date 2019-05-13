//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ddejonge.bandana.anac;

import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.dbraneTactics.Plan;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.Deal;
import ddejonge.bandana.negoProtocol.DiplomacyNegoClient;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Logger;
import ddejonge.bandana.tools.Utilities;
import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Dislodgement;
import es.csic.iiia.fabregues.dip.board.GameState;
import es.csic.iiia.fabregues.dip.board.Phase;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.comm.CommException;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.DSBOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.RTOOrder;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

class ANACPlayer extends Player {
    public static final int DEFAULT_FINAL_YEAR = 2000;
    public final int NEGOTIATION_LENGTH = 300;
    DBraneTactics dbraneTactics = new DBraneTactics();
    ANACNegotiator anacNegotiator;
    private Random random = new Random();
    Logger logger = new Logger();
    private IComm communicator;
    DiplomacyNegoClient negoClient;
    int gameServerPort;
    int negoServerPort;
    int finalYear;
    ArrayList<BasicDeal> confirmedDeals = new ArrayList();

    public ANACPlayer(ANACNegotiator anacNegotiator, String name, String logPath, int finalYear, int gameServerPort, int negoServerPort) {
        super(logPath);
        this.name = name;
        this.finalYear = finalYear;
        this.logPath = logPath;
        this.anacNegotiator = anacNegotiator;
        this.gameServerPort = gameServerPort;
        this.negoServerPort = negoServerPort;
        this.negoClient = new DiplomacyNegoClient(this, negoServerPort);
    }

    public void run() {
        File logFolder = new File(this.logPath);
        logFolder.mkdirs();

        try {
            InetAddress dipServerIp = InetAddress.getByName("localhost");
            this.communicator = new DaideComm(dipServerIp, this.gameServerPort, this.name);
            this.start(this.communicator);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public void init() {
        this.anacNegotiator.me = this.me;
        this.logger.enable(this.logPath, this.me.getName() + ".log");
        this.logger.logln(this.name + " playing as " + this.me.getName(), true);
        this.logger.writeToFile();
        this.negoClient.connect();
        this.negoClient.waitTillReady();
    }

    public void start() {
        this.anacNegotiator.start();
    }

    public List<Order> play() {
        this.anacNegotiator.game = this.game;
        ArrayList<Order> myOrders = new ArrayList();
        List<Power> myAllies = new ArrayList(1);
        myAllies.add(this.me);
        this.confirmedDeals.clear();
        Iterator var4 = this.negoClient.getConfirmedDeals().iterator();

        while(var4.hasNext()) {
            Deal confirmedDeal = (Deal)var4.next();
            if (Utilities.testValidity(this.game, (BasicDeal)confirmedDeal) == null) {
                this.confirmedDeals.add((BasicDeal)confirmedDeal);
            }
        }

        if (this.game.getPhase() != Phase.SPR && this.game.getPhase() != Phase.FAL) {
            return this.game.getPhase() != Phase.SUM && this.game.getPhase() != Phase.AUT ? this.dbraneTactics.getWinterOrders(this.game, this.me, myAllies) : this.generateRandomRetreats();
        } else {
            Iterator var6;
            try {
                long negotiationDeadline = System.currentTimeMillis() + NEGOTIATION_LENGTH;
                this.anacNegotiator.negotiate(negotiationDeadline);
                this.confirmedDeals.clear();
                var6 = this.negoClient.getConfirmedDeals().iterator();

                while(var6.hasNext()) {
                    Deal confirmedDeal = (Deal)var6.next();
                    if (Utilities.testValidity(this.game, (BasicDeal)confirmedDeal) == null) {
                        this.confirmedDeals.add((BasicDeal)confirmedDeal);
                    }
                }

                this.logger.logln();
                this.logger.logln(this.game.getYear() + " " + this.game.getPhase());
                this.logger.logln("Confirmed Deals: " + this.confirmedDeals);
                String report = Utilities.testConsistency(this.game, this.confirmedDeals);
                if (report != null) {
                    this.logger.logln("ERROR! confirmed deals are inconsistent: " + report);
                } else {
                    Plan plan = this.dbraneTactics.determineBestPlan(this.game, this.me, this.confirmedDeals, myAllies);
                    if (plan == null) {
                        this.logger.logln("ERROR! plan == null!");
                    } else {
                        myOrders.addAll(plan.getMyOrders());
                    }
                }
            } catch (Exception var9) {
                var9.printStackTrace();
            }

            List<Order> committedOrders = new ArrayList();
            List<DMZ> demilitarizedZones = new ArrayList();
            var6 = this.confirmedDeals.iterator();

            while(var6.hasNext()) {
                BasicDeal deal = (BasicDeal)var6.next();
                Iterator var8 = deal.getDemilitarizedZones().iterator();

                while(var8.hasNext()) {
                    DMZ dmz = (DMZ)var8.next();
                    if (dmz.getPhase().equals(this.game.getPhase()) && dmz.getYear() == this.game.getYear() && dmz.getPowers().contains(this.me)) {
                        demilitarizedZones.add(dmz);
                    }
                }

                var8 = deal.getOrderCommitments().iterator();

                while(var8.hasNext()) {
                    OrderCommitment orderCommitment = (OrderCommitment)var8.next();
                    if (orderCommitment.getPhase().equals(this.game.getPhase()) && orderCommitment.getYear() == this.game.getYear() && orderCommitment.getOrder().getPower().equals(this.me)) {
                        committedOrders.add(orderCommitment.getOrder());
                    }
                }
            }

            myOrders = Utilities.addHoldOrders(this.me, myOrders);
            this.logger.logln("Commitments to obey this turn: " + committedOrders + " " + demilitarizedZones);
            this.logger.logln("Orders submitted this turn: " + myOrders);
            this.logger.writeToFile();
            return myOrders;
        }
    }

    private List<Order> generateRandomRetreats() {
        List<Order> orders = new ArrayList(this.game.getDislodgedRegions().size());
        HashMap<Region, Dislodgement> units = this.game.getDislodgedRegions();
        List<Region> dislodgedUnits = this.game.getDislodgedRegions(this.me);
        Iterator var6 = dislodgedUnits.iterator();

        while(var6.hasNext()) {
            Region region = (Region)var6.next();
            Dislodgement dislodgement = (Dislodgement)units.get(region);
            List<Region> dest = new ArrayList();
            dest.addAll(dislodgement.getRetreateTo());
            if (dest.size() == 0) {
                orders.add(new DSBOrder(region, this.me));
            } else {
                int randomInt = this.random.nextInt(dest.size());
                orders.add(new RTOOrder(region, this.me, (Region)dest.get(randomInt)));
            }
        }

        return orders;
    }

    public void receivedOrder(Order arg0) {
        this.anacNegotiator.receivedOrder(arg0);
    }

    public void phaseEnd(GameState gameState) {
        if (this.game.getYear() == this.finalYear && this.game.getPhase() == Phase.FAL || this.game.getYear() > this.finalYear) {
            this.proposeDraw();
        }

    }

    void proposeDraw() {
        try {
            this.communicator.sendMessage(new String[]{"DRW"});
        } catch (CommException var2) {
            var2.printStackTrace();
        }

    }

    public void handleSMR(String[] message) {
        this.logger.writeToFile();
        this.communicator.stop();
        this.negoClient.closeConnection();
        this.exit();
    }

    public void submissionError(String[] message) {
        if (message.length < 2) {
            System.out.println("submissionError() " + Arrays.toString(message));
        } else {
            String illegalOrder = "";

            for(int i = 2; i < message.length - 4; ++i) {
                illegalOrder = illegalOrder + message[i] + " ";
            }

            System.out.println("Illegal order submitted: " + illegalOrder);
            String errorType = message[message.length - 2];
            switch(errorType.hashCode()) {
                case 67044:
                    if (errorType.equals("CST")) {
                        System.out.println("Reason: No coast specified for fleet build in StP, or an attempt to build a fleet inland, or an army at sea.");
                        return;
                    }
                    break;
                case 68949:
                    if (errorType.equals("ESC")) {
                        System.out.println("Reason: Not an empty supply centre");
                        return;
                    }
                    break;
                case 69367:
                    if (errorType.equals("FAR")) {
                        System.out.println("Reason: Unit is trying to move to a non-adjacent region, or is trying to support a move to a non-adjacent region.");
                        return;
                    }
                    break;
                case 71832:
                    if (errorType.equals("HSC")) {
                        System.out.println("Reason: Not a home supply centre");
                        return;
                    }
                    break;
                case 77056:
                    if (errorType.equals("NAS")) {
                        System.out.println("Reason: Not at sea (for a convoying fleet)");
                        return;
                    }
                    break;
                case 77411:
                    if (errorType.equals("NMB")) {
                        System.out.println("Reason: No more builds allowed");
                        return;
                    }
                    break;
                case 77427:
                    if (errorType.equals("NMR")) {
                        System.out.println("Reason: No more removals allowed");
                        return;
                    }
                    break;
                case 77578:
                    if (errorType.equals("NRN")) {
                        System.out.println("Reason: No retreat needed for this unit");
                        return;
                    }
                    break;
                case 77583:
                    if (errorType.equals("NRS")) {
                        System.out.println("Reason: Not the right season");
                        return;
                    }
                    break;
                case 77596:
                    if (errorType.equals("NSA")) {
                        System.out.println("Reason: No such army (for unit being ordered to CTO or for unit being CVYed)");
                        return;
                    }
                    break;
                case 77598:
                    if (errorType.equals("NSC")) {
                        System.out.println("Reason: Not a supply centre");
                        return;
                    }
                    break;
                case 77601:
                    if (errorType.equals("NSF")) {
                        System.out.println("Reason: No such fleet (in VIA section of CTO or the unit performing a CVY)");
                        return;
                    }
                    break;
                case 77611:
                    if (errorType.equals("NSP")) {
                        System.out.println("Reason: No such province.");
                        return;
                    }
                    break;
                case 77616:
                    if (errorType.equals("NSU")) {
                        System.out.println("Reason: No such unit.");
                        return;
                    }
                    break;
                case 77706:
                    if (errorType.equals("NVR")) {
                        System.out.println("Reason: Not a valid retreat space");
                        return;
                    }
                    break;
                case 77802:
                    if (errorType.equals("NYU")) {
                        System.out.println("Reason: Not your unit");
                        return;
                    }
                    break;
                case 88169:
                    if (errorType.equals("YSC")) {
                        System.out.println("Reason: Not your supply centre");
                        return;
                    }
            }

            System.out.println("submissionError() Received error message of unknown type: " + Arrays.toString(message));
        }
    }
}
