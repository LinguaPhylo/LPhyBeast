package lphybeast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Bayesian Skyline Plots
 * https://linguaphylo.github.io/tutorials/skyline-plots/
 * @author Walter Xie
 */
public class SkylinePlotsTutorialTest {

    private final int ntaxa = 63;
    private Path fPath;

    @BeforeEach
    public void setUp() {
        TestUtils.loadServices();
        fPath = TestUtils.getFileForResources("hcv.nexus");
    }

    @Test
    public void testBS() {
        final String fileStem = "hcv";
        String hcvLPhy = String.format("""
                data {
                     D = readNexus(file="%s");
                     taxa = D.taxa();
                     L = D.nchar();
                     numGroups = 4;
                     w = taxa.length()-1;
                   }
                   model {
                     π ~ Dirichlet(conc=[3.0,3.0,3.0,3.0]);
                     rates ~ Dirichlet(conc=[1.0, 2.0, 1.0, 1.0, 2.0, 1.0]);
                     Q = gtr(freq=π, rates=rates);
                     θ1 ~ LogNormal(meanlog=9.0, sdlog=2.0);
                     Θ ~ ExpMarkovChain(firstValue=θ1, n=numGroups);
                     A ~ RandomComposition(n=w, k=numGroups);
                     ψ ~ SkylineCoalescent(theta=Θ, taxa=taxa, groupSizes=A);
                     γ ~ LogNormal(meanlog=0.0, sdlog=2.0);
                     r ~ DiscretizeGamma(shape=γ, ncat=4, replicates=L);
                     D ~ PhyloCTMC(siteRates=r, Q=Q, tree=ψ, mu=0.00079);
                   }""", fPath.toAbsolutePath());

        String xml = TestUtils.lphyScriptToBEASTXML(hcvLPhy, fileStem);

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("id=\"pi\"") && xml.contains("id=\"rates\"") &&
                xml.contains("id=\"gamma\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") &&
                xml.contains("id=\"A\""), "Check parameters ID" );
        // pi_trait, I, R_trait
        assertTrue(xml.contains("id=\"MarkovChainDistribution\"") &&
                xml.contains("MarkovChainDistribution") &&
                xml.contains("groupSizes=\"@A\"") && xml.contains("popSizes=\"@Theta\"") &&
                xml.contains("spec=\"BayesianSkyline\""), "Bayesian Skyline" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"theta1.prior\"") &&
                xml.contains("arg=\"@Theta\"") && xml.contains("beastlabs.core.util.Slice") &&
                xml.contains("index=\"0\"") && xml.contains("name=\"M\">9.0</parameter>") &&
                xml.contains("name=\"S\">2.0</parameter>"), "Theta1 prior");

        assertTrue(xml.contains("x=\"@rates\"") && xml.contains("id=\"rates.prior\"") &&
                xml.contains("name=\"alpha\">1.0 2.0 1.0 1.0 2.0 1.0</parameter>"),  "GTR prior" );
        assertTrue(xml.contains("x=\"@pi\"") && xml.contains("id=\"pi.prior\"") &&
                xml.contains("name=\"alpha\">3.0 3.0 3.0 3.0</parameter>"),  "pi prior" );

        assertTrue(xml.contains("<substModel") && xml.contains("rates=\"@rates\"") &&
                xml.contains("substmodels.nucleotide.GTR"),  "GTR" );
        assertTrue(xml.contains("frequencies=\"@pi\"") && xml.contains("<frequencies"),  "frequencies" );

        assertTrue(xml.contains("name=\"clock.rate\">7.9E-4</parameter>"),  "clock rate" );

        assertTrue(xml.contains("x=\"@gamma\"") && xml.contains("name=\"M\">0.0</parameter>") &&
                xml.contains("name=\"S\">2.0</parameter>"),  "gamma shape prior" );
        assertTrue(xml.contains("gammaCategoryCount=\"4\"") && xml.contains("shape=\"@gamma\""), "SiteModel" );

        // 3 ScaleOperator, Tree scaled is replaced by BICEPS
        assertEquals(3, xml.split("BactrianScaleOperator", -1).length - 1,
                "BactrianScaleOperator" );

        assertTrue(xml.contains("Exchange") && xml.contains("BactrianSubtreeSlide") &&
                xml.contains("BactrianNodeOperator") && xml.contains("WilsonBalding"), "Tree Operator" );

       // 3 DeltaExchangeOperator
        assertEquals(3, xml.split("BactrianDeltaExchangeOperator", -1).length - 1,
                "BactrianDeltaExchangeOperator");

        assertTrue(xml.contains("chainLength=\"1000000\"") && xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"" + fileStem + ".log\"") && xml.contains("fileName=\"" + fileStem + ".trees\"") &&
                xml.contains("TreeWithMetaDataLogger") && xml.contains("mode=\"tree\"") && xml.contains("tree=\"@psi\""),
                "logger" );
    }

}