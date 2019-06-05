package cruz.agents;

import es.csic.iiia.fabregues.dip.board.Phase;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class UtilitiesTest {

    @Test
    public void getOrderedPhases(){
        ArrayList<Phase> orderedPhases = new ArrayList<>();
        orderedPhases.add(Phase.SPR);
        orderedPhases.add(Phase.SUM);
        orderedPhases.add(Phase.FAL);
        orderedPhases.add(Phase.AUT);
        orderedPhases.add(Phase.WIN);

        assertEquals(orderedPhases, Utilities.getOrderedPhasesInYear());
    }
}
