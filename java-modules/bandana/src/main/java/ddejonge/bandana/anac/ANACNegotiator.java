//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ddejonge.bandana.anac;

import ddejonge.bandana.dbraneTactics.DBraneTactics;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.DiplomacyProposal;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Logger;
import ddejonge.negoServer.Message;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Phase;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ANACNegotiator {
    public static final int DEFAULT_FINAL_YEAR = 2000;
    private ANACPlayer anacPlayer;
    public Game game;
    public Power me;

    public ANACNegotiator(String[] args) {
        String name = this.getClass().getSimpleName();
        String logPath = "log/";
        int gameServerPort = 16713;
        int negoPort = 16714;
        int finalYear = 2000;

        for(int i = 0; i < args.length; ++i) {
            if (args[i].equals("-name") && args.length > i + 1) {
                name = args[i + 1];
            }

            if (args[i].equals("-log") && args.length > i + 1) {
                logPath = args[i + 1];
            }

            if (args[i].equals("-fy") && args.length > i + 1) {
                try {
                    finalYear = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException var9) {
                    System.out.println("main() The final year argument is not a valid integer: " + args[i + 1]);
                    return;
                }
            }

            if (args[i].equals("-gamePort") && args.length > i + 1) {
                try {
                    gameServerPort = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException var10) {
                    System.out.println("main() The port number argument is not a valid integer: " + args[i + 1]);
                    return;
                }
            }

            if (args[i].equals("-negoPort") && args.length > i + 1) {
                try {
                    negoPort = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException var11) {
                    System.out.println("main() The port number argument is not a valid integer: " + args[i + 1]);
                    return;
                }
            }
        }

        if (!logPath.endsWith(File.separator)) {
            logPath = logPath + File.separator;
        }

        this.anacPlayer = new ANACPlayer(this, name, logPath, finalYear, gameServerPort, negoPort);
    }

    public abstract void negotiate(long var1);

    public final void run() {
        this.anacPlayer.run();
    }

    public abstract void start();

    public abstract void receivedOrder(Order var1);

    public final boolean hasMessage() {
        return this.anacPlayer.negoClient.hasMessage();
    }

    public final Message removeMessageFromQueue() {
        return this.anacPlayer.negoClient.removeMessageFromQueue();
    }

    public final ArrayList<DiplomacyProposal> getUnconfirmedProposals() {
        return this.anacPlayer.negoClient.getUnconfirmedProposals();
    }

    public final void proposeDeal(BasicDeal deal) {
        try {
            boolean containsOtherPower = false;
            Iterator var4 = deal.getDemilitarizedZones().iterator();

            while(var4.hasNext()) {
                DMZ commitment = (DMZ)var4.next();
                if (commitment.getPowers().size() > 1) {
                    containsOtherPower = true;
                    break;
                }

                if (!((Power)commitment.getPowers().get(0)).getName().equals(this.anacPlayer.getMe().getName())) {
                    containsOtherPower = true;
                    break;
                }
            }

            var4 = deal.getOrderCommitments().iterator();

            while(var4.hasNext()) {
                OrderCommitment commitment = (OrderCommitment)var4.next();
                if (!commitment.getOrder().getPower().getName().equals(this.anacPlayer.getMe().getName())) {
                    containsOtherPower = true;
                }

                if (!(commitment.getOrder() instanceof HLDOrder) && !(commitment.getOrder() instanceof MTOOrder) && !(commitment.getOrder() instanceof SUPOrder) && !(commitment.getOrder() instanceof SUPMTOOrder)) {
                    throw new RuntimeException("Error! In the ANAC competition you can only propose deals that involve orders of type HLDOrder, MTOOrder, SUPOrder or SUPMTOOrder");
                }
            }

            if (!containsOtherPower) {
                throw new RuntimeException("Error! The proposed deal is not valid! A deal must involve at least one power other than yourself.");
            }

            this.anacPlayer.negoClient.proposeDeal(deal);
        } catch (IOException var5) {
            var5.printStackTrace();
        }

    }

    public final void acceptProposal(String proposalID) {
        this.anacPlayer.negoClient.acceptProposal(proposalID);
    }

    public final void rejectProposal(String proposalID) {
        this.anacPlayer.negoClient.rejectProposal(proposalID);
    }

    public final DBraneTactics getTacticalModule() {
        return this.anacPlayer.dbraneTactics;
    }

    public final List<Power> getNegotiatingPowers() {
        List<String> negotiatingPowers = this.anacPlayer.negoClient.getRegisteredNames();
        List<Power> aliveNegotiatingPowers = new ArrayList(7);
        Iterator var4 = negotiatingPowers.iterator();

        while(var4.hasNext()) {
            String powerName = (String)var4.next();
            Power negotiatingPower = this.anacPlayer.getGame().getPower(powerName);
            if (!this.anacPlayer.getGame().isDead(negotiatingPower)) {
                aliveNegotiatingPowers.add(negotiatingPower);
            }
        }

        return aliveNegotiatingPowers;
    }

    public final Logger getLogger() {
        return this.anacPlayer.logger;
    }

    public final List<BasicDeal> getConfirmedDeals() {
        return new ArrayList(this.anacPlayer.confirmedDeals);
    }

    public final boolean isHistory(Phase phase, int year) {
        if (year == this.anacPlayer.getGame().getYear()) {
            return this.getPhaseValue(phase) < this.getPhaseValue(this.anacPlayer.getGame().getPhase());
        } else {
            return year < this.anacPlayer.getGame().getYear();
        }
    }

    final int getPhaseValue(Phase phase) {
        switch(phase.ordinal()) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 3;
            case 5:
                return 4;
            default:
                return -1;
        }
    }

    public final void proposeDraw() {
        this.anacPlayer.proposeDraw();
    }
}
