package mascot.lphybeast;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test {@link lphy.base.evolution.coalescent.StructuredCoalescentSkyline} conversion
 * to a Mascot {@link mascot.dynamics.StructuredSkyline}. Covers both interpolation
 * modes (constant → StructuredSkygrid, linear → Skygrowth) and the two M input
 * shapes (vectorised replicates and literal Double[]).
 */
public class StructuredCoalescentSkylineTest {

    @BeforeEach
    public void setUp() {
        Path lphybeastDir = Paths.get(UserDir.getUserDir().toAbsolutePath().getParent().toString(),
                "..", "LPhyBeast", "lphybeast");
        TestUtils.loadServices(lphybeastDir.toString());
        Path parentDir = UserDir.getUserDir().toAbsolutePath();
        TestUtils.loadServices(parentDir.toString());
    }

    /**
     * Default (constant) interpolation: converter should wire
     * mascot.parameterdynamics.StructuredSkygrid as the per-deme Ne dynamics.
     */
    @Test
    public void testSkylineConstantInterpolation() {
        String lphyScript = """
            K = 2;
            n = 2;
            stepSd = 1.0;
            rateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            M ~ LogNormal(meanlog=0.0, sdlog=1.0, replicates=K*(K-1));

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, M=M, rateShifts=rateShifts,
              taxa=taxa, demes=demes);

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineConstant");

        assertTrue(xml.contains("mascot.dynamics.StructuredSkyline"),
                "Should use StructuredSkyline dynamics");
        assertTrue(xml.contains("mascot.parameterdynamics.StructuredSkygrid"),
                "Constant mode should use StructuredSkygrid per-deme");
        assertTrue(!xml.contains("mascot.parameterdynamics.Skygrowth"),
                "Constant mode should NOT use Skygrowth");
        assertTrue(xml.contains("SkylineNe.A"), "Should assign Ne id for deme A");
        assertTrue(xml.contains("SkylineNe.B"), "Should assign Ne id for deme B");
        assertTrue(xml.contains("forwardsMigration=\"@M\""), "Should wire M into forwardsMigration");
    }

    /**
     * Linear interpolation mode: converter should wire
     * mascot.parameterdynamics.Skygrowth as the per-deme Ne dynamics.
     */
    @Test
    public void testSkylineLinearInterpolation() {
        String lphyScript = """
            K = 2;
            n = 2;
            stepSd = 1.0;
            rateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            M ~ LogNormal(meanlog=0.0, sdlog=1.0, replicates=K*(K-1));

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, M=M, rateShifts=rateShifts,
              taxa=taxa, demes=demes,
              interpolation="linear");

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineLinear");

        assertTrue(xml.contains("mascot.dynamics.StructuredSkyline"),
                "Should use StructuredSkyline dynamics");
        assertTrue(xml.contains("mascot.parameterdynamics.Skygrowth"),
                "Linear mode should use Skygrowth per-deme");
        assertTrue(!xml.contains("mascot.parameterdynamics.StructuredSkygrid"),
                "Linear mode should NOT use StructuredSkygrid");
        assertTrue(xml.contains("SkylineNe.A"), "Should assign Ne id for deme A");
        assertTrue(xml.contains("SkylineNe.B"), "Should assign Ne id for deme B");
    }

    /**
     * Alternative path: M as raw data (a literal Double[]) — produces one flat
     * RealParameter with no upstream generator. Ensures the converter accepts a
     * direct RealParameter without wrapping in CompoundRealParameter.
     */
    @Test
    public void testSkylineFlatMPath() {
        String lphyScript = """
            K = 2;
            n = 2;
            stepSd = 1.0;
            rateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            M = [0.1, 0.2];

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, M=M, rateShifts=rateShifts,
              taxa=taxa, demes=demes);

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineFlatM");

        assertTrue(xml.contains("mascot.dynamics.StructuredSkyline"),
                "Should use StructuredSkyline dynamics");
        // Default interpolation is "constant" → StructuredSkygrid.
        assertTrue(xml.contains("mascot.parameterdynamics.StructuredSkygrid"),
                "Should have per-deme StructuredSkygrid objects");
    }

    /**
     * Time-varying migration with linear interpolation: converter wires
     * StructuredMigrationSkyline with K per-deme Skygrowth for Ne and K*(K-1)
     * per-pair Skygrowth for migration.
     */
    @Test
    public void testSkylineTimeVaryingMigration_linear() {
        String lphyScript = """
            K = 2;
            n = 2;
            n_M = 2;
            stepSd = 1.0;
            rateShifts          = [0.0, 0.5];
            migrationRateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            initM ~ Normal(mean=0.0, sd=1.0, replicates=K*(K-1));
            logM  ~ GaussianRandomWalk(firstValue=initM, sd=stepSd, n=n_M);

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, logM=logM,
              rateShifts=rateShifts, migrationRateShifts=migrationRateShifts,
              taxa=taxa, demes=demes,
              interpolation="linear");

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineTimeVaryingM_linear");

        assertTrue(xml.contains("mascot.dynamics.StructuredMigrationSkyline"),
                "Time-varying migration should use StructuredMigrationSkyline");
        assertTrue(!xml.contains("mascot.dynamics.StructuredSkyline\""),
                "Should not emit plain StructuredSkyline when migration is time-varying");
        assertTrue(xml.contains("mascot.parameterdynamics.Skygrowth"),
                "Linear mode should use Skygrowth for Ne");
        assertTrue(xml.contains("SkylineNe.A"), "Should assign per-deme Ne IDs");
        assertTrue(xml.contains("SkylineNe.B"), "Should assign per-deme Ne IDs");
        assertTrue(xml.contains("SkylineM.A_to_B"), "Should assign per-pair M ID A→B");
        assertTrue(xml.contains("SkylineM.B_to_A"), "Should assign per-pair M ID B→A");
    }

    /**
     * Time-varying migration with constant interpolation. In this path Ne and
     * migration both use Skygrowth (the only Mascot NeDynamics that supports
     * independent per-trajectory rateShifts under StructuredMigrationSkyline).
     * With interpolation="constant", LPhy simulator and Mascot agree at knot
     * times but differ between knots.
     */
    @Test
    public void testSkylineTimeVaryingMigration_constant() {
        String lphyScript = """
            K = 2;
            n = 2;
            n_M = 2;
            stepSd = 1.0;
            rateShifts          = [0.0, 0.5];
            migrationRateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            initM ~ Normal(mean=0.0, sd=1.0, replicates=K*(K-1));
            logM  ~ GaussianRandomWalk(firstValue=initM, sd=stepSd, n=n_M);

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, logM=logM,
              rateShifts=rateShifts, migrationRateShifts=migrationRateShifts,
              taxa=taxa, demes=demes,
              interpolation="constant");

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineTimeVaryingM_constant");

        assertTrue(xml.contains("mascot.dynamics.StructuredMigrationSkyline"),
                "Time-varying migration should use StructuredMigrationSkyline");
        assertTrue(xml.contains("mascot.parameterdynamics.Skygrowth"),
                "Ne and migration should use Skygrowth in time-varying migration mode");
        assertTrue(!xml.contains("mascot.parameterdynamics.StructuredSkygrid"),
                "StructuredSkygrid not used in time-varying migration mode");
        assertTrue(xml.contains("SkylineM.A_to_B"), "Should assign per-pair M ID A→B");
        assertTrue(xml.contains("SkylineM.B_to_A"), "Should assign per-pair M ID B→A");
    }

    /**
     * Independent Ne and migration grids: n_Ne = 4 Ne epochs (classic skyline
     * resolution), n_M = 2 migration epochs (Toby's expansion vs endemic
     * split). Outer integration grid is the union.
     */
    @Test
    public void testSkylineTimeVaryingMigration_independentGrids() {
        String lphyScript = """
            K = 2;
            n = 4;
            n_M = 2;
            stepSd = 1.0;
            rateShifts          = [0.0, 0.25, 0.5, 0.75];
            migrationRateShifts = [0.0, 0.5];

            init  ~ Normal(mean=0.0, sd=1.0, replicates=K);
            logNe ~ GaussianRandomWalk(firstValue=init, sd=stepSd, n=n);

            initM ~ Normal(mean=0.0, sd=1.0, replicates=K*(K-1));
            logM  ~ GaussianRandomWalk(firstValue=initM, sd=stepSd, n=n_M);

            taxa = taxa(names=["t1_A","t2_A","t3_A","t4_A","t5_A","t1_B","t2_B","t3_B","t4_B","t5_B"]);
            demes = ["A","A","A","A","A","B","B","B","B","B"];

            ψ ~ StructuredCoalescentSkyline(
              logNe=logNe, logM=logM,
              rateShifts=rateShifts, migrationRateShifts=migrationRateShifts,
              taxa=taxa, demes=demes,
              interpolation="linear");

            D ~ PhyloCTMC(L=200, Q=jukesCantor(), tree=ψ);
            """;

        String xml = TestUtils.lphyScriptToBEASTXML(lphyScript, "skylineTimeVaryingM_independent");

        assertTrue(xml.contains("mascot.dynamics.StructuredMigrationSkyline"),
                "Should use StructuredMigrationSkyline");
        assertTrue(xml.contains("id=\"SkygrowthRateShifts\""), "Ne shifts object emitted");
        assertTrue(xml.contains("id=\"MigrationRateShifts\""), "Migration shifts object emitted");
        assertTrue(xml.contains("SkylineNe.A"), "per-deme Ne A");
        assertTrue(xml.contains("SkylineNe.B"), "per-deme Ne B");
        assertTrue(xml.contains("SkylineM.A_to_B"), "per-pair M A→B");
        assertTrue(xml.contains("SkylineM.B_to_A"), "per-pair M B→A");
    }
}
