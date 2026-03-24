package lphybeast.tobeast.values;

import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.inference.parameter.IntScalarParam;
import lphy.base.distribution.Poisson;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class PoissonValueToBEAST implements ValueToBEAST<Integer, IntScalarParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Integer
                && value.getGenerator() instanceof Poisson;
    }

    @Override
    public IntScalarParam valueToBEAST(Value<Integer> value, BEASTContext context) {
        IntScalarParam<NonNegativeInt> param =
                new IntScalarParam<>((Integer) value.value(), NonNegativeInt.INSTANCE);
        param.setID(value.getCanonicalId());
        return param;
    }

    @Override
    public Class getValueClass() {
        return Integer.class;
    }

    @Override
    public Class<IntScalarParam> getBEASTClass() {
        return IntScalarParam.class;
    }
}
