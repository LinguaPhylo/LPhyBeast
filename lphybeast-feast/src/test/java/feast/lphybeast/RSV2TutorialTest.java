package feast.lphybeast;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Time stamped data — requires feast extension (WeightedDirichlet -> Concatenate).
 * Moved from core lphybeast module.
 */
public class RSV2TutorialTest {

    private final int ntaxa = 129;
    private Path fPath;

    @BeforeEach
    public void setUp() {
        // Register feast extension services before loading core services
        Map<String, Set<String>> feastServices = Map.of(
                "lphybeast.spi.LPhyBEASTExt", Set.of("feast.lphybeast.spi.FeastLBImpl"),
                "lphybeast.spi.ValueHandler", Set.of("feast.lphybeast.FeastValueHandler")
        );
        BEASTClassLoader.classLoader.addServices("lphybeast-feast", feastServices);

        // Load core services from version.xml
        String parentDir = System.getProperty("user.dir") + "/../lphybeast";
        Path vfPath = Paths.get(parentDir, "version.xml");
        if (!Files.exists(vfPath))
            throw new IllegalArgumentException("Can't find LPhyBeast version.xml under dir : " + vfPath);
        LPhyBEASTLoader.addBEAST2Services(new String[]{vfPath.toAbsolutePath().toString()});

        fPath = Paths.get("src", "test", "resources", "RSV2.nex");
    }

    @Test
    public void testRSV2() throws IOException {
        final String fileStem = "RSV2";
        String RSV2LPhy = String.format("""
                    data {
                       options = {ageDirection="forward", ageRegex="s(\\d+)$"};
                       D = readNexus(file="%s", options=options);
                       taxa = D.taxa();
                       codon = D.charset(["3-629\\3","1-629\\3", "2-629\\3"]);
                       L = codon.nchar();
                       n=length(codon); // 3 partitions
                     }
                     model {
                       κ ~ LogNormal(meanlog=1.0, sdlog=0.5, replicates=n);
                       π ~ Dirichlet(conc=[2.0,2.0,2.0,2.0], replicates=n);
                       r ~ WeightedDirichlet(conc=rep(element=1.0, times=n), weights=L);
                       μ ~ LogNormal(meanlog=-5.0, sdlog=1.25);
                       Θ ~ LogNormal(meanlog=3.0, sdlog=2.0);
                       ψ ~ Coalescent(taxa=taxa, theta=Θ);
                       codon ~ PhyloCTMC(L=L, Q=hky(kappa=κ, freq=π, meanRate=r), mu=μ, tree=ψ);
                     }""", fPath.toAbsolutePath());

        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(RSV2LPhy, fileStem);

        assertNotNull(xml, "XML");
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"), "<beast></beast>");
        assertTrue(xml.contains("<run") && xml.contains("id=\"MCMC\""), "MCMC tag");

        // 3 alignments
        assertEquals(3, xml.split("spec=\"Alignment\"", -1).length - 1, "Codon alignment");

        assertTrue(xml.contains("id=\"pi_0\"") && xml.contains("id=\"pi_1\"") && xml.contains("id=\"pi_2\"") &&
                xml.contains("id=\"r_0\"") && xml.contains("id=\"r_1\"") && xml.contains("id=\"r_2\"") &&
                xml.contains("id=\"mu\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") &&
                xml.contains("id=\"kappa\""), "Check parameters ID");

        assertTrue(xml.contains("<trait") &&
                xml.contains("id=\"TraitSet\"") && xml.contains("traitname=\"date-backward\""), "TraitSet");

        assertTrue(xml.contains("id=\"WeightedDirichlet\"") &&
                xml.contains("<weights") && xml.contains("dimension=\"3\"") &&
                xml.contains("estimate=\"false\">209 210 210</weights>"), "r.prior WeightedDirichlet");

        // 3 TreeLikelihood
        assertEquals(3, xml.split("ThreadedTreeLikelihood", -1).length - 1, "Tree Likelihood");

        assertTrue(xml.contains("BactrianUpDownOperator") &&
                xml.contains("<up") && xml.contains("<down"), "BactrianUpDownOperator");
        // 4 DeltaExchangeOperator
        assertEquals(4, xml.split("BactrianDeltaExchangeOperator", -1).length - 1, "BactrianDeltaExchangeOperator");
    }
}
