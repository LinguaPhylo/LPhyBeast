package mascot.lphybeast;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
        // load ../LPhyBeast/version.xml
        Path lphybeastDir = Paths.get(UserDir.getUserDir().toAbsolutePath().getParent().toString(),
                "..","LPhyBeast");
        if (!Files.exists(lphybeastDir))
            throw new IllegalArgumentException("Cannot locate LPhyBeast Dir : " + lphybeastDir);

        TestUtils.loadServices(lphybeastDir.toString());
        // load mascot/version.xml
        Path parentDir = UserDir.getUserDir().toAbsolutePath();
        TestUtils.loadServices(parentDir.toString());
        fPath = TestUtils.getFileForResources("h3n2.nexus");
    }

    @Test
    public void testMascot() {
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
                     κ ~ LogNormal(meanlog=1.0, sdlog=1.25);
                     π ~ Dirichlet(conc=[2.0,2.0,2.0,2.0]);
                     γ ~ LogNormal(meanlog=0.0, sdlog=2.0);
                     r ~ DiscretizeGamma(shape=γ, ncat=4, replicates=L);
                     μ ~ LogNormal(meanlog=-5.298, sdlog=0.25);
                     Θ ~ LogNormal(meanlog=0.0, sdlog=1.0, replicates=S);
                     m ~ Exp(mean=1.0, replicates=dim);
                     M = migrationMatrix(theta=Θ, m=m);
                     ψ ~ StructuredCoalescent(M=M, taxa=taxa, demes=demes, sort=true);
                     D ~ PhyloCTMC(siteRates=r, Q=hky(kappa=κ, freq=π), mu=μ, tree=ψ);
                   }""", fPath.toAbsolutePath());

        String xml = TestUtils.lphyScriptToBEASTXML(h3n2LPhy, fileStem);

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("id=\"pi\"") && xml.contains("id=\"kappa\"") && xml.contains("id=\"mu\"") &&
                xml.contains("id=\"gamma\"") && xml.contains("id=\"Theta\"") && xml.contains("id=\"psi\"") &&
                xml.contains("id=\"m\""), "Check parameters ID" );
        // m, Theta
        assertTrue(xml.contains("dimension=\"6\" keys=\"Hong_Kong_New_York") &&
                xml.contains("dimension=\"3\" keys=\"Hong_Kong New_York New_Zealand\""), "Theta dimension" );

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet" );
        assertTrue(xml.contains("A/New_York/169/2000|CY000657|2000.005464|New_York=1.9863169999998718") &&
                xml.contains("A/New_York/273/2001|CY001720|2001.991781|New_York=0.0") &&
                xml.contains("A/Waikato/5/2001|CY013072|2001.367123|New_Zealand=0.624657999999954"), "Time" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("distribution.LogNormalDistributionModel") &&
                xml.contains("name=\"M\">0.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior");
        assertTrue(xml.contains("x=\"@m\"") && xml.contains("id=\"m.prior\"") &&
                        xml.contains("id=\"Exponential\"") && xml.contains("name=\"mean\">1.0</parameter>"),
                "m prior");
        assertTrue(xml.contains("x=\"@mu\"") && xml.contains("id=\"mu.prior\"") &&
                xml.contains("name=\"M\">-5.298</parameter>") && xml.contains("name=\"S\">0.25</parameter>"),
                "mu prior");

        assertTrue(xml.contains("x=\"@kappa\"") && xml.contains("id=\"kappa.prior\"") &&
                xml.contains("name=\"M\">1.0</parameter>") && xml.contains("name=\"S\">1.25</parameter>") &&
                xml.contains("spec=\"HKY\"") && xml.contains("kappa=\"@kappa\""), "kappa prior & HKY" );
        // frequencies
        assertTrue(xml.contains("x=\"@pi\"") && xml.contains("frequencies=\"@pi\"") &&
                xml.contains("id=\"pi.prior\"") && xml.contains("name=\"alpha\">2.0 2.0 2.0 2.0</parameter>"),  "pi prior" );
        // site models
        assertTrue(xml.contains("gammaCategoryCount=\"4\"") && xml.contains("shape=\"@gamma\"") &&
                xml.contains("x=\"@gamma\"") && xml.contains("name=\"M\">0.0</parameter>") &&
                xml.contains("name=\"S\">2.0</parameter>"),  "SiteModel & gamma shape prior" );

        assertTrue(xml.contains("id=\"Mascot\"") && xml.contains("tree=\"@psi\"") &&
                xml.contains("mascot.distribution.Mascot") && xml.contains("mascot.dynamics.Constant") &&
                xml.contains("<dynamics") && xml.contains("id=\"Constant\"") &&
                xml.contains("mascot.distribution.StructuredTreeIntervals") &&
                xml.contains("backwardsMigration=\"@m\""), "Mascot" );
        assertTrue(xml.contains("beast.base.evolution.tree.TraitSet") && xml.contains("traitname=\"deme\"") &&
                xml.contains("A/New_York/169/2000|CY000657|2000.005464|New_York=New_York") &&
                xml.contains("A/Hong_Kong/1269/2001|KP457669|2001.657534|Hong_Kong=Hong_Kong") &&
                xml.contains("A/Waikato/5/2000|CY011960|2000.661202|New_Zealand=New_Zealand") &&
                xml.contains("<typeTrait"), "Trait set" );

        // 7 ScaleOperator, incl. tree
        assertEquals(7, xml.split("BactrianScaleOperator", -1).length - 1, "BactrianScaleOperator" );

        assertTrue(xml.contains("Exchange") && xml.contains("BactrianSubtreeSlide") &&
                xml.contains("BactrianNodeOperator") && xml.contains("WilsonBalding"), "Tree Operator" );

        assertTrue(xml.contains("BactrianUpDownOperator") &&
                xml.contains("<up") && xml.contains("<down"), "BactrianUpDownOperator" );
        // 1 DeltaExchangeOperator
        assertEquals(1, xml.split("BactrianDeltaExchangeOperator", -1).length - 1, "BactrianDeltaExchangeOperator");

        assertTrue(xml.contains("chainLength=\"1000000\"") && xml.contains("logEvery=\"500\"") &&
                xml.contains("fileName=\"" + fileStem + ".log\"") && xml.contains("fileName=\"" + fileStem + ".trees\"") &&
                xml.contains("<log idref=\"posterior\"/>") && xml.contains("<log idref=\"likelihood\"/>") &&
                xml.contains("<log idref=\"prior\"/>") && xml.contains("<log idref=\"pi\"/>") &&
                xml.contains("<log idref=\"kappa\"/>") && xml.contains("<log idref=\"mu\"/>") &&
                xml.contains("<log idref=\"gamma\"/>") && xml.contains("<log idref=\"m\"/>") &&
                xml.contains("<log idref=\"Theta\"/>") && xml.contains("<log idref=\"D.treeLikelihood\"/>"),
                "logger" );

        assertTrue(xml.contains("<log idref=\"Mascot\"/>") && xml.contains("mascot=\"@Mascot\"") &&
                xml.contains("fileName=\"" + fileStem + ".mascot.trees\"") && xml.contains("mode=\"tree\"") &&
                xml.contains("mascot.logger.StructuredTreeLogger"),
                "Mascot logger" );
    }
}