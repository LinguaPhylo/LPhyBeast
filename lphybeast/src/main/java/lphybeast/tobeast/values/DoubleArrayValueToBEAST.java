package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import lphy.base.distribution.Dirichlet;
import lphy.base.distribution.WeightedDirichlet;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class DoubleArrayValueToBEAST implements ValueToBEAST<Double[], BEASTInterface> {

    @Override
    public boolean match(Value value) {
        // WeightedDirichlet requires Concatenate (feast extension)
        if (value.getGenerator() instanceof WeightedDirichlet) return false;
        return value.value() instanceof Double[];
    }

    @Override
    public BEASTInterface valueToBEAST(Value<Double[]> value, BEASTContext context) {

        Double lower = null;
        Double upper = null;
        // check domain
        if (value.getGenerator() instanceof Dirichlet) {
            lower = 0.0;
            upper = 1.0;
        }

        Parameter parameter = BEASTContext.createParameterWithBound(value, lower, upper, false);
        if (!(parameter instanceof RealParameter))
            throw new IllegalStateException("Expecting to create KeyRealParameter from " + value.getCanonicalId());

        return (RealParameter) parameter;
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}
