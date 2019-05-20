package cruz.agents;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MakeWeightsClass {
    static ArrayList<ArrayList<ArrayList<Double>>> listOfWeightMatrices = new ArrayList<>();

    public static void main (String args[]){
        MakeWeightsClass mwc = new MakeWeightsClass();
        setWeightsMatrices();
        mwc.createIt();
    }

    public void createIt() {
        try {
            for (int i = 0; i < listOfWeightMatrices.size(); i++) {
                String packagePath = "src/main/java/cruz/anacUtils";
                String className = "MyWeightsMatrix" + i;
                String classSource = packagePath + "/" + className + ".java";

                FileWriter aWriter = new FileWriter(classSource, false);
                aWriter.write("package cruz.anacUtils;\n");
                aWriter.write("import java.util.ArrayList;\n" +
                        "import java.util.List;\n");
                aWriter.write("public class "+ className + "{\n");

                ArrayList<ArrayList<Double>> currentMatrix = listOfWeightMatrices.get(i);
                aWriter.write("\tstatic double[][] matrix = {\n");

                for(int j = 0; j < currentMatrix.size(); j++) {
                    aWriter.write("{");
                    ArrayList<Double> currentRow = currentMatrix.get(j);

                    for(int k = 0; k < currentRow.size(); k++) {
                        aWriter.write(currentRow.get(k).toString());

                        if(k != currentRow.size() - 1) {
                            aWriter.write(", ");
                        }
                    }

                    aWriter.write("}");

                    if(j != currentMatrix.size() - 1) {
                        aWriter.write(",\n");
                    }
                }

                aWriter.write("};\n");

                aWriter.write("}\n");
                aWriter.flush();
                aWriter.close();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void setWeightsMatrices() {
        Gson gson = new Gson();
        String jsonPath = "/home/jazz/Projects/FEUP/dip-q/agents/dip-q-brain/dip_q_brain/path.json";
        Type type = new TypeToken<ArrayList<ArrayList<Object>>>() {}.getType();
        try {
            JsonReader reader = new JsonReader(new FileReader(jsonPath));
            ArrayList<ArrayList<Object>> listOfJsonLists = gson.fromJson(reader, type);

            for(ArrayList<Object> element: listOfJsonLists) {
                Object valueOfM = element.get(0);
                boolean isWeightMatrix = false; // if it's not a weight matrix, it's a result matrix

                if(valueOfM instanceof List<?>) {
                    isWeightMatrix = true;
                }

                if(isWeightMatrix) {
                    listOfWeightMatrices.add((ArrayList<ArrayList<Double>>) convertObjectToList(element));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<?> convertObjectToList(Object obj) {
        List<?> list = new ArrayList<>();
        if (obj.getClass().isArray()) {
            list = Arrays.asList((Object[])obj);
        } else if (obj instanceof Collection) {
            list = new ArrayList<>((Collection<?>)obj);
        }
        return list;
    }

}