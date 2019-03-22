package cruz.agents;

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

    private Map<String, Integer> powerNameToInt;
    private OpenAINegotiator agent;

    public OpenAIAdapter(OpenAINegotiator agent) {
        this.agent = agent;
        this.generatePowerNameToIntMap();
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

        return observationDataBuilder.build();
    }

    public BasicDeal generateDeal(ProtoMessage.DealData dealData) {
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
}
