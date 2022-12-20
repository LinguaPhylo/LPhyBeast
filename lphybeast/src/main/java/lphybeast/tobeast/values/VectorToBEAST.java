package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beastlabs.util.BEASTVector;
import lphy.core.distributions.VectorizedDistribution;
import lphy.core.functions.VectorizedFunction;
import lphy.graphicalModel.Value;
import lphy.graphicalModel.Vector;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;

public class VectorToBEAST implements ValueToBEAST<Object, BEASTVector> {

    @Override
    public BEASTVector valueToBEAST(Value<Object> value, BEASTContext context) {

        if (!(value instanceof Vector)) throw new IllegalArgumentException("Expecting a vector value!");
        Vector vectorValue = (Vector)value;

        List<BEASTInterface> beastValues = new ArrayList<>();
        for (int i = 0; i < vectorValue.size(); i++)  {
            Object component = vectorValue.getComponent(i);
            ValueToBEAST toBEAST = context.getValueToBEAST(component);

            BEASTInterface beastValue = toBEAST.valueToBEAST(new Value(null, component), context);
            beastValues.add(beastValue);
        }

        return new BEASTVector(beastValues);
    }

    @Override
    public Class getValueClass() {
        return Object.class;
    }

    public boolean match(Value value) {
        return (value instanceof Vector && (value.getGenerator() instanceof VectorizedFunction || value.getGenerator() instanceof VectorizedDistribution));
    }

    @Override
    public Class<BEASTVector> getBEASTClass() {
        return BEASTVector.class;
    }
}
