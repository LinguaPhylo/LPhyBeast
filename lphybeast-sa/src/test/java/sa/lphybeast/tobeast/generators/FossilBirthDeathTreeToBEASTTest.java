package sa.lphybeast.tobeast.generators;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XML can be used to sample from SA prior.
 * @author Walter Xie
 */
class FossilBirthDeathTreeToBEASTTest {

    private String simFossilsCompact = """
            lambda ~ Uniform(lower=1.0, upper=1.5);
            mu ~ Uniform(lower=0.5, upper=1.0);
            taxa = taxa(names=1:20);
            fossilTree ~ FossilBirthDeathTree(lambda=lambda, mu=mu, taxa=taxa, psi=1.0, rho=1.0);
            daCount = fossilTree.directAncestorCount();""";


    @BeforeEach
    public void setUp() {
        // load ../LPhyBeast/version.xml
        Path lphybeastDir = Paths.get(UserDir.getUserDir().toAbsolutePath().getParent().toString(),
                "..","LPhyBeast");
        if (!Files.exists(lphybeastDir))
            throw new IllegalArgumentException("Cannot locate LPhyBeast Dir : " + lphybeastDir);

        TestUtils.loadServices(lphybeastDir.toString());
        // load sa/version.xml
        Path parentDir = UserDir.getUserDir().toAbsolutePath();
        TestUtils.loadServices(parentDir.toString());
    }

    @Test
    public void testSimFossilsCompact() {
        String xml = TestUtils.lphyScriptToBEASTXML(simFossilsCompact, "simFossilsCompact");

        assertFalse(xml.contains("<data") && xml.contains("</data>"), "No alignment tag");

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet" );
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

        // operators
        assertTrue(xml.contains("sa.evolution.operators.SAScaleOperator") &&
                xml.contains("sa.evolution.operators.SAExchange") &&
                xml.contains("sa.evolution.operators.SAUniform") &&
                xml.contains("sa.evolution.operators.SAWilsonBalding") &&
                xml.contains("sa.evolution.operators.LeafToSampledAncestorJump"), "SA operators");

    }

}