package lphybeast.spi;

import beast.base.core.BEASTInterface;
import beast.base.inference.StateNode;
import beast.base.spec.type.Tensor;

import java.util.List;

/**
 * Handles package-specific value types that are embedded in core classes.
 * Extensions register handlers for their specific types (e.g., feast's
 * Concatenate and ExpCalculator).
 */
public interface ValueHandler {

    /**
     * Extract component parts from a compound value (e.g., Concatenate).
     * Returns null if this handler doesn't recognize the beastInterface.
     */
    List<BEASTInterface> extractParts(BEASTInterface beastInterface);

    /**
     * Extract state nodes from a compound value for inclusion in MCMC state.
     * Returns null if this handler doesn't recognize the beastInterface.
     */
    List<StateNode> extractStateNodes(BEASTInterface beastInterface);

    /**
     * Extract the arguments from an expression-like value so that they can
     * be registered with an UpDown operator. For {@code ExpCalculator} this
     * returns {@code realVectorsInput.get()}.
     * Returns null if not applicable.
     */
    List<? extends Tensor> extractArguments(BEASTInterface beastInterface);
}
