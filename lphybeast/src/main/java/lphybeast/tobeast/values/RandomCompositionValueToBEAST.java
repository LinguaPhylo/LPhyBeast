package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.inference.parameter.IntSimplexParam;
import lphy.base.distribution.RandomComposition;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

/**
 * Converts Integer[] values produced by a {@link RandomComposition} distribution
 * into beast3 {@link IntSimplexParam} with {@link PositiveInt} domain.
 * The elements are positive integers that sum to n (number of coalescent intervals).
 */
public class RandomCompositionValueToBEAST implements ValueToBEAST<Integer[], IntSimplexParam> {

    @Override
    public boolean match(Value value) {
        return value.value() instanceof Integer[]
                && value.getGenerator() instanceof RandomComposition;
    }

    @Override
    public IntSimplexParam valueToBEAST(Value<Integer[]> value, BEASTContext context) {
        Integer[] vals = value.value();
        int[] ivals = new int[vals.length];
        int sum = 0;
        for (int i = 0; i < vals.length; i++) {
            ivals[i] = vals[i];
            sum += ivals[i];
        }
        IntSimplexParam<PositiveInt> simplex =
                new IntSimplexParam<>(ivals, PositiveInt.INSTANCE, sum);
        simplex.setID(value.getCanonicalId());
        return simplex;
    }

    @Override
    public Class getValueClass() {
        return Integer[].class;
    }

    @Override
    public Class<IntSimplexParam> getBEASTClass() {
        return IntSimplexParam.class;
    }
}
