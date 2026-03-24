package lphybeast.tobeast.values;

import beast.base.spec.inference.parameter.BoolVectorParam;
import lphy.base.distribution.RandomBooleanArray;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class RandomBooleanArrayValueToBEAST implements ValueToBEAST<Boolean[], BoolVectorParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Boolean[]
                && value.getGenerator() instanceof RandomBooleanArray;
    }

    @Override
    public BoolVectorParam valueToBEAST(Value<Boolean[]> value, BEASTContext context) {
        Boolean[] vals = value.value();
        boolean[] bvals = new boolean[vals.length];
        for (int i = 0; i < vals.length; i++)
            bvals[i] = vals[i];
        BoolVectorParam param = new BoolVectorParam(bvals);
        param.setID(value.getCanonicalId());
        return param;
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
