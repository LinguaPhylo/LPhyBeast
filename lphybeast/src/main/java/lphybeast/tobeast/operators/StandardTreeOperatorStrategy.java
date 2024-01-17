package lphybeast.tobeast.operators;

import beast.base.evolution.operator.*;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;

/**
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class StandardTreeOperatorStrategy implements TreeOperatorStrategy {

    public StandardTreeOperatorStrategy() { }

    @Override
    public boolean applyStrategyToTree(Tree tree, BEASTContext context) {
        return true; // this is ignored for DefaultTreeOperatorStrategy
    }

    @Override
    public Operator getScaleOperator() {
        return new ScaleOperator();
    }

    @Override
    public Operator getUniformOperator() {
        return new Uniform();
    }

    @Override
    public Operator getExchangeOperator() {
        return new Exchange();
    }

    @Override
    public Operator getSubtreeSlideOperator() {
        return new SubtreeSlide();
    }

    @Override
    public Operator getWilsonBaldingOperator() {
        return new WilsonBalding();
    }

    @Override
    public String getName() {
        return "standard tree operator strategy";
    }

}
