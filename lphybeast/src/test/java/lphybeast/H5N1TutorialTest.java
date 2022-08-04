package lphybeast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static lphybeast.TestUtils.assertDPGLocations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Discrete phylogeography
 * https://linguaphylo.github.io/tutorials/discrete-phylogeography/
 * @author Walter Xie
 */
public class H5N1TutorialTest {

    private final int ntaxa = 43;
    private Path fPath;

    @BeforeEach
    public void setUp() {
        fPath = TestUtils.getFileForResources("H5N1.nex");
    }

    @Test
    public void testDPG() {
        final String fileStem = "h5n1";
        String h5n1LPhy = String.format("""
                data {
                    options = {ageDirection="forward", ageRegex="_(\\d+)$"};
                    D = readNexus(file="%s", options=options);
                    L = D.nchar();
                    taxa = D.taxa();
                    D_trait = extractTrait(taxa=taxa, sep="_", i=2);
                    K = D_trait.canonicalStateCount();
                    dim = K*(K-1)/2;
                  }
                  model {
                    κ ~ LogNormal(meanlog=1.0, sdlog=1.25);
                    π ~ Dirichlet(conc=[2.0,2.0,2.0,2.0]);
                    γ ~ LogNormal(meanlog=0.0, sdlog=2.0);
                    r ~ DiscretizeGamma(shape=γ, ncat=4, replicates=L);
                    Θ ~ LogNormal(meanlog=0.0, sdlog=1.0);
                    ψ ~ Coalescent(theta=Θ, taxa=taxa);
                    D ~ PhyloCTMC(siteRates=r, Q=hky(kappa=κ, freq=π), mu=0.004, tree=ψ);
                    π_trait ~ Dirichlet(conc=rep(element=3.0, times=K));
                    R_trait ~ Dirichlet(conc=rep(element=1.0, times=dim));
                    I ~ Bernoulli(p=0.5, replicates=dim, minSuccesses=dim-2);
                    μ_trait ~ LogNormal(meanlog=0, sdlog=1.25);
                    Q_trait = generalTimeReversible(freq=π_trait, rates=select(x=R_trait, indicator=I));
                    D_trait ~ PhyloCTMC(L=1, Q=Q_trait, mu=μ_trait, tree=ψ, dataType=D_trait.dataType());
                  }""", fPath.toAbsolutePath());

        String xml = TestUtils.lphyScriptToBEASTXML(h5n1LPhy, fileStem);

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("id=\"pi\"") && xml.contains("id=\"kappa\"") &&
                xml.contains("id=\"gamma\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") &&
                xml.contains("id=\"pi_trait\"") && xml.contains("id=\"I\"") && xml.contains("id=\"R_trait\"") &&
                xml.contains("id=\"mu_trait\""), "Check parameters ID" );
        // pi_trait, I, R_trait
        assertTrue(xml.contains("spec=\"parameter.RealParameter\" dimension=\"5\"") &&
                xml.contains("spec=\"parameter.BooleanParameter\" dimension=\"10\"") &&
                xml.contains("spec=\"parameter.RealParameter\" dimension=\"10\""), "DPG rates dimension" );

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet" );
        assertTrue(xml.contains("A_chicken_Fujian_1042_2005=0.0") &&
                xml.contains("A_Goose_Guangdong_1_1996=9.0") &&
                xml.contains("A_bird_HongKong_542_1997=8.0"), "Time" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("spec=\"beast.math.distributions.LogNormalDistributionModel\"") &&
                xml.contains("name=\"M\">0.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior");

        assertTrue(xml.contains("x=\"@kappa\"") && xml.contains("id=\"kappa.prior\"") &&
                xml.contains("name=\"M\">1.0</parameter>") && xml.contains("name=\"S\">1.25</parameter>"),  "kappa prior" );

        // 2 frequencies
        assertEquals(2,xml.split("<frequencies", -1).length - 1, "nuc & trait frequencies" );
        assertTrue(xml.contains("frequencies=\"@pi\"") && xml.contains("frequencies=\"@pi_trait\""),  "frequencies" );
        assertTrue(xml.contains("x=\"@pi\"") && xml.contains("id=\"pi.prior\"") &&
                xml.contains("name=\"alpha\">2.0 2.0 2.0 2.0</parameter>"),  "pi prior" );
        // dimension="5"
        assertTrue(xml.contains("x=\"@pi_trait\"") && xml.contains("id=\"pi_trait.prior\"") &&
                xml.contains("name=\"alpha\">3.0 3.0 3.0 3.0 3.0</parameter>"),  "pi_trait prior" );
        // dimension="10"
        assertTrue(xml.contains("x=\"@R_trait\"") && xml.contains("id=\"R_trait.prior\"") &&
                        xml.contains("name=\"alpha\">1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0</parameter>"),
                "R_trait prior" );
        assertTrue(xml.contains("parameter=\"@I\"") && xml.contains("id=\"BernoulliDistribution\"") &&
                        xml.contains("name=\"p\">0.5</parameter>") && xml.contains("IntegerParameter\">8</minSuccesses>"),
                "I prior" );

        // 2 site models
        assertEquals(2, xml.split("<siteModel", -1).length - 1, "2 site models" );
        assertTrue(xml.contains("gammaCategoryCount=\"4\"") && xml.contains("shape=\"@gamma\"") &&
                xml.contains("gammaCategoryCount=\"1\""), "SiteModel" );
        assertTrue(xml.contains("x=\"@gamma\"") && xml.contains("name=\"M\">0.0</parameter>") &&
                xml.contains("name=\"S\">2.0</parameter>"),  "gamma shape prior" );

        assertTrue(xml.contains("name=\"clock.rate\">0.004</parameter>"),  "clock rate" );

        assertTrue(xml.contains("x=\"@mu_trait\"") && xml.contains("name=\"M\">0.0</parameter>") &&
                xml.contains("name=\"S\">1.25</parameter>"),  "mu_trait prior" );

        assertTrue(xml.contains("spec=\"AncestralStateTreeLikelihood\"") && xml.contains("tag=\"location\"") &&
                xml.contains("tree=\"@psi\""),  "D_trait treeLikelihood" );
        assertTrue(xml.contains("spec=\"AlignmentFromTrait\"") && xml.contains("traitname=\"discrete\"") &&
                xml.contains("value=\"A_chicken_Fujian_1042_2005=Fujian"),  "Trait Alignment" );

        assertDPGLocations(xml);

        assertTrue(xml.contains("id=\"SVSGeneralSubstitutionModel\"") && xml.contains("rateIndicator=\"@I\"") &&
                xml.contains("rates=\"@R_trait\"") && xml.contains("clock.rate=\"@mu_trait\"") , "geo site model" );

        assertTrue(xml.contains("spec=\"BitFlipOperator\"") && xml.contains("parameter=\"@I\""), "I.bitFlip Operator" );

        // 6 ScaleOperator, incl. tree
        assertEquals(6, xml.split("ScaleOperator", -1).length - 1, "ScaleOperator" );

        assertTrue(xml.contains("Exchange") && xml.contains("SubtreeSlide") && xml.contains("Uniform") &&
                xml.contains("WilsonBalding"), "Tree Operator" );

        assertTrue(xml.contains("UpDownOperator") &&
                xml.contains("<up") && xml.contains("<down"), "UpDownOperator" );
        // 3 DeltaExchangeOperator
        assertEquals(3, xml.split("DeltaExchangeOperator", -1).length - 1, "DeltaExchangeOperator");

        assertTrue(xml.contains("chainLength=\"1000000\"") && xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"" + fileStem + ".log\"") && xml.contains("fileName=\"" + fileStem + ".trees\"") &&
                xml.contains("fileName=\"" + fileStem + "_with_trait.trees\"") &&  xml.contains("mode=\"tree\""),
                "logger" );

        assertTrue(xml.contains("<log idref=\"D_trait.treeLikelihood\"/>") &&
                xml.contains("<metadata idref=\"posterior\"/>") && xml.contains("id=\"svs\"") &&
                xml.contains("spec=\"SVSGeneralSubstitutionModelLogger\"") && xml.contains("dataType=\"@UserDataType\"") &&
                xml.contains("model=\"@SVSGeneralSubstitutionModel\""), "trait log" );
    }

}