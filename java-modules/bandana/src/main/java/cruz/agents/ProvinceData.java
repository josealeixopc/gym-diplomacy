package cruz.agents;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ProvinceData implements Serializable {
    int owner;
    boolean sc;

    Map<String, Integer> ownerStringToInt = new HashMap<>() {{
        put("AUS", 1);
        put("ENG", 2);
        put("FRA", 3);
        put("GER", 4);
        put("ITA", 5);
        put("RUS", 6);
        put("TUR", 7);
    }};

    ProvinceData(String owner, boolean sc) {
        this.owner = this.ownerStringToInt.get(owner);
        this.sc = sc;
    }
}