package lphybeast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Time stamped data
 * https://linguaphylo.github.io/tutorials/time-stamped-data/
 * @author Walter Xie
 */
public class RSV2TutorialTest {

    private final int ntaxa = 129;
    private Path fPath;

    @BeforeEach
    public void setUp() {
        fPath = TestUtils.getFileForResources("RSV2.nex");
    }

    @Test
    public void testRSV2() {
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

        String xml = TestUtils.lphyScriptToBEASTXML(RSV2LPhy, fileStem);

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        // 3 alignments
        assertEquals(3, xml.split("spec=\"Alignment\"", -1).length - 1, "Codon alignment");

        assertTrue(xml.contains("id=\"pi_0\"") && xml.contains("id=\"pi_1\"") && xml.contains("id=\"pi_2\"") &&
                xml.contains("id=\"r_0\"") && xml.contains("id=\"r_1\"") && xml.contains("id=\"r_2\"") &&
                xml.contains("id=\"mu\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") &&
                xml.contains("id=\"kappa\""), "Check parameters ID" );

        assertTrue(xml.contains("<trait") &&
                xml.contains("id=\"TraitSet\"") && xml.contains("traitname=\"date-backward\""), "TraitSet" );
        assertTrue(xml.contains("BE8078s92=10.0") &&
                xml.contains("BE332s102=0.0") && xml.contains("USALongs56=46.0"), "Time" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") && xml.contains("x=\"@Theta\"") &&
                xml.contains("spec=\"beast.base.inference.distribution.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">2.0</parameter>"), "Theta prior");

        assertTrue(xml.contains("x=\"@kappa\"") && xml.contains("id=\"kappa.prior\"") &&
                xml.contains("name=\"M\">1.0</parameter>") && xml.contains("name=\"S\">0.5</parameter>"),  "kappa prior" );
        assertTrue(xml.contains("x=\"@pi_2\"") && xml.contains("id=\"pi_2.prior\"") &&
                xml.contains("name=\"alpha\">2.0 2.0 2.0 2.0</parameter>"),  "pi_2 prior" );

        assertTrue(xml.contains("<frequencies") && xml.contains("spec=\"Frequencies\"") &&
                xml.contains("frequencies=\"@pi_1\""),  "Frequencies" );

        assertTrue(xml.contains("id=\"WeightedDirichlet\"") &&
                xml.contains("<weights") && xml.contains("dimension=\"3\"") &&
                xml.contains("estimate=\"false\">209 210 210</weights>"), "r.prior WeightedDirichlet" );

        assertTrue(xml.contains("x=\"@mu\"") && xml.contains("name=\"M\">-5.0</parameter>") &&
                xml.contains("name=\"S\">1.25</parameter>"),  "mu prior" );

        // 3 TreeLikelihood
        assertEquals(3,xml.split("ThreadedTreeLikelihood", -1).length - 1, "Tree Likelihood" );

        // 5 ScaleOperator, incl. tree
        assertEquals(5, xml.split("ScaleOperator", -1).length - 1, "ScaleOperator" );

        assertTrue(xml.contains("Exchange") && xml.contains("SubtreeSlide") && xml.contains("Uniform") &&
                xml.contains("WilsonBalding"), "Tree Operator" );

        assertTrue(xml.contains("UpDownOperator") &&
                xml.contains("<up") && xml.contains("<down"), "UpDownOperator" );
        // 4 DeltaExchangeOperator
        assertEquals(4, xml.split("DeltaExchangeOperator", -1).length - 1, "DeltaExchangeOperator");

        assertTrue(xml.contains("chainLength=\"1000000\"") && xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"" + fileStem + ".log\"") && xml.contains("fileName=\"" + fileStem + ".trees\""),
                "logger" );
    }

}