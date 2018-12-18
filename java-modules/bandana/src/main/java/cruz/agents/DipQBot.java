package cruz.agents;

import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;

public class DipQBot extends Player {

    public static final int DEFAULT_GAME_SERVER_PORT = 16713;
    public static final int FINAL_YEAR = 1905;

    /**
     * Main method to start the agent. This is needed because a .jar file will be created from this.
     * @param args
     */
    public static void main(String[] args){

        System.out.println("Hello. I'm DipQBot. Nice to meet you!");


        String name = "DipQ";
        int finalYear = FINAL_YEAR;

        for(int i=0; i < args.length; i++){

            if(args[i].equals("-name") && args.length > i+1){
                name = args[i+1];
            }

            //set the final year
            if(args[i].equals("-fy") && args.length > i+1){
                try{
                    finalYear = Integer.parseInt(args[i+1]);
                }catch (NumberFormatException e) {
                    System.out.println("main() The final year argument is not a valid integer: " + args[i+1]);
                    return;
                }
            }
        }


        DipQBot dipQBot = new DipQBot(name, finalYear, DEFAULT_GAME_SERVER_PORT);

        try{

            //start the agent.
            dipQBot.start(dipQBot.comm);

        }catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**Client to connect with the game server.*/
    private IComm comm;
    int finalYear;

    DipQBot(String name, int finalYear, int gameServerPort){
        this.name = "DipQBot";
        this.finalYear = finalYear;

        //Initialize the client
        try {
            InetAddress gameServerIp =  InetAddress.getLocalHost();
            this.comm = new DaideComm(gameServerIp, gameServerPort, name);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called once, at the start of the game, before the 'game' field is set.
     *
     * It is called when the HLO message is received from the game server.
     * The HLO contains information about the game such as the power assigned to you, and the deadlines.
     *
     * The power you are playing is stored in the field 'me'.
     * The game field will still be null when this method is called.
     *
     * It is not necessary to implement this method.
     */
    @Override
    public void init() {
        System.out.println("Player " + this.name + " has started and is playing as: " + me.getName());

        //Note: this.name is the name of the player, e.g. 'RandomBot'. On the other hand, me.getName() returns the name
        // of the Power that this agent is playing, e.g.  'AUS', 'ENG', 'FRA', etcetera.
    }

    /**
     * This method is automatically called at the start of the game, after the 'game' field is set.
     *
     * It is called when the first NOW message is received from the game server.
     * The NOW message contains the current phase and the positions of all the units.
     *
     * Note: the init() method is called before the start() method.
     *
     * It is not necessary to implement this method
     *
     */
    @Override
    public void start() {

        System.out.println(Utilities.getAllProvincesInformation(this.game));

    }


    /**
     * This is the most important method of your agent!
     * Here is where you actually implement the behavior of your agent.
     *
     * This method is automatically called every time when the game is in a new phase.
     * You must implement this method to return a list of orders for your units.
     *
     * @return An order FOR EACH unit of the power you are playing.
     */
    @Override
    public List<Order> play() {

        /**
         * TYPES OF ORDER
         *
         * BLD: Build Order
         * DSB: Disband Order
         * HLD: Hold Order
         * MTO: Move To Order
         * REM: Remove Order
         * RTO: Retreat Order
         * SUPMTO: Support Move To Order
         * SUP: Support Order
         * WVE: Waive Order (do not use any buildings even if we have buildings available)
         */

        MTOOrder order = new MTOOrder(me, me.getControlledRegions().get(1), me.getControlledRegions().get(2));

        String serializedObject = "";

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(order);
            so.flush();
            serializedObject = bo.toString();
        } catch (Exception e) {
            System.out.println(e);
        }



        return null;
    }


    /**
     * After each power has submitted its orders, this method is called several times:
     * once for each order submitted by any power.
     *
     * You can use this to verify whether your allies have obeyed their agreements.
     */
    @Override
    public void receivedOrder(Order order) {

    }
}
