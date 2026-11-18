package lphybeast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Default operators for real parameters: a scale operator multiplies by a positive factor and can never change a
 * value's sign, so parameters whose prior supports negative values need a random walk.
 */
public class DefaultOperatorChoiceTest {

    @BeforeEach
    void setUp() {
        TestUtils.loadServices();
    }

    private static String operatorFor(String xml, String id) {
        Matcher m = Pattern.compile("<operator\\b[^>]*parameter=\"@" + Pattern.quote(id) + "\"[^>]*>").matcher(xml);
        assertTrue(m.find(), "no operator for " + id);
        return m.group();
    }

    @Test
    void realLineParametersGetRandomWalk() {
        String lphy = """
                model {
                  b ~ Normal(mean=0.0, sd=1.0, replicates=3);
                  s ~ Normal(mean=0.0, sd=1.0);
                  r ~ LogNormal(meanlog=0.0, sdlog=1.0, replicates=3);
                  p ~ LogNormal(meanlog=0.0, sdlog=1.0);
                  psi ~ Coalescent(theta=p, n=6);
                  D ~ PhyloCTMC(L=50, Q=jukesCantor(), tree=psi);
                }""";
        String xml = TestUtils.lphyScriptToBEASTXML(lphy, "operatorChoice");
        assertNotNull(xml);
        for (String id : new String[]{"b", "s"}) {
            String op = operatorFor(xml, id);
            assertTrue(op.contains("RandomWalkOperator"), id + " needs a random walk: " + op);
        }
        for (String id : new String[]{"r", "p"}) {
            String op = operatorFor(xml, id);
            assertTrue(op.contains("ScaleOperator"), id + " should keep a scale operator: " + op);
        }
    }
}
