package de.aspua.framework.Unit.Controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.aspua.framework.Controller.CausalRejectionController.ClingoRemoteSolver;
import de.aspua.framework.Controller.CausalRejectionController.ELPParser;
import de.aspua.framework.Model.ASP.ELP.ELPProgram;

public class ClingoRemoteServerTest {
    
    @Test
    public void ClingoRemoteServer_Solve_EmptyParameter()
    {
        ClingoRemoteSolver solver = new ClingoRemoteSolver();
        List<String> result = solver.computeModels(null);
        assertNull(result);

        result = solver.computeModels(new ELPProgram());
        assertNull(result);
    }

    @Test
    public void ClingoRemoteServer_Solve_ValidProgram()
    {
        String programString =  "a(x) :- not -b(y, z)." + System.lineSeparator()
                            +   "-b(y, z) :- not a(x)." + System.lineSeparator()
                            +   "d :- c";
        
        ELPProgram program = new ELPParser().parseProgram(programString, null);
        ClingoRemoteSolver solver = new ClingoRemoteSolver();

        List<String> expectedAnswerSets = new ArrayList<>();
        expectedAnswerSets.add("a(x)");
        expectedAnswerSets.add("-b(y,z)");

        List<String> actualAnswerSets = solver.computeModels(program);
        assertEquals(2, actualAnswerSets.size());

        for (String expectedAnswerSet : expectedAnswerSets)
            assertTrue(actualAnswerSets.contains(expectedAnswerSet));
    }
}
