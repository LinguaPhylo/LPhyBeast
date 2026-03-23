package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.base.distribution.ExpMarkovChain;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

/**
 * Converts Double[] values produced by an {@link ExpMarkovChain} distribution
 * into beast3 {@link RealVectorParam} with {@link PositiveReal} domain.
 */
public class ExpMarkovChainValueToBEAST implements ValueToBEAST<Double[], RealVectorParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double[]
                && value.getGenerator() instanceof ExpMarkovChain;
    }

    @Override
    public RealVectorParam valueToBEAST(Value<Double[]> value, BEASTContext context) {
        Double[] vals = value.value();
        double[] dvals = new double[vals.length];
        for (int i = 0; i < vals.length; i++)
            dvals[i] = vals[i];
        RealVectorParam<PositiveReal> vec =
                new RealVectorParam<>(dvals, PositiveReal.INSTANCE);
        vec.setID(value.getCanonicalId());
        return vec;
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
