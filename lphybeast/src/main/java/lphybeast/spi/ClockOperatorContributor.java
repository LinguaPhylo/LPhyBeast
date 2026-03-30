package lphybeast.spi;

import beast.base.spec.evolution.branchratemodel.UCRelaxedClockModel;
import beast.base.evolution.tree.Tree;
import beast.base.inference.Operator;
import lphybeast.BEASTContext;

import java.util.List;

/**
 * Contributes operators for relaxed clock analyses.
 * Extensions (e.g., ORC) register contributors that add
 * optimised operators when a relaxed clock model is present.
 */
public interface ClockOperatorContributor {

    /**
     * Create operators for a relaxed clock analysis.
     *
     * @param tree              the tree
     * @param relaxedClockModel the relaxed clock model
     * @param context           the BEAST context
     * @return list of operators to add
     */
    List<Operator> createOperators(Tree tree, UCRelaxedClockModel relaxedClockModel, BEASTContext context);
}
