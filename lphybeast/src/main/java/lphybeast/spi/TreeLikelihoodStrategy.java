package lphybeast.spi;

import beast.base.core.BEASTInterface;
import beast.base.evolution.alignment.Alignment;

/**
 * Strategy for creating tree likelihood objects.
 * The default creates ThreadedTreeLikelihood; extensions can provide
 * alternatives (e.g., MATreeLikelihood for mutable alignment).
 */
public interface TreeLikelihoodStrategy {

    boolean appliesTo(Alignment alignment, boolean isObserved);

    BEASTInterface createTreeLikelihood(Alignment alignment, boolean isObserved);
}
