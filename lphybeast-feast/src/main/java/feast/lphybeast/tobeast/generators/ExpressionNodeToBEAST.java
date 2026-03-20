package feast.lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import feast.expressions.ExpCalculator;
import lphy.core.model.ExpressionNode;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;
import lphybeast.tobeast.ExpressionUtils;

import java.util.List;

public class ExpressionNodeToBEAST implements GeneratorToBEAST<ExpressionNode, ExpCalculator> {
    @Override
    public ExpCalculator generatorToBEAST(ExpressionNode expression, BEASTInterface value, BEASTContext context) {

        Value output = (Value) context.getGraphicalModelNode(value);

        if (ExpressionUtils.isNamedModelValue(output, context)) {

            if (!output.getCanonicalId().equals(value.getID()))
                throw new IllegalArgumentException("The LPhy expression output ID " + output.getCanonicalId() +
                        " should match BEAST ID " + value.getID());

            ExpCalculator expCalculator = new ExpCalculator();

            List<RandomVariable> args = ExpressionUtils.findArgs(expression);

            for (RandomVariable randomVariable : args) {
                BEASTInterface beastInterface = context.getBEASTObject(randomVariable);
                expCalculator.setInputValue("arg", beastInterface);
            }

            expCalculator.setInputValue("value", expression.getExpression());
            expCalculator.setInputValue("useCaching", false);
            expCalculator.setID(value.getID());
            expCalculator.initAndValidate();

            context.removeBEASTObject(value);

            return expCalculator;

        } else
            return null;
    }

    @Override
    public Class<ExpressionNode> getGeneratorClass() {
        return ExpressionNode.class;
    }

    @Override
    public Class<ExpCalculator> getBEASTClass() {
        return ExpCalculator.class;
    }
}
