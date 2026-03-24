package lphybeast.tobeast.values;

import beast.base.spec.inference.parameter.BoolScalarParam;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

public class BooleanValueToBEAST implements ValueToBEAST<Boolean, BoolScalarParam> {

    @Override
    public BoolScalarParam valueToBEAST(Value<Boolean> value, BEASTContext context) {

        BoolScalarParam parameter = new BoolScalarParam(value.value());
        parameter.setID(value.getCanonicalId());
        return parameter;
    }

    @Override
    public Class getValueClass() {
        return Boolean.class;
    }

    @Override
    public Class<BoolScalarParam> getBEASTClass() {
        return BoolScalarParam.class;
    }
}
