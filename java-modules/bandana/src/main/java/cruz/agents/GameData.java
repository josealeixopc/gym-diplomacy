package cruz.agents;

import java.io.Serializable;
import java.util.*;

public class GameData implements Serializable {

    List<ProvinceData> provinces;

    public GameData() {
        this.provinces = new ArrayList<>();
    }

    public void addProvince(ProvinceData provinceData) {
        this.provinces.add(provinceData);
    }

    public List<ProvinceData> getProvinces(){
        return this.provinces;
    }
}
