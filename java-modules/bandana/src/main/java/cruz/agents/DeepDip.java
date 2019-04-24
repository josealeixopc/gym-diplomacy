package cruz.agents;

import ddejonge.bandana.tools.Utilities;
import ddejonge.bandana.tools.Logger;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.dip.orders.SUPOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("Duplicates")

public class DeepDip extends DumbBot {
    // The OpenAI Adapter contains the necessary functions and fields to make the connection to the Open AI environment
    OpenAIAdapter openAIAdapter;
    Logger logger = new Logger();

    private DeepDip(String name, int finalYear, String logPath) {
        super(name, finalYear, logPath);
        this.openAIAdapter = new OpenAIAdapter(this);
    }

    /**
     * Main method to start the agent.
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        String name = "DeepDip";
        String logPath = "log/";
        int finalYear = 1905;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-name") && args.length > i + 1) {
                name = args[i + 1];
            }

            //set the path to store the log file
            if (args[i].equals("-log") && args.length > i + 1) {
                logPath = args[i + 1];
            }

            //set the final year
            if (args[i].equals("-fy") && args.length > i + 1) {
                try {
                    finalYear = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.err.println("main() The final year argument is not a valid integer: " + args[i + 1]);
                    return;
                }
            }
        }
        
        File logFolder = new File(logPath);
        logFolder.mkdirs();
        DeepDip deepDip = new DeepDip(name, finalYear, logPath);

        try {
            deepDip.start(deepDip.comm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void init() {
        this.logger.enable(this.logPath, this.me.getName() + ".log");
        this.logger.logln(this.name + " playing as " + this.me.getName(), true);
        this.logger.writeToFile();
    }

    @Override
    public void start() {
        this.openAIAdapter.beginningOfGame();
    }
    
    List<Order> generateOrders() {
        switch (game.getPhase().ordinal() + 1) {
            case 1:
            case 3:
                List<Order> orders = this.openAIAdapter.getOrdersFromDeepDip();

                if (this.isValidOrders(orders)) {
                    return orders;
                } else {
                    return this.generateMovementOrders();
                }
            case 2:
            case 4:
                return this.generateRetreatOrders();
            case 5:
                int nBuilds = this.me.getOwnedSCs().size() - this.me.getControlledRegions().size();
                if (nBuilds < 0) {
                    return this.generateRemoveOrders(-nBuilds);
                } else {
                    if (nBuilds > 0) {
                        return this.generateBuildOrders(nBuilds);
                    }

                    return new ArrayList<>();
                }
            default:
                return null;
        }
    }

    private boolean isValidOrders(List<Order> orders) {
        try {
            if (orders == null) {
                throw new Exception("EMPTY ORDERS");
            }

            for(Order unit_order : orders) {
                Region r = unit_order.getLocation();
                if (!this.me.getControlledRegions().contains(r)) {
                    throw new Exception("Not controlled region. " + r + " not in " + this.me.getControlledRegions());
                }

                List<Region> adjacent_regions = r.getAdjacentRegions();
                if (unit_order instanceof MTOOrder && !adjacent_regions.contains(((MTOOrder) unit_order).getDestination())) {
                    throw new Exception("Bad destination in a MTOOrder.");
                } else if (unit_order instanceof SUPOrder && !adjacent_regions.contains(((SUPOrder) unit_order).getSupportedRegion())) {
                    throw new Exception("Bad supported region in a SUPOrder.");
                } else if (unit_order instanceof SUPMTOOrder && !adjacent_regions.contains(((SUPMTOOrder) unit_order).getSupportedRegion())) {
                    throw new Exception("Bad supported region in a SUPMTOOrder.");
                }

            }
        } catch (NullPointerException e) {
            System.err.println("INVALID UNDEFINED ORDER");
            return false;
        } catch (Exception e) {
            System.err.println("INVALID ORDER: " + e.getMessage());
            return false;
        }

        return true;
    }
    
    public Logger getLogger() {
        return this.logger;
    }

    public Game getGame() {
        return this.game;
    }

    public Power getMe() {
        return this.me;
    }
}
