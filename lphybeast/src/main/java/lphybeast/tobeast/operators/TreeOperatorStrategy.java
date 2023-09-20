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

        operators.add(OperatorFactory.createTreeScaleOperator(tree, context));
        operators.add(OperatorFactory.createRootHeightOperator(tree, context));
        operators.add(OperatorFactory.createExchangeOperator(tree, context, true));
        operators.add(OperatorFactory.createExchangeOperator(tree, context, false));
        operators.add(OperatorFactory.createTreeUniformOperator(tree, context));

        operators.add(OperatorFactory.createSubtreeSlideOperator(tree, context));
        operators.add(OperatorFactory.createWilsonBaldingOperator(tree, context));

        return operators;
    }

    String getName();

}
