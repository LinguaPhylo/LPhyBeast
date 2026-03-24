package lphybeast.tobeast.values;

import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.base.distribution.Beta;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class BetaValueToBEAST implements ValueToBEAST<Double, RealScalarParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double
                && value.getGenerator() instanceof Beta;
    }

    @Override
    public RealScalarParam valueToBEAST(Value<Double> value, BEASTContext context) {
        RealScalarParam<UnitInterval> param =
                new RealScalarParam<>((Double) value.value(), UnitInterval.INSTANCE);
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
