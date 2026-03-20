package mm.lphybeast.tobeast.generators;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LewisMKToBEASTTest {

    private String lewisMK = """
            Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);
            ψ ~ Coalescent(n=%1$s, theta=Θ);
            Q = lewisMK(numStates=%2$s);
            D ~ PhyloCTMC(L=20, Q=Q, tree=ψ, dataType=standard(%2$s));""";

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-mm", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("mm.lphybeast.spi.MMLBImpl")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testLewisMK() throws IOException {
        int ntaxa = 16;
        int nState = 3;
        LPhyBeast lPhyBeast = new LPhyBeast();
        String xml = lPhyBeast.lphyStrToXML(String.format(lewisMK, ntaxa, nState), "lewisMK");

        assertNotNull(xml);
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"));
        assertTrue(xml.contains("<data") && xml.contains("</data>"), "alignment tag");

        assertTrue(xml.contains("<userDataType") && xml.contains("codeMap=\"0=0,1=1,2=2,") &&
                xml.contains("states=\"" + nState + "\""), "userDataType");

        assertTrue(xml.contains("<substModel") && xml.contains("id=\"LewisMK\"") &&
                xml.contains("morphmodels.evolution.substitutionmodel.LewisMK") &&
                xml.contains("stateNumber=\"" + nState + "\""), "LewisMK substModel");

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("distribution.LogNormalDistributionModel") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior");

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Coalescent\""), "Coalescent");
        assertTrue(xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\""), "popSize");
    }
}
