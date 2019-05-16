package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.*;

import java.util.*;

/**
 * The class that makes the connection between the Open AI environment and the BANDANA player.
 */
public class OpenAIAdapterStrategy extends OpenAIAdapter{

    /**
     * Reward given for winning the game
     */
    public static final int WON_GAME_REWARD = +100;

    /**
     * Reward given for losing the game
     */
    public static final int LOST_GAME_REWARD = -100;

    /**
     * Reward given for generating an invalid order
     */
    public static final int INVALID_ORDER_REWARD = -5;

    /**
     * Reward given for capturing a Supply Center (SC). Losing a SC gives a negative reward with the same value.
     */
    public static final int CAPTURED_SC_REWARD = +50;


    private DeepDip agent;
    int wrongBordersNum = 0;
    int orderNotFoundNum = 0;

    OpenAIAdapterStrategy(DeepDip agent) {
        this.agent = agent;
    }

    /**
     * This function retrieves a list of Orders from the OpenAI module that is connected to the localhost on port 5000.
     *
     * @return List of Orders created with data from the OpenAI module.
     */
    public List<Order> getOrdersFromDeepDip() {
        this.agent.getLogger().logln("GAME STATUS: " + this.openAIObserver.getGameStatus(), true);
        this.generatePowerNameToIntMap();

        ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

        ProtoMessage.ObservationData observationData = this.generateObservationData();

        bandanaRequestBuilder.setObservation(observationData);
        bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.GET_DEAL_REQUEST);

        ProtoMessage.BandanaRequest message = bandanaRequestBuilder.build();

        ProtoMessage.DiplomacyGymOrdersResponse diplomacyGymResponse = this.serviceClient.getTacticAction(message);

        // If something went wrong with getting the response from Python module
        if (diplomacyGymResponse == null) {
            return new ArrayList<>();
        }

        List<Order> generatedOrders = this.generateOrders(diplomacyGymResponse.getOrders());

        return generatedOrders;
    }

    private List<Order> generateOrders(ProtoMessage.OrdersData ordersData) {

        this.wrongBordersNum = 0;
        this.orderNotFoundNum = 0;

        List<Order> orders = new ArrayList<>();
        List<ProtoMessage.OrderData> support_orders = new ArrayList<>();
        List<Province> game_provinces = this.agent.getGame().getProvinces();
        List<Region> game_regions = this.agent.getGame().getRegions();

        for (ProtoMessage.OrderData order : ordersData.getOrdersList()) {
            if (order.getStart() == -1){
                break;
            }
            Province start_province = game_provinces.get(order.getStart());
            Province destination_province = game_provinces.get(order.getDestination());

            Region start = game_regions.stream()
                    .filter(r -> r.getProvince().getName().equals(start_province.getName()))
                    .findAny()
                    .orElse(null);
            Region destination = game_regions.stream()
                    .filter(r -> r.getProvince().getName().equals(destination_province.getName()))
                    .findAny()
                    .orElse(null);

            if (order.getAction() == 0) {
                orders.add(new HLDOrder(this.agent.getMe(), start));
            } else if (destination.getAdjacentRegions().contains(start)){
                if (order.getAction() == 1) {
                    orders.add(new MTOOrder(this.agent.getMe(), start, destination));
                } else if (order.getAction() >= 2) {
                    support_orders.add(order);
                }
            } else {
                System.err.println("WRONG BORDER: For order of type " + order.getAction() + ", the destination " + destination + " is not a border with current province " + start);
                this.wrongBordersNum++;
                orders.add(new HLDOrder(this.agent.getMe(), start));
            }
        }

        for (ProtoMessage.OrderData support_order : support_orders) {
            Region start = game_regions.get(support_order.getStart());
            Region destination = game_regions.get(support_order.getDestination());
            Order order_to_support = orders.stream()
                    .filter(order -> destination.equals(order.getLocation()))
                    .findAny()
                    .orElse(null);
            if (order_to_support == null) {
                System.err.println("ORDER TO SUPPORT NOT FOUND");
                this.orderNotFoundNum++;
                orders.add(new HLDOrder(this.agent.getMe(), start));
            } else if (order_to_support instanceof MTOOrder) {
                orders.add(new SUPMTOOrder(this.agent.getMe(), start, (MTOOrder) order_to_support));
            } else {
                orders.add(new SUPOrder(this.agent.getMe(), start, order_to_support));
            }
        }
        return orders;
    }


    @Override
    protected float calculateReward() {
        return INVALID_ORDER_REWARD * (this.orderNotFoundNum + this.wrongBordersNum);
    }

    @Override
    protected Power getPower() {
        return this.agent.getMe();
    }

    @Override
    protected Game getGame() {
        return this.agent.getGame();
    }
}
