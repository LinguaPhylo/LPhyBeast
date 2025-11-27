package lphybeast.tobeast;

import feast.expressions.ExpCalculator;
import lphy.core.model.ExpressionNode;
import lphy.core.model.GraphicalModelNode;
import lphy.core.model.RandomVariable;
import lphy.core.model.Value;
import lphy.core.vectorization.operation.Slice;
import lphy.core.vectorization.operation.SliceDoubleArray;
import lphybeast.BEASTContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils static method to handle expressions,
 * such as exp(a+b); a ~ Normal();
 */
public final class ExpressionUtils {


    /**
     * Determine if the {@link Value} is named and generated inside the model block.
     * This identifies the expression would contain random var.
     * @param value     {@link Value}
     * @param context   {@link BEASTContext}
     * @return    true if it is named and inside the model block.
     * @see lphybeast.tobeast.generators.ExpressionNodeToBEAST
     */
    public static boolean isNamedModelValue(Value value, BEASTContext context) {
        return context.getParserDictionary().getModelDictionary().values().contains(value);
    }

    /**
     * Recursively find all random vars given a {@link ExpressionNode},
     * so that they can be used as the list of arg when converting to the beast obj {@link ExpCalculator}.
     * @param expression  {@link ExpressionNode}, such as exp(a+b);
     * @return  list of {@link RandomVariable}.
     * @see lphybeast.tobeast.generators.ExpressionNodeToBEAST
     */
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
//            else if (input instanceof Value value && value.getGenerator() instanceof Slice slice
//                    && slice.getParams().get("array") instanceof RandomVariable array) {
//                args.add(array);
//            }
        }
    }
}
