package mascot.lphybeast;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Mascot - Structured coalescent
 * https://linguaphylo.github.io/tutorials/structured-coalescent/
 * @author Walter Xie
 */
public class H3N2TutorialTest {

    private final int ntaxa = 24;
    private Path fPath;

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-mascot", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("mascot.lphybeast.spi.MascotLBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
        fPath = Paths.get("src", "test", "resources", "h3n2.nexus");
    }

    @Test
    public void testMascot() throws IOException {
        final String fileStem = "h3n2";
        String h3n2LPhy = String.format("""
                data {
                     options = {ageDirection="forward", ageRegex=".*\\|.*\\|(\\d*\\.\\d+|\\d+\\.\\d*)\\|.*$"};
                     D = readNexus(file="%s", options=options);
                     taxa = D.taxa();
                     L = D.nchar();
                     demes = split(str=D.taxaNames(), regex="\\|", i=3);
                     S = length(unique(demes));
                     dim = S*(S-1);
                   }
                   model {
                     \u03ba ~ LogNormal(meanlog=1.0, sdlog=1.25);
                     \u03c0 ~ Dirichlet(conc=[2.0,2.0,2.0,2.0]);
                     \u03b3 ~ LogNormal(meanlog=0.0, sdlog=2.0);
                     r ~ DiscretizeGamma(shape=\u03b3, ncat=4, replicates=L);
                     \u03bc ~ LogNormal(meanlog=-5.298, sdlog=0.25);
                     \u0398 ~ LogNormal(meanlog=0.0, sdlog=1.0, replicates=S);
                     m ~ Exp(mean=1.0, replicates=dim);
                     M = migrationMatrix(theta=\u0398, m=m);
                     \u03c8 ~ StructuredCoalescent(M=M, taxa=taxa, demes=demes, sort=true);
                     D ~ PhyloCTMC(siteRates=r, Q=hky(kappa=\u03ba, freq=\u03c0), mu=\u03bc, tree=\u03c8);
                   }""", fPath.toAbsolutePath());

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(h3n2LPhy, fileStem);
        assertNotNull(xml, "XML");

        // basic structure
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"), "<beast>");
        assertTrue(xml.contains("id=\"MCMC\""), "MCMC");

        // Mascot
        assertTrue(xml.contains("id=\"Mascot\"") &&
                xml.contains("mascot.distribution.Mascot") && xml.contains("mascot.dynamics.Constant") &&
                xml.contains("<dynamics") &&
                xml.contains("mascot.distribution.StructuredTreeIntervals"), "Mascot");

        // Trait set
        assertTrue(xml.contains("beast.base.evolution.tree.TraitSet") && xml.contains("traitname=\"deme\"") &&
                xml.contains("<typeTrait"), "Trait set");

        // Mascot logger
        assertTrue(xml.contains("mascot.logger.StructuredTreeLogger") &&
                xml.contains("mascot=\"@Mascot\"") &&
                xml.contains("fileName=\"" + fileStem + ".mascot.trees\""), "Mascot logger");
    }
}
