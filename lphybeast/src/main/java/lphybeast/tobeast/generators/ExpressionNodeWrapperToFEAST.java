package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import lphy.graphicalModel.GraphicalModelNode;
import lphy.graphicalModel.Value;
import lphy.parser.functions.ExpressionNodeWrapper;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

public class ExpressionNodeWrapperToFEAST implements GeneratorToBEAST<ExpressionNodeWrapper, feast.expressions.ExpCalculator> {
    @Override
    public feast.expressions.ExpCalculator generatorToBEAST(ExpressionNodeWrapper wrapper, BEASTInterface value, BEASTContext context) {

        feast.expressions.ExpCalculator expCalculator = new feast.expressions.ExpCalculator();
        expCalculator.setInputValue("value", wrapper.getName());

        List<Function> functionList = new ArrayList<>();
        for (Object input : wrapper.getInputs()) {
            GraphicalModelNode node = (GraphicalModelNode) input;
            BEASTInterface beastInterface = context.getBEASTObject(node);
            if (beastInterface instanceof Function) {
                functionList.add((Function) node);
            }
        }
        if (functionList.size() > 0) expCalculator.setInputValue("arg", functionList);
        expCalculator.initAndValidate();

        return expCalculator;
    }

    @Override
    public void modifyBEASTValues(ExpressionNodeWrapper generator, BEASTInterface value, BEASTContext context) {
        Value lphyValue = (Value) context.getGraphicalModelNode(value);
        context.removeBEASTObject(value);
        /** call {@link BEASTContext#addToContext(GraphicalModelNode, BEASTInterface)} **/
        context.putBEASTObject(lphyValue, generatorToBEAST(generator, value, context));
    }

    @Override
    public Class<ExpressionNodeWrapper> getGeneratorClass() {
        return ExpressionNodeWrapper.class;
    }

    @Override
    public Class<feast.expressions.ExpCalculator> getBEASTClass() {
        return feast.expressions.ExpCalculator.class;
    }
}
