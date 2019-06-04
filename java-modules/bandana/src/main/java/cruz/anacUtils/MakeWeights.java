package cruz.anacUtils;

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

@SuppressWarnings("Duplicates")

public class MakeWeights {
    static ArrayList<ArrayList<ArrayList<Double>>> listOfWeightMatrices = new ArrayList<>();

    public static void main (String args[]){
        setWeightsMatrices();
        createIt();
    }

    public static void createIt() {
        try {
            for (int i = 0; i < listOfWeightMatrices.size(); i++) {
                String packagePath = "src/main/java/cruz/anacUtils";

                if (i < 2) {
                    int numOfFiles = 0;
                    int maxNumOfRows = 100;
                    int currentNumOfRows = 0;
                    boolean finishedProcessing = false;

                    while(!finishedProcessing) {

                        String className = "MyWeightsMatrix" + i + "" + numOfFiles;
                        String classSource = packagePath + "/" + className + ".java";
                        FileWriter aWriter = new FileWriter(classSource, false);
                        aWriter.write("package cruz.anacUtils;\n");
                        aWriter.write("public class " + className + "{\n");

                        ArrayList<ArrayList<Double>> currentMatrix = listOfWeightMatrices.get(i);
                        aWriter.write("\tstatic double[][] matrix = {\n");

                        while (currentNumOfRows < currentMatrix.size()) {
                            aWriter.write("{");
                            ArrayList<Double> currentRow = currentMatrix.get(currentNumOfRows);

                            for (int k = 0; k < currentRow.size(); k++) {
                                aWriter.write(currentRow.get(k).toString());

                                if (k != currentRow.size() - 1) {
                                    aWriter.write(", ");
                                }
                            }

                            aWriter.write("}");

                            if (currentNumOfRows != currentMatrix.size() - 1) {
                                aWriter.write(",\n");
                            }

                            currentNumOfRows++;

                            if(currentNumOfRows % maxNumOfRows == 0) {
                                break;
                            }

                            if(currentNumOfRows == currentMatrix.size() - 1) {
                                finishedProcessing = true;
                            }
                        }

                        aWriter.write("};\n");

                        aWriter.write("}\n");
                        aWriter.flush();
                        aWriter.close();

                        numOfFiles++;
                    }
                }
                else {
                    String className = "MyWeightsMatrix" + i + "0";
                    String classSource = packagePath + "/" + className + ".java";
                    FileWriter aWriter = new FileWriter(classSource, false);
                    aWriter.write("package cruz.anacUtils;\n");
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

            // The order is pi layer, vf layer, pi layer, vf layer, ...
            // Check the extractor.py file for details
            // We want the policy weights, which correspond to the ACTOR (from GitHub issue)
            // "The “Critic” estimates the value function. This could be the action-value (the Q value) or state-value (the V value).
            // The “Actor” updates the policy distribution in the direction suggested by the Critic (such as with policy gradients)." Source: https://towardsdatascience.com/understanding-actor-critic-methods-931b97b6df3f

            boolean isPiLayer = true;
            int index = 0;

            for(ArrayList<Object> element: listOfJsonLists) {
                if(index == 0 || index == 4 || index == 10) {
                    listOfWeightMatrices.add((ArrayList<ArrayList<Double>>) convertObjectToList(element));
                }
                index++;
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
