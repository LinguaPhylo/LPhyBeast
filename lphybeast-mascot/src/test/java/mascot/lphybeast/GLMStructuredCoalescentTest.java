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
 * Test GLM-based structured coalescent conversion to MASCOT GLM.
 * @author Alexei Drummond
 */
public class GLMStructuredCoalescentTest {

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-mascot", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("mascot.lphybeast.spi.MascotLBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testGLMMigrationOnly() throws IOException {
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

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(glmMigrationLPhy, "glmMigration");
        assertNotNull(xml, "XML");

        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");
        assertTrue(xml.contains("migrationGLM"), "Should have migrationGLM");
        assertTrue(xml.contains("neGLM") || xml.contains("NeGLM"), "Should have neGLM");
    }

    @Test
    public void testGLMNeAndMigration() throws IOException {
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

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(glmFullLPhy, "glmFull");
        assertNotNull(xml, "XML");

        assertTrue(xml.contains("mascot.dynamics.GLM"), "Should use GLM dynamics");
        assertTrue(xml.contains("migrationGLM"), "Should have migrationGLM");
        assertTrue(xml.contains("neGLM") || xml.contains("NeGLM"), "Should have neGLM");
    }
}
