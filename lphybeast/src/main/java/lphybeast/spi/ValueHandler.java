package lphybeast.spi;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.inference.StateNode;

import java.util.List;

/**
 * Handles package-specific value types that are embedded in core classes.
 * Extensions register handlers for their specific types (e.g., feast's
 * Concatenate and ExpCalculator).
 */
public interface ValueHandler {

    /**
     * Return the beastInterface as a Function if this handler recognizes it
     * as a computable expression (e.g., ExpCalculator). Otherwise return null.
     */
    Function asFunction(BEASTInterface beastInterface);

    /**
     * Extract component parts from a compound value (e.g., Concatenate).
     * Returns null if this handler doesn't recognize the beastInterface.
     */
    List<Function> extractParts(BEASTInterface beastInterface);

    /**
     * Extract state nodes from a compound value for inclusion in MCMC state.
     * Returns null if this handler doesn't recognize the beastInterface.
     */
    List<StateNode> extractStateNodes(BEASTInterface beastInterface);

    /**
     * Extract the underlying function arguments from an expression-like value.
     * For ExpCalculator: returns functionsInput.get().
     * Returns null if not applicable.
     */
    List<Function> extractArguments(BEASTInterface beastInterface);
}
