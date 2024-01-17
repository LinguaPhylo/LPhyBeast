package lphybeast;

import lphy.core.io.UserDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Walter Xie
 */
public class TestUtils {

    private static LPhyBeast lPhyBEAST;

    private TestUtils() { }

    /**
     * user.dir/../version.xml
     */
    public static void loadServices() {
        loadServices(UserDir.getUserDir().toAbsolutePath().getParent().toString());
    }

    public static void loadServices(String parentDir) {
        // TODO better way?
        Path vfPath = Paths.get(parentDir, "version.xml");
        LPhyBEASTLoader.addBEAST2Services(new String[]{vfPath.toAbsolutePath().toString()});
    }

    public static LPhyBeast getLPhyBeast() {
        if(lPhyBEAST == null) {
            lPhyBEAST = new LPhyBeast();
        }
        return lPhyBEAST;
    }

    public static Path getFileForResources(String fileName) {
        System.out.println("WD = " + UserDir.getUserDir());
        Path fPath = Paths.get("src","test", "resources", fileName);
        System.out.println("Input file = " + fPath.toAbsolutePath());
        return fPath;
    }

    public static String lphyScriptToBEASTXML(String lphyScript, String fileNameStem) {
        LPhyBeast lPhyBEAST = getLPhyBeast();

        String xml = null;
        try {
            System.out.println("\n" + lphyScript + "\n");

            xml = lPhyBEAST.lphyStrToXML(lphyScript, fileNameStem);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull(xml, "XML");
        assertXMLTags(xml);

        return xml;
    }

    private static void assertXMLTags(String xml) {
        assertTrue(xml.contains("<beast") && xml.contains("</beast>"), "<beast></beast>");

        assertTrue(xml.contains("<run") && xml.contains("id=\"MCMC\""), "MCMC tag" );
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"posterior\""), "posterior tag");
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"prior\""), "prior tag");
        assertTrue(xml.contains("<distribution") && xml.contains("id=\"likelihood\""), "likelihood tag");
        assertTrue(xml.contains("<operator"), "operator tag");
        assertTrue(xml.contains("<logger") && xml.contains("id=\"Logger\""), "logger tag");
    }

    public static void assertXMLNTaxa(String xml, int ntaxa) {
        assertTrue(xml.contains("<data") && xml.contains("</data>"), "alignment tag");

        // take the 1st <data> ... </data>
        String alig = xml.substring(xml.indexOf("<data"), xml.indexOf("</data>"));
        // count how many <sequence in the 1st pair of <data> ... </data>
        String temp = alig.replace("<sequence", "");
        int occ = (alig.length() - temp.length()) / "<sequence".length();
        assertEquals(ntaxa,  occ, "ntaxa");
    }

    public static void assertJC(String xml) {
        // <substModel id="JukesCantor" spec="JukesCantor"/>
        int jcId1 = xml.indexOf("<substModel");
        int jcId2 = xml.indexOf("spec=\"JukesCantor\"/>");
        assertTrue(jcId1 > 100 && jcId2 > jcId1, "substModel");
        // remove all spaces
        String jcId = xml.substring(jcId1, jcId2).replaceAll("\\s+","");
        assertEquals("<substModelid=\"JukesCantor\"", jcId, "JukesCantor");
    }

    public static void assertDPGLocations(String xml) {
        // <userDataType id="UserDataType" spec="beast.evolution.datatype.UserDataType"
        // codeMap="Fujian=0,Guangdong=1,Guangxi=2,HongKong=3,Hunan=4, ? = 0 1 2 3 4" codelength="-1" states="5"/>
        assertTrue(xml.contains("<userDataType") && xml.contains("codelength=\"-1\"") &&
                xml.contains("states=\"5\""), "UserDataType" );
        int start = xml.indexOf("codeMap=\"");
        int end = xml.indexOf("codelength=\"-1\"");
        assertTrue(start > 100 && end > start, "codeMap");
        // remove all spaces
        String codeMap = xml.substring(start, end).replaceAll("\\s+","");
        assertEquals("codeMap=\"Fujian=0,Guangdong=1,Guangxi=2,HongKong=3,Hunan=4,?=01234\"", codeMap, "codeMap");
    }

    // DPG <metadata idref="D_trait.treeLikelihood"/> is compulsory
    public static void assertTreeWithTraitLogger(String xml) {
        int start = xml.indexOf("<logger id=\"TreeWithTraitLogger");
        int end = xml.indexOf("</run>");
        assertTrue(start > 100 && end > start, "TreeWithTraitLogger");

        String traitLogger = xml.substring(start, end);
        assertTrue(traitLogger.contains("idref=\"D_trait.treeLikelihood\"") &&
                traitLogger.contains("tree=\"@psi\"") && xml.contains("mode=\"tree\"") &&
                xml.contains("<metadata"), "TreeWithTraitLogger metadata : " + traitLogger );
    }
}
