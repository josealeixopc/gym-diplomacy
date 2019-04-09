package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import ddejonge.bandana.tools.Logger;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAIAdapter {

    public static final int REJECTED_DEAL_REWARD = -5;
    public static final int ACCEPTED_DEAL_REWARD = +5;

    public OpenAINegotiator agent;
    public DeepDip agent2;
    private Map<String, Integer> powerNameToInt;

    private float previousActionReward;
    public boolean done;
    private String info;

    public boolean firstTurn;
    public int numberOfGamesStarted;

    public OpenAIObserver openAIObserver;

    OpenAIAdapter(OpenAINegotiator agent) {
        this.agent = agent;
        this.init();
    }
    
    OpenAIAdapter(DeepDip agent) {
        this.agent2 = agent;
        this.init();
    }
    
    private void init(){
        this.resetReward();
        this.done = false;
        this.info = null;

        this.numberOfGamesStarted = 0;
    }

    public void createObserver() {
        String openAIObserverPath = "log" + File.separator + "OpenAIObserver" + Logger.getDateString();
        File logFile = new File(openAIObserverPath);
        logFile.mkdirs();

        this.openAIObserver = new OpenAIObserver(openAIObserverPath, this);
        this.openAIObserver.connectToServer();
    }

    /**
     * This function retrieves a deal from the Open AI module that is connected to the localhost on port 5000.
     *
     * @return A BasicDeal created with data from the Open AI module.
     */
    public BasicDeal getDealFromDipQ() {
        try {
            this.agent.getLogger().logln("GAME STATUS: " + this.openAIObserver.getGameStatus(), true);
            this.generatePowerNameToIntMap();

            ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

            ProtoMessage.ObservationData observationData = this.generateObservationData();

            bandanaRequestBuilder.setObservation(observationData);

            if(firstTurn) {
                bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.SEND_INITIAL_OBSERVATION);

                byte[] message = bandanaRequestBuilder.build().toByteArray();

                SocketClient socketClient = new SocketClient("127.0.1.1", 5000, this.agent.getLogger());
                byte[] response = socketClient.sendMessageAndReceiveResponse(message);

                // If something went wrong with getting the response from Python module
                if (response == null) {
                    return null;
                }

                ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymResponse.parseFrom(response);
                BasicDeal generatedDeal = this.generateDeal(diplomacyGymResponse.getDeal());
                return generatedDeal;
            }
            else {
                return null;
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    /**
     * This function retrieves a list of Orders from the OpenAI module that is connected to the localhost on port 5000.
     *
     * @return List of Orders created with data from the OpenAI module.
     */
    public List<Order> getOrdersFromDeepDip() {
        try {
            this.agent2.getLogger().logln("GAME STATUS: " + this.openAIObserver.getGameStatus(), true);
            this.generatePowerNameToIntMap();

            ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();

            ProtoMessage.ObservationData observationData = this.generateObservationData();

            bandanaRequestBuilder.setObservation(observationData);

            if(firstTurn) {
                bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.SEND_INITIAL_OBSERVATION);

                byte[] message = bandanaRequestBuilder.build().toByteArray();

                SocketClient socketClient = new SocketClient("127.0.1.1", 5000, this.agent2.getLogger());
                byte[] response = socketClient.sendMessageAndReceiveResponse(message);

                // If something went wrong with getting the response from Python module
                if (response == null) {
                    return null;
                }

                ProtoMessage.DiplomacyGymOrdersResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymOrdersResponse.parseFrom(response);
                List<Order> generatedOrders = this.generateOrders(diplomacyGymResponse.getOrders());
                return generatedOrders;
            }
            else {
                return null;
            }

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public void sendEndOfGameNotification() {
        try {
            ProtoMessage.BandanaRequest.Builder bandanaRequestBuilder = ProtoMessage.BandanaRequest.newBuilder();
            bandanaRequestBuilder.setType(ProtoMessage.BandanaRequest.Type.SEND_GAME_END);

            ProtoMessage.ObservationData observationData = this.generateObservationData();
            bandanaRequestBuilder.setObservation(observationData);

            byte[] message = bandanaRequestBuilder.build().toByteArray();

            

            SocketClient socketClient = new SocketClient("127.0.1.1", 5000, agent2 == null? this.agent.getLogger():this.agent2.getLogger());
            byte[] response = socketClient.sendMessageAndReceiveResponse(message);

            if (response == null) {
                return;
            }

            ProtoMessage.DiplomacyGymResponse diplomacyGymResponse = ProtoMessage.DiplomacyGymResponse.parseFrom(response);

            if(diplomacyGymResponse.getType() != ProtoMessage.DiplomacyGymResponse.Type.CONFIRM) {
                throw new Exception("The response from DiplomacyGym to the end of game notification is not 'CONFIRM'.");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void endOfGame() {
        this.done = true;
        this.sendEndOfGameNotification();
        this.openAIObserver.exit();
    }

    private void generatePowerNameToIntMap() {
        this.powerNameToInt = new HashMap<>();
        this.powerNameToInt.put("NONE", 0);

        int id = 1;

        List<Power> powers = (agent2 == null)? this.agent.game.getPowers():this.agent2.getGame().getPowers();

        for(Power pow : powers) {
            powerNameToInt.put(pow.getName(), id);
            id++;
        }
    }

    public ProtoMessage.ObservationData generateObservationData() {
        ProtoMessage.ObservationData.Builder observationDataBuilder = ProtoMessage.ObservationData.newBuilder();

        Map<String, ProtoMessage.ProvinceData.Builder> nameToProvinceDataBuilder = new HashMap<>();

        int id = 1;

        // FIRST PROCESS ALL PROVINCES
        List<Province> provinces = (this.agent2 == null)? this.agent.game.getProvinces():this.agent2.getGame().getProvinces();
        for (Province p : provinces) {
            ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder();
            int isSc = p.isSC() ? 1 : 0;

            provinceDataBuilder.setId(id);
            provinceDataBuilder.setSc(isSc);

            nameToProvinceDataBuilder.put(p.getName(), provinceDataBuilder);

            id++;
        }

        // THEN ADD THE OWNERS OF EACH PROVINCE
        List<Power> powers = (this.agent2 == null)? this.agent.game.getPowers():this.agent2.getGame().getPowers();
        for (Power pow : powers) {
            for (Region r : pow.getControlledRegions()) {
                Province p = r.getProvince();

                // Get the correspondent province builder and add the current owner of the province
                ProtoMessage.ProvinceData.Builder provinceDataBuilder = nameToProvinceDataBuilder.get(p.getName());
                provinceDataBuilder.setOwner(powerNameToInt.get(pow.getName()));
            }
        }

        // ADD CREATED PROVINCES TO OBSERVATION
        for (Map.Entry<String, ProtoMessage.ProvinceData.Builder> entry : nameToProvinceDataBuilder.entrySet()) {
            observationDataBuilder.addProvinces(entry.getValue().build());
        }

        observationDataBuilder.setPreviousActionReward(this.previousActionReward);
        observationDataBuilder.setDone(this.done);

        if(this.info != null){
            observationDataBuilder.setInfo(this.info);
        }
        
        String agent_name = (this.agent2 == null)?this.agent.getName():this.agent2.getName();
        observationDataBuilder.setPlayer(powerNameToInt.get(agent_name));

        return observationDataBuilder.build();
    }

    private BasicDeal generateDeal(ProtoMessage.DealData dealData) {
        List<DMZ> dmzs = new ArrayList<>();
        List<OrderCommitment> ocs = new ArrayList<>();

        Province startProvince = this.agent.game.getProvinces().get(dealData.getStartProvince());
        Province destinationProvince = this.agent.game.getProvinces().get(dealData.getDestinationProvince());


        Order o = new MTOOrder(
                this.agent.me,
                startProvince.getRegions().get(0),
                destinationProvince.getRegions().get(0));

        OrderCommitment oc = new OrderCommitment(this.agent.game.getYear(), this.agent.game.getPhase(), o);

        ocs.add(oc);

        return new BasicDeal(ocs, dmzs);
    }
    
    private List<Order> generateOrders(ProtoMessage.OrdersData ordersData) {
        List<Order> orders = new ArrayList<>();
        System.out.println(ordersData);
        return orders;
    }

    void addReward(int reward) {
        this.previousActionReward += reward;
    }

    private void resetReward() {
        this.previousActionReward = 0;
    }

    public void finish() {
        this.done = true;
    }

    public void setInfo(String s) {
        this.info = s;
    }
}
