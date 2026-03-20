package sa.lphybeast.tobeast.generators;

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

class SimFBDAgeToBEASTTest {

    private String simFBDAge = """
            tree ~ SimFBDAge(lambda=1, mu=0.6, frac=0.3, psi=0.4, originAge=4);
            daCount = tree.directAncestorCount();""";

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-sa", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("sa.lphybeast.spi.SALBImpl")
        ));
        Path vfPath = Paths.get(System.getProperty("user.dir"), "../lphybeast/version.xml");
        if (!Files.exists(vfPath))
            throw new IllegalArgumentException("Can't find version.xml: " + vfPath);
        LPhyBEASTLoader.addBEAST2Services(new String[]{vfPath.toAbsolutePath().toString()});
    }

    @Test
    public void testSimFBDAge() throws IOException {
        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(simFBDAge, "simFBDAge");

        assertNotNull(xml);
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"));

        assertFalse(xml.contains("<data") && xml.contains("</data>"), "No alignment tag");

        assertTrue(xml.contains("<trait") && xml.contains("id=\"TraitSet\"") &&
                xml.contains("traitname=\"date-backward\""), "TraitSet");
        assertTrue(xml.contains("id=\"SABirthDeathModel\"") &&
                xml.contains("conditionOnSampling=\"true\"") && xml.contains("origin=\"@tree.origin\"") &&
                xml.contains("sa.evolution.speciation.SABirthDeathModel"), "SABirthDeathModel");

        assertTrue(xml.contains("\"birthRate\">1.0</parameter>") &&
                xml.contains("\"deathRate\">0.6</parameter>") && xml.contains("\"samplingRate\">0.4</parameter>") &&
                xml.contains("\"removalProbability\">0.0</parameter>") && xml.contains("\"rho\">0.3</parameter>"),
                "SABirthDeath parameters");

        assertEquals(8, xml.split("<operator", -1).length - 1, "operators");
        assertTrue(xml.contains("sa.evolution.operators.SAScaleOperator") &&
                xml.contains("sa.evolution.operators.SAExchange") &&
                xml.contains("sa.evolution.operators.SAUniform") &&
                xml.contains("sa.evolution.operators.SAWilsonBalding") &&
                xml.contains("sa.evolution.operators.LeafToSampledAncestorJump"), "SA operators");
    }
}
