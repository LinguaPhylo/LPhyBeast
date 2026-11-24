package lphybeast;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public void testRootFrequencies() {
        String script = """
            data {
              L = 200;
              taxa = taxa(names=1:10);
            }
            model {
              Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);
              ψ ~ Coalescent(theta=Θ, taxa=taxa);
              rootPi ~ Dirichlet(conc=[10.0, 1.0, 1.0, 10.0]);
              D ~ PhyloCTMC(tree=ψ, L=L, Q=jukesCantor(), freq=rootPi);
            }""";
        String xml = TestUtils.lphyScriptToBEASTXML(script, "rootFreq");
        assertTrue(xml.contains("<rootFrequencies") && xml.contains("frequencies=\"@rootPi\""),
                "PhyloCTMC freq maps to the tree likelihood's rootFrequencies");

        String noRoot = TestUtils.lphyScriptToBEASTXML(String.format(LPhyScripts.simpleCoal, 10), "noRootFreq");
        assertFalse(noRoot.contains("rootFrequencies"), "no rootFrequencies without PhyloCTMC freq");
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

    /** mu must reach clock.rate of the UCRelaxedClockModel for both 1 and n partitions. */
    @Test
    public void testUCLNClockRate() {
        int ntaxa = 16;
        String xml = TestUtils.lphyScriptToBEASTXML(String.format(LPhyScripts.uclnClock, ntaxa), "uclnClock");
        assertTrue(xml.contains("UCRelaxedClockModel"), "UCRelaxedClockModel");
        assertTrue(xml.contains("clock.rate=\"@mRate\""), "clock.rate in single partition");
    }

    @Test
    public void testUCLNClockRatePartitioned() {
        int ntaxa = 16;
        String xml = TestUtils.lphyScriptToBEASTXML(String.format(LPhyScripts.uclnClockPartitioned, ntaxa), "uclnClockPart");
        assertTrue(xml.contains("UCRelaxedClockModel"), "UCRelaxedClockModel");
        assertTrue(xml.contains("clock.rate=\"@mRate\""), "clock.rate in partitioned analysis");
    }

}
