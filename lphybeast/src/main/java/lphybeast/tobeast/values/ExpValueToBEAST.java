package lphybeast.tobeast.values;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.base.distribution.Exp;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class ExpValueToBEAST implements ValueToBEAST<Double, RealScalarParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double
                && value.getGenerator() instanceof Exp;
    }

    @Override
    public RealScalarParam valueToBEAST(Value<Double> value, BEASTContext context) {
        RealScalarParam<NonNegativeReal> param =
                new RealScalarParam<>((Double) value.value(), NonNegativeReal.INSTANCE);
        param.setID(value.getCanonicalId());
        return param;
    }

    @Override
    public Class getValueClass() {
        return Double.class;
    }

    @Override
    public Class<RealScalarParam> getBEASTClass() {
        return RealScalarParam.class;
    }
}
