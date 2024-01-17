package lphybeast.tobeast.operators;

import beast.base.inference.Operator;

import java.util.List;

/**
 * Defines a family of parameter operators
 * which will create in {@link DefaultOperatorStrategy}.
 * @author Walter Xie
 */
public interface OperatorStrategy {

    /**
     * create the operators for that states retrieved by {@link lphybeast.BEASTContext}
     * @return
     */
    List<Operator> createOperators();

    // get the instance, assign the inputs later

    Operator getScaleOperator();

    Operator getDeltaExchangeOperator();

    Operator getRandomWalkOperator();

    Operator getIntRandomWalkOperator();

    Operator getBitFlipOperator();

}
