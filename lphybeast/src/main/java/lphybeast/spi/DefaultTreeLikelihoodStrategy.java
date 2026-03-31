package lphybeast.spi;

import beast.base.evolution.alignment.Alignment;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beast.base.spec.evolution.likelihood.ThreadedTreeLikelihood;

/**
 * Default strategy that creates spec ThreadedTreeLikelihood for observed alignments.
 */
public class DefaultTreeLikelihoodStrategy implements TreeLikelihoodStrategy {

    @Override
    public boolean appliesTo(Alignment alignment, boolean isObserved) {
        return isObserved;
    }

    @Override
    public GenericTreeLikelihood createTreeLikelihood(Alignment alignment, boolean isObserved) {
        ThreadedTreeLikelihood treeLikelihood = new ThreadedTreeLikelihood();
        treeLikelihood.setInputValue("useAmbiguities", true);
        return treeLikelihood;
    }
}
