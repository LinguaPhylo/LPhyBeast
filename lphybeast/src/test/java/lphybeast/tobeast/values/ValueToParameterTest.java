package lphybeast.tobeast.values;

import beast.base.inference.parameter.RealParameter;
import lphy.graphicalModel.Value;
import org.junit.jupiter.api.Disabled;

/**
 * test unicode
 * @author Walter Xie
 */
public class ValueToParameterTest {

    @Disabled
//    @Test
    void testUnicodeVarName() {
        char[] chars = new char[]{'Ã','˦','ᕧ','⁂','∫','☕'};
        for (char c : chars) {
            System.out.println(c);

            Value<Double> value = new Value<>(Character.toString(c), 1.0);
            System.out.println(value.getCanonicalId());

            RealParameter parameter = new RealParameter("0.1");
            parameter.setID(value.getCanonicalId());
            System.out.println(parameter.getID());
            System.out.println(parameter.toXML());
        }
    }

}