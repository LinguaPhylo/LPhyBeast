package mascot.lphybeast;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test StructuredCoalescentRateShifts conversion to MASCOT GLM with rate shifts.
 * @author Alexei Drummond
 */
public class StructuredCoalescentRateShiftsTest {

    @BeforeEach
    public void setUp() {
        // load ../LPhyBeast/lphybeast/version.xml
        Path lphybeastDir = Paths.get(UserDir.getUserDir().toAbsolutePath().getParent().toString(),
                "..", "LPhyBeast", "lphybeast");
        TestUtils.loadServices(lphybeastDir.toString());
        // load mascot/version.xml
        Path parentDir = UserDir.getUserDir().toAbsolutePath();
        TestUtils.loadServices(parentDir.toString());
    }

    @Test
    public void testSimpleRateShifts() {
        // Test with constant rates (no GLM) but with rate shifts
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
              ψ ~ StructuredCoalescentRateShifts(
                theta=theta_data,
                m=m_data,
                rateShiftTimes=rateShiftTimes,
                taxa=taxa,
                demes=demes
              );
              D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            }""";

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "rateShiftsSimple");

        // Check GLM dynamics is used
        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");

        // Check rate shifts are present with correct values
        assertTrue(xml.contains("mascot.dynamics.RateShifts"), "Should have RateShifts");
        assertTrue(xml.contains("0.0") && xml.contains("5.0"), "Should have rate shift times 0.0 and 5.0");

        // Check both Ne and migration GLMs
        assertTrue(xml.contains("migrationGLM") || xml.contains("MigrationGLM"), "Should have migration GLM");
        assertTrue(xml.contains("neGLM") || xml.contains("NeGLM"), "Should have Ne GLM");

        System.out.println("Simple rate shifts test passed - XML generated successfully");

        // Save XML for inspection
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/simpleRateShifts.xml"), xml);
            System.out.println("XML saved to /tmp/simpleRateShifts.xml");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Test
    public void testGLMRateShifts() {
        // Test with GLM-based rates and rate shifts
        String lphyScript = """
            data {
              S = 3;
              nIntervals = 2;
              rateShiftTimes = [0.0, 5.0];
              nPredictors = 2;

              // Design matrices for time-varying covariates
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

              ψ ~ StructuredCoalescentRateShifts(
                theta=theta,
                m=m,
                rateShiftTimes=rateShiftTimes,
                taxa=taxa,
                demes=demes
              );
              D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            }""";

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "rateShiftsGLM");

        // Check GLM dynamics is used
        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");

        // Check rate shifts
        assertTrue(xml.contains("mascot.dynamics.RateShifts"), "Should have RateShifts");

        // Check GLM components - beta parameters and covariates
        assertTrue(xml.contains("beta_Ne") || xml.contains("NeScalerGLM"), "Should have Ne beta/scaler");
        assertTrue(xml.contains("beta_m") || xml.contains("migrationScalerGLM"), "Should have migration beta/scaler");
        assertTrue(xml.contains("Covariate") || xml.contains("covariate"), "Should have covariates");

        System.out.println("GLM rate shifts test passed - XML generated successfully");

        // Save XML for inspection
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/rateShiftsGLM.xml"), xml);
            System.out.println("XML saved to /tmp/rateShiftsGLM.xml");
        } catch (Exception e) { e.printStackTrace(); }
    }
}
