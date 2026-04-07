package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.spec.inference.parameter.VectorElement;
import beast.base.spec.type.RealVector;
import lphy.core.model.Value;
import lphy.core.vectorization.operation.SliceDoubleArray;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.VectorSlice;

public class SliceDoubleArrayToBEAST implements GeneratorToBEAST<SliceDoubleArray, BEASTInterface> {
    @Override
    public BEASTInterface generatorToBEAST(SliceDoubleArray slice, BEASTInterface value, BEASTContext context) {

        Integer start = slice.start().value();
        Integer end = slice.end().value();
        int count = end - start + 1;

        RealVector<?> vector = (RealVector<?>) context.getBEASTObject(slice.array());

        if (count == 1) {
            VectorElement<?> element = new VectorElement<>(vector, start);
            element.setID(slice.getUniqueId());
            return element;
        } else {
            VectorSlice<?> vs = new VectorSlice<>(vector, start, count);
            vs.setID(slice.getUniqueId());
            return vs;
        }
    }

    @Override
    public void modifyBEASTValues(SliceDoubleArray generator, BEASTInterface value, BEASTContext context) {

        Value lphyValue = (Value) context.getGraphicalModelNode(value);

        context.removeBEASTObject(value);
        context.putBEASTObject(lphyValue, generatorToBEAST(generator, value, context));
    }

    @Override
    public Class<SliceDoubleArray> getGeneratorClass() {
        return SliceDoubleArray.class;
    }

    @Override
    public Class<BEASTInterface> getBEASTClass() {
        return BEASTInterface.class;
    }
}
