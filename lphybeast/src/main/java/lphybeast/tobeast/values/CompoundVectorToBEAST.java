package lphybeast.tobeast.values;

import beast.base.core.BEASTInterface;
import beastlabs.util.BEASTVector;
import lphy.graphicalModel.CompoundVector;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphy.graphicalModel.types.StringValue;
import lphybeast.BEASTContext;
import lphybeast.ValueToBEAST;

import java.util.ArrayList;
import java.util.List;

public class CompoundVectorToBEAST implements ValueToBEAST<Object, BEASTVector> {

    @Override
    public BEASTVector valueToBEAST(Value<Object> value, BEASTContext context) {

        if (!(value instanceof CompoundVector)) throw new IllegalArgumentException("Expecting a compound vector value!");
        CompoundVector vectorValue = (CompoundVector)value;

        List<BEASTInterface> beastValues = new ArrayList<>();
        for (int i = 0; i < vectorValue.size(); i++)  {
            Value componentValue = vectorValue.getComponentValue(i);
            if (componentValue instanceof StringValue)
                return null;
            ValueToBEAST toBEAST = context.getMatchingValueToBEAST(componentValue);

            BEASTInterface beastValue = toBEAST.valueToBEAST(componentValue, context);
            beastValues.add(beastValue);
            /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
            context.putBEASTObject(componentValue, beastValue);
        }

        return new BEASTVector(beastValues, value.getId());
    }

    @Override
    public Class getValueClass() {
        return Object.class;
    }

    public boolean match(Value value) {
        return (value instanceof CompoundVector);
    }

    @Override
    public Class<BEASTVector> getBEASTClass() {
        return BEASTVector.class;
    }
}
