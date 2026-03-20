package ma.lphybeast;

import beast.base.inference.Operator;
import beast.base.inference.StateNode;
import lphy.core.logger.LoggerUtils;
import lphybeast.BEASTContext;
import lphybeast.spi.OperatorContributor;
import mutablealignment.MutableAlignment;
import mutablealignment.MutableAlignmentOperator;

import java.util.List;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * Creates MutableAlignmentOperator for MutableAlignment state nodes.
 */
public class MAOperatorContributor implements OperatorContributor {

    @Override
    public boolean canHandle(StateNode stateNode) {
        return stateNode instanceof MutableAlignment;
    }

    @Override
    public List<Operator> createOperators(StateNode stateNode, BEASTContext context) {
        MutableAlignment mutableAlignment = (MutableAlignment) stateNode;
        MutableAlignmentOperator operator = new MutableAlignmentOperator();
        operator.setInputValue("mutableAlignment", mutableAlignment);
        operator.setInputValue("weight", getOperatorWeight(
                mutableAlignment.getTaxonCount() * mutableAlignment.getSiteCount() - 1, 0.5));
        operator.initAndValidate();
        operator.setID("alignmentOperator");
        LoggerUtils.log.info("Created mutable alignment operator.");
        return List.of(operator);
    }
}
