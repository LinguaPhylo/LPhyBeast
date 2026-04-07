package flc.lphybeast;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test LocalClock conversion to FlexibleLocalClockModel.
 */
public class LocalClockTest {

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-flc", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("flc.lphybeast.spi.FLCLBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testLocalClock() throws IOException {
        String lphyScript = """
            data {
              L = 200;
              taxa = taxa(names=["A", "B", "C", "D", "E", "F"]);
            }
            model {
              \u0398 ~ LogNormal(meanlog=1.0, sdlog=1.0);
              \u03c8 ~ Coalescent(theta=\u0398, taxa=taxa);

              clade1 = mrca(tree=\u03c8, taxa=["A", "B", "C"]);

              rootRate ~ LogNormal(meanlog=-2.0, sdlog=0.5);
              cladeRate ~ LogNormal(meanlog=-1.0, sdlog=0.5);

              branchRates = localClock(tree=\u03c8, clades=[clade1],
                  cladeRates=[cladeRate], rootRate=rootRate, includeStem=true);

              D ~ PhyloCTMC(L=L, Q=jukesCantor(), tree=\u03c8, branchRates=branchRates);
            }""";

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(lphyScript, "localClock");
        assertNotNull(xml, "XML should not be null");

        // basic BEAST XML structure
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"), "<beast>");
        assertTrue(xml.contains("id=\"MCMC\""), "MCMC");

        // FLC model
        assertTrue(xml.contains("mf.beast.evolution.branchratemodel.FlexibleLocalClockModel"),
                "Should contain FlexibleLocalClockModel");
        assertTrue(xml.contains("mf.beast.evolution.branchratemodel.StrictLineageClockModel"),
                "Should contain StrictLineageClockModel for root clock");
        assertTrue(xml.contains("mf.beast.evolution.branchratemodel.StrictCladeModel"),
                "Should contain StrictCladeModel for clade clock");

        // clock.rate inputs
        assertTrue(xml.contains("rootClockModel"), "Should have rootClockModel input");
        assertTrue(xml.contains("cladeClockModel"), "Should have cladeClockModel input");
    }
}
