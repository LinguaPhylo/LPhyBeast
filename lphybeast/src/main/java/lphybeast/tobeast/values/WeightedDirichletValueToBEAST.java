package lphybeast.tobeast.values;

import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;
import lphybeast.tobeast.operators.DefaultOperatorStrategy;

/**
 * Converts Double[] values produced by a {@link WeightedDirichlet} distribution
 * into beast3 {@link RealVectorParam} with a delta exchange operator.
 * Domain is NonNegativeReal since WeightedDirichlet values are weighted rates in [0, +∞).
 */
public class WeightedDirichletValueToBEAST implements ValueToBEAST<Double[], RealVectorParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double[]
                && value.getGenerator() instanceof WeightedDirichlet;
    }

    @Override
    public RealVectorParam valueToBEAST(Value<Double[]> value, BEASTContext context) {
        Double[] vals = value.value();
        double[] dvals = new double[vals.length];
        for (int i = 0; i < vals.length; i++)
            dvals[i] = vals[i];

        RealVectorParam<NonNegativeReal> param = new RealVectorParam<>(dvals, NonNegativeReal.INSTANCE);
        param.setID(value.getCanonicalId());

        DefaultOperatorStrategy.addDeltaExchangeOperator(value, param, context);

        return param;
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
