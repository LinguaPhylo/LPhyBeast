package lphybeast.tobeast.operators;

import beast.core.Operator;
import beast.evolution.operators.*;
import beast.evolution.tree.Tree;
import lphybeast.BEASTContext;

/**
 * @author Walter Xie
 * @author Alexei Drommand
 */
public class DefaultTreeOperatorStrategy implements TreeOperatorStrategy {

    public DefaultTreeOperatorStrategy() { }

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
        return "default tree operator strategy";
    }

}