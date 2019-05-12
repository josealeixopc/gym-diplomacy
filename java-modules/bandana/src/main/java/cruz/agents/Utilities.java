package cruz.agents;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Phase;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import jdk.internal.vm.compiler.collections.Pair;

import java.util.ArrayList;
import java.util.Vector;

public class Utilities {

    public static String getAllProvincesInformation(Game game) {
        Vector<Province> provinces = game.getProvinces();

        StringBuilder sb = new StringBuilder();

        for (Province p : provinces) {
            String name = p.getName();
            Vector<Region> regions = p.getRegions();
            boolean isSupplyCenter = p.isSC();

            sb.append("Province[Name: " + name + "; Regions: ");

            for (Region r : regions) {
                sb.append(r.getName() + ", ");
            }

            sb.append("; isSupplyCenter: " + isSupplyCenter + "]\n");
        }

        return sb.toString();

    }

    public static ArrayList<Phase> getOrderedPhasesInYear() {
        ArrayList<Phase> orderedPhases = new ArrayList<>();
        orderedPhases.add(Phase.SPR);
        orderedPhases.add(Phase.SUM);
        orderedPhases.add(Phase.FAL);
        orderedPhases.add(Phase.AUT);
        orderedPhases.add(Phase.WIN);

        return orderedPhases;
    }

    public static Pair<Integer, Phase> calculatePhaseAndYear(int currentYear, Phase currentPhase, int numberOfPhasesAhead) {
        ArrayList<Phase> orderedPhases = getOrderedPhasesInYear();

        int currentPhaseIndex = orderedPhases.indexOf(currentPhase);
        int phasesAhead = numberOfPhasesAhead % orderedPhases.size();
        int yearsAhead = numberOfPhasesAhead / orderedPhases.size();

        int nextPhaseIndex = (currentPhaseIndex + phasesAhead) % orderedPhases.size();
        int nextYear = currentYear + yearsAhead;

        Phase nextPhase = orderedPhases.get(nextPhaseIndex);

        return new Pair<>(nextYear, nextPhase);
    }
}
