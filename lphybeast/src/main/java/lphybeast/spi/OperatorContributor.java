package lphybeast.spi;

import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import lphybeast.BEASTContext;

import java.util.List;

/**
 * Contributes operators for package-specific state nodes.
 * Extensions register contributors for their state node types
 * (e.g., MutableAlignmentOperator for MutableAlignment).
 */
public interface OperatorContributor {

    /**
     * @param stateNode the state node to check
     * @return true if this contributor can create operators for the given state node
     */
    boolean canHandle(StateNode stateNode);

    /**
     * Create operators for the given state node.
     *
     * @param stateNode the state node
     * @param context   the BEAST context
     * @return list of operators for the state node
     */
    List<Operator> createOperators(StateNode stateNode, BEASTContext context);
}
