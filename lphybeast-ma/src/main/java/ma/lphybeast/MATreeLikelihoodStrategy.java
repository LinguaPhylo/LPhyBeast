package ma.lphybeast;

import beast.base.evolution.alignment.Alignment;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import lphybeast.spi.TreeLikelihoodStrategy;
import mutablealignment.MATreeLikelihood;

/**
 * Creates MATreeLikelihood for unobserved (mutable) alignments.
 */
public class MATreeLikelihoodStrategy implements TreeLikelihoodStrategy {

    @Override
    public boolean appliesTo(Alignment alignment, boolean isObserved) {
        return !isObserved;
    }

    @Override
    public GenericTreeLikelihood createTreeLikelihood(Alignment alignment, boolean isObserved) {
        MATreeLikelihood treeLikelihood = new MATreeLikelihood();
        treeLikelihood.setInputValue("useAmbiguities", false);
        return treeLikelihood;
    }
}
