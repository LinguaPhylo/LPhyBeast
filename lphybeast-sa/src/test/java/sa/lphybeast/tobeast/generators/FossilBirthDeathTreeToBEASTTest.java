package sa.lphybeast.tobeast.generators;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FossilBirthDeathTreeToBEASTTest {

    private String simFossilsCompact = """
            lambda ~ Uniform(lower=1.0, upper=1.5);
            mu ~ Uniform(lower=0.5, upper=1.0);
            taxa = taxa(names=1:20);
            fossilTree ~ FossilBirthDeathTree(lambda=lambda, mu=mu, taxa=taxa, psi=1.0, rho=1.0);
            daCount = fossilTree.directAncestorCount();""";

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-sa", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("sa.lphybeast.spi.SALBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testSimFossilsCompact() throws IOException {
        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(simFossilsCompact, "simFossilsCompact");

        assertNotNull(xml);
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"));

        assertFalse(xml.contains("<data") && xml.contains("</data>"), "No alignment tag");

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet");
        assertTrue(xml.contains("id=\"SABirthDeathModel\"") && xml.contains("birthRate=\"@lambda\"") &&
                xml.contains("deathRate=\"@mu\"") && xml.contains("conditionOnSampling=\"true\"") &&
                xml.contains("origin=\"@fossilTree.origin\"") &&
                xml.contains("sa.evolution.speciation.SABirthDeathModel"), "SABirthDeathModel");

        assertTrue(xml.contains("id=\"lambda\"") && xml.contains("id=\"mu\"") &&
                        xml.contains("\"samplingRate\">1.0</parameter>") &&
                xml.contains("\"removalProbability\">0.0</parameter>") && xml.contains("\"rho\">1.0</parameter>"),
                "SABirthDeath parameters");

        assertTrue(xml.contains("distribution.Uniform") &&
                xml.contains("lower=\"0.5\"") && xml.contains("lower=\"1.0\"") && xml.contains("upper=\"1.5\"") &&
                xml.contains("x=\"@mu\"") && xml.contains("x=\"@lambda\""), "Uniform prior");

        assertTrue(xml.contains("sa.evolution.operators.SAScaleOperator") &&
                xml.contains("sa.evolution.operators.SAExchange") &&
                xml.contains("sa.evolution.operators.SAUniform") &&
                xml.contains("sa.evolution.operators.SAWilsonBalding") &&
                xml.contains("sa.evolution.operators.LeafToSampledAncestorJump"), "SA operators");
    }
}
