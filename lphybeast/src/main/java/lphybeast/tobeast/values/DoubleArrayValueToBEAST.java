package lphybeast.tobeast.values;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.model.GenerativeDistribution1D;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.vectorization.IID;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class DoubleArrayValueToBEAST implements ValueToBEAST<Double[], RealVectorParam> {

    @Override
    public boolean match(Value value) {
        // WeightedDirichlet handled by WeightedDirichletValueToBEAST (SimplexParam)
        if (value.getGenerator() instanceof WeightedDirichlet) return false;
        return value.value() instanceof Double[];
    }

    @Override
    public RealVectorParam valueToBEAST(Value<Double[]> value, BEASTContext context) {

        Double[] vals = value.value();
        double[] primitives = new double[vals.length];
        for (int i = 0; i < vals.length; i++) primitives[i] = vals[i];

        Real domain = inferDomain(value);

        RealVectorParam param = new RealVectorParam<>(primitives, domain);

        if (!(value instanceof RandomVariable))
            param.setInputValue("estimate", false);

        param.setID(value.getCanonicalId());

        return param;
    }

    private Real inferDomain(Value<Double[]> value) {
        GenerativeDistribution1D<Double> gd1d = null;

        if (value.getGenerator() instanceof GenerativeDistribution1D<?> gd) {
            gd1d = (GenerativeDistribution1D<Double>) gd;
        } else if (value.getGenerator() instanceof IID<?> iid
                && iid.getBaseDistribution() instanceof GenerativeDistribution1D<?>) {
            gd1d = (GenerativeDistribution1D<Double>) iid.getBaseDistribution();
        }

        if (gd1d != null) {
            Double[] bounds = gd1d.getDomainBounds();
            double lower = bounds[0];
            double upper = bounds[1];

            if (lower == 0.0 && upper == 1.0) return UnitInterval.INSTANCE;
            if (lower == 0.0) return NonNegativeReal.INSTANCE;
            if (lower > 0.0) return PositiveReal.INSTANCE;
        }

        return Real.INSTANCE;
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }
}
