package cruz.agents;

import com.google.protobuf.InvalidProtocolBufferException;
import ddejonge.bandana.negoProtocol.BasicDeal;
import ddejonge.bandana.negoProtocol.DMZ;
import ddejonge.bandana.negoProtocol.OrderCommitment;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAIAdapter {

    public static final int REJECTED_DEAL_REWARD = -5;
    public static final int ACCEPTED_DEAL_REWARD = +5;

    private OpenAINegotiator agent;
    private Map<String, Integer> powerNameToInt;

    private float previousActionReward;
    private boolean done;
    private String info;

    public boolean firstTurn;

    OpenAIAdapter(OpenAINegotiator agent) {
        this.agent = agent;

        this.resetReward();
        this.done = false;
        this.info = null;

        this.firstTurn = true;
    }

    /**
     * This function retrieves a deal from the Open AI module that is connected to the localhost on port 5000.
     *
     * @return A BasicDeal created with data from the Open AI module.
     */
    public BasicDeal getDealFromDipQ() {
        try {
            this.generatePowerNameToIntMap();

            ProtoMessage.ObservationData observationData = this.generateObservationData();
            byte[] observationByteArray = observationData.toByteArray();

            SocketClient socketClient = new SocketClient("127.0.1.1", 5000, this.agent.getLogger());
            byte[] message = socketClient.sendMessageAndReceiveResponse(observationByteArray);

            // If something went wrong with getting the message from Python module
            if(message == null) {
                return null;
            }

            ProtoMessage.DealData dealData = ProtoMessage.DealData.parseFrom(message);

            return this.generateDeal(dealData);

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void generatePowerNameToIntMap() {
        this.powerNameToInt = new HashMap<>();
        this.powerNameToInt.put("NONE", 0);

        int id = 1;

        for(Power pow : this.agent.game.getPowers()) {
            powerNameToInt.put(pow.getName(), id);
            id++;
        }
    }

    public ProtoMessage.ObservationData generateObservationData() {

        ProtoMessage.ObservationData.Builder observationDataBuilder = ProtoMessage.ObservationData.newBuilder();

        Map<String, ProtoMessage.ProvinceData.Builder> nameToProvinceDataBuilder = new HashMap<>();

        int id = 1;

        // FIRST PROCESS ALL PROVINCES
        for (Province p : this.agent.game.getProvinces()) {
            ProtoMessage.ProvinceData.Builder provinceDataBuilder = ProtoMessage.ProvinceData.newBuilder();
            int isSc = p.isSC() ? 1 : 0;

            provinceDataBuilder.setId(id);
            provinceDataBuilder.setSc(isSc);

            nameToProvinceDataBuilder.put(p.getName(), provinceDataBuilder);

            id++;
        }

        // THEN ADD THE OWNERS OF EACH PROVINCE
        for (Power pow : this.agent.game.getPowers()) {
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

        return observationDataBuilder.build();
    }

    private BasicDeal generateDeal(ProtoMessage.DealData dealData) {
        List<DMZ> dmzs = new ArrayList<>();
        List<OrderCommitment> ocs = new ArrayList<>();

        Province startProvince = this.agent.game.getProvinces().get(dealData.getStartProvince());
        Province destinationProvince = this.agent.game.getProvinces().get(dealData.getDestinationProvince());


        Order o = new MTOOrder(
                this.agent.me, this.agent.game.getRegion(startProvince.getName() + "AMY"),
                this.agent.game.getRegion(destinationProvince.getName() + "AMY"));

        OrderCommitment oc = new OrderCommitment(this.agent.game.getYear(), this.agent.game.getPhase(), o);

        ocs.add(oc);

        return new BasicDeal(ocs, dmzs);
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
