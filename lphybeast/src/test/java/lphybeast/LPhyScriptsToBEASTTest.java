package lphybeast;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check the XML
 * @author Walter Xie
 */
public class LPhyScriptsToBEASTTest {

    @Test
    public void testSimpleCoalescent() {
        int ntaxa = 10;
        String simpleCoal = String.format(LPhyScripts.simpleCoal, ntaxa);
        String xml = TestUtils.lphyScriptToBEASTXML(simpleCoal, "simpleCoal");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        TestUtils.assertJC(xml);

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") && xml.contains("x=\"@Theta\"") &&
                xml.contains("distribution.LogNormalDistributionModel") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Coalescent\""), "Coalescent" );
        assertTrue(xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\""), "popSize" );
    }

    @Test
    public void testRelaxClock() {
        int ntaxa = 16;
        String script = String.format(LPhyScripts.relaxClock, ntaxa, ntaxa);
        String xml = TestUtils.lphyScriptToBEASTXML(script, "relaxClock");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        TestUtils.assertJC(xml);

        int dim = 2*ntaxa-2;
        assertTrue(xml.contains("id=\"branchRates\"") && xml.contains("dimension=\""+dim+"\"") &&
                xml.contains("id=\"lambda\"") && xml.contains("id=\"psi\""), "Check parameters" );

        assertTrue(xml.contains("id=\"lambda.prior\"") && xml.contains("x=\"@lambda\"") &&
                xml.contains("distribution.LogNormalDistributionModel") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"),
                "lambda prior" );
        assertTrue(xml.contains("birthDiffRate=\"@lambda\"") && xml.contains("id=\"YuleModel\""),
                "YuleModel" );

        assertTrue(xml.contains("id=\"branchRates.prior\"") && xml.contains("x=\"@branchRates\"") &&
                        xml.contains("name=\"M\">-0.25</parameter>") && xml.contains("name=\"S\">0.5</parameter>"),
                "branchRates prior" );
        assertTrue(xml.contains("<branchRateModel") && xml.contains("id=\"branchRates.model\"") &&
                        xml.contains("beast.base.evolution.branchratemodel.UCRelaxedClockModel") &&
                        xml.contains("distr=\"@LogNormalDistributionModel") &&
                        xml.contains("rates=\"@branchRates\""),
                "UCRelaxedClockModel prior" );

        //TODO operators
    }

}