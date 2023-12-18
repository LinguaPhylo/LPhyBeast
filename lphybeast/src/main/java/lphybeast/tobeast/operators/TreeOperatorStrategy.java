package lphybeast.tobeast.operators;

import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;

import java.util.ArrayList;
import java.util.List;


/**
 * Defines a family of tree operators.
 * @author Walter Xie
 */
public interface TreeOperatorStrategy {

    boolean applyStrategyToTree(Tree tree, BEASTContext context);

    Operator getScaleOperator();

    Operator getUniformOperator();

    Operator getExchangeOperator();

    Operator getSubtreeSlideOperator();

    Operator getWilsonBaldingOperator();


    default List<Operator> createTreeOperators(Tree tree, BEASTContext context) {
        List<Operator> operators = new ArrayList<>();

        operators.add(StandardOperatorFactory.createTreeScaleOperator(tree, context));
        operators.add(StandardOperatorFactory.createRootHeightOperator(tree, context));
        operators.add(StandardOperatorFactory.createExchangeOperator(tree, context, true));
        operators.add(StandardOperatorFactory.createExchangeOperator(tree, context, false));
        operators.add(StandardOperatorFactory.createTreeUniformOperator(tree, context));

        operators.add(StandardOperatorFactory.createSubtreeSlideOperator(tree, context));
        operators.add(StandardOperatorFactory.createWilsonBaldingOperator(tree, context));

        return operators;
    }

    String getName();

}
