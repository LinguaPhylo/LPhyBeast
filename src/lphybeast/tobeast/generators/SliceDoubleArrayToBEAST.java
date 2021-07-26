package lphybeast.tobeast.generators;

import beast.core.BEASTInterface;
import beast.core.Function;
import beast.core.util.Slice;
import feast.function.Concatenate;
import lphy.core.functions.SliceDoubleArray;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.SliceFactory;

public class SliceDoubleArrayToBEAST implements GeneratorToBEAST<SliceDoubleArray, Slice> {
    @Override
    public Slice generatorToBEAST(SliceDoubleArray slice, BEASTInterface value, BEASTContext context) {

        Integer start = slice.start().value();
        Integer end =  slice.end().value();
        Integer count = end - start + 1;

        return SliceFactory.createSlice(context.getBEASTObject(slice.array()),
                start, count, slice.getUniqueId());
    }

    @Override
    public void modifyBEASTValues(SliceDoubleArray generator, BEASTInterface value, BEASTContext context) {

        Value lphyValue = (Value)context.getGraphicalModelNode(value);
        BEASTInterface slicedParameter = context.getBEASTObject(generator.array());

        if (slicedParameter instanceof Concatenate && generator.size() == 1) {
            Concatenate concatenate = (Concatenate)slicedParameter;

            Function element = concatenate.functionsInput.get().get(generator.start().value());

            if (element instanceof BEASTInterface) {
                context.removeBEASTObject(value);
                /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
                context.putBEASTObject(lphyValue, (BEASTInterface)element);
            }
        } else {

            context.removeBEASTObject(value);
            /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
            context.putBEASTObject(lphyValue, generatorToBEAST(generator, value, context));
        }
    }

    @Override
    public Class<SliceDoubleArray> getGeneratorClass() { return SliceDoubleArray.class; }

    @Override
    public Class<Slice> getBEASTClass() {
        return Slice.class;
    }
}
