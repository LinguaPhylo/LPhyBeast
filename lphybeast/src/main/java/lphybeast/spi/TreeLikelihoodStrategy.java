package lphybeast.spi;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.likelihood.GenericTreeLikelihood;

/**
 * Strategy for creating tree likelihood objects.
 * The default creates ThreadedTreeLikelihood; extensions can provide
 * alternatives (e.g., MATreeLikelihood for mutable alignment).
 */
public interface TreeLikelihoodStrategy {

    /**
     * @param alignment   the BEAST alignment
     * @param isObserved  whether the alignment is observed data
     * @return true if this strategy should handle the given alignment
     */
    boolean appliesTo(Alignment alignment, boolean isObserved);

    /**
     * Create the tree likelihood for the given alignment.
     *
     * @param alignment   the BEAST alignment
     * @param isObserved  whether the alignment is observed data
     * @return a configured GenericTreeLikelihood (not yet fully initialized)
     */
    GenericTreeLikelihood createTreeLikelihood(Alignment alignment, boolean isObserved);
}
