package mm.lphybeast.tobeast.generators;

import lphy.core.io.UserDir;
import lphybeast.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Check the XML
 * @author Walter Xie
 */
public class LewisMKToBEASTTest {

    private String lewisMK = """
            Θ ~ LogNormal(meanlog=3.0, sdlog=1.0);
            ψ ~ Coalescent(n=%1$s, theta=Θ);
            Q = lewisMK(numStates=%2$s);
            D ~ PhyloCTMC(L=20, Q=Q, tree=ψ, dataType=standard(%2$s));""";


    @BeforeEach
    public void setUp() {
        // load ../LPhyBeast/version.xml
        Path lphybeastDir = Paths.get(UserDir.getUserDir().toAbsolutePath().getParent().toString(),
                "..","LPhyBeast");
        if (!Files.exists(lphybeastDir))
            throw new IllegalArgumentException("Cannot locate LPhyBeast Dir : " + lphybeastDir);

        // load mm/version.xml
        Path parentDir = UserDir.getUserDir().toAbsolutePath();
        TestUtils.loadServices(parentDir.toString());
    }

    @Test
    public void testLewisMK() {
        int ntaxa = 16;
        int nState = 3;
        String xml = TestUtils.lphyScriptToBEASTXML(String.format(lewisMK, ntaxa, nState), "lewisMK");

        TestUtils.assertXMLNTaxa(xml, ntaxa);

        assertTrue(xml.contains("<userDataType") && xml.contains("codeMap=\"0=0,1=1,2=2,") &&
                xml.contains("states=\"" + nState + "\""), "userDataType" );

        assertTrue(xml.contains("<substModel") && xml.contains("id=\"LewisMK\"") &&
                xml.contains("morphmodels.evolution.substitutionmodel.LewisMK") &&
                xml.contains("stateNumber=\"" + nState + "\""), "LewisMK substModel" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Theta.prior\"") &&
                xml.contains("x=\"@Theta\"") && xml.contains("distribution.LogNormalDistributionModel") &&
                xml.contains("name=\"M\">3.0</parameter>") && xml.contains("name=\"S\">1.0</parameter>"), "Theta prior" );

        assertTrue(xml.contains("<distribution") && xml.contains("id=\"Coalescent\""), "Coalescent" );
        assertTrue(xml.contains("<populationModel") && xml.contains("popSize=\"@Theta\""), "popSize" );
    }

}