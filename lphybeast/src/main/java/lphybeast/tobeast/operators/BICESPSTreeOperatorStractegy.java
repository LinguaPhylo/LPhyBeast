package lphybeast.tobeast.operators;

import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import beast.base.spec.evolution.operator.IntervalScaleOperator;
import lphybeast.BEASTContext;

import static lphybeast.BEASTContext.getOperatorWeight;

/**
 * IntervalScaleOperator replaces the BICESPS EpochFlex + TreeStretch combo.
 * @author Walter Xie
 */
public interface BICESPSTreeOperatorStractegy {

    static Operator createIntervalScaleOperator(Tree tree, BEASTContext context) {
        Operator operator = new IntervalScaleOperator();
        operator.setInputValue("tree", tree);
        operator.setInputValue("scaleFactor", 0.1);
        operator.setInputValue("weight", getOperatorWeight(tree.getInternalNodeCount()));
        operator.initAndValidate();
        operator.setID(tree.getID() + "." + "intervalScale");
        context.getElements().put(operator, null);
        return operator;
    }
}
