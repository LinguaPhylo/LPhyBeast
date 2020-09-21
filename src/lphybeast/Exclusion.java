package lphybeast;

import lphy.core.functions.*;
import lphy.evolution.alignment.CharSetAlignment;
import lphy.graphicalModel.Generator;
import lphy.graphicalModel.Value;

/**
 * Utils class to exclude {@link lphy.graphicalModel.Value}
 * or {@link lphy.graphicalModel.Generator}.
 * @author Walter Xie
 */
public class Exclusion {

    public static boolean isExcludedValue(Value<?> val) {
        Object ob = val.value();
        // ignore all String: d = nexus(file="Dengue4.nex");
        return ob instanceof String || ob instanceof String[] || ob instanceof CharSetAlignment;
    }

    public static boolean isExcludedGenerator(Generator generator) {
        return ((generator instanceof NTaxaFunction) || (generator instanceof NCharFunction) ||
                (generator instanceof TaxaFunction) || (generator instanceof Nexus) ||
                (generator instanceof Range) || (generator instanceof Partition) );
    }
}