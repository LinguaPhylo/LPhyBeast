package lphybeast.tobeast.operators;

import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;

import java.util.ArrayList;
import java.util.List;

import static lphybeast.BEASTContext.getOperatorWeight;


/**
 * Defines a family of tree operators.
 * @author Walter Xie
 */
public interface TreeOperatorStrategy {

    // choose the strategy which defines the family of operators
    boolean applyStrategyToTree(Tree tree, BEASTContext context);

    // implement these getters and creators to specify
    // the required operators for different tree model/prior

    // get the instance, assign the inputs later

    Operator getScaleOperator();

    Operator getUniformOperator();

    Operator getExchangeOperator();

    Operator getSubtreeSlideOperator();

    Operator getWilsonBaldingOperator();

    // static creator, so could use context.addSkipOperator((Tree) tree)
    // combining with context.addExtraOperator(
    // TODO improve the design not to use static ?
    static Operator createTreeScaleOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "scale");
        context.getElements().put(operator, null);
        return operator;
    }

    static Operator createRootHeightOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator operator = treeOperatorStrategy.getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("rootOnly", true);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "rootAgeScale");
        context.getElements().put(operator, null);
        return operator;
    }

    static Operator createExchangeOperator(Tree tree, BEASTContext context, boolean isNarrow) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator exchange = treeOperatorStrategy.getExchangeOperator();
        exchange.setInputValue("tree", tree);
        double pow = (isNarrow) ? 0.7 : 0.2; // WideExchange size^0.2
        exchange.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount(), pow));
        exchange.setInputValue("isNarrow", isNarrow);
        exchange.initAndValidate();
        exchange.setID(tree.getID() + "." + ((isNarrow) ? "narrow" : "wide") + "Exchange");
        context.getElements().put(exchange, null);
        return exchange;
    }

    static Operator createTreeUniformOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator uniform = treeOperatorStrategy.getUniformOperator();
        uniform.setInputValue("tree", tree);
        uniform.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        uniform.initAndValidate();
        uniform.setID(tree.getID() + "." + "uniform");
        context.getElements().put(uniform, null);
        return uniform;
    }

    static Operator createSubtreeSlideOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator subtreeSlide = treeOperatorStrategy.getSubtreeSlideOperator();
        subtreeSlide.setInputValue("tree", tree);
        subtreeSlide.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        subtreeSlide.setInputValue("size", tree.getRoot().getHeight() / 10.0);
        subtreeSlide.initAndValidate();
        subtreeSlide.setID(tree.getID() + "." + "subtreeSlide");
        context.getElements().put(subtreeSlide, null);
        return subtreeSlide;
    }

    static Operator createWilsonBaldingOperator(Tree tree, BEASTContext context) {
        TreeOperatorStrategy treeOperatorStrategy = context.resolveTreeOperatorStrategy(tree);
        Operator wilsonBalding = treeOperatorStrategy.getWilsonBaldingOperator();
        wilsonBalding.setInputValue("tree", tree);
        wilsonBalding.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount(), 0.2));
        wilsonBalding.initAndValidate();
        wilsonBalding.setID(tree.getID() + "." + "wilsonBalding");
        context.getElements().put(wilsonBalding, null);
        return wilsonBalding;
    }


    // overwrite this main method if another package requires a different set of tree operators

    default List<Operator> createTreeOperators(Tree tree, BEASTContext context) {
        List<Operator> operators = new ArrayList<>();

        operators.add(createTreeScaleOperator(tree, context));
        operators.add(createRootHeightOperator(tree, context));
        operators.add(createExchangeOperator(tree, context, true));
        operators.add(createExchangeOperator(tree, context, false));
        operators.add(createTreeUniformOperator(tree, context));

        operators.add(createSubtreeSlideOperator(tree, context));
        operators.add(createWilsonBaldingOperator(tree, context));

        return operators;
    }

    String getName();

}
