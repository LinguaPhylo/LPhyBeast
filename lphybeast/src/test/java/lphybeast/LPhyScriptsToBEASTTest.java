package lphybeast;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check the XML
 * @author Walter Xie
 */
public class LPhyScriptsToBEASTTest {

    @BeforeEach
    void setUp() {
        TestUtils.loadServices();
    }

    @Test
    public void testSimpleCoalescent() {
        int ntaxa = 10;
        String simpleCoal = String.format(LPhyScripts.simpleCoal, ntaxa);
        String xml = TestUtils.lphyScriptToBEASTXML(simpleCoal, "simpleCoal");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        TestUtils.assertJC(xml);

        assertTrue(xml.contains("id=\"Theta.prior\"") && xml.contains("param=\"@Theta\"") &&
                xml.contains("distribution.LogNormal"), "Theta prior");

        assertTrue(xml.contains("id=\"Coalescent\""), "Coalescent");
        assertTrue(xml.contains("popSize=\"@Theta\""), "popSize");
    }

    @Test
    public void testRelaxClock() {
        int ntaxa = 16;
        String script = String.format(LPhyScripts.relaxClock, ntaxa, ntaxa);
        String xml = TestUtils.lphyScriptToBEASTXML(script, "relaxClock");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        TestUtils.assertJC(xml);

        assertTrue(xml.contains("id=\"branchRates\"") &&
                xml.contains("id=\"lambda\"") && xml.contains("id=\"psi\""), "Check parameters");

        assertTrue(xml.contains("id=\"lambda.prior\"") && xml.contains("param=\"@lambda\"") &&
                xml.contains("distribution.LogNormal"), "lambda prior");
        assertTrue(xml.contains("birthDiffRate=\"@lambda\"") && xml.contains("id=\"YuleModel\""),
                "YuleModel");

        assertTrue(xml.contains("id=\"branchRates.prior\"") &&
                xml.contains("distribution.IID"), "branchRates IID prior");
        assertTrue(xml.contains("<branchRateModel") && xml.contains("id=\"branchRates.model\"") &&
                        xml.contains("UCRelaxedClockModel"),
                "UCRelaxedClockModel");

        //TODO operators
    }

}
