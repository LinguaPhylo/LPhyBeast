package lphybeast.tobeast.operators;

import beast.base.evolution.operator.Exchange;
import beast.base.evolution.operator.WilsonBalding;
import beast.base.evolution.operator.kernel.BactrianNodeOperator;
import beast.base.evolution.operator.kernel.BactrianSubtreeSlide;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.spec.evolution.operator.ScaleTreeOperator;
import lphybeast.BEASTContext;

/**
 * Default tree operators. Currently, they are
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
        return new ScaleTreeOperator();
    }

    @Override
    public Operator getUniformOperator() {
        return new BactrianNodeOperator();
    }

    @Override
    public Operator getSubtreeSlideOperator() {
        return new BactrianSubtreeSlide();
    }

    //*** not changed ***//
    @Override
    public Operator getExchangeOperator() {
        return new Exchange();
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
