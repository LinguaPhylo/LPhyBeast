package lphybeast.tobeast.generators;

import beast.base.core.BEASTInterface;
import feast.expressions.ExpCalculator;
import lphy.core.model.ExpressionNode;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphybeast.BEASTContext;
import lphybeast.GeneratorToBEAST;

import java.util.ArrayList;
import java.util.List;

public class ExpressionNodeToBEAST implements GeneratorToBEAST<ExpressionNode, ExpCalculator> {
    @Override
    public ExpCalculator generatorToBEAST(ExpressionNode expression, BEASTInterface value, BEASTContext context) {


        Value output = (Value) context.getGraphicalModelNode(value);

        // this can only be used, when expression contains random var, such as exp(a+b); a ~ Normal();
        // and the expression must be the model block
        if (isNamedModelValue(output, context)) {

            if (!output.getCanonicalId().equals(value.getID()))
                throw new IllegalArgumentException("The LPhy expression output ID " + output.getCanonicalId() +
                        " should match BEAST ID " + value.getID());

            ExpCalculator expCalculator = new ExpCalculator();

            // recursive to add args

            List<RandomVariable> args = findArgs(expression);

            for (RandomVariable randomVariable : args) {
                //TODO other types of parameters?
                BEASTInterface beastInterface = context.getBEASTObject(randomVariable);
                expCalculator.setInputValue("arg", beastInterface);
            }

            expCalculator.setInputValue("value", expression.getExpression());
            expCalculator.setInputValue("useCaching", false);
            expCalculator.setID(value.getID());
            expCalculator.initAndValidate();

            // this beast obj must be replaced by ExpCalculator
            context.removeBEASTObject(value);

            // Note: use getAsFunctionOrRealParameter(Value value)
            // not getAsRealParameter(Value value)

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

    public static boolean isNamedModelValue(Value value, BEASTContext context) {
        return context.getParserDictionary().getModelDictionary().values().contains(value);
    }

    public static List<RandomVariable> findArgs(ExpressionNode expression) {
        List<RandomVariable> args = new ArrayList<>();

        addArgs(expression, args);

        return args;
    }

    private static void addArgs(ExpressionNode expression, List<RandomVariable> args) {
        List<GraphicalModelNode> inputs = expression.getInputs();
        for (GraphicalModelNode input : inputs) {
            if (input instanceof RandomVariable randomVariable)
                args.add(randomVariable);
            else if (input instanceof Value value && value.getGenerator() instanceof ExpressionNode newExpression)
                addArgs(newExpression, args);
        }
    }
}
