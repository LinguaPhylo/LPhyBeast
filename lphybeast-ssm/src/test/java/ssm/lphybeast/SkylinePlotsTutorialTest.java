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
        // Spec BayesianSkyline
        assertTrue(xml.contains("popSizes=\"@Theta\"") &&
                xml.contains("spec=\"beast.base.spec.evolution.tree.coalescent.BayesianSkyline\""), "Bayesian Skyline" );

        // Theta1 prior: spec LogNormal with ScalarSlice into Theta[0]
        assertTrue(xml.contains("id=\"theta1.prior\"") &&
                xml.contains("spec=\"beast.base.spec.inference.distribution.LogNormal\""), "Theta1 prior");

        // Spec Dirichlet distributions (no Prior wrapper)
        assertTrue(xml.contains("id=\"rates.prior\"") &&
                xml.contains("spec=\"beast.base.spec.inference.distribution.Dirichlet\""),  "GTR prior" );
        assertTrue(xml.contains("id=\"pi.prior\"") &&
                xml.contains("spec=\"beast.base.spec.inference.distribution.Dirichlet\""),  "pi prior" );

        assertTrue(xml.contains("substmodels.nucleotide.GTR"),  "GTR" );

        // Gamma shape prior: spec LogNormal
        assertTrue(xml.contains("id=\"gamma.prior\"") &&
                xml.contains("spec=\"beast.base.spec.inference.distribution.LogNormal\""),  "gamma shape prior" );
        assertTrue(xml.contains("gammaCategoryCount=\"4\"") && xml.contains("shape=\"@gamma\""), "SiteModel" );

        // 1 ScaleOperator (Theta via spec ScaleOperator) + tree operators via BICEPS
        // gamma and theta1 get spec ScaleOperator
        assertTrue(xml.contains("spec=\"beast.base.spec.inference.operator.ScaleOperator\""), "spec ScaleOperator");

        assertTrue(xml.contains("Exchange") && xml.contains("BactrianSubtreeSlide") &&
                xml.contains("BactrianNodeOperator") && xml.contains("WilsonBalding"), "Tree Operator" );

        // 3 DeltaExchangeOperators: pi, rates (SimplexParam), A (IntSimplexParam)
        assertTrue(xml.contains("spec=\"beast.base.spec.inference.operator.DeltaExchangeOperator\""),
                "spec DeltaExchangeOperator");

        assertTrue(xml.contains("chainLength=\"1000000\"") && xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"" + fileStem + ".log\"") && xml.contains("fileName=\"" + fileStem + ".trees\"") &&
                xml.contains("TreeWithMetaDataLogger") && xml.contains("mode=\"tree\"") && xml.contains("tree=\"@psi\""),
                "logger" );
    }

}