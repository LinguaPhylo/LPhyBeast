package mascot.lphybeast;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test GLM-based structured coalescent conversion to MASCOT GLM.
 * @author Alexei Drummond
 */
public class GLMStructuredCoalescentTest {

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
    public void testGLMMigrationOnly() {
        // Test GLM for migration rates only (constant Ne)
        String glmMigrationLPhy = """
            data {
              S = 3;
              nPredictors = 2;
              X = [[0.5, 1.0],
                   [2.0, 0.0],
                   [0.5, 1.0],
                   [1.5, 0.0],
                   [2.0, 0.0],
                   [1.5, 0.0]];
              // Taxa with deme assignments for 3 demes, 5 samples each
              taxa = taxa(names=["A1","A2","A3","A4","A5","B1","B2","B3","B4","B5","C1","C2","C3","C4","C5"]);
              demes = ["A","A","A","A","A","B","B","B","B","B","C","C","C","C","C"];
            }
            model {
              beta ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
              migrationScale ~ LogNormal(meanlog=-2.0, sdlog=1.0);
              m = generalLinearFunction(beta=beta, x=X, link="log", scale=migrationScale);
              Theta ~ LogNormal(meanlog=-3.0, sdlog=1.0, replicates=S);
              M = migrationMatrix(theta=Theta, m=m);
              psi ~ StructuredCoalescent(M=M, taxa=taxa, demes=demes, sort=true);
              D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=psi);
            }""";

        String xml = TestUtils.lphyScriptToBEASTXML(glmMigrationLPhy, "glmMigration");

        // Check GLM dynamics is used
        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");
        assertTrue(xml.contains("migrationGLM"), "Should have migrationGLM");
        assertTrue(xml.contains("neGLM") || xml.contains("NeGLM"), "Should have neGLM");

        // Check migration GLM components
        assertTrue(xml.contains("migrationScalerGLM") || xml.contains("id=\"beta\""),
                "Should have migration scaler/beta parameter");
        assertTrue(xml.contains("migrationClockGLM") || xml.contains("id=\"migrationScale\""),
                "Should have migration clock parameter");

        // Check covariates
        assertTrue(xml.contains("Covariate") || xml.contains("covariate"),
                "Should have covariates");

        System.out.println("GLM Migration test passed - XML generated successfully");
    }

    @Test
    public void testGLMNeAndMigration() {
        // Test GLM for both Ne and migration rates
        String glmFullLPhy = """
            data {
              S = 3;
              nPredictors = 2;
              X_Ne = [[2.0, 0.8],
                      [1.0, 0.3],
                      [1.5, 0.6]];
              X_m = [[0.5, 0.9],
                     [2.0, 0.2],
                     [0.5, 0.9],
                     [1.5, 0.4],
                     [2.0, 0.2],
                     [1.5, 0.4]];
              // Taxa with deme assignments for 3 demes, 5 samples each
              taxa = taxa(names=["A1","A2","A3","A4","A5","B1","B2","B3","B4","B5","C1","C2","C3","C4","C5"]);
              demes = ["A","A","A","A","A","B","B","B","B","B","C","C","C","C","C"];
            }
            model {
              beta_Ne ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
              Ne_scale ~ LogNormal(meanlog=0.0, sdlog=1.0);
              Theta = generalLinearFunction(beta=beta_Ne, x=X_Ne, link="log", scale=Ne_scale);

              beta_m ~ Normal(mean=0.0, sd=1.0, replicates=nPredictors);
              m_scale ~ LogNormal(meanlog=-2.0, sdlog=1.0);
              m = generalLinearFunction(beta=beta_m, x=X_m, link="log", scale=m_scale);

              M = migrationMatrix(theta=Theta, m=m);
              psi ~ StructuredCoalescent(M=M, taxa=taxa, demes=demes, sort=true);
              D ~ PhyloCTMC(L=500, Q=jukesCantor(), tree=psi);
            }""";

        String xml = TestUtils.lphyScriptToBEASTXML(glmFullLPhy, "glmFull");

        // Check GLM dynamics is used
        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");

        // Check both Ne and migration GLMs
        assertTrue(xml.contains("migrationGLM"), "Should have migrationGLM");
        assertTrue(xml.contains("neGLM") || xml.contains("NeGLM"), "Should have neGLM");

        // Check Ne GLM components
        assertTrue(xml.contains("neScalerGLM") || xml.contains("id=\"beta_Ne\""),
                "Should have Ne scaler/beta parameter");
        assertTrue(xml.contains("neClockGLM") || xml.contains("id=\"Ne_scale\""),
                "Should have Ne clock parameter");

        // Check migration GLM components
        assertTrue(xml.contains("migrationScalerGLM") || xml.contains("id=\"beta_m\""),
                "Should have migration scaler/beta parameter");
        assertTrue(xml.contains("migrationClockGLM") || xml.contains("id=\"m_scale\""),
                "Should have migration clock parameter");

        System.out.println("Full GLM test passed - XML generated successfully");
    }
}
