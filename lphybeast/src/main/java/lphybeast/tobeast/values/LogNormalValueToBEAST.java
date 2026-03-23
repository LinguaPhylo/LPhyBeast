package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.base.distribution.LogNormal;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

/**
 * Converts Double values produced by a {@link LogNormal} distribution
 * into beast3 {@link RealScalarParam} with {@link PositiveReal} domain.
 */
public class LogNormalValueToBEAST implements ValueToBEAST<Double, RealScalarParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double
                && value.getGenerator() instanceof LogNormal;
    }

    @Override
    public RealScalarParam valueToBEAST(Value<Double> value, BEASTContext context) {
        RealScalarParam<PositiveReal> param =
                new RealScalarParam<>((Double) value.value(), PositiveReal.INSTANCE);
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
