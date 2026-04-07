package lphybeast.tobeast.values;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class NumberArrayValueToBEAST implements ValueToBEAST<Number[], RealVectorParam> {

    @Override
    public boolean match(Value value) {
        return Number[].class.isAssignableFrom(value.value().getClass());
    }

    @Override
    public RealVectorParam valueToBEAST(Value<Number[]> value, BEASTContext context) {
        Number[] vals = value.value();
        double[] dvals = new double[vals.length];
        for (int i = 0; i < vals.length; i++)
            dvals[i] = vals[i].doubleValue();
        Real domain = DoubleValueToBEAST.inferDomain(value);
        RealVectorParam<?> param = new RealVectorParam<>(dvals, domain);
        if (!(value instanceof RandomVariable))
            param.setInputValue("estimate", false);
        param.setID(value.getCanonicalId());
        return param;
    }

    @Override
    public Class getValueClass() {
        return Number[].class;
    }

    @Override
    public Class<RealVectorParam> getBEASTClass() {
        return RealVectorParam.class;
    }

}
