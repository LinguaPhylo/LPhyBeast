package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import lphy.graphicalModel.Value;

public class ValueToParameter {

    public static void setID(BEASTInterface parameter, Value value) {
        if (!value.isAnonymous()) parameter.setID(value.getCanonicalId());
    }
}
