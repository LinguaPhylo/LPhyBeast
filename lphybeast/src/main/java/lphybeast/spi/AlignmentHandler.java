package lphybeast.spi;

import beast.base.evolution.alignment.Alignment;

/**
 * Handles creation of alignment objects for observed/unobserved data.
 * The default creates standard Alignment; extensions can provide
 * alternatives (e.g., MutableAlignment for unobserved data).
 */
public interface AlignmentHandler {

    /**
     * @param isObserved whether the alignment is observed data
     * @return true if this handler should be used for the given observation status
     */
    boolean appliesTo(boolean isObserved);

    /**
     * Create the alignment object.
     *
     * @return a new Alignment instance (not yet configured with sequences)
     */
    Alignment createAlignment();
}
