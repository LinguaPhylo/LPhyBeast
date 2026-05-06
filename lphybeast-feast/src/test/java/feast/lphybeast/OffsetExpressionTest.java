package feast.lphybeast;

import beast.pkgmgmt.BEASTClassLoader;
import lphybeast.LPhyBEASTLoader;
import lphybeast.LPhyBeast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for issue #202: support offset for distributions.
 * <p>
 * The expected translation: {@code Z = Y + c} (or {@code c + Y}, {@code Y - c}) where Y is a
 * RandomVariable used only by this deterministic should produce a single sampled parameter
 * Z whose prior is {@link beast.base.spec.inference.distribution.OffsetReal} wrapping Y's
 * underlying ScalarDistribution.
 */
public class OffsetExpressionTest {

    @BeforeEach
    public void setUp() {
        BEASTClassLoader.classLoader.addServices("lphybeast-feast", Map.of(
                "lphybeast.spi.LPhyBEASTMapping", Set.of("feast.lphybeast.spi.FeastLBImpl"),
                "lphybeast.spi.ValueHandler", Set.of("feast.lphybeast.FeastValueHandler")
        ));
        LPhyBEASTLoader.loadServicesForTest(System.getProperty("user.dir") + "/../lphybeast");
    }

    @Test
    public void testOffsetExpForBirthDeath() throws IOException {
        String script = """
                data {
                  L = 200;
                  taxa = taxa(names=1:10);
                }
                model {
                  div ~ Exp(mean=0.1);
                  diversification = div + 0.1;
                  turnover ~ Beta(alpha=2.0, beta=2.0);
                  rootAge ~ LogNormal(meanlog=2.366645, sdlog=0.25);
                  T ~ BirthDeathSampling(diversification=diversification, turnover=turnover, rho=0.5, rootAge=rootAge);
                  D ~ PhyloCTMC(tree=T, L=L, Q=jukesCantor());
                }""";

        String xml = new LPhyBeast().lphyStrToXML(script, "offsetExp");
        assertNotNull(xml);

        assertTrue(xml.contains("OffsetReal"), "OffsetReal present in XML");
        assertTrue(xml.contains("id=\"diversification.prior\""), "OffsetReal wired with id <param>.prior");
        assertTrue(xml.contains("id=\"diversification\""), "diversification is the sampled parameter");
        assertTrue(xml.contains("birthDiffRate=\"@diversification\"") || xml.contains("@diversification"),
                "BD model references diversification");
        assertFalse(xml.contains("id=\"div\""), "div parameter is subsumed (not a separate state node)");
        assertFalse(xml.contains("id=\"div.prior\""), "div's standalone Exp prior is replaced by OffsetReal");

        // diversification must be a sampled state node with an operator
        String stateBlock = xml.substring(xml.indexOf("<state"), xml.indexOf("</state>"));
        assertTrue(stateBlock.contains("@diversification") || stateBlock.contains("id=\"diversification\""),
                "diversification appears in <state>");
        assertTrue(xml.contains("@diversification") &&
                xml.contains("ScaleOperator") &&
                xml.matches("(?s).*parameter=\"@diversification\".*"),
                "ScaleOperator targets diversification");

    }

}
