package lphybeast.tobeast.values;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntScalarParam;
import lphy.core.model.GenerativeDistribution1D;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class IntegerValueToBEAST implements ValueToBEAST<Integer, IntScalarParam> {

    @Override
    public IntScalarParam valueToBEAST(Value<Integer> value, BEASTContext context) {

        Int domain = Int.INSTANCE;

        if (value.getGenerator() instanceof GenerativeDistribution1D) {
            GenerativeDistribution1D<Integer> gd = (GenerativeDistribution1D<Integer>) value.getGenerator();
            Integer[] bounds = gd.getDomainBounds();
            int lower = bounds[0];

            if (lower >= 1) {
                domain = PositiveInt.INSTANCE;
            } else if (lower >= 0) {
                domain = NonNegativeInt.INSTANCE;
            }
        }

        IntScalarParam param = new IntScalarParam<>(value.value(), domain);
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
