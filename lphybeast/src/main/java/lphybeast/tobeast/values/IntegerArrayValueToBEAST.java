package lphybeast.tobeast.values;

import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import lphy.base.distribution.RandomComposition;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class IntegerArrayValueToBEAST implements ValueToBEAST<Integer[], IntegerParameter> {

    @Override
    public IntegerParameter valueToBEAST(Value<Integer[]> value, BEASTContext context) {

//        KeyIntegerParameter parameter = new KeyIntegerParameter();
//        List<Integer> values = Arrays.asList(value.value());
//        parameter.setInputValue("value", values);
//        parameter.setInputValue("dimension", values.size());
//        // set estimate="false" for IntegerArray values that are not RandomVariables.
//        if (!(value instanceof RandomVariable)) parameter.setInputValue("estimate", false);

        // check domain
        Integer lower = null;
        if (value.getGenerator() instanceof RandomComposition) {
//            parameter.setInputValue("lower", 1);
            lower = 1;
        }
        Integer upper = null;

        Parameter parameter = BEASTContext.createParameterWithBound(value, lower, upper, false);
        if (!(parameter instanceof IntegerParameter))
            throw new IllegalStateException("Expecting to create KeyIntegerParameter from " + value.getCanonicalId());

        return (IntegerParameter) parameter;
    }

    @Override
    public Class getValueClass() {
        return Integer[].class;
    }

    @Override
    public Class<IntegerParameter> getBEASTClass() {
        return IntegerParameter.class;
    }

}
