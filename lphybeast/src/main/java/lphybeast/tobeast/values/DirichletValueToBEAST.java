package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beast.base.spec.inference.parameter.SimplexParam;
import lphy.base.distribution.Dirichlet;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

/**
 * Converts Double[] values produced by a {@link Dirichlet} distribution
 * into beast3 {@link SimplexParam}, which satisfies both the Dirichlet
 * prior and any model component that expects a {@code RealVector}.
 */
public class DirichletValueToBEAST implements ValueToBEAST<Double[], SimplexParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Double[]
                && value.getGenerator() instanceof Dirichlet;
    }

    @Override
    public SimplexParam valueToBEAST(Value<Double[]> value, BEASTContext context) {
        Double[] vals = value.value();
        double[] dvals = new double[vals.length];
        for (int i = 0; i < vals.length; i++)
            dvals[i] = vals[i];
        SimplexParam simplex = new SimplexParam(dvals);
        simplex.setID(value.getCanonicalId());
        return simplex;
    }

    @Override
    public Class getValueClass() {
        return Double[].class;
    }

    @Override
    public Class<SimplexParam> getBEASTClass() {
        return SimplexParam.class;
    }
}
