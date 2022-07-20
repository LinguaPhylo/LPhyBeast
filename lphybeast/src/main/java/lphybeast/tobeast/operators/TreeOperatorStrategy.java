package lphybeast.tobeast.operators;

import beast.core.BEASTInterface;
import beast.core.Operator;
import beast.evolution.tree.Tree;
import com.google.common.collect.Multimap;
import lphy.graphicalModel.GraphicalModelNode;
import lphybeast.BEASTContext;

import java.util.ArrayList;
import java.util.List;

import static lphybeast.BEASTContext.getOperatorWeight;

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

        Multimap<BEASTInterface, GraphicalModelNode<?>> elements = context.getElements();
        operators.add(createTreeScaleOperator(tree, elements));
        operators.add(createRootHeightOperator(tree, elements));
        operators.add(createExchangeOperator(tree, true, elements));
        operators.add(createExchangeOperator(tree, false, elements));
        operators.add(createTreeUniformOperator(tree, elements));

        operators.add(createSubtreeSlideOperator(tree, elements));
        operators.add(createWilsonBaldingOperator(tree, elements));

        return operators;
    }


    default Operator createTreeScaleOperator(Tree tree, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator operator = getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "scale");
        elements.put(operator, null);
        return operator;
    }

    default Operator createRootHeightOperator(Tree tree, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator operator = getScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("rootOnly", true);
        operator.setInputValue("scaleFactor", 0.75);
        // set the upper of the scale factor
        operator.setInputValue("upper", 0.975);
        operator.setInputValue("weight", getOperatorWeight(1));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "rootAgeScale");
        elements.put(operator, null);
        return operator;
    }

    default Operator createTreeUniformOperator(Tree tree, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator uniform = getUniformOperator();
        uniform.setInputValue("tree", tree);
        uniform.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        uniform.initAndValidate();
        uniform.setID(tree.getID() + "." + "uniform");
        elements.put(uniform, null);
        return uniform;
    }

    default Operator createExchangeOperator(Tree tree, boolean isNarrow,
                                            Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator exchange = getExchangeOperator();
        exchange.setInputValue("tree", tree);
        exchange.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        exchange.setInputValue("isNarrow", isNarrow);
        exchange.initAndValidate();
        exchange.setID(tree.getID() + "." + ((isNarrow) ? "narrow" : "wide") + "Exchange");
        elements.put(exchange, null);
        return exchange;
    }

    default Operator createSubtreeSlideOperator(Tree tree, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator subtreeSlide = getSubtreeSlideOperator();
        subtreeSlide.setInputValue("tree", tree);
        subtreeSlide.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        subtreeSlide.setInputValue("size", tree.getRoot().getHeight() / 10.0);
        subtreeSlide.initAndValidate();
        subtreeSlide.setID(tree.getID() + "." + "subtreeSlide");
        elements.put(subtreeSlide, null);
        return subtreeSlide;
    }

    default Operator createWilsonBaldingOperator(Tree tree, Multimap<BEASTInterface, GraphicalModelNode<?>> elements) {
        Operator wilsonBalding = getWilsonBaldingOperator();
        wilsonBalding.setInputValue("tree", tree);
        wilsonBalding.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        wilsonBalding.initAndValidate();
        wilsonBalding.setID(tree.getID() + "." + "wilsonBalding");
        elements.put(wilsonBalding, null);
        return wilsonBalding;
    }

    String getName();

}
