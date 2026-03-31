package lphybeast.spi;

import beast.base.evolution.alignment.Alignment;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;

/**
 * Strategy for creating tree likelihood objects.
 * The default creates spec ThreadedTreeLikelihood; extensions can provide
 * alternatives (e.g., MATreeLikelihood for mutable alignment).
 */
public interface TreeLikelihoodStrategy {

    boolean appliesTo(Alignment alignment, boolean isObserved);

    GenericTreeLikelihood createTreeLikelihood(Alignment alignment, boolean isObserved);
}
