package lphybeast.tobeast.values;

import beast.base.spec.inference.parameter.BoolVectorParam;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class BooleanArrayValueToBEAST implements ValueToBEAST<Boolean[], BoolVectorParam> {

    @Override
    public BoolVectorParam valueToBEAST(Value<Boolean[]> value, BEASTContext context) {

        Boolean[] vals = value.value();
        boolean[] bvals = new boolean[vals.length];
        for (int i = 0; i < vals.length; i++)
            bvals[i] = vals[i];
        BoolVectorParam parameter = new BoolVectorParam(bvals);
        parameter.setID(value.getCanonicalId());
        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Boolean[].class;
    }

    @Override
    public Class<BoolVectorParam> getBEASTClass() {
        return BoolVectorParam.class;
    }
}
