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

    /**
     * Infer the real domain from a generative distribution's bounds.
     */
    public static Real inferDomain(Value<?> value) {
        if (value.getGenerator() instanceof GenerativeDistribution1D<?> gd) {
            Object[] bounds = gd.getDomainBounds();
            if (bounds[0] instanceof Number lower && bounds[1] instanceof Number upper) {
                double lo = lower.doubleValue();
                double hi = upper.doubleValue();
                if (lo == 0.0 && hi == 1.0) return UnitInterval.INSTANCE;
                if (lo == 0.0) return NonNegativeReal.INSTANCE;
                if (lo > 0.0) return PositiveReal.INSTANCE;
            }
        }
        return Real.INSTANCE;
    }

    @Override
    public RealScalarParam valueToBEAST(Value<Double> value, BEASTContext context) {
        Real domain = inferDomain(value);
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
