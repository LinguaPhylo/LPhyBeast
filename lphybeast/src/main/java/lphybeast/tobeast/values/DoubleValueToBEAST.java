package lphybeast.tobeast.values;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import lphy.core.model.GenerativeDistribution1D;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class DoubleValueToBEAST implements ValueToBEAST<Double, RealScalarParam> {

    @Override
    public RealScalarParam valueToBEAST(Value<Double> value, BEASTContext context) {

        Real domain = Real.INSTANCE;

        if (value.getGenerator() instanceof GenerativeDistribution1D) {
            GenerativeDistribution1D<Double> gd = (GenerativeDistribution1D<Double>) value.getGenerator();
            Double[] bounds = gd.getDomainBounds();
            double lower = bounds[0];
            double upper = bounds[1];

            if (lower == 0.0 && upper == 1.0) {
                domain = UnitInterval.INSTANCE;
            } else if (lower == 0.0) {
                domain = NonNegativeReal.INSTANCE;
            } else if (lower > 0.0 || (lower == 0.0 && !Double.isInfinite(lower))) {
                domain = PositiveReal.INSTANCE;
            }
        }

        RealScalarParam param = new RealScalarParam<>(value.value(), domain);
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
