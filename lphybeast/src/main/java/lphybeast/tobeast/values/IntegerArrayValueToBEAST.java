package lphybeast.tobeast.values;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntVectorParam;
import lphy.base.distribution.RandomComposition;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class IntegerArrayValueToBEAST implements ValueToBEAST<Integer[], IntVectorParam> {

    @Override
    public IntVectorParam valueToBEAST(Value<Integer[]> value, BEASTContext context) {
        Integer[] vals = value.value();
        int[] ivals = new int[vals.length];
        for (int i = 0; i < vals.length; i++)
            ivals[i] = vals[i];

        IntVectorParam<?> param;
        if (value.getGenerator() instanceof RandomComposition) {
            param = new IntVectorParam<>(ivals, PositiveInt.INSTANCE);
        } else {
            param = new IntVectorParam<>(ivals, Int.INSTANCE);
        }
        if (!(value instanceof RandomVariable))
            param.setInputValue("estimate", false);
        param.setID(value.getCanonicalId());
        return param;
    }

    @Override
    public Class getValueClass() {
        return Integer[].class;
    }

    @Override
    public Class<IntVectorParam> getBEASTClass() {
        return IntVectorParam.class;
    }

}
