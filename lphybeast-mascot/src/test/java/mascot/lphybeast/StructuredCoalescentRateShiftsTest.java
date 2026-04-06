package mascot.lphybeast;

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
 * Test StructuredCoalescentRateShifts conversion to MASCOT GLM with rate shifts.
 * @author Alexei Drummond
 */
public class StructuredCoalescentRateShiftsTest {

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-mascot", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("mascot.lphybeast.spi.MascotLBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testSimpleRateShifts() throws IOException {
        String lphyScript = """
            data {
              S = 3;
              nIntervals = 2;
              rateShiftTimes = [0.0, 5.0];

              taxa = taxa(names=["A1","A2","A3","B1","B2","B3","C1","C2","C3"]);
              demes = ["A","A","A","B","B","B","C","C","C"];

              // theta: nIntervals * nDemes = 6 values
              theta_data = [10.0, 8.0, 12.0, 5.0, 4.0, 6.0];

              // m: nIntervals * nMigRates = 2 * 6 = 12 values
              m_data = [0.2, 0.1, 0.2, 0.15, 0.1, 0.15, 0.05, 0.02, 0.05, 0.03, 0.02, 0.03];
            }
            model {
              psi ~ StructuredCoalescentRateShifts(
                theta=theta_data,
                m=m_data,
                rateShiftTimes=rateShiftTimes,
                taxa=taxa,
                demes=demes
              );
              D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=psi);
            }""";

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(lphyScript, "rateShiftsSimple");
        assertNotNull(xml, "XML");

        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");
        assertTrue(xml.contains("mascot.dynamics.RateShifts"), "Should have RateShifts");
    }

    @Test
    public void testGLMRateShifts() throws IOException {
        String lphyScript = """
            data {
              S = 3;
              nIntervals = 2;
              rateShiftTimes = [0.0, 5.0];
              nPredictors = 2;

              X_Ne = [[2.0, 0.8], [1.5, 0.5], [1.8, 0.6],
                      [2.0, 0.2], [1.5, 0.1], [1.8, 0.15]];
              X_m = [[0.5, 0.9], [2.0, 0.5], [0.5, 0.9], [1.5, 0.6], [2.0, 0.5], [1.5, 0.6],
                     [0.5, 0.3], [2.0, 0.1], [0.5, 0.3], [1.5, 0.2], [2.0, 0.1], [1.5, 0.2]];

              taxa = taxa(names=["A1","A2","A3","B1","B2","B3","C1","C2","C3"]);
              demes = ["A","A","A","B","B","B","C","C","C"];
            }
            model {
              beta_Ne ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
              Ne_scale ~ LogNormal(meanlog=0.0, sdlog=1.0);
              theta = generalLinearFunction(beta=beta_Ne, x=X_Ne, link="log", scale=Ne_scale);

              beta_m ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
              m_scale ~ LogNormal(meanlog=-2.0, sdlog=1.0);
              m = generalLinearFunction(beta=beta_m, x=X_m, link="log", scale=m_scale);

              psi ~ StructuredCoalescentRateShifts(
                theta=theta,
                m=m,
                rateShiftTimes=rateShiftTimes,
                taxa=taxa,
                demes=demes
              );
              D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=psi);
            }""";

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(lphyScript, "rateShiftsGLM");
        assertNotNull(xml, "XML");

        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");
        assertTrue(xml.contains("mascot.dynamics.RateShifts"), "Should have RateShifts");
    }
}
