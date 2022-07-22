package sa.lphybeast.tobeast.generators;

import lphybeast.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XML can be used to sample from SA prior.
 * @author Walter Xie
 */
class SimFBDAgeToBEASTTest {

    private String simFBDAge = """
            tree ~ SimFBDAge(lambda=1, mu=0.6, frac=0.3, psi=0.4, originAge=4);
            daCount = tree.directAncestorCount();""";
    //TODO It requires 2nd cmd to make "tree" added into List<StateNode> state in BEASTContext

    @Test
    public void testSimFBDAge() {
        String xml = TestUtils.lphyScriptToBEASTXML(simFBDAge, "simFBDAge");

        assertFalse(xml.contains("<data") && xml.contains("</data>"), "No alignment tag");

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet" );
        assertTrue(xml.contains("id=\"SABirthDeathModel\"") &&
                xml.contains("conditionOnSampling=\"true\"") && xml.contains("origin=\"@tree.origin\"") &&
                xml.contains("spec=\"beast.evolution.speciation.SABirthDeathModel\""), "SABirthDeathModel");

        assertTrue(xml.contains("\"birthRate\">1.0</parameter>") &&
                xml.contains("\"deathRate\">0.6</parameter>") && xml.contains("\"samplingRate\">0.4</parameter>") &&
                xml.contains("\"removalProbability\">0.0</parameter>") && xml.contains("\"rho\">0.3</parameter>"),
                "SABirthDeath parameters");

        // operators
        assertEquals(8,xml.split("<operator", -1).length - 1, "operators" );
        assertTrue(xml.contains("spec=\"SAScaleOperator\"") && xml.contains("spec=\"SAExchange\"") &&
                xml.contains("spec=\"SAUniform\"") && xml.contains("spec=\"SAWilsonBalding\"") &&
                xml.contains("spec=\"LeafToSampledAncestorJump\""), "SA operators");

    }

}